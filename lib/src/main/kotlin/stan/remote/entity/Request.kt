package stan.remote.entity

interface Request {
    enum class Type {
        GET, POST
    }

    val type: Type
    val query: String
    val headers: Map<String, String>
}

private class RequestImpl(
    override val type: Request.Type,
    override val query: String,
    override val headers: Map<String, String>
) : Request {
    override fun toString(): String {
        val headersString = if (headers.isEmpty()) null else "headers=$headers"
        return listOfNotNull(
            "type=$type",
            "query=$query",
            headersString
        ).joinToString(prefix = "Request{", separator = ",", postfix = "}")
    }
}

fun request(
    type: Request.Type,
    query: String,
    headers: Map<String, String>
): Request {
    return RequestImpl(
        type = type,
        query = query,
        headers = headers
    )
}

interface RequestWithBody : Request {
    val body: ByteArray
    val contentType: ContentType
}

private class RequestWithBodyImpl(
    override val type: Request.Type,
    override val query: String,
    override val headers: Map<String, String>,
    override val body: ByteArray,
    override val contentType: ContentType
) : RequestWithBody {
    override fun toString(): String {
        val headersString = if (headers.isEmpty()) null else "headers=$headers"
        val bodyString = when (contentType) {
            ContentType.Text -> "body=\"${String(body)}\""
            else -> null
        }
        return listOfNotNull(
            "type=$type",
            "query=$query",
            headersString,
            bodyString,
            "contentType=$contentType",
            "contentLength=${body.size}"
        ).joinToString(prefix = "Request{", separator = ",", postfix = "}")
    }
}

fun request(
    type: Request.Type,
    query: String,
    headers: Map<String, String>,
    body: ByteArray,
    contentType: ContentType
): Request {
    return RequestWithBodyImpl(
        type = type,
        query = query,
        headers = headers,
        body = body,
        contentType = contentType
    )
}
