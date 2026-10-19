import java.util.Properties

plugins { base }

subprojects {
    val facts = file("target.properties")
    if (facts.isFile) {
        val values = Properties().apply { facts.inputStream().use { load(it) } }
        values.forEach { key, value -> extensions.extraProperties[key.toString()] = value.toString() }
    }
}

tasks.register<Exec>("verifyTargets") {
    group = "verification"
    commandLine("powershell", "-NoProfile", "-File", "eng/structure/Verify-RepositoryLayout.ps1")
}
listOf("spotlessApply", "spotlessCheck").forEach { taskName ->
    tasks.register(taskName) {
        group = "verification"
        dependsOn(gradle.includedBuild("glass-spotless").task(":$taskName"))
    }
}
mapOf(
    "testAll" to "test",
    "checkAll" to "check",
    "buildAll" to "build",
    "collectArtifacts" to "buildAndCollect",
).forEach { (aggregate, targetTask) ->
    tasks.register(aggregate) {
        group = if (aggregate in listOf("testAll", "checkAll")) "verification" else "build"
        dependsOn("verifyTargets", ":version:26.2-fabric:$targetTask")
        if (aggregate == "checkAll") dependsOn("spotlessCheck")
    }
}
