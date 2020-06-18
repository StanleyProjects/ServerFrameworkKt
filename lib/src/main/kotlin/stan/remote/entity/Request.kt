package stan.remote.entity

/**
 * Data used in HTTP GET requests.
 *
 * @version 1
 * @since 0.0.1-3
 */
interface Request {
    enum class Type {
        GET, POST
    }

    val type: Type

    /**
     * It shows what local resource is being requested. This part of the URL is optional.
     *
     * http://www.example.com:88 **&#47;home** ?item=book
     *
     * @version 1
     * @since 0.0.1-3
     */
    val path: String

    /**
     * It is used to send data to the server. This part of the URL is optional.
     *
     * http://www.example.com:88/home? **item=book**
     *
     * @version 1
     * @since 0.0.1-3
     */
    val queryParameters: Map<String, String>

    val headers: Map<String, String>
}

private class RequestImpl(
    override val type: Request.Type,
    override val path: String,
    override val queryParameters: Map<String, String>,
    override val headers: Map<String, String>
) : Request {
    override fun toString(): String {
        val queryParametersString = if (queryParameters.isEmpty()) null else "queryParameters=$headers"
        val headersString = if (headers.isEmpty()) null else "headers=$headers"
        return listOfNotNull(
            "type=$type",
            "path=$path",
            queryParametersString,
            headersString
        ).joinToString(prefix = "Request{", separator = ",", postfix = "}")
    }
}

fun request(
    type: Request.Type,
    path: String,
    queryParameters: Map<String, String>,
    headers: Map<String, String>
): Request {
    return RequestImpl(
        type = type,
        path = path,
        queryParameters = queryParameters,
        headers = headers
    )
}

interface RequestWithBody : Request {
    val body: ByteArray
    val contentType: ContentType
}

private class RequestWithBodyImpl(
    override val type: Request.Type,
    override val path: String,
    override val queryParameters: Map<String, String>,
    override val headers: Map<String, String>,
    override val body: ByteArray,
    override val contentType: ContentType
) : RequestWithBody {
    override fun toString(): String {
        val queryParametersString = if (queryParameters.isEmpty()) null else "queryParameters=$headers"
        val headersString = if (headers.isEmpty()) null else "headers=$headers"
        val bodyString = when (contentType) {
            ContentType.Text -> "body=\"${String(body)}\""
            else -> null
        }
        return listOfNotNull(
            "type=$type",
            "path=$path",
            queryParametersString,
            headersString,
            bodyString,
            "contentType=$contentType",
            "contentLength=${body.size}"
        ).joinToString(prefix = "Request{", separator = ",", postfix = "}")
    }
}

fun request(
    type: Request.Type,
    path: String,
    queryParameters: Map<String, String>,
    headers: Map<String, String>,
    body: ByteArray,
    contentType: ContentType
): Request {
    return RequestWithBodyImpl(
        type = type,
        path = path,
        queryParameters = queryParameters,
        headers = headers,
        body = body,
        contentType = contentType
    )
}
