import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.nio.charset.Charset

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.denshlk"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        create("IC", "2025.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here, example:
        // bundledPlugin("com.intellij.java")
        bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "233"
            untilBuild = "251.*"
        }

        changeNotes = """
      Initial version of Collapse Files plugin.
      Features:
      - Collapse sequences of unused folders and files in project view
      - Keep open files and their parent folders always visible
      - Click to expand/collapse grouped items
    """.trimIndent()
    }

    signing {
        val certificateChainText = File("secrets/chain.crt").readText(Charsets.UTF_8)
        val privateKeyText = File("secrets/private.pem").readText(Charsets.UTF_8)
        certificateChain.set(certificateChainText)
        privateKey.set(privateKeyText)
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    }

    publishing {
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    }
}
