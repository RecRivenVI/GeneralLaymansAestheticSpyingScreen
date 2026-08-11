import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.AbstractArchiveTask

abstract class VerifyTargetContractTask : DefaultTask() {
    @get:Input
    abstract val targetId: Property<String>

    @get:Input
    abstract val minecraftVersion: Property<String>

    @get:Input
    abstract val status: Property<String>

    @get:Input
    abstract val expectedStatus: Property<String>

    @get:Input
    abstract val client: Property<Boolean>

    @get:Input
    abstract val server: Property<Boolean>

    @get:Input
    abstract val targetJava: Property<Int>

    @get:Input
    abstract val minecraftCompatibility: Property<String>

    @get:Input
    abstract val loaderCompatibility: Property<String>

    @get:Input
    abstract val loaderBuildChainApplied: Property<Boolean>

    @TaskAction
    fun verify() {
        val id = targetId.get()
        val match =
            Regex("^(fabric|forge|neoforge)-([0-9]+(?:\\.[0-9]+){1,2})$").matchEntire(id)
                ?: throw GradleException(
                    "Target '$id' must match <loader>-<minecraft-version>; " +
                        "loader must be fabric, forge, or neoforge.",
                )
        check(match.groupValues[2] == minecraftVersion.get()) {
            "Target '$id' declares Minecraft ${minecraftVersion.get()}."
        }
        check(status.get() in setOf("planned", "implemented")) {
            "Target '$id' has unsupported status '${status.get()}'."
        }
        check(status.get() == expectedStatus.get()) {
            "Target '$id' must have status '${expectedStatus.get()}', found '${status.get()}'."
        }
        check(client.get() || server.get()) {
            "Target '$id' must support at least one runtime side."
        }
        check(targetJava.get() in setOf(17, 21, 25)) {
            "Target '$id' has unsupported Java level ${targetJava.get()}."
        }
        check(minecraftCompatibility.get().isNotBlank()) {
            "Target '$id' must declare target.minecraft.compatibility."
        }
        check(loaderCompatibility.get().isNotBlank()) {
            "Target '$id' must declare target.loader.compatibility."
        }
        check(loaderBuildChainApplied.get() == (status.get() == "implemented")) {
            if (status.get() == "planned") {
                "Target '$id' is planned but applies a Minecraft loader build chain."
            } else {
                "Target '$id' is implemented but does not apply its Minecraft loader build chain."
            }
        }
    }
}

abstract class VerifyImplementedTargetTask : DefaultTask() {
    @get:Input
    abstract val targetId: Property<String>

    @get:Input
    abstract val status: Property<String>

    @TaskAction
    fun verify() {
        check(status.get() == "implemented") {
            "Target '${targetId.get()}' is '${status.get()}'; " +
                "only implemented targets may execute lifecycle or run tasks."
        }
    }
}

abstract class VerifyTargetRuntimeTask : DefaultTask() {
    @get:Input
    abstract val targetId: Property<String>

    @get:Input
    abstract val runtimeSide: Property<String>

    @get:Input
    abstract val supported: Property<Boolean>

    @TaskAction
    fun verify() {
        check(supported.get()) {
            "Target '${targetId.get()}' does not support a ${runtimeSide.get()} runtime."
        }
    }
}

val sc = extensions.getByType<StonecutterBuildExtension>()
val targetId = sc.current.project
val targetMinecraftVersion = sc.current.version
val targetStatus = property("target.status").toString()
val targetClient = property("target.client").toString().toBooleanStrict()
val targetServer = property("target.server").toString().toBooleanStrict()
val expectedTargetStatus = if (targetId == "fabric-26.2") "implemented" else "planned"
val loaderPluginIds =
    setOf(
        "dev.kikugie.loom-back-compat",
        "net.minecraftforge.gradle",
        "net.neoforged.moddev",
    )
val loaderBuildChainConfigured = loaderPluginIds.any(pluginManager::hasPlugin)

val verifyTargetContract =
    tasks.register<VerifyTargetContractTask>("verifyTargetContract") {
        group = "verification"
        description = "Verifies the Stonecutter target naming and capability contract."
        this.targetId.set(sc.current.project)
        minecraftVersion.set(targetMinecraftVersion)
        status.set(targetStatus)
        expectedStatus.set(expectedTargetStatus)
        client.set(targetClient)
        server.set(targetServer)
        targetJava.set(project.property("target.java").toString().toInt())
        minecraftCompatibility.set(project.property("target.minecraft.compatibility").toString())
        loaderCompatibility.set(project.property("target.loader.compatibility").toString())
        loaderBuildChainApplied.set(loaderBuildChainConfigured)
    }

val verifyTarget =
    tasks.register<VerifyImplementedTargetTask>("verifyTarget") {
        group = "verification"
        description = "Fails unless the Stonecutter target is implemented."
        dependsOn(verifyTargetContract)
        this.targetId.set(sc.current.project)
        status.set(targetStatus)
    }

val verifyClientTarget =
    tasks.register<VerifyTargetRuntimeTask>("verifyClientTarget") {
        group = "verification"
        description = "Fails unless the Stonecutter target supports a client runtime."
        this.targetId.set(sc.current.project)
        runtimeSide.set("client")
        supported.set(targetClient)
    }

val verifyServerTarget =
    tasks.register<VerifyTargetRuntimeTask>("verifyServerTarget") {
        group = "verification"
        description = "Fails unless the Stonecutter target supports a dedicated server runtime."
        this.targetId.set(sc.current.project)
        runtimeSide.set("dedicated server")
        supported.set(targetServer)
    }

if (targetStatus == "implemented") {
    tasks.matching { it.name == "runClient" }.configureEach {
        dependsOn(verifyClientTarget)
    }

    tasks.matching { it.name == "runServer" }.configureEach {
        dependsOn(verifyServerTarget)
    }

    tasks.matching { it.name == "collectArtifact" }.configureEach {
        dependsOn(rootProject.tasks.named("clearCollectedArtifacts"))
    }
} else {
    val blockedAliases =
        setOf(
            "collectArtifact",
            "remapJar",
            "remapSourcesJar",
            "reobfJar",
            "reobfSourcesJar",
            "runClient",
            "runServer",
            "sourcesJar",
        )
    blockedAliases.filterNot(tasks.names::contains).forEach { alias ->
        tasks.register(alias) {
            group = if (alias.startsWith("run")) "run" else "build"
            description = "Blocked for the planned target '$targetId'."
        }
    }

    val blockedExactNames =
        setOf(
            "assemble",
            "build",
            "buildDependents",
            "buildNeeded",
            "check",
            "classes",
            "javadoc",
            "test",
            "testClasses",
        )
    val blockedPrefixes =
        listOf(
            "compile",
            "process",
            "remap",
            "reobf",
            "run",
            "stonecutterGenerate",
            "stonecutterMerge",
        )
    val plannedFailure =
        "Target '$targetId' has status 'planned'. " +
            "This task is blocked before source generation, compilation, loader dependency resolution, " +
            "packaging, or runtime launch. Set [${targetId.substringBefore('-')}.\"$targetMinecraftVersion\"]" +
            ".target.status = \"implemented\" in stonecutter.properties.toml only after the target " +
            "implementation and validation are complete."

    tasks.configureEach {
        val blocked =
            name in blockedAliases ||
                name in blockedExactNames ||
                blockedPrefixes.any(name::startsWith) ||
                this is AbstractArchiveTask
        if (blocked) {
            setDependsOn(emptyList<Any>())
            doFirst("rejectPlannedTarget") {
                throw GradleException(plannedFailure)
            }
        }
    }
}
