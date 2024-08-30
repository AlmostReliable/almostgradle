# AlmostGradle

Utility plugin for setting up a NeoForge Mod when using [ModDevGradle](https://github.com/neoforged/ModDevGradle). The
main usage is to reduce some boilerplate each project has to deal with, like `processResources` or creating the base mod
run or a test mod.

## Applying the plugin

Same as MDG, the plugin is hosted at the [gradle plugin portal](please add link to the plugin here).
To create a minimalistic project, all we have to do is to apply the ModDevGradle and AlmostGradle plugin.

#### `build.gradle`

```kts
plugins {
    id("net.neoforged.moddev") version "1.0.9"
    id("com.almostreliable.almostgradle") version "1.0"
}
```

## Configurations

The java plugin in gradles comes with some pre-defined configurations. `Transitive` means that if for an example *
*Project A** relies on _Project B_, then **Project A** will also load the dependencies into the respective classpath.

### Standard configurations

| Configuration    | Compile | Runtime | Transitive |
|------------------|:-------:|:-------:|:----------:|
| `implementation` |   ✔️    |   ✔️    |  Runtime   |
| `api`            |   ✔️    |   ✔️    |    Both    |
| `compileOnly`    |   ✔️    |    ❌    |    None    |
| `compileOnlyApi` |   ✔️    |    ❌    |  Compile   |
| `runtimeOnly`    |    ❌    |   ✔️    |  Runtime   |

### Additional configurations

Sometimes you want to load a mod only into the runtime classpath but without being transitive. For this case, similiar
to `loom`, `AlmostGradle` adds the `localRuntime` configuration.

| Configuration  | Compile | Runtime | Transitive |
|----------------|:-------:|:-------:|:----------:|
| `localRuntime` |    ❌    |   ✔️    |    None    |

```kts
dependency {
    localRuntime("com.example:example-mod:1.0")
}
```

### Test configurations

To load dependencies into the test classpath, we can simply prefix the configuration
with `test`. `implementation` -> `testImplementation` etc.

```kts
dependency {
    testCocalRuntime("com.example:example-mod:1.0")
}
```

## Automatic project setup

`AlmostGradle` offers an automatic setup for most of the common use cases.

```kts
plugins {
    id("net.neoforged.moddev") version "1.0.9"
    id("com.almostreliable.almostgradle") version "1.0"
}

almostgradle.setup {
    // configure stuff e.g.
    // testMod = true
}
```

Using the automatic setup, requires to define some properties inside the `gradle.properties`.

```properties
group=com.example
# Mod options
modId=your-mod-id
modName=YourModName
modVersion=1.0
# Common
minecraftVersion=1.21
neoforgeVersion=21.0.146
```

### BuildConfig

`AlmostGradle` uses the [BuildConfig](https://github.com/gmazzo/gradle-buildconfig-plugin) plugin to create a class with
some mod info.

Default to `true`. On `build` the class will be generated under `group.modId.BuildConfig`. But you can define a
different package and class name inside the `gradle.properties`.

```properties
almostgradle.buildconfig.package=some.example.package.path
almostgradle.buildconfig.name=MyModInformation
```

### Basic options

#### DataGen

Default to `false`. When activating, a run config for data generation will be created. The output path for the data will
be in `src/generated/resources`.

#### Process Resources

Default to `true`. Automatically creates a `processResources` task to replace placeholders inside `neoforge.mods.toml`
and `pack.mcmeta`. Placeholders are defined as `${key}` and the task will replace them with the respective values inside
the `gradle.properties`. So `${modId}` will be replaced with the value of `modId` inside our `gradle.properties`.

If a key is missing, it will throw an exception.

#### Sources Jar

Default to `true`. Will create a sources jar for the mod on build.

#### Test Mod

Default to `false`. When activating, a test mod will be created inside MDG with its own run config. For the test mod the
default `test` source set will be used.

It's still required to create an entry mod class and the `neoforge.mods.toml` file. A simple `neoforge.mods.toml` for
testing could look like this:

```toml
modLoader = "javafml"
loaderVersion = "[2,)"
license = "ARR"

[[mods]]
modId = "testmod"
version = "0.0.0"
displayName = "Test Mod"
```

### Recipe Viewers

Setup multiple recipe viewers and how they should be loaded. You can load each recipe viewer in its own `run config`, so
no static toggle with gradle reloads are required.

* The `mode` defines if the `api`, `full` mod or `none` should be loaded into the `compile` classpath. Default
  to `none`.
* The `runConfig` defines if a specific run config should be created for the recipe viewer. Default to `false`.

If no `version` is defined for specific recipe viewers, their will not be handled at all, so you can choose on your own
which viewer should be used and how.

#### Configure via `setup`

```kts
almostgradle.setup {
    testMod = true
    resizeClient = true
    recipeViewers {
        emi {
            runConfig = true
            mode = LoadingMode.API
            version = "x.x.x"
        }

        rei {
            runConfig = true
            mode = LoadingMode.API
            version = "x.x.x"
        }

        jei {
            runConfig = false
            mode = LoadingMode.API
            version = "x.x.x"
            /**
             * Target a custom minecraft version just for this recipe viewer. Useful when the recipe viewer is
             * not updated yet, but we rely on their API for compilation.
             */
            minecraftVersion = "1.20"

        }
    }
}
```

#### Configure via `gradle.properties`

```properties
almostgradle.recipeViewers.emi.runConfig=true
almostgradle.recipeViewers.emi.mode=API
almostgradle.recipeViewers.emi.version=x.x.x
```

```properties
almostgradle.recipeViewers.rei.runConfig=true
almostgradle.recipeViewers.rei.mode=API
almostgradle.recipeViewers.rei.version=x.x.x
```

```properties
almostgradle.recipeViewers.jei.runConfig=true
almostgradle.recipeViewers.jei.mode=API
almostgradle.recipeViewers.jei.version=x.x.x
```

#### Custom usage

It's also possible to only set the `version` for a recipe viewer and create the dependency on demand. This is useful if
you have special cases, or you want to load the recipe viewer in a specific configuration.

```kts
dependency {
    // Load the basic dependency into `localRuntime`
    localRuntime(almostgradle.recipeViewers.emi.dependency)

    // Or load the API into `compileOnly`
    compileOnly(almostgradle.recipeViewers.emi.apiDependency)
}
```

### Launch Args

#### Resize Client

Default to `false`. When activating, all client run configs will be resized to `1920x1080`, when starting the game.

#### `build.gradle`

```kts
almostgradle.setup {
    // other stuff
    launchArgs {
        resizeClient = true
    }
}
```

#### `gradle.properties`

````properties
almostgradle.launchArgs.resizeClient=true
````

#### Auto World Join

Default to `false`. When activating, the game will automatically join the world when starting the game. The value can be
either `true`, `false` or a world name.

#### `build.gradle`

```kts
almostgradle.setup {
    // other stuff
    launchArgs {
        autoWorldJoin = true
    }
}
```

#### `gradle.properties`

````properties
almostgradle.launchArgs.autoWorldJoin=true
````

### Logging Level

Default to `DEBUG`. Will set the logging level for the mod.

#### `build.gradle`

```kts
almostgradle.setup {
    // other stuff
    launchArgs {
        loggingLevel = "INFO"
    }
}
```

#### `gradle.properties`

````properties
almostgradle.launchArgs.loggingLevel=INFO
````

### Mixin Debug Output

Default to `false`. Will set the Mixin debug for the mod.

#### `build.gradle`

```kts
almostgradle.setup {
    // other stuff
    launchArgs {
        mixinDebugOutput = true
    }
}
```

#### `gradle.properties`

````properties
almostgradle.launchArgs.mixinDebugOutput=true
````
