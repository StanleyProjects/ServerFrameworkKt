package stan.remote

import java.lang.Exception

sealed class Response(
    val code: Int,
    val headers: Map<String, String>
)
class ResponseJust(
    code: Int,
    headers: Map<String, String> = emptyMap()
): Response(code, headers)
class ResponseWithBody(
    code: Int,
    headers: Map<String, String> = emptyMap(),
    val body: ByteArray,
    contentType: Content.Type
): Response(code, headers) {
    val content = Content(contentType, body.size)
}
fun responseText(
    code: Int,
    body: String,
    headers: Map<String, String> = emptyMap()
) = ResponseWithBody(
    code,
    headers,
    body.toByteArray(),
    contentType = Content.Type.TEXT
)

sealed class Request(
    val type: Type,
    val query: String,
    val headers: Map<String, String>
) {
    enum class Type {
        GET, POST
    }
}
class GetRequest(
    query: String,
    headers: Map<String, String>
): Request(Type.GET, query, headers) {
    override fun toString(): String {
        return "{" +
            "query=$query,"+
            "headers=$headers,"+
            "type=GET"+
            "}"
    }
}
class PostRequest(
    query: String,
    headers: Map<String, String>,
    val body: ByteArray,
    contentType: Content.Type
): Request(Type.POST, query, headers) {
    val content = Content(contentType, body.size)

    override fun toString(): String {
        return "{" +
            "query=$query,"+
            "headers=$headers,"+
            "type=POST,"+
            "content=$content"+
            "}"
    }
}

const val CONTENT_LENGTH_HEADER_NAME = "content-length"
const val CONTENT_TYPE_HEADER_NAME = "content-type"

data class Content(
    val type: Type,
    val length: Int
) {
    enum class Type(val contentTypeValue: String) {
        JSON("application/json"),
        TEXT("text/plain")
    }
}

class EmptyRequestException: Exception()
class UnknownRequestTypeException: Exception()
class UnknownRequestQueryException: Exception()
class UnknownContentLengthException: Exception()
class UnknownContentTypeException: Exception()
class BadRequestTypeException: Exception()