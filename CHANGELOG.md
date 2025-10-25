# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog], and this project adheres to [Semantic Versioning].

## Unreleased

- added plugin version output to setup log messages
- added possibility to load multiple dependencies for recipe viewers ([#4](https://github.com/AlmostReliable/almostgradle/pull/4))
- added automatically injected placeholders to specify recipe viewer minimum version constraints ([#4](https://github.com/AlmostReliable/almostgradle/pull/4))
- fixed JEI recipe viewer not loading the common API sources ([#4](https://github.com/AlmostReliable/almostgradle/pull/4))
- changed resize client launch argument to be enabled by default

## [1.3.0] - 2025-06-03

- added option for disable loading the test mod in recipe viewer run configs
- fixed recipe viewer run configs not loading the test source set and not executing compile tasks

## [1.2.0] - 2025-04-17

- added check for minimal Gradle version
- added check for minimal ModDevGradle version
- updated internal ModDevGradle version to 2.0.80
- updated minimum ModDevGradle version to 2.0.64-beta
- updated Gradle wrapper to 8.12.1
- updated minimum Gradle version to 8.12.1
- changed datagen run directory to temporary directory to avoid crashes with file-based runtime mods
- changed game test namespace to the test mod instead of the main mod
- changed game test run directory to temporary directory to improve load time

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
[1.2.0]: https://github.com/AlmostReliable/almostgradle/releases/tag/v1.2.0
[1.3.0]: https://github.com/AlmostReliable/almostgradle/releases/tag/v1.3.0
