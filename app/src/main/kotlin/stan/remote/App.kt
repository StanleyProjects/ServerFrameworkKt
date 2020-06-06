package stan.remote

import java.net.InetAddress
import stan.remote.entity.Code
import stan.remote.entity.ContentType
import stan.remote.entity.Request
import stan.remote.entity.RequestWithBody
import stan.remote.entity.Response
import stan.remote.entity.response
import stan.remote.server.startServer
import stan.remote.server.stopServer

fun main() {
    val address = InetAddress.getLocalHost()
    val portNumber = 8888
    println("""
    Hello:
        host name - ${address.hostName}
        host address - ${address.hostAddress}
        port number - $portNumber
""".trimIndent())
    startServer(
        portNumber = portNumber,
        mapper = { request ->
            println("request: $request")
            val response = onRequest(request, portNumber = portNumber)
            println("response: $response")
            response
        }
    )
    // todo ssl
}

private fun onRequest(request: Request, portNumber: Int): Response {
    when {
        request.query == "/quit" -> {
            stopServer(portNumber)
            return response(code = Code.SUCCESS_OK, body = "bye")
        }
        request.query.startsWith("/test") -> when {
            request.query == "/test/get" -> when (request.type) {
                Request.Type.GET -> {
                    return response(code = Code.SUCCESS_OK, body = "success")
                }
            }
            request.query.startsWith("/test/post") -> when {
                request.query == "/test/post/text" &&
                request.type == Request.Type.POST &&
                request is RequestWithBody &&
                request.contentType == ContentType.Text -> {
                    return response(code = Code.SUCCESS_OK, body = "echo: " + String(request.body))
                }
            }
        }
    }
    return response(code = Code.BAD_REQUEST, body = "unknown command")
}
