plugins {
    id("dev.kikugie.loom-back-compat") version "0.4.2"
}

apply(from = rootProject.file("gradle/stonecutter-target-conventions.gradle.kts"))

version = "${property("mod.version")}+${sc.current.project}"
group = property("mod.group") as String
base.archivesName = property("mod.id") as String

val targetJava = property("target.java").toString().toInt()
val requiredJava = JavaVersion.toVersion(targetJava)
val gameWindowWidth = 2560
val gameWindowHeight = 1440
val runtimeWorld = providers.gradleProperty("runtime.world").orNull
val runtimeUsername = providers.gradleProperty("runtime.username").orNull

repositories {
    mavenCentral()
    maven("https://maven.terraformersmc.com/releases") { name = "Terraformers" }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric.loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric.api")}")
    modImplementation("com.terraformersmc:modmenu:${sc.properties.get<String>("deps.modmenu")}")
    testImplementation(platform("org.junit:junit-bom:${property("deps.junit")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loom {
    fabricModJsonPath = project.file("src/main/resources/fabric.mod.json")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1")
    }

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        val runtimeSide = if (name.contains("server", ignoreCase = true)) "server" else "client"
        runDirectory = rootProject.file("run/${sc.current.project}/$runtimeSide")
        jvmArguments.add("-Dmixin.debug.export=true")
    }
}

java {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava
    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(targetJava)
    }
}

sourceSets.main {
    resources.exclude("META-INF/mods.toml", "META-INF/neoforge.mods.toml")
}

tasks {
    test {
        useJUnitPlatform()
    }

    processResources {
        val props =
            mapOf(
                "id" to project.property("mod.id") as String,
                "name" to project.property("mod.name") as String,
                "version" to project.property("mod.version") as String,
                "minecraft" to sc.properties.get<String>("target.minecraft.compatibility"),
                "loader" to sc.properties.get<String>("target.loader.compatibility"),
                "java" to targetJava,
                "description" to project.property("mod.description") as String,
                "authors" to project.property("mod.authors") as String,
                "license" to project.property("mod.license") as String,
                "homepage" to project.property("mod.homepage") as String,
                "sources" to project.property("mod.sources") as String,
                "issues" to project.property("mod.issues") as String,
            )
        inputs.properties(props)
        filesMatching("fabric.mod.json") { expand(props) }
    }

    named<JavaExec>("runClient") {
        args(
            "--graphicsBackend",
            "VULKAN",
            "--width",
            gameWindowWidth.toString(),
            "--height",
            gameWindowHeight.toString(),
        )

        if (runtimeWorld != null) {
            require(runtimeWorld.isNotBlank() && '\r' !in runtimeWorld && '\n' !in runtimeWorld) {
                "runtime.world must be a non-empty single-line world directory name"
            }
            args("--quickPlaySingleplayer", runtimeWorld)
        }
        if (runtimeUsername != null) {
            require(runtimeUsername.matches(Regex("[A-Za-z0-9_]{3,16}"))) {
                "runtime.username must be a valid Minecraft profile name"
            }
            args("--username", runtimeUsername)
        }
    }

    register<Sync>("collectArtifact") {
        group = "build"
        dependsOn("build")
        from(loomx.modJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("artifacts/${sc.current.project}"))
    }
}
