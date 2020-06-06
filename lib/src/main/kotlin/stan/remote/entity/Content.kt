package stan.remote.entity

interface Content {
    val type: ContentType
    val length: Int
}

private data class ContentImpl(
    override val type: ContentType,
    override val length: Int
) : Content

fun content(
    type: ContentType,
    length: Int
): Content {
    return ContentImpl(
        type = type,
        length = length
    )
}
