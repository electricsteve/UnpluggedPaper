plugins {
    id("java-library")
    kotlin("jvm") version "2.3.0"
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.run.paper)
}

repositories {
    mavenCentral()
    mavenLocal() // This is currently used for FakePlayerPlus
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper.api.get())
    compileOnly(libs.fake.player.plus) // MavenLocal for now
    implementation(libs.lamp.common)
    implementation(libs.lamp.bukkit)
    implementation(libs.kotlinx.coroutines.core)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

kotlin {
    jvmToolchain(25)
}

configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
    jvmToolchain(25)
}

tasks {
    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs("-Xms2G", "-Xmx2G", "-Dcom.mojang.eula.agree=true", "-Ddisable.watchdog=true")
        downloadPlugins {
//            github("xiplugin", "FakePlayerPlus", "v2.2.1", "FakePlayerPlus-2.2.1.jar")
            // We are using a locally built dev version, so you'll have to place it in the plugin folder yourself
        }
    }

    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    // Suggested by Lamp
    withType<JavaCompile> {
        // Preserve parameter names in the bytecode
        options.compilerArgs.add("-parameters")
    }
}
