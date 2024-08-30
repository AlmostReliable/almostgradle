@file:Suppress("UnstableApiUsage")

group = "com.almostreliable"
version = "1.0"


plugins {
    id("java")
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish") version "1.2.2"
}

allprojects {
    repositories {
        maven("https://plugins.gradle.org/m2/")
        mavenCentral()
    }
}

gradlePlugin {
    website = "https://almostreliable.com"
    vcsUrl = "https://github.com/AlmostReliable/almostgradle.git"
    plugins {
        create("almostgradle") {
            id = "com.almostreliable.almostgradle"
            displayName = "AlmostGradle"
            description = "Utility plugin for modding in Minecraft"
            implementationClass = "com.almostreliable.almostgradle.AlmostGradlePlugin"
        }
    }
}

tasks.withType<Javadoc> {
    (options as CoreJavadocOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
}

buildscript {
    dependencies {
        classpath("net.neoforged:moddev-gradle:2.0.19-beta")
        classpath("com.github.gmazzo.buildconfig:plugin:5.4.0")
    }

}

dependencies {
    compileOnly("net.neoforged:moddev-gradle:2.0.19-beta")
    implementation("com.github.gmazzo.buildconfig:plugin:5.4.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
