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
    // Paper 26.2 (Minecraft "Chaos Cubed", released 2026-06-17). This artifact declares a
    // minimum JVM runtime of 25, so the build toolchain must be 25 as well.
    compileOnly("io.papermc.paper:paper-api:26.2.build.124-stable")
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}
