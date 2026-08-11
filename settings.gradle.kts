pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

includeBuild("gradle/spotless") {
    name = "glass-spotless"
}

plugins {
    id("dev.kikugie.stonecutter") version "0.10-alpha.5"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_25)) {
    "The Stonecutter 0.10 build must run on JDK 25 or newer; current runtime is ${JavaVersion.current()}."
}

stonecutter {
    val targetStatuses = mutableMapOf<String, String>()

    fun targetStatus(
        loader: String,
        minecraftVersion: String,
    ): String =
        properties.getOrNull<String>("$loader.$minecraftVersion.target.status")
            ?: properties.get<String>("target.status")

    create(rootProject) {
        fun target(
            loader: String,
            minecraftVersion: String,
        ) {
            val targetId = "$loader-$minecraftVersion"
            val status = targetStatus(loader, minecraftVersion)
            targetStatuses[targetId] = status
            val buildscript =
                when (status) {
                    "implemented" -> {
                        "build.$loader.gradle.kts"
                    }

                    "planned" -> {
                        "build.planned.gradle.kts"
                    }

                    else -> {
                        error(
                            "Target '$loader-$minecraftVersion' has unsupported status '$status'.",
                        )
                    }
                }
            version(targetId, minecraftVersion).buildscript(buildscript)
        }

        target("forge", "1.20.1")
        target("neoforge", "1.21.1")
        target("neoforge", "26.1.2")
        target("fabric", "26.2")
        target("neoforge", "26.2")
        vcsVersion = "fabric-26.2"
    }

    val plannedMaintenanceTasks =
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
            "verifyClientTarget",
            "verifyServerTarget",
            "verifyTarget",
            "verifyTargetContract",
        )
    gradle.startParameter.taskNames.forEach { requestedTask ->
        val path = requestedTask.split(':').filter(String::isNotBlank)
        if (path.size != 2) return@forEach
        val targetId = path[0]
        val taskName = path[1]
        if (targetStatuses[targetId] != "planned" || taskName in plannedMaintenanceTasks) {
            return@forEach
        }

        val loader = targetId.substringBefore('-')
        val minecraftVersion = targetId.substringAfter('-')
        error(
            "Target '$targetId' has status 'planned'. " +
                "Direct task '$requestedTask' is blocked during settings evaluation, before any " +
                "source generation, compilation, loader dependency resolution, packaging, or " +
                "runtime launch. Set [$loader.\"$minecraftVersion\"].target.status = \"implemented\" " +
                "in stonecutter.properties.toml only after the target implementation and validation " +
                "are complete.",
        )
    }
}

rootProject.name = "GeneralLaymansAestheticSpyingScreen"
