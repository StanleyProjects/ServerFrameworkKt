package stan.remote.server.socket

import java.io.BufferedReader
import stan.remote.entity.ContentType
import stan.remote.entity.Request
import stan.remote.entity.request

private fun requireRequestType(type: String): Request.Type {
    require(type.isNotEmpty()) { "Request type must be exists." }
    val result = Request.Type.values().firstOrNull {
        type.equals(it.name, ignoreCase = true)
    }
    requireNotNull(result) { "Unknown request type! Type \"$type\" not supported." }
    return result
}

private fun requireContentLength(headers: List<Pair<String, String>>): Int {
    val header = ContentType.HEADER_LENGTH
    val (_, value) = headers.firstOrNull { (key, _) ->
        key.trim().equals(header, ignoreCase = true)
    } ?: error("Unknown content length! Header \"$header\" not found.")
    return value.toIntOrNull() ?: error("Unknown content length!")
}

private fun requireContentType(headers: List<Pair<String, String>>): ContentType {
    val pair = headers.firstOrNull { (key, _) ->
        key.trim().equals(ContentType.HEADER_TYPE, ignoreCase = true)
    }
    return when (val value = pair?.second) {
        null -> ContentType.None
        else -> {
            setOf(
                ContentType.Json,
                ContentType.Text
            ).firstOrNull {
                it.value.equals(value, ignoreCase = true)
            } ?: ContentType.Unknown(value = value)
        }
    }
}

internal fun getRequest(bufferedReader: BufferedReader): Request {
    val firstHeader = bufferedReader.readLine()
    requireNotNull(firstHeader) { "Empty request!" }
    require(firstHeader.isNotEmpty()) { "Empty request! First header must be exists." }
    val split = firstHeader.split(" ")
    require(split.isNotEmpty()) { "Unknown request type! First header split problem." }
    val requestType = requireRequestType(type = split[0])
    require(split.size > 1) { "Unknown request query!" }
    val query = split[1].split("?")
    val path: String
    val queryParameters: Map<String, String>
    when (query.size) {
        0 -> {
            path = ""
            queryParameters = emptyMap()
        }
        1 -> {
            path = query[0]
            queryParameters = emptyMap()
        }
        2 -> {
            path = query[0]
            val parameters = query[1].split("&").map { it.split("=") }
            require(parameters.all { it.size == 2 }) { "Unknown request query! Error split quire parameters." }
            queryParameters = parameters.map { it[0] to it[1] }.toMap()
        }
        else -> error("Unknown request query! Error split by \"?\".")
    }
    val headers = mutableMapOf<String, String>()
    while (true) {
        val header = bufferedReader.readLine()
        if (header.isNullOrEmpty()) break
        val headerSplit = header.split(": ")
        if (headerSplit.size != 2) continue
        val key = headerSplit[0]
        headers[key] = header.substring("$key: ".length)
    }
    require(headers.isNotEmpty()) { "Empty request! Headers must be exist." }
    val isWithBody = when (requestType) {
        Request.Type.GET -> false
        Request.Type.POST -> true
    }
    if (isWithBody) {
        val headersList = headers.toList()
        val contentLength = requireContentLength(headers = headersList)
        val contentType = requireContentType(headers = headersList)
        val body = ByteArray(contentLength) {
            bufferedReader.read().toByte()
        } // todo post binary!
        return when (requestType) {
            Request.Type.POST -> request(
                type = requestType,
                path = path,
                queryParameters = queryParameters,
                headers = headers,
                body = body,
                contentType = contentType
            )
            else -> error("Request type \"$requestType\" not supported!")
        }
    }
    return when (requestType) {
        Request.Type.GET -> request(
            type = requestType,
            path = path,
            queryParameters = queryParameters,
            headers = headers
        )
        else -> error("Request type \"$requestType\" not supported!")
    }
}
