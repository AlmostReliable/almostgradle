@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.plugin.publish)
}

val minimumGradleVersion: String by project
val minimumMdgVersion: String by project

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
    implementation(libs.buildconfig)
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

buildConfig {
    buildConfigField("String", "VERSION", "\"${project.version}\"")
    buildConfigField("String", "BUILDCONFIG_VERSION", "\"${libs.versions.buildconfig.get()}\"")
    buildConfigField("String", "MINIMUM_GRADLE_VERSION", "\"$minimumGradleVersion\"")
    buildConfigField("String", "MINIMUM_MDG_VERSION", "\"$minimumMdgVersion\"")
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
