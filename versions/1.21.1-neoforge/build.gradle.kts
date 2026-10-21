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

tasks.register<Jar>("probeJar") {
    archiveClassifier.set("probe")
    from(provider { sourceSets["probe"].output })
}

val compatibilityApis by
    tasks.registering(Sync::class) {
        for (name in listOf("sable", "sodium")) {
            from(provider { zipTree(target.input(name).singleFile) }) {
                include("META-INF/jarjar/*.jar")
                eachFile { relativePath = RelativePath(true, this.name) }
                includeEmptyDirs = false
            }
        }
        into(layout.buildDirectory.dir("compatibility_apis"))
    }

dependencies {
    compileOnly(target.input("sable"))
    compileOnly(
        fileTree(layout.buildDirectory.dir("compatibility_apis")) { include("*.jar") }
            .builtBy(compatibilityApis),
    )
}
