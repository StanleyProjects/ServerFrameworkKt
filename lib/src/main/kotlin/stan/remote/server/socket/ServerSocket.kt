package stan.remote.server.socket

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import stan.remote.entity.ContentType
import stan.remote.entity.Request
import stan.remote.entity.Response
import stan.remote.entity.ResponseWithBody
import stan.remote.entity.Server
import stan.remote.entity.getContent
import stan.remote.server.DEFAULT_CODE_DESCRIPTION_HOOK
import stan.remote.server.DEFAULT_ERROR_HOOK

private fun send(
    socket: Socket,
    response: Response
) {
    var data = "HTTP/1.1 " + response.code + " " + DEFAULT_CODE_DESCRIPTION_HOOK(response.code) + "\r\n"
    when (response) {
        is ResponseWithBody -> {
            val content = response.getContent()
            data += ContentType.HEADER_TYPE + ": " + content.type.value + "\r\n"
            data += ContentType.HEADER_LENGTH + ": " + content.length + "\r\n"
        }
    }
    data += "\r\n"
    val outputStream = socket.getOutputStream()
    outputStream.write(data.toByteArray())
    outputStream.flush()
    when (response) {
        is ResponseWithBody -> {
            outputStream.write(response.body)
        }
    }
    outputStream.flush()
}

private fun onSocketAccept(
    socket: Socket,
    mapper: (Request) -> Response
//    customContentTypes: Map<String, ContentType.Custom> // todo
//    codeDescriptionHook: (Int) -> String // todo
) {
    val response = try {
        mapper(getRequest(bufferedReader = BufferedReader(InputStreamReader(socket.getInputStream()))))
    } catch (e: Throwable) {
        DEFAULT_ERROR_HOOK(e)
    }
    send(socket, response)
}

internal class ServerSocketBased(
    private val portNumber: Int,
    mapper: (Request) -> Response
) : Server {
    private var mapper: ((Request) -> Response)? = mapper
    private var serverSocket: ServerSocket? = null
    private var mainExecutorService: ExecutorService? = null
    private var processExecutorService: ExecutorService? = null
    private var processCount: AtomicInteger? = null
    private val isClosedManually = AtomicBoolean(false)
    private val isStopped = AtomicBoolean(false)

    override fun start() {
        check(!isStopped.get()) { "Instance already stopped!" }
        val mainExecutorService: ExecutorService = Executors.newFixedThreadPool(2)
        this.mainExecutorService = mainExecutorService
        val serverSocket = ServerSocket(portNumber)
        this.serverSocket = serverSocket
        val processExecutorService: ExecutorService = Executors.newCachedThreadPool()
        this.processExecutorService = processExecutorService
        val processCount = AtomicInteger(0)
        this.processCount = processCount
        val mapper = requireNotNull(mapper)
        mainExecutorService.execute {
            while (true) {
                if (isStopped.get()) break
                if (isClosedManually.get()) break
                val socket = try {
                    serverSocket.accept()
                } catch (e: Throwable) {
                    if (e is SocketException && isClosedManually.get()) break
                    // todo
                    throw e
                }
                processExecutorService.execute {
                    processCount.incrementAndGet()
                    try {
                        socket.use {
                            onSocketAccept(
                                socket = it,
                                mapper = mapper
                            )
                        }
                    } catch (e: Throwable) {
                        // todo
                    } finally {
                        processCount.decrementAndGet()
                    }
                }
            }
        }
    }

    override fun stop() {
        if (isStopped.get()) return
        if (!isClosedManually.compareAndSet(false, true)) return
        val mainExecutorService = requireNotNull(mainExecutorService)
        val processCount = requireNotNull(processCount)
        val serverSocket = requireNotNull(serverSocket)
        mainExecutorService.execute {
            while (processCount.get() != 0) {
                // wait while process count != 0
            }
            try {
                serverSocket.close()
            } catch (e: Throwable) {
                // todo
            }
        }
        mainExecutorService.shutdown()
        requireNotNull(processExecutorService).shutdown()
        this.mainExecutorService = null
        this.serverSocket = null
        this.processExecutorService = null
        this.processCount = null
        this.mapper = null
        isStopped.set(true)
    }

    override fun toString(): String {
        return "Server{portNumber=$portNumber,isStopped=${isStopped.get()}}"
    }
}
