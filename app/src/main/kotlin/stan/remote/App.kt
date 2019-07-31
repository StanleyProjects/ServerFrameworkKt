package stan.remote

import stan.remote.server.startServer
import stan.remote.server.stopServer
import java.net.InetAddress

fun main() {
	val address = InetAddress.getLocalHost()
	println(
		"Hello:\n" +
		"\thost name - "+address.hostName+"\n"+
		"\thost address - "+address.hostAddress
	)
	startServer(
//		keyStoreInputStream = Any::class.java.getResourceAsStream("/keystore.jks"),
//		storePassword = "storepass",
//		keyPassword = "keypass",
		portNumber = 8888,
		mapper = { request ->
			println("request: $request")
			val response = onRequest(request)
			println("response: $response")
			response
		}
	)
}

private fun onRequest(request: Request): Response {
	when {
		request.query == "/quit" -> {
			stopServer(8888)
			return responseText(200, "bye")
		}
		request.query.startsWith("/test") -> when {
			request.query == "/test/get" -> when(request) {
				is GetRequest -> {
					return responseText(200, "success")
				}
			}
			request.query.startsWith("/test/post") -> when(request) {
				is PostRequest -> when {
					request.query == "/test/post/text" -> when {
						request.content.type === ContentType.TEXT ->
						return responseText(200, "echo: " + String(request.body))
					}
				}
			}
		}
	}
	return responseText(400, "unknown command")
}