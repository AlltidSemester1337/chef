plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20"
    id("com.google.cloud.tools.jib")
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.formulae.chef.rotw.MainKt")
}

dependencies {
    implementation("com.google.firebase:firebase-admin:9.4.2")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.slf4j:slf4j-simple:2.0.16")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

val gcpProjectId: String = System.getenv("GCP_PROJECT_ID") ?: "YOUR_PROJECT_ID"

jib {
    from {
        image = "eclipse-temurin:17-jre"
    }
    to {
        image = "gcr.io/$gcpProjectId/rotw-job"
    }
    container {
        mainClass = "com.formulae.chef.rotw.MainKt"
        jvmFlags = listOf("-Xms256m", "-Xmx512m")
    }
}
