@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.plugin.publish)
}

repositories {
    gradlePluginPortal()
}

buildscript {
    dependencies {
        classpath(libs.buildconfig)
        classpath(libs.moddevgradle)
    }
}

dependencies {
    compileOnly(libs.buildconfig)
    compileOnly(libs.moddevgradle)
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
