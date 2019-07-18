# ServerFrameworkKt
Framework for create `http`/`https` server with [Kotlin](https://kotlinlang.org/)

[![Download](https://api.bintray.com/packages/stnlprjcts/remote/server.framework.kt/images/download.svg)](https://bintray.com/stnlprjcts/remote/server.framework.kt/_latestVersion)

#### Run `http` server on `8888` port

```kotlin
startServer(8888) { request ->
  println(request)
  if(request.query == "/q") {
    stopServer(8888)
    responseText(200, "bye")
  } else {
  	responseText(400, "unknown command")
  }
}
```

#### Run `https` server on `8888` port

```kotlin
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
```