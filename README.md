![version](https://img.shields.io/static/v1?label=version&message=0.0.1-4&labelColor=212121&color=2962ff&style=flat)

# ServerFrameworkKt
Framework for create `http`/`https` server with [Kotlin](https://kotlinlang.org/)

[![Download](https://api.bintray.com/packages/stnlprjcts/remote/server.framework.kt/images/download.svg)](https://bintray.com/stnlprjcts/remote/server.framework.kt/_latestVersion)

#### Run `http` server on `8888` port

```kotlin
startServer(8888) { request ->
  println(request)
  if(request.query == "/q") {
    stopServer(8888)
    response(code = Code.SUCCESS_OK, body = "bye")
  } else {
    response(code = Code.BAD_REQUEST, body = "unknown command")
  }
}
```

#### Run `https` server on `8888` port

soon...
