plugins { `java-library` }

dependencies {
    compileOnly(libs.joml)
    testImplementation(libs.joml)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}
