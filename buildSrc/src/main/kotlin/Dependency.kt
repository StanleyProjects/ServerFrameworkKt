private object Group {
    const val pinterest = "com.pinterest"
}

data class Dependency(
    val group: String,
    val name: String,
    val version: String
) {
    companion object {
        val kotlinLint = Dependency(
            group = Group.pinterest,
            name = "ktlint",
            version = Version.kotlinLint
        )
    }
}

data class Plugin(
    val name: String,
    val version: String
)
