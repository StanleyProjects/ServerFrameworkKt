object MavenUtil {
    private const val MAVEN_APACHE_URL = "http://maven.apache.org"
    private fun mavenApacheUrlPom(modelVersion: String): String {
        return "$MAVEN_APACHE_URL/POM/$modelVersion"
    }

    fun pom(
        modelVersion: String,
        groupId: String,
        artifactId: String,
        version: String,
        packaging: String
    ): String {
        val mavenApacheUrlPom = mavenApacheUrlPom(modelVersion = modelVersion)
        val project = setOf(
            "xsi:schemaLocation" to "$mavenApacheUrlPom $MAVEN_APACHE_URL/xsd/maven-$modelVersion.xsd",
            "xmlns" to mavenApacheUrlPom,
            "xmlns:xsi" to "http://www.w3.org/2001/XMLSchema-instance"
        ).joinToString(separator = " ") { (key, value) ->
            "$key=\"$value\""
        }
        return setOf(
            "modelVersion" to modelVersion,
            "groupId" to groupId,
            "artifactId" to artifactId,
            "version" to version,
            "packaging" to packaging
        ).joinToString(
            prefix = "<project $project>",
            separator = "",
            postfix = "</project>"
        ) { (key, value) ->
            "<$key>$value</$key>"
        }
    }
}
