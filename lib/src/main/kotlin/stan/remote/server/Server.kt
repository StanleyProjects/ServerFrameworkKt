package stan.remote.server

import stan.remote.ContentType
import stan.remote.Request
import stan.remote.Response
import stan.remote.responseText
import java.io.InputStream
import java.net.ServerSocket
import java.net.SocketException
import java.security.KeyStore
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

private val servers = mutableMapOf<Int, Server>()

private val DEFAULT_ERROR_HOOK: (Throwable) -> Response = {
    responseText(500, "error: ${it.javaClass.name} ${it.message}")
}
private val DEFAULT_CODE_DESCRIPTION_HOOK: (Int) -> String = { code ->
    when(code) {
        200 -> "Success"
        400 -> "Bad Request"
        401 -> "Unauthorized"
        403 -> "Forbidden"
        404 -> "Not Found"
        500 -> "Internal Server Error"
        else -> "Unknown"
    }
}

fun startServer(
    portNumber: Int,
    customContentTypes: Map<String, ContentType.Custom> = emptyMap(),
    errorHook: (Throwable) -> Response = DEFAULT_ERROR_HOOK,
    codeDescriptionHook: (Int) -> String = DEFAULT_CODE_DESCRIPTION_HOOK,
    mapper: (Request) -> Response
) = startServer(
    ServerSocket(portNumber),
    customContentTypes,
    errorHook,
    codeDescriptionHook,
    mapper
)

const val KEY_STORE_TYPE_JKS = "JKS"
const val KEY_STORE_TYPE_PKCS12 = "PKCS12"
const val PROTOCOL_NAME_TLS = "TLS"
private val DEFAULT_ALGORITHM_NAME by lazy {
    KeyManagerFactory.getDefaultAlgorithm()
}

fun startServer(
    portNumber: Int,
    keyStoreInputStream: InputStream,
    storePassword: String,
    keyPassword: String = storePassword,
    keyStoreType: String = KEY_STORE_TYPE_JKS,
    algorithmName: String = DEFAULT_ALGORITHM_NAME,
    protocolName: String = PROTOCOL_NAME_TLS,
    customContentTypes: Map<String, ContentType.Custom> = emptyMap(),
    errorHook: (Throwable) -> Response = DEFAULT_ERROR_HOOK,
    codeDescriptionHook: (Int) -> String = DEFAULT_CODE_DESCRIPTION_HOOK,
    mapper: (Request) -> Response
) {
    val serverSocket = try {
        serverSocket(
            portNumber,
            keyStoreInputStream,
            storePassword,
            keyPassword,
            keyStoreType,
            algorithmName,
            protocolName
        )
    } catch(throwable: Throwable) {
        //todo
        throw throwable
    }
    startServer(
        serverSocket,
        customContentTypes,
        errorHook,
        codeDescriptionHook,
        mapper
    )
}

private fun startServer(
    serverSocket: ServerSocket,
    customContentTypes: Map<String, ContentType.Custom>,
    errorHook: (Throwable) -> Response,
    codeDescriptionHook: (Int) -> String,
    mapper: (Request) -> Response
) {
    val portNumber = serverSocket.localPort
    if(servers[portNumber] != null) throw IllegalStateException(
        "Server on port number: $portNumber already started"
    )
    val server = Server(
        serverSocket,
        customContentTypes,
        mapper,
        errorHook,
        codeDescriptionHook
    )
    server.start()
    println("Server on port number: $portNumber started")
    servers[portNumber] = server
}
fun stopServer(portNumber: Int) {
    val server = servers[portNumber] ?: return
    server.stop()
    println("Server on port number: $portNumber stopped")
    servers.remove(portNumber)
}

private const val DEFAULT_CORE_POOL_SIZE = 0
private val DEFAULT_MAXIMUM_POOL_SIZE by lazy {
    Runtime.getRuntime().availableProcessors()
}
private const val DEFAULT_KEEP_ALIVE_TIME = 60_000L
private val DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS
private val DEFAULT_BLOCKING_QUEUE = LinkedBlockingQueue<Runnable>()
private val DEFAULT_THREAD_FACTORY by lazy {
    Executors.defaultThreadFactory()
}
private val DEFAULT_REJECTED_EXECUTION_HANDLER by lazy {
    ThreadPoolExecutor.AbortPolicy()
}
private fun executorService(
    corePoolSize: Int = DEFAULT_CORE_POOL_SIZE,
    maximumPoolSize: Int = DEFAULT_MAXIMUM_POOL_SIZE,
    keepAliveTime: Long = DEFAULT_KEEP_ALIVE_TIME,
    timeUnit: TimeUnit = DEFAULT_TIME_UNIT,
    blockingQueue: BlockingQueue<Runnable> = DEFAULT_BLOCKING_QUEUE,
    threadFactory: ThreadFactory = DEFAULT_THREAD_FACTORY,
    rejectedExecutionHandler: RejectedExecutionHandler = DEFAULT_REJECTED_EXECUTION_HANDLER
): ExecutorService = ThreadPoolExecutor(
    corePoolSize,
    maximumPoolSize,
    keepAliveTime,
    timeUnit,
    blockingQueue,
    threadFactory,
    rejectedExecutionHandler
)

private fun serverSocket(
    portNumber: Int,
    keyStoreInputStream: InputStream,
    storePassword: String,
    keyPassword: String,
    keyStoreType: String,
    algorithmName: String,
    protocolName: String
): ServerSocket {
    val context = getSSLContext(
        keyStoreInputStream,
        storePassword.toCharArray(),
        keyPassword.toCharArray(),
        keyStoreType,
        algorithmName,
        protocolName
    )
    val result = context.serverSocketFactory.createServerSocket(portNumber, 0)
    return result
}

private fun getSSLContext(
    keyStoreInputStream: InputStream,
    storePassword: CharArray,
    keyPassword: CharArray,
    keyStoreType: String,
    algorithmName: String,
    protocolName: String
): SSLContext {
    val keyStore = KeyStore.getInstance(keyStoreType)
    keyStore.load(keyStoreInputStream, storePassword)
    val keyManagerFactory = KeyManagerFactory.getInstance(algorithmName)
    keyManagerFactory.init(keyStore, keyPassword)
    val trustManagerFactory = TrustManagerFactory.getInstance(algorithmName)
    trustManagerFactory.init(keyStore)
    val result = SSLContext.getInstance(protocolName)
    result.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)
    return result
}

private class Server(
    private val serverSocket: ServerSocket,
    private val customContentTypes: Map<String, ContentType.Custom>,
    private val mapper: (Request) -> Response,
    private val errorHook: (Throwable) -> Response,
    private val codeDescriptionHook: (Int) -> String
) {
    private val isClosedManually = AtomicBoolean(false)
    private val processCount = AtomicInteger(0)

    private val mainExecutorService = executorService(maximumPoolSize = 2)
    private val processExecutorService = executorService()

    fun start() {
        mainExecutorService.execute {
            while(true) {
                val socket = try {
                    serverSocket.accept()
                } catch(throwable: Throwable) {
                    if(throwable is SocketException) {
                        if(isClosedManually.get()) break
                    }
                    //todo
                    throw throwable
                }
                processExecutorService.execute {
                    processCount.incrementAndGet()
                    try {
                        socket.use {
                            processRequest(
                                it,
                                customContentTypes,
                                mapper,
                                errorHook,
                                codeDescriptionHook
                            )
                        }
                    } catch(throwable: Throwable) {
                        //todo
                    } finally {
                        processCount.decrementAndGet()
                    }
                }
            }
        }
    }
    fun stop() {
        isClosedManually.set(true)
        mainExecutorService.execute {
            while(!processCount.compareAndSet(0, 0));
            try {
                serverSocket.close()
            } catch(throwable: Throwable) {
                //todo
            }
        }
        mainExecutorService.shutdown()
        processExecutorService.shutdown()
    }
}