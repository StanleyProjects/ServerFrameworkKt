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
		keyStoreInputStream = Any::class.java.getResourceAsStream("/keystore.jks"),
		storePassword = "storepass",
		keyPassword = "keypass",
		portNumber = 8888
	) { request ->
		println(request)
		if(request.query == "/q") {
			stopServer(8888)
			responseText(200, "bye")
		} else {
			responseText(400, "unknown command")
		}
	}
}