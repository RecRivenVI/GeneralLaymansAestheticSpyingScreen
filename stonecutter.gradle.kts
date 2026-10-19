import dev.kikugie.stonecutter.controller.flag.StonecutterFlag

plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.2-fabric"

stonecutter flags {
    set(StonecutterFlag.APPEND_SOURCES_AFTER_EVAL, false)
    set(StonecutterFlag.GENERATE_SOURCES_ON_SYNC, false)
}

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)
    constants { match(loader, "fabric", "forge", "neoforge") }
    dependencies["fapi"] = project.findProperty("deps.fabric.api")?.toString() ?: "0"
}

stonecutter.tasks.order("createMinecraftArtifacts") {
    metadata.project.endsWith("-forge") || metadata.project.endsWith("-neoforge")
}
