# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog], and this project adheres to [Semantic Versioning].

## Unreleased

- fixed recipe viewers using long internal name instead of short id (REI is now `rei` instead of `roughlyenoughitems`)

## [2.1.0] - 2026-04-26

- added option to split run configuration directories
- fixed data generation feature not working with Minecraft 26.1+ because of client and server separation
- fixed inconsistent internal names for recipe viewer run configurations
- changed build directory resolving to use built-in Gradle method

## [2.0.0] - 2026-04-23

**Note**: This release is a new major version and includes breaking changes.

- added support for NeoForge test framework
- added support for JUnit tests
- added global option to change mod package
- added API JAR option to build JAR and sources from sub-package
- removed support for dedicated API source set
- changed test mod definition to decouple it from vanilla game tests
- changed build config feature to use global mod package path
- updated internal ModDevGradle version to 2.0.138
- updated minimum ModDevGradle version to 2.0.138 for compatibility with unobfuscated sources
- updated default Java target version to Java 25
- fixed wrong Roughly Enough Items recipe viewer mod id
- fixed datagen properties not working
- fixed Maven publishing not using the mod id as path

## [1.5.2] - 2026-03-24

- revert fix for ignoring commented properties
- introduced error for commented properties due to conflicts with Gradle property expansion

## [1.5.1] - 2026-03-24

- fixed commented lines in mods.toml files not being ignored when checking for placeholders

## [1.5.0] - 2026-01-04

- added option to set the project Java version, defaults to Java 21
- added option to validate access transformers, enabled by default
- removed explicit loading of the BuildConfig plugin
- fixed classpath being resolved too early

## [1.4.1] - 2025-10-25

- fixed BuildConfig plugin not being published transitively

## [1.4.0] - 2025-10-25

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
[1.4.0]: https://github.com/AlmostReliable/almostgradle/releases/tag/v1.4.0
[1.4.1]: https://github.com/AlmostReliable/almostgradle/releases/tag/v1.4.1
[1.5.0]: https://github.com/AlmostReliable/almostgradle/releases/tag/v1.5.0
[1.5.1]: https://github.com/AlmostReliable/almostgradle/releases/tag/v1.5.1
[1.5.2]: https://github.com/AlmostReliable/almostgradle/releases/tag/v1.5.2
[2.0.0]: https://github.com/AlmostReliable/almostgradle/releases/tag/v2.0.0
[2.1.0]: https://github.com/AlmostReliable/almostgradle/releases/tag/v2.1.0
