plugins {
    id("java")
}

group = property("group") as String
version = property("version") as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Paper 26.3 (Minecraft "Wilderness Bound", released 2026-09-16). This artifact declares a
    // minimum JVM runtime of 25, so the build toolchain must be 25 as well.
    compileOnly("io.papermc.paper:paper-api:26.3.build.8-alpha")
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}
