package stan.remote.server

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

private val DEFAULT_ERROR_HOOK: (Throwable) -> Response = { throwable ->
    responseText(500, body = "error: " + throwable.message)
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
    errorHook: (Throwable) -> Response = DEFAULT_ERROR_HOOK,
    codeDescriptionHook: (Int) -> String = DEFAULT_CODE_DESCRIPTION_HOOK,
    mapper: (Request) -> Response
) = startServer(ServerSocket(portNumber), errorHook, codeDescriptionHook, mapper)

fun startServer(
    portNumber: Int,
    keyStoreInputStream: InputStream,
    storePassword: String,
    keyPassword: String,
    errorHook: (Throwable) -> Response = DEFAULT_ERROR_HOOK,
    codeDescriptionHook: (Int) -> String = DEFAULT_CODE_DESCRIPTION_HOOK,
    mapper: (Request) -> Response
) {
    val serverSocket = try {
        serverSocket(portNumber, keyStoreInputStream, storePassword, keyPassword)
    } catch(throwable: Throwable) {
        //todo
        throw throwable
    }
    startServer(
        serverSocket,
        errorHook,
        codeDescriptionHook,
        mapper
    )
}

private fun startServer(
    serverSocket: ServerSocket,
    errorHook: (Throwable) -> Response,
    codeDescriptionHook: (Int) -> String,
    mapper: (Request) -> Response
) {
    val portNumber = serverSocket.localPort
    if(servers[portNumber] != null) throw IllegalStateException(
        "Server on port number: $portNumber already started"
    )
    val server = Server(
        serverSocket, mapper, errorHook, codeDescriptionHook
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

private val DEFAULT_REJECTED_EXECUTION_HANDLER = ThreadPoolExecutor.AbortPolicy()
private fun executorService(
    corePoolSize: Int = 0,
    maximumPoolSize: Int = 8,
    keepAliveTime: Long = 60_000,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
    blockingQueue: BlockingQueue<Runnable> = SynchronousQueue(),
    threadFactory: ThreadFactory = Executors.defaultThreadFactory(),
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
    keyPassword: String
): ServerSocket {
    val context = getSSLContext(
        keyStoreInputStream,
        storePassword.toCharArray(),
        keyPassword.toCharArray()
    )
    val result = context.serverSocketFactory.createServerSocket(portNumber, 0)
    return result
}
private fun getSSLContext(
    keyStoreInputStream: InputStream,
    storePassword: CharArray,
    keyPassword: CharArray
): SSLContext {
    val keyStore = KeyStore.getInstance("JKS")
    val algorithm = KeyManagerFactory.getDefaultAlgorithm()
    keyStore.load(keyStoreInputStream, storePassword)
    val keyManagerFactory = KeyManagerFactory.getInstance(algorithm)
    keyManagerFactory.init(keyStore, keyPassword)
    val trustManagerFactory = TrustManagerFactory.getInstance(algorithm)
    trustManagerFactory.init(keyStore)
    val result = SSLContext.getInstance("TLS")
    result.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)
    return result
}

private class Server(
    private val serverSocket: ServerSocket,
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
                            processRequest(it, mapper, errorHook, codeDescriptionHook)
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