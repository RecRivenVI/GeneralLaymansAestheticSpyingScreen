import com.diffplug.spotless.LineEnding

plugins {
    id("com.diffplug.spotless") version "8.9.0"
}

repositories {
    mavenCentral()
}

layout.buildDirectory = file("build/spotless")

spotless {
    lineEndings = LineEnding.UNIX
    encoding = Charsets.UTF_8

    java {
        target("versions/*/src/**/*.java")
        targetExclude("**/build/**", "run/**")
        googleJavaFormat().aosp()
        formatAnnotations()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts", "gradle/**/*.gradle.kts", "versions/**/*.gradle.kts", "buildSrc/**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("structuredText") {
        target(
            "**/*.json",
            "**/*.json5",
            "**/*.mcmeta",
            "**/*.toml",
            "**/*.yml",
            "**/*.yaml",
            "**/*.md",
            "**/*.gradle",
            "**/*.properties",
            ".editorconfig",
            ".gitattributes",
            ".gitignore",
        )
        targetExclude(
            "**/build/**",
            "run/**",
            "instances/**",
            "validations/*/instance/**",
            "validations/*/result/**",
            ".gradle/**",
            "gradle/wrapper/gradle-wrapper.properties",
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}
