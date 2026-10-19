val repository = layout.projectDirectory.dir("../..")
val pythonEnvironment = layout.buildDirectory.dir("python")
val pythonExecutable = pythonEnvironment.map { it.file("Scripts/python.exe") }
val ruffExecutable = pythonEnvironment.map { it.file("Scripts/ruff.exe") }
val ruffVersion = libs.versions.ruff.get()
val createPythonEnvironment =
    tasks.register<Exec>("createPythonEnvironment") {
        commandLine("python", "-m", "venv", pythonEnvironment.get().asFile)
        outputs.file(pythonExecutable)
    }
val installRuff =
    tasks.register<Exec>("installRuff") {
        dependsOn(createPythonEnvironment)
        commandLine(
            pythonExecutable.get().asFile,
            "-m",
            "pip",
            "install",
            "--disable-pip-version-check",
            "ruff==$ruffVersion",
        )
        inputs.property("ruffVersion", ruffVersion)
        outputs.file(ruffExecutable)
        outputs.dir(pythonEnvironment.map { it.dir("Lib/site-packages/ruff") })
    }
val formatPython =
    tasks.register<Exec>("formatPython") {
        dependsOn(installRuff)
        commandLine(
            ruffExecutable.get().asFile,
            "format",
            layout.projectDirectory.file("runtime_validation.py").asFile,
            layout.projectDirectory.file("mcp_alpha.py").asFile,
            layout.projectDirectory.file("native_gui.py").asFile,
        )
    }
val checkPython =
    tasks.register<Exec>("checkPython") {
        dependsOn(installRuff)
        commandLine(
            ruffExecutable.get().asFile,
            "format",
            "--check",
            layout.projectDirectory.file("runtime_validation.py").asFile,
            layout.projectDirectory.file("mcp_alpha.py").asFile,
            layout.projectDirectory.file("native_gui.py").asFile,
        )
    }

tasks.named("spotlessApply") { dependsOn(formatPython) }

val validate =
    tasks.register<Exec>("validateProfiles") {
        workingDir(repository)
        commandLine("python", "components/runtime_validation/runtime_validation.py", "check")
    }

tasks.named("check") { dependsOn(validate, checkPython) }

for (scene in listOf("S1", "S2", "M1", "M2")) {
    tasks.register<Exec>("prepare$scene") {
        workingDir(repository)
        commandLine(
            "python",
            "components/runtime_validation/runtime_validation.py",
            "prepare",
            scene,
        )
    }
}

tasks.register<Exec>("prepareReference") {
    workingDir(repository)
    commandLine(
        "python",
        "components/runtime_validation/runtime_validation.py",
        "prepare-reference",
    )
}

tasks.register<Exec>("reviewReference") {
    workingDir(repository)
    commandLine("python", "components/runtime_validation/runtime_validation.py", "review-reference")
}

val mcpActions =
    mapOf(
        "installMcpRuntime" to "install",
        "smokeMcpMinimal" to "minimal",
        "smokeMcpReference" to "reference",
        "launchAcceptance" to "acceptance",
    )

for ((taskName, action) in mcpActions) {
    tasks.register<Exec>(taskName) {
        workingDir(repository)
        commandLine("python", "components/runtime_validation/mcp_alpha.py", action)
    }
}

tasks.register<Exec>("verifyNativeGui") {
    workingDir(repository)
    commandLine("python", "components/runtime_validation/native_gui.py")
}

tasks.register<Exec>("launchGuiAcceptance") {
    mustRunAfter("verifyNativeGui")
    workingDir(repository)
    commandLine("python", "components/runtime_validation/native_gui.py", "acceptance")
}
