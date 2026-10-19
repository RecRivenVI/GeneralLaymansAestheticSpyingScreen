pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

includeBuild("gradle/spotless") {
    name = "glass-spotless"
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.7"
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_25)) {
    "The G.L.A.S.S. Stonecutter build must run on JDK 25 or newer; current runtime is ${JavaVersion.current()}."
}

include(":version")
// Stonecutter creates <controller>/versions/<target>; use the repository root
// during creation, then map the namespace descriptor to its physical directory.
project(":version").projectDir = rootDir

val targets = listOf("1.20.1-forge", "1.21.1-neoforge", "26.1.2-neoforge", "26.2-fabric", "26.2-neoforge")
stonecutter {
    create("version") {
        targets.forEach { target ->
            version(target, target.substringBeforeLast('-')).buildscript("versions/$target/build.gradle.kts")
        }
        vcsVersion = "26.2-fabric"
    }
}
project(":version").projectDir = file("versions")
project(":version").buildFileName = "../stonecutter.gradle.kts"
targets.forEach { target ->
    project(":version:$target").projectDir = file("versions/$target")
    project(":version:$target").buildFileName = "build.gradle.kts"
}

val maintenance =
    setOf(
        "artifactTransforms",
        "buildEnvironment",
        "clean",
        "dependencies",
        "dependencyInsight",
        "help",
        "javaToolchains",
        "kotlinDslAccessorsReport",
        "outgoingVariants",
        "projects",
        "properties",
        "resolvableConfigurations",
        "tasks",
    )
val unscopedBusiness =
    setOf(
        "build",
        "check",
        "test",
        "assemble",
        "jar",
        "sourcesJar",
        "buildAndCollect",
        "runClient",
        "runClientMultiplayer",
        "runServer",
    )
gradle.startParameter.taskNames.forEach { request ->
    val path = request.split(':').filter(String::isNotBlank)
    if ((path.size == 1 || (path.size == 2 && path[0] == "version")) &&
        path.last() in unscopedBusiness
    ) {
        error(
            "Unscoped business task '$request' could select planned targets. Use an explicit :version:26.2-fabric task or root aggregate.",
        )
    }
    val target = if (path.size == 3 && path[0] == "version") path[1] else null
    if (target in targets && target != "26.2-fabric" && path.last() !in maintenance) {
        error(
            "Target '$target' has status 'planned'. Direct task '$request' is blocked before Loader resolution, compilation, packaging or runtime launch.",
        )
    }
}
rootProject.name = "GLASS"
