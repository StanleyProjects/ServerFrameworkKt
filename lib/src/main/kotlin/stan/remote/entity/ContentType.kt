package stan.remote.entity

sealed class ContentType(val value: String) {
    companion object {
        const val HEADER_LENGTH = "content-length"
        const val HEADER_TYPE = "content-type"
    }

    object None : ContentType(value = "none")
    object Json : ContentType(value = "application/json")
    object Text : ContentType(value = "text/plain")

    class Unknown(value: String) : ContentType(value)
    class Custom(value: String) : ContentType(value)

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is ContentType -> other === this || other.value == value
            else -> false
        }
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "{$HEADER_TYPE=$value}"
    }
}
