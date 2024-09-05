@file:Suppress("UnstableApiUsage")

group = "com.almostreliable"
version = "1.1.0"

plugins {
    id("com.gradle.plugin-publish") version "1.2.2"
}

gradlePlugin {
    website = "https://almostreliable.com"
    vcsUrl = "https://github.com/AlmostReliable/almostgradle.git"
    plugins {
        create(project.name) {
            id = "${project.group}.${project.name}"
            displayName = "AlmostGradle"
            description = "A utility plugin for setting up NeoForge mods with ModDevGradle."
            implementationClass = "${project.group}.${project.name}.AlmostGradlePlugin"
            tags.set(listOf("minecraft", "modding", "moddevgradle", "utility"))
        }
    }
}

tasks {
    withType<Jar> {
        if (name == "javadocJar") {
            enabled = false
        }
    }
    withType<Javadoc> {
        enabled = false
    }
}

repositories {
    gradlePluginPortal()
}

buildscript {
    dependencies {
        classpath("net.neoforged:moddev-gradle:2.0.+")
        classpath("com.github.gmazzo.buildconfig:plugin:5.4.0")
    }
}

dependencies {
    compileOnly("net.neoforged:moddev-gradle:2.0.+")
    implementation("com.github.gmazzo.buildconfig:plugin:5.4.0")
}
