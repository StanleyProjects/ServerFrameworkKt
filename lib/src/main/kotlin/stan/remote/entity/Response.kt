package stan.remote.entity

import java.nio.charset.Charset

interface Response {
    val code: Int
    val headers: Map<String, String>
}

private class ResponseImpl(
    override val code: Int,
    override val headers: Map<String, String>
) : Response {
    override fun toString(): String {
        val headersString = if (headers.isEmpty()) null else "headers=$headers"
        return listOfNotNull(
            "code=$code",
            headersString
        ).joinToString(prefix = "Response{", separator = ",", postfix = "}")
    }
}

fun response(
    code: Int,
    headers: Map<String, String>
): Response {
    return ResponseImpl(
        code = code,
        headers = headers
    )
}

interface ResponseWithBody : Response {
    val body: ByteArray
    val contentType: ContentType
}

fun ResponseWithBody.getContent(): Content {
    return content(type = contentType, length = body.size)
}

private class ResponseWithBodyImpl(
    override val code: Int,
    override val headers: Map<String, String>,
    override val body: ByteArray,
    override val contentType: ContentType
) : ResponseWithBody {
    override fun toString(): String {
        val headersString = if (headers.isEmpty()) null else "headers=$headers"
        val bodyString = when (contentType) {
            ContentType.Text -> "body=\"${String(body)}\""
            else -> null
        }
        return listOfNotNull(
            "code=$code",
            headersString,
            bodyString,
            "contentType=$contentType",
            "contentLength=${body.size}"
        ).joinToString(prefix = "Response{", separator = ",", postfix = "}")
    }
}

fun response(
    code: Int,
    headers: Map<String, String>,
    body: ByteArray,
    contentType: ContentType
): Response {
    return ResponseWithBodyImpl(
        code = code,
        headers = headers,
        body = body,
        contentType = contentType
    )
}

fun response(
    code: Int,
    headers: Map<String, String> = emptyMap(),
    body: String,
    charset: Charset = Charsets.UTF_8
): Response {
    return ResponseWithBodyImpl(
        code = code,
        headers = headers,
        body = body.toByteArray(charset),
        contentType = ContentType.Text
    )
}
