import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
    id("net.neoforged.moddev.legacyforge") version "2.0.140"
    id("forge-mutex")
}

group = property("mod.group") as String
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-forge"

val targetJava = property("target.java").toString().toInt()
val requiredJava = JavaVersion.toVersion(targetJava)
val runtime = rootProject.projectDir.resolve("run/instances/${sc.current.project}").canonicalFile

repositories {
    mavenCentral()
}

legacyForge {
    version = property("deps.forge.loader") as String
    validateAccessTransformers = true
    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }
    runs {
        register("client") {
            gameDirectory = runtime.resolve("client/primary/s2")
            programArgument("--username")
            programArgument("RecRivenVI")
            programArgument("--width")
            programArgument("2560")
            programArgument("--height")
            programArgument("1440")
            client()
        }
        register("clientMultiplayer") {
            gameDirectory = runtime.resolve("client/secondary/m1")
            programArgument("--username")
            programArgument("CNN_MOYAN")
            programArgument("--width")
            programArgument("2560")
            programArgument("--height")
            programArgument("1440")
            client()
        }
        register("server") {
            gameDirectory = runtime.resolve("server/dedicated/m2")
            programArgument("nogui")
            server()
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

dependencies {
    testImplementation(platform("org.junit:junit-bom:${property("deps.junit")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets.main {
    resources.exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")
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
                "description" to project.property("mod.description") as String,
                "authors" to project.property("mod.authors") as String,
                "license" to project.property("mod.license") as String,
                "homepage" to project.property("mod.homepage") as String,
                "sources" to project.property("mod.sources") as String,
                "issues" to project.property("mod.issues") as String,
            )
        inputs.properties(props)
        filesMatching("META-INF/mods.toml") { expand(props) }
    }
    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }
    register<Sync>("buildAndCollect") {
        group = "build"
        description = "Builds and collects the Forge production and sources JARs."
        dependsOn("reobfJar")
        from(
            layout.buildDirectory.file("libs/${base.archivesName.get()}-${project.version}.jar"),
            named<Jar>("sourcesJar").flatMap { it.archiveFile },
        )
        into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
        duplicatesStrategy = DuplicatesStrategy.FAIL
    }
}
