package stan.remote.server

import stan.remote.entity.Request
import stan.remote.entity.Response
import stan.remote.entity.Server
import stan.remote.server.socket.ServerSocketBased

private val servers = mutableMapOf<Int, Server>()

fun startServer(
    portNumber: Int,
//    errorHook: (Throwable) -> Response, // todo
//    customContentTypes: Map<String, ContentType.Custom> // todo
//    codeDescriptionHook: (Int) -> String // todo
    mapper: (Request) -> Response
) {
    check(!servers.containsKey(portNumber)) {
        "Server on port number: $portNumber already started!"
    }
    val server: Server = ServerSocketBased(
        portNumber = portNumber,
        mapper = mapper
    )
    server.start()
    println("Server on port number: $portNumber started")
    servers[portNumber] = server
}

// todo ssl

fun stopServer(portNumber: Int) {
    val server = servers[portNumber] ?: return
    server.stop()
    println("Server on port number: $portNumber stopped")
    servers.remove(portNumber)
}
