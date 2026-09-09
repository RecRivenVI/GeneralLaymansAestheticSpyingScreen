import dev.kikugie.stonecutter.data.tree.ProjectNode
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.2-fabric"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    properties {
        tags(version, loader)
    }
    constants {
        match(loader, "fabric", "forge", "neoforge")
    }
    swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    swaps["target_id"] = "\"${current.project}\";"
    constants["release"] = false
    dependencies["fapi"] = properties.getOrNull<String>("deps.fabric.api") ?: "0"
}

stonecutter.tasks.order("createMinecraftArtifacts") {
    metadata.project.endsWith("-forge") || metadata.project.endsWith("-neoforge")
}

abstract class VerifyGlassProject : DefaultTask() {
    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @get:Input
    abstract val activeTarget: Property<String>

    @get:Input
    abstract val expectedTargets: ListProperty<String>

    @get:Input
    abstract val actualStatuses: ListProperty<String>

    @TaskAction
    fun verify() {
        val root = repositoryRoot.get().asFile
        val expected = expectedTargets.get().sorted()
        val versions = root.resolve("versions")
        val actual =
            versions
                .listFiles()
                ?.filter(File::isDirectory)
                ?.map(File::getName)
                ?.sorted()
                .orEmpty()
        check(actual == expected) { "Stonecutter nodes must be exactly $expected, found $actual." }

        val selected = versions.resolve("current").readText().trim()
        check(selected == activeTarget.get()) {
            "versions/current must contain '${activeTarget.get()}', found '$selected'."
        }

        val expectedStatuses =
            expected
                .map { target ->
                    "$target=${if (target == activeTarget.get()) "implemented" else "planned"}"
                }.toSet()
        check(actualStatuses.get().toSet() == expectedStatuses) {
            "Target statuses must be exactly $expectedStatuses, found ${actualStatuses.get()}."
        }

        expected.forEach { target ->
            check(!versions.resolve(target).resolve("src").exists()) {
                "Authored source is forbidden under versions/$target/src."
            }
        }

        val sourceLayers =
            root
                .resolve("src")
                .listFiles()
                ?.filter(File::isDirectory)
                ?.map(File::getName)
                ?.sorted()
                .orEmpty()
        check(sourceLayers.all { it == "main" || it == "test" }) {
            "Only src/main and src/test are permitted, found $sourceLayers."
        }
        check(root.resolve("src/main/java/io/github/recrivenvi/glass").isDirectory) {
            "Production package root must be io.github.recrivenvi.glass."
        }
        check(!root.resolve("src/main/java/me/ichun/mods/glass").exists()) {
            "Legacy Java package remains in authored source."
        }
        check(root.resolve("src/main/resources/assets/glass").isDirectory) {
            "Asset namespace must be assets/glass."
        }
        check(root.resolve("src/main/resources/data/glass").isDirectory) {
            "Data namespace must be data/glass."
        }

        listOf(
            "src/main/resources/fabric.mod.json",
            "src/main/resources/META-INF/mods.toml",
            "src/main/resources/META-INF/neoforge.mods.toml",
        ).forEach { relative ->
            check(root.resolve(relative).isFile) { "Missing co-located Loader metadata: $relative" }
        }

        check(!root.resolve("build.planned.gradle.kts").exists()) {
            "V2 must not use a custom planned-node build script."
        }
        check(!root.resolve("build/artifacts").exists()) {
            "Legacy build/artifacts output is forbidden; use build/libs/<mod-version>."
        }
        check(!root.resolve(".github/workflows").exists()) { "GitHub workflows must be absent." }
        listOf("AGENTS.md", "README.md", "CREDITS.md", "docs").forEach { relative ->
            check(!root.resolve(relative).exists()) { "Port-generated documentation must be absent: $relative" }
        }
    }
}

val targetIds =
    listOf(
        "1.20.1-forge",
        "1.21.1-neoforge",
        "26.1.2-neoforge",
        "26.2-fabric",
        "26.2-neoforge",
    )
val activeTargetId = "26.2-fabric"
val activeTarget: ProjectNode.() -> Boolean = { metadata.project == activeTargetId }

val verifyTargets =
    tasks.register<VerifyGlassProject>("verifyTargets") {
        group = "verification"
        description = "Verifies G.L.A.S.S. Identity, V2 topology, source ownership, and business status."
        repositoryRoot.set(layout.projectDirectory)
        activeTarget.set(activeTargetId)
        expectedTargets.set(targetIds)
        actualStatuses.set(
            targetIds.map { target -> "$target=${project(":$target").property("target.status")}" },
        )
    }

tasks.register("spotlessApply") {
    group = "formatting"
    description = "Applies the GLASS project formatting extension."
    dependsOn(gradle.includedBuild("glass-spotless").task(":spotlessApply"))
}

tasks.register("spotlessCheck") {
    group = "verification"
    description = "Checks the GLASS project formatting extension."
    dependsOn(gradle.includedBuild("glass-spotless").task(":spotlessCheck"))
}

fun registerActiveAggregate(
    name: String,
    nodeTask: String,
    groupName: String,
) = tasks.register(name) {
    group = groupName
    description = "$name is an explicit GLASS aggregate for the active business target."
    dependsOn(verifyTargets)
    dependsOn(stonecutter.tasks.named(nodeTask, activeTarget).map { it.values })
}

registerActiveAggregate("testAll", "test", "verification")
registerActiveAggregate("checkAll", "check", "verification")
registerActiveAggregate("buildAll", "build", "build")
registerActiveAggregate("collectArtifacts", "buildAndCollect", "build")

tasks.named("checkAll") {
    dependsOn("spotlessCheck")
}
