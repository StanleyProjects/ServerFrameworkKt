import java.io.File
import java.nio.charset.Charset

fun checkFileExists(file: File) {
    if (!file.exists()) error("File by path \"${file.absolutePath}\" must be exists!")
}

fun File.requireText(charset: Charset = Charsets.UTF_8): String {
    checkFileExists(this)
    return readText(charset)
}

fun File.requireFilledText(charset: Charset = Charsets.UTF_8): String {
    val text = requireText(charset)
    if (text.isEmpty()) error("File by path $absolutePath must be not empty!")
    return text
}
