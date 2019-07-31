package stan.remote

import java.lang.Exception

sealed class Response(
    val code: Int,
    val headers: Map<String, String>
)
class ResponseJust(
    code: Int,
    headers: Map<String, String> = emptyMap()
): Response(code, headers) {
    override fun toString(): String {
        return "{" +
            "code=$code," +
            "headers=$headers" +
            "}"
    }
}
class ResponseWithBody(
    code: Int,
    headers: Map<String, String> = emptyMap(),
    val body: ByteArray,
    contentType: ContentType
): Response(code, headers) {
    val content = Content(contentType, body.size)

    override fun toString(): String {
        return "{" +
            "code=$code," +
            "headers=$headers" +
            "content=$content" +
            if(content.type === ContentType.TEXT) {
                "body=\"" + String(body) +"\""
            } else "" +
            "}"
    }
}
fun responseText(
    code: Int,
    body: String,
    headers: Map<String, String> = emptyMap()
) = ResponseWithBody(
    code,
    headers,
    body.toByteArray(),
    contentType = ContentType.TEXT
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
    contentType: ContentType
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

sealed class ContentType {
    object NONE: ContentType() {
        override fun toString(): String {
            return "{$CONTENT_TYPE_HEADER_NAME=none}"
        }
    }
    data class Unknown(val contentTypeValue: String): ContentType() {
        override fun toString(): String {
            return "{$CONTENT_TYPE_HEADER_NAME=$contentTypeValue}"
        }
    }
    data class Custom(val contentTypeValue: String): ContentType() {
        override fun toString(): String {
            return "{$CONTENT_TYPE_HEADER_NAME=$contentTypeValue}"
        }
    }
    object JSON: ContentType() {
        val contentTypeValue = "application/json"

        override fun toString(): String {
            return "{$CONTENT_TYPE_HEADER_NAME=$contentTypeValue}"
        }
    }
    object TEXT: ContentType() {
        val contentTypeValue = "text/plain"

        override fun toString(): String {
            return "{$CONTENT_TYPE_HEADER_NAME=$contentTypeValue}"
        }
    }
}
data class Content(
    val type: ContentType,
    val length: Int
)

class EmptyRequestException: Exception()
class UnknownRequestTypeException: Exception()
class UnknownRequestQueryException: Exception()
class UnknownContentLengthException: Exception()
class UnknownContentTypeException: Exception()
class BadRequestTypeException: Exception()