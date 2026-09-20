pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

include(
        ":vertexai:app",
        ":backend:rotw-job"
)
