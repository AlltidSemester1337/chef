import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    id("org.jetbrains.kotlin.jvm") version "2.1.20" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.github.ben-manes.versions") version "0.51.0" apply true
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
    id("com.google.cloud.tools.jib") version "3.4.4" apply false
}

allprojects {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
    }
}

val ktlint by configurations.creating

dependencies {
    ktlint("com.pinterest:ktlint:0.49.1") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

// Kotlin sources of every module. Nested worktrees (.claude/worktrees, .trees) are excluded so running
// from the main checkout doesn't lint other branches' copies of the code.
val ktlintExcludes = listOf("**/build/**", "**/node_modules/**", "**/.claude/**", "**/.trees/**")
val ktlintSources = fileTree(rootDir) {
    include("**/src/**/*.kt")
    exclude(ktlintExcludes)
}

fun JavaExec.configureKtlint(reportName: String, vararg extraArgs: String) {
    val outputFile = layout.buildDirectory.file("reports/ktlint/$reportName.xml")

    // See: https://medium.com/@vanniktech/making-your-gradle-tasks-incremental-7f26e4ef09c3
    inputs.files(ktlintSources)
    outputs.file(outputFile)

    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    workingDir = rootDir

    args(
        *extraArgs,
        "--code-style=android_studio",
        "--reporter=plain",
        "--reporter=checkstyle,output=${outputFile.get().asFile}",
        "**/src/**/*.kt",
        *ktlintExcludes.map { "!$it" }.toTypedArray()
    )

    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
}

// Fails on violations without touching files — this is what CI and the pre-push checklist run.
tasks.register<JavaExec>("ktlintCheck") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style (fails on violations)"
    configureKtlint("ktlint-checkstyle-report")
}

// Rewrites files in place to fix auto-correctable violations.
tasks.register<JavaExec>("ktlintFormat") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Fix Kotlin code style violations in place"
    configureKtlint("ktlint-format-report", "--format")
}

fun notFromFirebase(candidate: ModuleComponentIdentifier): Boolean {
    return candidate.group != "com.google.firebase"
}

fun isNonStable(candidate: ModuleComponentIdentifier): Boolean {
    return listOf("alpha", "beta", "rc", "snapshot", "-m", "final").any { keyword ->
        keyword in candidate.version.lowercase()
    }
}

fun isBlockListed(candidate: ModuleComponentIdentifier): Boolean {
    return listOf(
            "com.google.guava"
    ).any { keyword ->
        keyword in candidate.toString().lowercase()
    }
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        (isNonStable(candidate) && notFromFirebase(candidate)) || isBlockListed(candidate)
    }
}

tasks {
    register("clean", Delete::class) {
        delete(layout.buildDirectory.asFile)
    }
}
