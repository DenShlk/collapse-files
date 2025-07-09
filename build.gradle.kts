import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.6.0"
}

group = "com.denshlk"
version = "1.1.0"

repositories {
    mavenCentral()
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://www.jetbrains.com/intellij-repository/snapshots")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    intellijPlatform {
        defaultRepositories()
    }
}


// Create integration test source set
sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val integrationTestImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        create("IC", "2025.1")
//        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Starter)

        // Add necessary plugin dependencies for compilation here, example:
        // bundledPlugin("com.intellij.java")
        bundledPlugin("com.intellij.java")
    }

    integrationTestImplementation("org.junit.jupiter:junit-jupiter-api:5.12.2")
    integrationTestImplementation("org.junit.jupiter:junit-jupiter-engine:5.12.2")
    integrationTestImplementation("org.junit.platform:junit-platform-launcher:1.12.2")
    integrationTestImplementation("org.kodein.di:kodein-di-jvm:7.25.0")
    integrationTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.1")

    // IntelliJ Starter Framework dependencies (exact versions from working uppercut project)
    // should be like com.jetbrains.intellij.tools:ide-starter-driver:LATEST-EAP-SNAPSHOT
    integrationTestImplementation("com.jetbrains.intellij.tools:ide-starter-squashed:251.23774.435")
    integrationTestImplementation("com.jetbrains.intellij.tools:ide-starter-junit5:251.23774.435")
    integrationTestImplementation("com.jetbrains.intellij.tools:ide-starter-driver:251.23774.435")
    integrationTestImplementation("com.jetbrains.intellij.driver:driver-client:251.23774.435")
    integrationTestImplementation("com.jetbrains.intellij.driver:driver-sdk:251.23774.435")
    integrationTestImplementation("com.jetbrains.intellij.driver:driver-model:251.23774.435")
    integrationTestImplementation("com.jetbrains.intellij.tools:ide-util-common:251.23774.435")
    integrationTestImplementation("com.jetbrains.intellij.tools:ide-performance-testing-commands:251.23774.435")
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

    pluginVerification {
        ides {
            ide(IntelliJPlatformType.PyCharmProfessional, "latest")
            recommended()
        }
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

    register<Test>("integrationTest") {
        val integrationTestSourceSet = sourceSets.getByName("integrationTest")
        testClassesDirs = integrationTestSourceSet.output.classesDirs
        classpath = integrationTestSourceSet.runtimeClasspath
        systemProperty("path.to.build.plugin", prepareSandbox.get().pluginDirectory.get().asFile)
        useJUnitPlatform {
            excludeEngines("junit-vintage")
            includeEngines("junit-jupiter")
        }
        dependsOn(prepareSandbox)
    }
}