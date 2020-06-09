buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath(kotlin("gradle-plugin", version = Version.kotlin))
    }
}

val kotlinLint: Configuration by configurations.creating

dependencies {
    kotlinLint(Dependency.kotlinLint.notation())
}

val reportsPath = "${rootProject.buildDir}/reports"
val analysisPath = "$reportsPath/analysis"
val analysisStylePath = "$analysisPath/style"
val analysisStyleHtmlPath = "$analysisStylePath/html/report.html"

task<JavaExec>("verifyCodeStyle") {
    classpath = kotlinLint
    main = "com.pinterest.ktlint.Main"
    args(
        "build.gradle.kts",
        "settings.gradle.kts",
        "buildSrc/src/main/kotlin/**/*.kt",
        "buildSrc/build.gradle.kts",
        "lib/src/main/kotlin/**/*.kt",
        "lib/build.gradle.kts",
        "app/src/main/kotlin/**/*.kt",
        "app/build.gradle.kts",
        "--reporter=html,output=$analysisStyleHtmlPath"
    )
}

task("verifyReadme") {
    doLast {
        val file = File(rootDir, "README.md")
        val text = file.requireFilledText()
        val lines = text.split(SystemUtil.newLine)
        val versionBadge = MarkdownUtil.image(
            text = "version",
            url = badgeUrl(
                label = "version",
                message = Version.Application.name + "-" + Version.Application.code,
                color = "2962ff"
            )
        )
        val bintrayProject = "stnlprjcts/remote/" + Common.applicationId
        val downloadLatestBadge = "[" +
            "![Download](https://api.bintray.com/packages/$bintrayProject/images/download.svg)" +
        "](https://bintray.com/$bintrayProject/_latestVersion)"
        listOf(
            versionBadge,
            downloadLatestBadge
        ).forEach {
            check(lines.contains(it)) { "File by path ${file.absolutePath} must contains \"$it\" line!" }
        }
    }
}

task("verifyLicense") {
    doLast {
        val file = File(rootDir, "LICENSE")
        val text = file.requireFilledText()
//        val lines = text.split(SystemUtil.newLine)
    }
}

task("verifyAll") {
    dependsOn(setOf(
        "CodeStyle",
        "Readme",
        "License"
    ).map { "verify$it" })
}

task<Delete>("clean") {
    delete = setOf(rootProject.buildDir)
}

task<Delete>("cleanAll") {
    dependsOn("clean")
    delete = setOf(File(rootDir, "buildSrc/build"))
}

allprojects {
    repositories {
        jcenter()
    }
}
