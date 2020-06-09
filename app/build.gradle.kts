plugins {
    application
    kotlin("jvm")
}

application {
    mainClassName = "stan.remote.AppKt"
}

dependencies {
    // implementation(project(":lib"))

    implementation(kotlin("stdlib"))

    // val libVersion = "0.00.03"
    val libVersion = "0.0.1-2-Snapshot"
    implementation(group = "stnlprjcts.remote", name = "server.framework.kt", version = libVersion)
}
