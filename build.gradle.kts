@file:Suppress("UnstableApiUsage")

group = "com.almostreliable"
version = "1.0.0"

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
            description = "Utility plugin for modding in Minecraft"
            implementationClass = "${project.group}.${project.name}.AlmostGradlePlugin"
        }
    }
}

tasks.withType<Javadoc> {
    (options as CoreJavadocOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
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
