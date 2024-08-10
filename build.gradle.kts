group = "com.almostreliable.plugin"
version = "1.0"


plugins {
    id("java")
    id("java-gradle-plugin")
    id("maven-publish")
}

allprojects {
    repositories {
        maven("https://plugins.gradle.org/m2/")
        mavenCentral()
    }
}

gradlePlugin {
    plugins {
        create("almostGradle") {
            id = "com.almostreliable.almostgradle"
            implementationClass = "com.almostreliable.almostgradle.AlmostGradlePlugin"
        }
    }
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
