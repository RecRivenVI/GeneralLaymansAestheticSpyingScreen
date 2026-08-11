plugins {
    id("net.neoforged.moddev") version "2.0.140"
}

apply(from = rootProject.file("gradle/stonecutter-target-conventions.gradle.kts"))

version = "${property("mod.version")}+${sc.current.project}"
group = property("mod.group") as String
base.archivesName = property("mod.id") as String

val targetJava = property("target.java").toString().toInt()
val requiredJava = JavaVersion.toVersion(targetJava)
val gameWindowWidth = 2560
val gameWindowHeight = 1440

neoForge {
    version = property("deps.neoforge.loader") as String
    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }
    runs {
        register("client") {
            gameDirectory = rootProject.file("run/${sc.current.project}/client")
            client()
            programArgument("--width")
            programArgument(gameWindowWidth.toString())
            programArgument("--height")
            programArgument(gameWindowHeight.toString())
        }
        register("server") {
            gameDirectory = rootProject.file("run/${sc.current.project}/server")
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

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:${property("deps.junit")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets.main {
    resources.exclude("fabric.mod.json", "META-INF/mods.toml")
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
                "description" to project.property("mod.description") as String,
                "authors" to project.property("mod.authors") as String,
                "license" to project.property("mod.license") as String,
                "homepage" to project.property("mod.homepage") as String,
                "sources" to project.property("mod.sources") as String,
                "issues" to project.property("mod.issues") as String,
            )
        inputs.properties(props)
        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
    }

    register<Sync>("collectArtifact") {
        group = "build"
        dependsOn("build")
        from(named<Jar>("jar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("artifacts/${sc.current.project}"))
    }
}
