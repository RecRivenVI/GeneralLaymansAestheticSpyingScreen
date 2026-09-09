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

stonecutter {
    val statuses = mutableMapOf<String, String>()

    fun targetStatus(
        loader: String,
        version: String,
    ): String =
        properties.getOrNull<String>("$loader.$version.target.status")
            ?: properties.get<String>("target.status")

    create(rootProject) {
        fun match(
            project: String,
            vararg loaders: String,
            version: String = project,
        ) {
            for (loader in loaders) {
                val target = "$project-$loader"
                statuses[target] = targetStatus(loader, version)
                version(target, version).buildscript("build.$loader.gradle.kts")
            }
        }

        match("1.20.1", "forge")
        match("1.21.1", "neoforge")
        match("26.1.2", "neoforge")
        match("26.2", "fabric", "neoforge")
        vcsVersion = "26.2-fabric"
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
    gradle.startParameter.taskNames.forEach { request ->
        val path = request.split(':').filter(String::isNotBlank)
        if (path.size != 2) return@forEach
        val target = path[0]
        val task = path[1]
        if (statuses[target] != "planned" || task in maintenance) return@forEach

        val version = target.substringBeforeLast('-')
        val loader = target.substringAfterLast('-')
        error(
            "Target '$target' has status 'planned'. Direct task '$request' is blocked during " +
                "settings evaluation before source generation, compilation, Loader dependency " +
                "resolution, packaging, or runtime launch. Set [$loader.\"$version\"]" +
                ".target.status = \"implemented\" only after implementation and qualification.",
        )
    }
}

rootProject.name = "GLASS"
