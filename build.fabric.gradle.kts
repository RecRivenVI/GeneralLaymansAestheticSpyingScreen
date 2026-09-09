import groovy.json.JsonSlurper
import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
    id("dev.kikugie.loom-back-compat")
}

data class RepositoryProfile(
    val gameDirectory: File,
    val modsDirectory: File,
    val playerName: String?,
    val requiredJvmArgs: List<String>,
)

fun resolveRepositoryPath(relativePath: String): File {
    require(relativePath.isNotBlank() && !File(relativePath).isAbsolute) {
        "Runtime paths must be non-empty and repository-relative: $relativePath"
    }
    val root = rootProject.projectDir.canonicalFile
    val resolved = root.resolve(relativePath).canonicalFile
    require(resolved.toPath().startsWith(root.toPath())) {
        "Runtime path escapes the repository: $relativePath"
    }
    return resolved
}

fun loadRepositoryProfile(
    role: String,
    profile: String,
): RepositoryProfile {
    val profileOwner =
        if (role == "SERVER") {
            "server/dedicated"
        } else {
            "client/${role.lowercase()}"
        }
    val record =
        rootProject.projectDir.resolve(
            "run/profiles/${sc.current.project}/$profileOwner/${profile.lowercase()}.json",
        )
    require(record.isFile) { "Missing repository-local profile record: $record" }
    @Suppress("UNCHECKED_CAST")
    val values = JsonSlurper().parse(record) as Map<String, Any?>
    require(values["node"] == sc.current.project) { "Runtime record has the wrong node: $record" }
    require(values["role"] == role && values["profile"] == profile) {
        "Runtime record role/profile mismatch: $record"
    }
    if (role != "SERVER") {
        require(values["resolution"] == "2560x1440") { "GLASS clients must use 2560x1440." }
    }

    val gameDirectory = resolveRepositoryPath(values.getValue("gameDirectory").toString())
    val modsDirectory = resolveRepositoryPath(values.getValue("modsDirectory").toString())
    require(modsDirectory == gameDirectory.resolve("mods")) {
        "modsDirectory must equal gameDirectory/mods: $record"
    }
    val requiredJvmArgs =
        (values["requiredJvmArgs"] as? List<*>)?.map {
            requireNotNull(it).toString()
        } ?: emptyList()
    return RepositoryProfile(
        gameDirectory,
        modsDirectory,
        values["playerName"]?.toString(),
        requiredJvmArgs,
    )
}

group = property("mod.group") as String
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-fabric"

val targetJava = property("target.java").toString().toInt()
val requiredJava = JavaVersion.toVersion(targetJava)
val requestedProfile =
    providers
        .gradleProperty("runtime.profile")
        .orElse("S2")
        .get()
        .uppercase()
require(requestedProfile in setOf("S1", "S2", "M1", "M2")) {
    "runtime.profile must be S1, S2, M1, or M2"
}
val secondaryProfile = if (requestedProfile == "M2") "M2" else "M1"
val clientProfile = loadRepositoryProfile("PRIMARY", requestedProfile)
val multiplayerProfile = loadRepositoryProfile("SECONDARY", secondaryProfile)
val serverProfile = loadRepositoryProfile("SERVER", "M2")
val runtimeWorld = providers.gradleProperty("runtime.world").orNull

repositories {
    mavenCentral()
    maven("https://maven.terraformersmc.com/releases") { name = "Terraformers" }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric.loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric.api")}")
    modCompileOnly("com.terraformersmc:modmenu:${sc.properties.get<String>("deps.modmenu")}")
    testImplementation(platform("org.junit:junit-bom:${property("deps.junit")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loom {
    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1")
    }
    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        jvmArguments.add("-Dmixin.debug.export=true")
    }
    runConfigs {
        named("client") {
            runDirectory = clientProfile.gameDirectory
            programArgs("--username", requireNotNull(clientProfile.playerName))
            clientProfile.requiredJvmArgs.forEach(::vmArg)
            if (clientProfile.modsDirectory.listFiles()?.isNotEmpty() == true) {
                vmArg("-Dfabric.addMods=${clientProfile.modsDirectory}")
            }
        }
        create("clientMultiplayer") {
            client()
            preferGradleTask = true
            generateRunConfig = true
            runDirectory = multiplayerProfile.gameDirectory
            programArgs("--username", requireNotNull(multiplayerProfile.playerName))
            multiplayerProfile.requiredJvmArgs.forEach(::vmArg)
            if (multiplayerProfile.modsDirectory.listFiles()?.isNotEmpty() == true) {
                vmArg("-Dfabric.addMods=${multiplayerProfile.modsDirectory}")
            }
        }
        named("server") {
            runDirectory = serverProfile.gameDirectory
            serverProfile.requiredJvmArgs.forEach(::vmArg)
        }
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
    withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
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
        args("--graphicsBackend", "VULKAN", "--width", "2560", "--height", "1440")
        if (runtimeWorld != null) {
            require(runtimeWorld.isNotBlank() && '\r' !in runtimeWorld && '\n' !in runtimeWorld) {
                "runtime.world must be a non-empty single-line world directory name"
            }
            args("--quickPlaySingleplayer", runtimeWorld)
        }
    }
    named<JavaExec>("runClientMultiplayer") {
        args("--graphicsBackend", "VULKAN", "--width", "2560", "--height", "1440")
    }
    register<Sync>("buildAndCollect") {
        group = "build"
        description = "Builds and collects the Fabric production and sources JARs."
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
        duplicatesStrategy = DuplicatesStrategy.FAIL
    }
}
