plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.google.auto)
    annotationProcessor(libs.google.auto)
}
