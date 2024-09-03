<h1 align="center">
    <p>Almost Gradle</p>
</h1>

<div align="center">

A utility [Gradle] plugin for setting up [NeoForge] mods with [ModDevGradle].

[![Workflow Status][workflow_status_badge]][workflow_status_link]
![License][license_badge]
[![Version][version_badge]][version_link]
[![Discord][discord_badge]][discord]

</div>

# Applying the Plugin

Almost Gradle is hosted at the Gradle plugin portal. Since [ModDevGradle] is hosted there as well, no additional
repositories are necessary.

To create a minimal project, only [ModDevGradle] and Almost Gradle have to be applied. Note that Almost Gradle does not
automatically apply the [ModDevGradle] plugin, allowing to choose its version manually.

```kts
plugins {
    id("net.neoforged.moddev") version "2.0.+"
    id("com.almostreliable.almostgradle") version "1.0.+"
}
```

# Automated Setup

Almost Gradle offers an automated setup for common use cases. The following example shows the minimal setup.

```kts
plugins {
    id("net.neoforged.moddev") version "2.0.+"
    id("com.almostreliable.almostgradle") version "1.0.+"
}

almostgradle.setup {}
```

This step requires some entries inside the `gradle.properties` file. If they are missing, the plugin will throw an error
and stop the setup.

```properties
group = com.example
modId = mod_id
modName = ModName
modVersion = 1.0.0
minecraftVersion = 1.21.1
neoforgeVersion = 21.1.34
```

After specifying the required entries and calling the `setup` method, Almost Gradle will do the following things:

- basics
    - set the project group
    - set the project version as `minecraftVersion-modVersion`
    - set the base archive name to `modId-neoforge`
    - [enable generation of a source JAR](#sources-jar)
- [process resources](#process-resources)
    - collect all placeholder properties from resource files
    - validate if all placeholders have a respective property in the `gradle.properties` file
    - create a `processResources` task that replaces the placeholders with the respective values on build
- [build config](#build-config)
    - generate a class with mod constants
- apply mod basics
    - set the NeoForge version
    - create the main mod from the main source set
    - ensure that the default run configurations `client` and `server` only load the main mod

The behavior of this process and additional features can be modified by the following configuration options.

## Sources Jar

This feature enables the generation of a source JAR for the mod. The artifact is generated when the `build` task is
invoked.

### Defaults:

Enabled: `true`

### Configuration:

This feature can be disabled in the `setup` block.

```kts
almostgradle.setup {
    withSourcesJar = false
}
```

## Process Resources

This feature creates a `processResources` task to replace placeholders in resource files. Placeholders are defined as
`${key}` and the task will replace them with the respective values inside the `gradle.properties` file. It throws an
exception if a key is missing.

### Defaults:

Enabled: `true`

### Configuration:

This feature can be disabled in the `setup` block.

```kts
almostgradle.setup {
    processResources = false
}
```

## Build Config

This feature generates a class with mod constants. To achieve this, Almost Gradle internally uses the [Build Config]
plugin. When the `build` task is invoked, the [Build Config] class will be generated.

### Defaults:

Enabled: `true`<br>
Package: `group.modId`<br>
Name: `BuildConfig`

### Configuration:

This feature can be disabled in the `setup` block.

```kts
almostgradle.setup {
    buildConfig = false
}
```

When enabled, it's possible to define a custom package and class name inside the `gradle.properties` file.

```properties
almostgradle.buildconfig.package = some.example.package.path
almostgradle.buildconfig.name = ModConstants
```

## Launch Arguments

This feature allows setting specific launch arguments for the game.

### Resize Client

This launch argument will resize all client run configurations to `1920x1080`.

#### Defaults:

Enabled: `false`

#### Configuration:

This feature can be enabled in the `setup` block.

```kts
almostgradle.setup {
    launchArgs {
        resizeClient = true
    }
}
```

Alternatively, it can be enabled via property in the `gradle.properties` file.

````properties
almostgradle.launchArgs.resizeClient = true
````

### Auto World Join

This launch argument will enable auto world joining when the client is started.

#### Defaults:

Enabled: `false`<br>
World Name: `New World`

#### Configuration:

This feature can be enabled in the `setup` block.

```kts
almostgradle.setup {
    launchArgs {
        autoWorldJoin = true
    }
}
```

When a custom world name is required, it's also possible to specify the property as a string. It will be used as the
world name and enable the feature automatically.

```kts
almostgradle.setup {
    launchArgs {
        autoWorldJoin = "My World"
    }
}
```

Alternatively, it can be enabled via property in the `gradle.properties` file.

````properties
almostgradle.launchArgs.autoWorldJoin = true
almostgradle.launchArgs.autoWorldJoin = My World
````

### Log Level

This launch argument will set the log level for all run configurations.

#### Defaults:

Level: `DEBUG`

#### Configuration:

This feature can be modified in the `setup` block.

```kts
almostgradle.setup {
    launchArgs {
        loggingLevel = "INFO"
    }
}
```

Alternatively, it can be modified via property in the `gradle.properties` file.

````properties
almostgradle.launchArgs.loggingLevel = INFO
````

### Mixin Debug Output

This launch argument will enable mixin debug output for all run configurations.

#### Defaults:

Enabled: `false`

#### Configuration:

This feature can be enabled in the `setup` block.

```kts
almostgradle.setup {
    launchArgs {
        mixinDebugOutput = true
    }
}
```

Alternatively, it can be enabled via property in the `gradle.properties` file.

````properties
almostgradle.launchArgs.mixinDebugOutput = true
````

## Data Generation

This feature generates a run configuration for data generation.

### Defaults:

Enabled: `false`<br>
Path: `src/generated/resources`

### Configuration:

This feature can be enabled in the `setup` block.

```kts
almostgradle.setup {
    dataGen = true
}
```

When a custom path is required, it's also possible to specify the property as a string. It will be used as the path and
enable the feature automatically.

```kts
almostgradle.setup {
    dataGen = "src/main/resources/generated"
}
```

## Test Mod

This feature creates a test mod with its own run configuration. It will use the default `test` source set. An additional
run configuration is created for running game tests.

The test mod requires a main mod class and its own `neoforge.mods.toml` file. It should look like this:

```toml
modLoader = "javafml"
loaderVersion = "[2,)"

[[mods]]
modId = "testmod"
version = "0.0.0"
displayName = "Test Mod"
```

### Defaults:

Enabled: `false`

### Configuration:

This feature can be enabled in the `setup` block.

```kts
almostgradle.setup {
    testMod = true
}
```

## Recipe Viewers

This feature allows configuration of recipe viewers and how they should be loaded. Recipe viewers can be loaded in their
own run configurations to avoid static toggles. If no version is defined for a recipe viewer, it will not be handled.

Currently supported recipe viewers are JEI, REI, and EMI.

*Mode* refers to the behavior of the recipe viewer.<br>
Possible values are:

- `api` - only load the API artifact into the compile time classpath
- `full` - load the full mod into the compile time classpath
- `none` - don't load anything into the compile time classpath

*Run Config* refers to whether a run configuration should be created for the recipe viewer.

### Defaults:

Mode: `none`<br>
Run Config: `false`
Minecraft Version: same as project
Maven Repository: default for the respective recipe viewer

### Configuration:

This feature can be enabled and modified in the `setup` block.

```kts
almostgradle.setup {
    recipeViewers {
        emi {
            runConfig = true
            mode = LoadingMode.API
            version = "x.x.x"
            /**
             * Tries to fetch the artifact from a specific Maven repository.
             */
            mavenRepository = "https://modmaven.dev"
        }
        rei {
            runConfig = true
            mode = LoadingMode.FULL
            version = "x.x.x"
        }
        jei {
            runConfig = false
            mode = LoadingMode.API
            version = "x.x.x"
            /**
             * Targets a custom Minecraft version. Useful when the recipe viewer is
             * not updated yet, but code relies on its API for compilation.
             */
            minecraftVersion = "1.20"
        }
    }
}
```

Alternatively, it can be enabled and modified via properties in the `gradle.properties` file.

```properties
almostgradle.recipeViewers.emi.runConfig = true
almostgradle.recipeViewers.emi.mode = API
almostgradle.recipeViewers.emi.version = x.x.x
almostgradle.recipeViewers.emi.maven = https://modmaven.dev

almostgradle.recipeViewers.rei.runConfig = true
almostgradle.recipeViewers.rei.mode = FULL
almostgradle.recipeViewers.rei.version = x.x.x

almostgradle.recipeViewers.jei.runConfig = true
almostgradle.recipeViewers.jei.mode = API
almostgradle.recipeViewers.jei.version = x.x.x
almostgradle.recipeViewers.jei.minecraftVersion = 1.20
```

Additionally, it's possible to set the version for a recipe viewer and create the dependency on demand. This is useful
if you have special cases, or if you want to load the recipe viewer in a specific configuration.

```kts
dependency {
    // Loads the basic dependency into `localRuntime`.
    localRuntime(almostgradle.recipeViewers.emi.dependency)

    // Loads the API into `compileOnly`.
    compileOnly(almostgradle.recipeViewers.emi.apiDependency)
}
```

# Configurations

Next to the default ones, the plugin offers additional configurations to load dependencies into the classpath.

*Transitive* refers to the behavior of the dependencies.<br>
For example, if *Project B* has some transitive dependency and *Project A* depends on *Project B*, *Project A* will also
load the dependency.

## Default Configurations

The [Java plugin] ships some default configurations.

| Configuration    | Compile | Runtime | Transitive |
|------------------|:-------:|:-------:|:----------:|
| `runtimeOnly`    |    ❌    |   ✔️    |  Runtime   |
| `compileOnly`    |   ✔️    |    ❌    |    None    |
| `compileOnlyApi` |   ✔️    |    ❌    |  Compile   |
| `implementation` |   ✔️    |   ✔️    |  Runtime   |
| `api`            |   ✔️    |   ✔️    |    Both    |

## Additional Configurations

Almost Gradle offers additional configurations.

| Configuration  | Compile | Runtime | Transitive |
|----------------|:-------:|:-------:|:----------:|
| `localRuntime` |    ❌    |   ✔️    |    None    |

## Test Configurations

Almost Gradle also allows applying dependencies to the test classpath only. To do that, the configuration has to be
prefixed with `test`.

<!-- Badges -->
[workflow_status_badge]: https://img.shields.io/github/actions/workflow/status/AlmostReliable/almostgradle/build.yml?branch=main&style=for-the-badge
[workflow_status_link]: https://github.com/AlmostReliable/almostgradle/actions
[license_badge]: https://img.shields.io/badge/License-ARR-ffa200?style=for-the-badge
[version_badge]: https://img.shields.io/github/v/release/AlmostReliable/almostgradle?include_prereleases&style=for-the-badge
[version_link]: https://github.com/AlmostReliable/almostgradle/releases/latest
[discord_badge]: https://img.shields.io/discord/917251858974789693?color=5865f2&label=Discord&logo=discord&style=for-the-badge

<!-- Links -->
[gradle]: https://gradle.org/
[neoforge]: https://neoforged.net/
[moddevgradle]: https://github.com/neoforged/ModDevGradle
[discord]: https://discord.com/invite/ThFnwZCyYY
[java plugin]: https://docs.gradle.org/current/userguide/java_plugin.html
[build config]: https://github.com/gmazzo/gradle-buildconfig-plugin
