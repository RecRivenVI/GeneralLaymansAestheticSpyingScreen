plugins {
    id("net.neoforged.moddev")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.test { useJUnitPlatform() }

afterEvaluate {
    neoForge {
        unitTest {
            enable()
            testedMod = mods[target.fact("mod_id")]
        }
    }
}

tasks.test {
    systemProperty("glass.test.resources", sourceSets.main.get().output.resourcesDir!!.absolutePath)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "1000"))
}
