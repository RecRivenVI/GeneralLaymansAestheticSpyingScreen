import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

interface ForgeMutex : BuildService<BuildServiceParameters.None>

val mutex =
    gradle.sharedServices.registerIfAbsent("createForgeMinecraftArtifactsMutex", ForgeMutex::class.java) {
        maxParallelUsages.set(1)
    }

tasks.named { it == "createMinecraftArtifacts" }.configureEach {
    usesService(mutex)
}
