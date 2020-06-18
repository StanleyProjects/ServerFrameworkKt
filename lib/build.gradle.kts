plugins {
    kotlin("jvm")
    apply(Plugin.dokka, withVersion = true)
}

version = Version.Application.name + "-" + Version.Application.code

dependencies {
    implementation(kotlin("stdlib"))
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks

setOf(
    "Release",
    "ReleaseCandidate",
    "Snapshot"
).forEach {
    val buildName = Common.applicationId
    val versionName = when (it) {
        "Release" -> version.toString()
        else -> "$version-$it"
    }
    task<Jar>("assemble$it") {
        dependsOn(compileKotlin)
        archiveBaseName.set(buildName)
        archiveVersion.set(versionName)
        from(compileKotlin.destinationDir)
    }
    task<Jar>("assemble${it}Source") {
        archiveBaseName.set(buildName)
        archiveVersion.set(versionName)
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }
    task("assemble${it}Pom") {
        doLast {
            val parent = File(buildDir, "libs")
            if (!parent.exists()) parent.mkdirs()
            val file = File(parent, "$buildName-$versionName.pom")
            if (file.exists()) file.delete()
            file.createNewFile()
            check(file.exists()) { "File by path: ${file.absolutePath} must be exists!" }
            val text = MavenUtil.pom(
                modelVersion = "4.0.0",
                groupId = "stnlprjcts.remote", // todo
                artifactId = buildName,
                version = versionName,
                packaging = "jar"
            )
            file.writeText(text)
        }
    }
}

val reportsPath = "${buildDir.absolutePath}/reports"
val documentationPath = "$reportsPath/documentation"
val documentationHtmlPath = "$documentationPath/html"

task<org.jetbrains.dokka.gradle.DokkaTask>("collectDocumentation") {
    // todo logging: loading modules... 0.11.0?
    outputFormat = "html"
    outputDirectory = documentationHtmlPath
    configuration {
        moduleName = "ServerFrameworkKt"
    }
}
