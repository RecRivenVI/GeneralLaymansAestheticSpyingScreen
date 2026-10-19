pluginManagement {
    includeBuild("gradle/conventions")
    repositories {
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.fabricmc.net/")
    }
}

plugins { id("io.github.recrivenvi.conventions") }

rootProject.name = "GlassMod"

targets {
    register("1.20.1-forge")
    register("1.21.1-neoforge")
    register("26.1.2-neoforge")
    register("26.3-neoforge")
    register("26.3-fabric")
}

components {
    product("projection_math")
    tool("runtime_validation")
}

compliance { strict = true }
