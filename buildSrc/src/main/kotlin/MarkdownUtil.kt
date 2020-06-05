object MarkdownUtil {
    fun image(
        text: String,
        url: String
    ): String {
        return "![$text]($url)"
    }

    fun table(
        heads: List<String>,
        dividers: List<String>,
        rows: List<List<String>>
    ): String {
        require(heads.size > 1) { "Size of heads must be more than 1!" }
        require(heads.size == dividers.size) {
            "Size of heads and size of dividers must be equal!"
        }
        val firstRow = rows.firstOrNull()
        requireNotNull(firstRow) { "Rows must be exist!" }
        for (i in 1 until rows.size) {
            require(firstRow.size == rows[i].size) {
                "Size of columns in all rows must be equal!"
            }
        }
        require(heads.size == firstRow.size) {
            "Size of heads and size of rows must be equal!"
        }
        val result = mutableListOf(
            heads.joinToString(separator = "|"),
            dividers.joinToString(separator = "|")
        )
        result.addAll(rows.map { it.joinToString(separator = "|") })
        return result.joinToString(separator = SystemUtil.newLine)
    }
}
