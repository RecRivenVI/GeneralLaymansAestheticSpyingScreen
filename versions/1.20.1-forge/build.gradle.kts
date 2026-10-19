plugins { id("net.neoforged.moddev.legacyforge") }

dependencies {
    annotationProcessor(target.module("org.spongepowered:mixin", "mixin_version") + ":processor")
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

mixin {
    add(sourceSets.main.get(), "glass.refmap.json")
    config("glass.mixins.json")
}

tasks.jar { manifest.attributes("MixinConfigs" to "glass.mixins.json") }

afterEvaluate { legacyForge.addModdingDependenciesTo(sourceSets.test.get()) }

tasks.test {
    useJUnitPlatform()
    systemProperty("glass.test.resources", sourceSets.main.get().output.resourcesDir!!.absolutePath)
}

tasks.withType<JavaCompile>().configureEach {}

tasks.test {
    val testDirectory = layout.buildDirectory.dir("unit-test")
    workingDir(testDirectory)
    doFirst { testDirectory.get().asFile.mkdirs() }
}
