import dev.kikugie.stonecutter.data.tree.ProjectNode
import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT" apply false
    id("net.fabricmc.fabric-loom-remap") version "1.17-SNAPSHOT" apply false
}

repositories {
    mavenCentral()
}

stonecutter flags {
    this["line_separator"] = "\n"
}

stonecutter active file("versions/current")

stonecutter parameters {
    val loader = current.project.substringBefore('-')
    val version = current.project.substringAfter('-')

    properties {
        tags(version, loader)
    }

    constants {
        match(loader, "fabric", "forge", "neoforge")
    }

    swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    constants["release"] = false
    targets["fapi"] = properties.getOrNull<String>("deps.fabric.api") ?: "0"
}

abstract class VerifyTargetRegistryTask : DefaultTask() {
    @get:Input
    abstract val repositoryPath: Property<String>

    @get:Input
    abstract val activeTarget: Property<String>

    @get:Input
    abstract val expectedTargets: ListProperty<String>

    @get:Input
    abstract val expectedStatuses: ListProperty<String>

    @get:Input
    abstract val actualStatuses: ListProperty<String>

    @TaskAction
    fun verify() {
        val repository = File(repositoryPath.get())
        val versions = repository.resolve("versions")
        val actualTargets =
            versions
                .listFiles()
                ?.filter(File::isDirectory)
                ?.map(File::getName)
                ?.sorted()
                .orEmpty()
        val expectedTargetNames = expectedTargets.get().sorted()
        check(actualTargets == expectedTargetNames) {
            "Stonecutter targets must be exactly $expectedTargetNames, found $actualTargets."
        }

        val selectedTarget = versions.resolve("current").readText().trim()
        check(selectedTarget == activeTarget.get()) {
            "versions/current must contain '${activeTarget.get()}', found '$selectedTarget'."
        }

        val expectedStatusEntries = expectedStatuses.get().sorted()
        val actualStatusEntries = actualStatuses.get().sorted()
        check(actualStatusEntries == expectedStatusEntries) {
            "Target statuses must be exactly $expectedStatusEntries, found $actualStatusEntries."
        }

        expectedTargetNames.forEach { target ->
            val targetDirectory = versions.resolve(target)
            val resourceRoot = targetDirectory.resolve("src/main/resources")
            check(resourceRoot.isDirectory) {
                "Target '$target' must provide src/main/resources."
            }
            val metadata =
                when {
                    target.startsWith("fabric-") -> resourceRoot.resolve("fabric.mod.json")
                    target.startsWith("forge-") -> resourceRoot.resolve("META-INF/mods.toml")
                    else -> resourceRoot.resolve("META-INF/neoforge.mods.toml")
                }
            check(metadata.isFile) {
                "Target '$target' must provide ${metadata.relativeTo(repository).invariantSeparatorsPath}."
            }
        }
    }
}

val targetIds =
    listOf(
        "forge-1.20.1",
        "neoforge-1.21.1",
        "neoforge-26.1.2",
        "fabric-26.2",
        "neoforge-26.2",
    )
val expectedTargetStatuses =
    targetIds.map { target ->
        "$target=${if (target == "fabric-26.2") "implemented" else "planned"}"
    }
val actualTargetStatuses =
    targetIds.map { target ->
        "$target=${project(":$target").property("target.status")}"
    }

val implementedTargets: ProjectNode.() -> Boolean = {
    project.property("target.status").toString() == "implemented"
}

val targetContracts = stonecutter.tasks.named("verifyTargetContract")
val targetTests = stonecutter.tasks.named("test", implementedTargets)
val targetChecks = stonecutter.tasks.named("check", implementedTargets)
val targetBuilds = stonecutter.tasks.named("build", implementedTargets)
val targetArtifacts = stonecutter.tasks.named("collectArtifact", implementedTargets)
val clearCollectedArtifacts =
    tasks.register<Delete>("clearCollectedArtifacts") {
        description = "Removes previously collected production artifacts."
        delete(layout.buildDirectory.dir("artifacts"))
    }

val verifyTargetRegistry =
    tasks.register<VerifyTargetRegistryTask>("verifyTargetRegistry") {
        group = "verification"
        description = "Verifies the exact target registry, statuses, active target, and metadata layout."
        repositoryPath.set(layout.projectDirectory.asFile.absolutePath)
        activeTarget.set("fabric-26.2")
        expectedTargets.set(targetIds)
        expectedStatuses.set(expectedTargetStatuses)
        actualStatuses.set(actualTargetStatuses)
    }

tasks.register("spotlessApply") {
    group = "formatting"
    description = "Applies repository formatting through the isolated Spotless build."
    dependsOn(gradle.includedBuild("glass-spotless").task(":spotlessApply"))
}

tasks.register("spotlessCheck") {
    group = "verification"
    description = "Checks repository formatting through the isolated Spotless build."
    dependsOn(gradle.includedBuild("glass-spotless").task(":spotlessCheck"))
}

tasks.register("verifyTargets") {
    group = "verification"
    description = "Verifies all five target names, statuses, capabilities, directories, and metadata."
    dependsOn(verifyTargetRegistry, targetContracts)
}

tasks.register("testAll") {
    group = "verification"
    description = "Runs tests for every implemented Stonecutter target."
    dependsOn("verifyTargets", targetTests)
}

tasks.register("checkAll") {
    group = "verification"
    description = "Runs checks for every implemented Stonecutter target."
    dependsOn("spotlessCheck", "verifyTargets", targetChecks)
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds every implemented Stonecutter target."
    dependsOn("verifyTargets", targetBuilds)
}

tasks.register("collectArtifacts") {
    group = "build"
    description = "Collects production artifacts from every implemented Stonecutter target."
    dependsOn("verifyTargets", clearCollectedArtifacts, targetArtifacts)
}
