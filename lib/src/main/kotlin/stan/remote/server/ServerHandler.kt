package stan.remote.server

import stan.remote.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket

fun processRequest(
    socket: Socket,
    mapper: (Request) -> Response,
    errorHook: (Throwable) -> Response,
    codeDescriptionHook: (Int) -> String
) {
    val response = try {
        mapper(catchRequest(socket))
    } catch(throwable: Throwable) {
        errorHook(throwable)
    }
    send(socket, response, codeDescriptionHook)
}

private fun catchRequest(socket: Socket): Request {
    val inputStream = socket.getInputStream()
    val bufferedReader = BufferedReader(InputStreamReader(inputStream))
    val firstHeader = bufferedReader.readLine()
    if(firstHeader.isNullOrEmpty()) throw EmptyRequestException()
    val split = firstHeader.split(" ")
    if(split.isEmpty()) throw UnknownRequestTypeException()
    val requestTypeString = split[0]
    val requestType = Request.Type.values().find {
        it.name.toLowerCase() == requestTypeString.toLowerCase()
    } ?: throw UnknownRequestTypeException()
    if(split.size == 1) throw UnknownRequestQueryException()
    val query = split[1]
    if(query.isEmpty()) throw UnknownRequestQueryException()
    val headers = mutableMapOf<String, String>()
    while(true) {
        val header = bufferedReader.readLine()
        if(header.isNullOrEmpty()) break
        when(header.toLowerCase().trim()) {
            CONTENT_LENGTH_HEADER_NAME -> {}
            CONTENT_TYPE_HEADER_NAME -> {}
        }
        val headerSplit = header.split(": ")
        if(headerSplit.size != 2) continue
        val key = headerSplit[0]
        headers[key] = header.substring("$key: ".length)
    }
    if(headers.isEmpty()) throw EmptyRequestException()
    val isWithBody = when(requestType) {
        Request.Type.GET -> false
        Request.Type.POST -> true
    }
    if(isWithBody) {
        val headersList = headers.toList()
        val contentLengthHeader = headersList.find { (key, _) ->
            key.toLowerCase().trim() == CONTENT_LENGTH_HEADER_NAME
        } ?: throw UnknownContentLengthException()
        val contentLengthValue = contentLengthHeader.second
        val contentLength = contentLengthValue.toIntOrNull() ?: throw UnknownContentLengthException()
        val contentTypeHeader = headersList.find { (key, _) ->
            key.toLowerCase().trim() == CONTENT_TYPE_HEADER_NAME
        } ?: throw UnknownContentTypeException()
        val contentTypeValue = contentTypeHeader.second
        val contentType = Content.Type.values().find {
            it.contentTypeValue.toLowerCase() == contentTypeValue.toLowerCase()
        } ?: throw UnknownContentTypeException()
        val body = ByteArray(contentLength).also { inputStream.read(it) }
        return when(requestType) {
            Request.Type.POST -> PostRequest(
                query = query,
                headers = headers,
                body = body,
                contentType = contentType
            )
            else -> throw BadRequestTypeException()
        }
    }
    return when(requestType) {
        Request.Type.GET -> GetRequest(query, headers)
        else -> throw BadRequestTypeException()
    }
}

private fun send(
    socket: Socket,
    response: Response,
    codeDescriptionHook: (Int) -> String
) {
    var data = "HTTP/1.1 "+response.code+" " + codeDescriptionHook(response.code) + "\r\n"
    when(response) {
        is ResponseWithBody -> {
            data += CONTENT_TYPE_HEADER_NAME + ": " + response.content.type.contentTypeValue + "\r\n"
            data += CONTENT_LENGTH_HEADER_NAME + ": " + response.content.length + "\r\n"
        }
    }
    data += "\r\n"
    val outputStream = socket.getOutputStream()
    outputStream.write(data.toByteArray())
    outputStream.flush()
    when(response) {
        is ResponseWithBody -> {
            outputStream.write(response.body)
        }
    }
    outputStream.flush()
}