plugins { id("net.fabricmc.fabric-loom") }

repositories { maven("https://maven.terraformersmc.com/releases") }

dependencies {
    testImplementation(sourceSets["client"].output)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
    implementation(target.module("net.fabricmc.fabric-api:fabric-api", "fabric_api_version"))
    compileOnly(target.module("com.terraformersmc:modmenu", "modmenu_version"))
}

tasks.test { useJUnitPlatform() }

tasks.test {
    val testDirectory = layout.buildDirectory.dir("unit-test")
    workingDir(testDirectory)
    doFirst { testDirectory.get().asFile.mkdirs() }
}
