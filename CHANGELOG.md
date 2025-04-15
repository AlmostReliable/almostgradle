# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog], and this project adheres to [Semantic Versioning].

## Unreleased

- added check for minimal Gradle version
- added check for minimal ModDevGradle version
- updated internal ModDevGradle version to 2.0.80
- updated minimum ModDevGradle version to 2.0.64-beta
- updated Gradle wrapper to 8.12.1
- updated minimum Gradle version to 8.12.1
- changed datagen run directory to temporary directory to avoid crashes with file-based runtime mods

## [1.1.1] - 2024-09-16

- fixed compile error with newest ModDevGradle ([MDG#158](https://github.com/neoforged/ModDevGradle/pull/158))

## [1.1.0] - 2024-09-05

- added `localImplementation` and `testLocalImplementation` to load dependencies into compile & runtime classpath without being transitive for consumers

## [1.0.1] - 2024-09-04

- fixed `minecraftVersion` not being overridden by recipe viewer configs

## [1.0.0] - 2024-09-01

Initial release.

<!-- Links -->
[keep a changelog]: https://keepachangelog.com/en/1.0.0/
[semantic versioning]: https://semver.org/spec/v2.0.0.html

<!-- Versions -->
[1.1.0]: https://github.com/AlmostReliable/almostgradle/releases/tag/v1.1.0
[1.0.1]: https://github.com/AlmostReliable/almostgradle/releases/tag/v1.0.1
[1.0.0]: https://github.com/AlmostReliable/almostgradle/releases/tag/v1.0.0
[1.1.1]: https://github.com/AlmostReliable/almostgradle/releases/tag/v1.1.1
