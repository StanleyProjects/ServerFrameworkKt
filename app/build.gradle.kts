plugins {
    application
    kotlin("jvm")
}

application {
    mainClassName = "stan.remote.AppKt"
}

dependencies {
    implementation(project(":lib"))

    implementation(kotlin("stdlib"))

//    implementation(group = "stnlprjcts.remote", name = "server.framework.kt", version = "0.00.03")
}
