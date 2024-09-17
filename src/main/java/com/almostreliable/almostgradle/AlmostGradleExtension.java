package com.almostreliable.almostgradle;

import com.almostreliable.almostgradle.dependency.RecipeViewers;
import com.github.gmazzo.buildconfig.BuildConfigExtension;
import net.neoforged.moddevgradle.dsl.ModModel;
import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.SourceSet;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;

public abstract class AlmostGradleExtension {
    public static final String NAME = "almostgradle";
    public static final String MAVEN = "mavenJava";

    private final Project project;
    private final RecipeViewers recipeViewers;
    private final LaunchArgs launchArgs;

    @Inject
    public AlmostGradleExtension(Project project) {
        this.project = project;
        this.recipeViewers = project.getObjects().newInstance(RecipeViewers.class);
        this.launchArgs = project.getObjects().newInstance(LaunchArgs.class);
        var providers = project.getProviders();

        getTestMod().convention(false);
        getApiSourceSet().convention(false);
        getMavenPublish().convention(false);
        getDataGen().set(providers.gradleProperty("datagen").map(s -> {
            if (s.equals("true")) return true;
            if (s.equals("false")) return false;
            return s;
        }).orElse(false));

        getWithSourcesJar().convention(true);
        getBuildConfig().convention(true);
        getProcessResources().set(true);
    }

    public abstract Property<Boolean> getProcessResources();

    public abstract Property<Boolean> getWithSourcesJar();

    public abstract Property<Boolean> getTestMod();

    public abstract Property<Boolean> getBuildConfig();

    public abstract Property<Object> getDataGen();

    public abstract Property<Boolean> getApiSourceSet();

    public abstract Property<Boolean> getMavenPublish();

    public RecipeViewers getRecipeViewers() {
        return recipeViewers;
    }

    public void recipeViewers(Action<RecipeViewers> action) {
        action.execute(recipeViewers);
    }

    public LaunchArgs getLaunchArgs() {
        return launchArgs;
    }

    public void launchArgs(Action<LaunchArgs> action) {
        action.execute(launchArgs);
    }

    public String getNeoforgeVersion() {
        return this.getProperty("neoforgeVersion");
    }

    public String getModId() {
        return this.getProperty("modId");
    }

    public String getModName() {
        return this.getProperty("modName");
    }

    public String getModVersion() {
        return this.getProperty("modVersion");
    }

    public String getMinecraftVersion() {
        return this.getProperty("minecraftVersion");
    }

    public void setup(Action<AlmostGradleExtension> onSetup) {
        onSetup.execute(this);
        log("📕Setting up project through AlmostGradle plugin...");

        applyBasics();
        Utils.log(project, "\t* Project Version", project.getVersion());
        Utils.log(project, "\t* Project Group", project.getGroup());
        Utils.log(project, "\t* Minecraft Version", getMinecraftVersion());
        Utils.log(project, "\t* Mod Id", getModId());
        Utils.log(project, "\t* Mod Name", getModName());
        Utils.log(project, "\t* Mod Version", getModVersion());

        createProcessResourcesTask();
        applyBuildConfig();
        applyApiSourceSet();
        applyBasicMod();
        applyTestMod();
        getRecipeViewers().createRuns();
        onPostRunConfigs();
    }

    private void createProcessResourcesTask() {
        if (getProcessResources().get()) {
            project
                    .getTasks()
                    .named("processResources", ProcessResources.class)
                    .configure(new ProcessResourceHandler(project));
        }
    }

    private void onPostRunConfigs() {
        log("📕Generated run configs:");
        var neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        neoForge.getRuns().forEach(run -> {
            launchArgs.applyRunArguments(run);
            log("\t* " + run.getIdeName().get());
        });
    }

    private void applyApiSourceSet() {
        if (!getApiSourceSet().get()) {
            return;
        }

        var javaPlugin = project.getExtensions().getByType(JavaPluginExtension.class);
        var main = javaPlugin.getSourceSets().getByName("main");
        var api = javaPlugin.getSourceSets().create("api");
        var neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        neoForge.addModdingDependenciesTo(api);
        project.getDependencies().add(main.getImplementationConfigurationName(), api.getOutput());

        var apiJar = project.getTasks().register("apiJar", Jar.class, jar -> {
            jar.getArchiveClassifier().set("api");
            jar.from(api.getOutput());
        });

        var apiSources = project.getTasks().register("apiSources", Jar.class, jar -> {
            jar.getArchiveClassifier().set("api-sources");
            jar.from(api.getAllJava());
        });

        project.getTasks().named("jar", Jar.class, jar -> {
            jar.dependsOn(apiJar);
            jar.from(api.getOutput());
        });

        project.artifacts(a -> {
            a.add("archives", apiJar);
            a.add("archives", apiSources);
        });

        var maven = project.getExtensions().getByType(PublishingExtension.class);
        maven.getPublications().withType(MavenPublication.class).configureEach(pub -> {
            pub.artifact(apiJar);
            pub.artifact(apiSources);
        });

        if (getWithSourcesJar().get()) {
            project.getTasks().named("sourcesJar", Jar.class, jar -> {
                jar.from(api.getAllJava());
            });
        }
    }

    private void applyBasicMod() {
        var neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        var javaPlugin = project.getExtensions().getByType(JavaPluginExtension.class);
        neoForge.getVersion().set(getNeoforgeVersion());
        var mainMod = neoForge.getMods().maybeCreate(getModId());
        var mainSourceSet = javaPlugin.getSourceSets().getByName("main");

        mainMod.sourceSet(mainSourceSet);
        if (getApiSourceSet().get()) {
            mainMod.sourceSet(javaPlugin.getSourceSets().getByName("api"));
        }

        neoForge.getRuns().create("client", (run) -> {
            run.client();
            run.getLoadedMods().set(Set.of(mainMod));
        });
        neoForge.getRuns().create("server", (run) -> {
            run.server();
            run.getLoadedMods().set(Set.of(mainMod));
        });

        applyDataGen(neoForge, mainMod, mainSourceSet);
    }

    private void applyDataGen(NeoForgeExtension neoForge, ModModel mainMod, SourceSet mainSourceSet) {
        Object o = getDataGen().get();

        String generatedPath;
        if (o instanceof Boolean b) {
            if (!b) return;
            generatedPath = "src/generated/resources";
        } else if (o instanceof String s) {
            generatedPath = s;
        } else {
            generatedPath = "";
        }

        mainSourceSet.resources(sourceSet -> {
            sourceSet.srcDir(generatedPath);
            sourceSet.exclude("**/.cache/**");
        });
        neoForge.getRuns().create("datagen", (run) -> {
            run.data();
            run.getLoadedMods().set(Set.of(mainMod));
            run
                    .getProgramArguments()
                    .addAll("--mod",
                            getModId(),
                            "--all",
                            "--output",
                            project.file(generatedPath).getAbsolutePath(),
                            "--existing",
                            project.file("src/main/resources").getAbsolutePath());
        });

        log("📕Applied datagen output under: " + generatedPath.replace('/', '.'));
    }

    private void applyBasics() {
        if (project.getGroup().toString().isEmpty()) {
            throw new GradleException("Project group cannot be empty!");
        }

        String v = project.getVersion().toString();
        if (v.isEmpty() || v.equals("unspecified")) {
            project.setVersion(getMinecraftVersion() + "-" + getModVersion());
        }

        BasePluginExtension base = project.getExtensions().getByType(BasePluginExtension.class);
        base.getArchivesName().set(getModId() + "-neoforge");

        if (getWithSourcesJar().get()) {
            var javaPlugin = project.getExtensions().getByType(JavaPluginExtension.class);
            javaPlugin.withSourcesJar();
        }

        if (getMavenPublish().get()) {
            project.getPlugins().apply("maven-publish");
            var maven = project.getExtensions().getByType(PublishingExtension.class);
            var pub = maven.getPublications().create(MAVEN, MavenPublication.class);
            pub.from(project.getComponents().getByName("java"));
        }
    }

    private void applyBuildConfig() {
        if (!getBuildConfig().get()) {
            return;
        }

        try {
            String id = "com.github.gmazzo.buildconfig";
            String v = "5.4.0";
            if (!this.project.getPlugins().hasPlugin(id)) {
                project.getBuildscript().getDependencies().add("classpath", id + ":" + v);
                project.getPlugins().apply(id);
            }

            var buildConfig = project.getExtensions().getByType(BuildConfigExtension.class);
            buildConfig.useJavaOutput();
            buildConfig.buildConfigField("String", "MOD_ID", "\"" + this.getModId() + "\"");
            buildConfig.buildConfigField("String", "MOD_NAME", "\"" + this.getModName() + "\"");
            buildConfig.buildConfigField("String", "MOD_VERSION", "\"" + project.getVersion() + "\"");

            Object packageName = project.findProperty(NAME + ".buildconfig.package");
            if (packageName == null) {
                packageName = project.getGroup() + "." + getModId();
            }

            buildConfig.packageName(packageName.toString());

            Object className = project.findProperty(NAME + ".buildconfig.name");
            if (className != null) {
                buildConfig.className(className.toString());
            }

            log("📕Applied buildconfig output under: " + packageName +
                (className == null ? ".BuildConfig" : "." + className));
        } catch (Exception e) {
            project.getLogger().error("... Failed to apply buildconfig", e);
        }
    }

    private void applyTestMod() {
        if (!getTestMod().get()) {
            return;
        }

        var javaPlugin = this.project.getExtensions().getByType(JavaPluginExtension.class);
        var neoForge = this.project.getExtensions().getByType(NeoForgeExtension.class);
        var testSourceSet = javaPlugin.getSourceSets().getByName("test");
        var modId = this.getModId();
        neoForge.mods((mods) -> {
            mods.create("testmod", (mod) -> {
                mod.sourceSet(testSourceSet);
            });
        });

        neoForge.addModdingDependenciesTo(testSourceSet);
        neoForge.runs((runs) -> {
            var exampleScripts = this.project.getRootDir().toPath().resolve("example_scripts").toString();
            runs.create("gametest", (run) -> {
                run.server();
                run.getSourceSet().set(testSourceSet);
                run.systemProperty("neoforge.gameTestServer", "true");
                run.systemProperty("neoforge.enabledGameTestNamespaces", modId);
                run.systemProperty(modId + ".example_scripts", exampleScripts);
            });
            runs.create("testmod", (run) -> {
                run.client();
                run.getSourceSet().set(testSourceSet);
                run.systemProperty("neoforge.gameTestServer", "true");
                run.systemProperty("neoforge.enabledGameTestNamespaces", modId);
                run.systemProperty(modId + ".example_scripts", exampleScripts);
            });
        });
    }

    public String getProperty(String propertyName) {
        return Optional
                .ofNullable(this.project.findProperty(propertyName))
                .map(Object::toString)
                .orElseThrow(() -> new RuntimeException("Property " + propertyName + " is missing!"));
    }

    private void log(String msg) {
        project.getLogger().lifecycle(msg);
    }
}
