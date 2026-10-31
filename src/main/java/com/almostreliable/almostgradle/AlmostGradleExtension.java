package com.almostreliable.almostgradle;

import com.almostreliable.almostgradle.dependency.RecipeViewers;
import com.github.gmazzo.buildconfig.BuildConfigExtension;
import net.neoforged.moddevgradle.dsl.ModModel;
import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import net.neoforged.moddevgradle.dsl.RunModel;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.SourceSet;
import org.gradle.jvm.tasks.Jar;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.jetbrains.gradle.ext.Gradle;
import org.jetbrains.gradle.ext.IdeaExtPlugin;
import org.jetbrains.gradle.ext.ProjectSettings;
import org.jetbrains.gradle.ext.RunConfigurationContainer;

import javax.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public abstract class AlmostGradleExtension {
    public static final String NAME = "almostgradle";
    public static final String MAVEN = "mavenJava";
    public static final int DEFAULT_JAVA_VERSION = 25;

    private final Project project;
    private final RecipeViewers recipeViewers;
    private final LaunchArgs launchArgs;
    private final TestSettings testSettings;

    @Inject
    public AlmostGradleExtension(Project project) {
        this.project = project;
        this.recipeViewers = project.getObjects().newInstance(RecipeViewers.class);
        this.launchArgs = project.getObjects().newInstance(LaunchArgs.class);
        this.testSettings = project.getObjects().newInstance(TestSettings.class);
        var providers = project.getProviders();

        getModPackage().convention(project.getGroup() + "." + getModId());
        getJavaVersion().convention(DEFAULT_JAVA_VERSION);
        getMavenPublish().convention(false);
        getSplitRunDirs().convention(true);
        getDownloadJavadoc().convention(false);
        getDownloadSources().convention(true);
        getDataGen().set(providers.gradleProperty(NAME + ".datagen").map(s -> {
            if (s.equals("true")) return true;
            if (s.equals("false")) return false;
            return s;
        }).orElse(false));

        getWithSourcesJar().convention(true);
        getWithApiJar().convention(false);
        getWithAccessTransformerValidation().convention(true);
        getAccessTransformerPublish().convention(false);
        getBuildConfig().set(providers.gradleProperty(NAME + ".buildconfig").map(s -> {
            if (s.equals("true")) return true;
            if (s.equals("false")) return false;
            return s;
        }).orElse(true));
        getProcessResources().set(true);
        getProcessResourceTargets().convention(List.of());
    }

    public abstract Property<String> getModPackage();

    public abstract Property<Integer> getJavaVersion();

    public abstract Property<Boolean> getProcessResources();

    public abstract ListProperty<String> getProcessResourceTargets();

    public abstract Property<Boolean> getWithSourcesJar();

    public abstract Property<Boolean> getWithApiJar();

    public abstract Property<Boolean> getWithAccessTransformerValidation();

    public abstract Property<Boolean> getAccessTransformerPublish();

    public abstract Property<Object> getBuildConfig();

    public abstract Property<Object> getDataGen();

    public abstract Property<Boolean> getMavenPublish();

    public abstract Property<Boolean> getSplitRunDirs();

    public abstract Property<Boolean> getDownloadJavadoc();

    public abstract Property<Boolean> getDownloadSources();

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

    public TestSettings getTestSettings() {
        return testSettings;
    }

    public void tests(Action<TestSettings> action) {
        testSettings.enableTests();
        action.execute(testSettings);
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

    public String getPackage() {
        if (project.findProperty("modPackage") != null) {
            return getProperty("modPackage");
        }
        return getModPackage().get();
    }

    public String getMinecraftVersion() {
        return this.getProperty("minecraftVersion");
    }

    public void setup(Action<AlmostGradleExtension> onSetup) {
        Utils.ensureMinimalGradleVersion(project, BuildConfig.MINIMUM_GRADLE_VERSION);
        Utils.ensureMinimalPluginVersion(project, "net.neoforged.moddev", BuildConfig.MINIMUM_MDG_VERSION);

        onSetup.execute(this);
        log("📕Setting up project through AlmostGradle v" + BuildConfig.VERSION + " ...");

        applyBasics();
        Utils.log(project, "\t* Project Version", project.getVersion());
        Utils.log(project, "\t* Project Group", project.getGroup());
        Utils.log(project, "\t* Minecraft Version", getMinecraftVersion());
        Utils.log(project, "\t* Mod Id", getModId());
        Utils.log(project, "\t* Mod Name", getModName());
        Utils.log(project, "\t* Mod Version", getModVersion());

        createProcessResourcesTask();
        applyBuildConfig();
        applyIdeaDownloads();
        applyBasicMod();
        getTestSettings().apply();
        getRecipeViewers().createRuns();
        onPostRunConfigs();
    }

    private void createProcessResourcesTask() {
        if (getProcessResources().get()) {
            project
                    .getTasks()
                    .named("processResources", ProcessResources.class)
                    .configure(new ProcessResourceHandler(project, getRecipeViewers(), getProcessResourceTargets().get()));
        }
    }

    private void onPostRunConfigs() {
        log("📕Generated run configs:");
        var neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        var tmpDir = project.getLayout().getBuildDirectory().dir("tmp").get().getAsFile().toPath();
        neoForge.getRuns().forEach(run -> {
            launchArgs.applyRunArguments(run);

            var isTmpRun = run.getGameDirectory().get().getAsFile().toPath().startsWith(tmpDir);
            if (getSplitRunDirs().get() && !isTmpRun) {
                var dir = run.getGameDirectory().get().dir(run.getName());
                run.getGameDirectory().set(dir);
            }
            log("\t* " + run.getIdeName().get());
        });
    }

    private void applyBasicMod() {
        var neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        var javaPlugin = project.getExtensions().getByType(JavaPluginExtension.class);
        neoForge.setVersion(getNeoforgeVersion());
        var mainMod = neoForge.getMods().maybeCreate(getModId());
        var mainSourceSet = javaPlugin.getSourceSets().getByName("main");

        mainMod.sourceSet(mainSourceSet);

        if (getWithAccessTransformerValidation().get()) {
            neoForge.getValidateAccessTransformers().set(true);
        }
        if (getAccessTransformerPublish().get()) {
            neoForge.getAccessTransformers().publish(project.file("src/main/resources/META-INF/accesstransformer.cfg"));
        }

        neoForge.getRuns().create("client", run -> {
            run.client();
            run.getLoadedMods().set(Set.of(mainMod));
            run.systemProperty(TestSettings.GAME_TEST_PROPERTY, "false");
        });
        neoForge.getRuns().create("server", run -> {
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

        var generatedDir = project.file(generatedPath);
        mainSourceSet.resources(sourceSet -> sourceSet.exclude("**/.cache/**"));

        // each side needs its own output because a data run deletes every file in its output that it did not generate
        createDataGenRun(
                neoForge,
                mainMod,
                mainSourceSet,
                generatedDir,
                "client",
                "DataGen (Client)",
                RunModel::clientData
        );
        createDataGenRun(
                neoForge,
                mainMod,
                mainSourceSet,
                generatedDir,
                "server",
                "DataGen (Server)",
                RunModel::serverData
        );
        createCombinedDataGenRun();

        log("📕Applied datagen output under: " + generatedPath.replace('/', '.'));
    }

    private void createDataGenRun(
            NeoForgeExtension neoForge,
            ModModel mainMod,
            SourceSet mainSourceSet,
            File generatedDir,
            String side,
            String ideName,
            Action<RunModel> runType
    ) {
        var outputDir = new File(generatedDir, side);
        mainSourceSet.getResources().srcDir(outputDir);

        neoForge.getRuns().create("datagen_" + side, run -> {
            runType.execute(run);
            run.getGameDirectory()
                    .set(project.getLayout().getBuildDirectory().dir("tmp").get().dir("datagenRuns").dir(side));
            run.getLoadedMods().set(Set.of(mainMod));
            run.getIdeName().set(ideName);
            run.getProgramArguments().addAll(
                    "--mod",
                    getModId(),
                    "--all",
                    "--output",
                    outputDir.getAbsolutePath(),
                    "--existing",
                    project.file("src/main/resources").getAbsolutePath()
            );
        });
    }

    private void createCombinedDataGenRun() {
        var clientTask = "runDatagen_client";
        var serverTask = "runDatagen_server";
        var dataGenTask = project.getTasks().register("runDatagen", task -> {
            task.setGroup("mod development");
            task.setDescription("Runs the client and server data generation.");
            task.dependsOn(clientTask, serverTask);
        });
        project.getTasks().configureEach(task -> {
            if (task.getName().equals(serverTask)) {
                task.mustRunAfter(clientTask);
            }
        });

        var rootProject = project.getRootProject();
        rootProject.getPlugins().withType(IdeaExtPlugin.class, plugin -> {
            var ideaProject = rootProject.getExtensions().getByType(IdeaModel.class).getProject();
            if (ideaProject == null) return;

            var projectSettings = ((ExtensionAware) ideaProject).getExtensions().getByType(ProjectSettings.class);
            var runConfigurations = ((ExtensionAware) projectSettings).getExtensions().getByType(RunConfigurationContainer.class);
            var gradleRun = new Gradle("DataGen (All)");
            gradleRun.setProject(project);
            gradleRun.setTaskNames(List.of(dataGenTask.getName()));
            runConfigurations.add(gradleRun);
        });
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

        JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        java.toolchain(spec -> spec.getLanguageVersion().set(JavaLanguageVersion.of(getJavaVersion().get())));

        if (getWithSourcesJar().get()) {
            var javaPlugin = project.getExtensions().getByType(JavaPluginExtension.class);
            javaPlugin.withSourcesJar();
        }

        if (getWithApiJar().get()) {
            applyApiJar();
        }

        if (getMavenPublish().get()) {
            project.getPlugins().apply("maven-publish");
            var maven = project.getExtensions().getByType(PublishingExtension.class);

            var pub = maven.getPublications().create(MAVEN, MavenPublication.class);
            pub.from(project.getComponents().getByName("java"));
            pub.setArtifactId(getModId());

            var apiJar = project.getTasks().named("apiJar", Jar.class);
            var apiSources = project.getTasks().named("apiSources", Jar.class);
            maven.getPublications().withType(MavenPublication.class).configureEach(p -> {
                p.artifact(apiJar);
                p.artifact(apiSources);
            });
        }
    }

    private void applyIdeaDownloads() {
        if (getDownloadSources().get() || getDownloadJavadoc().get()) {
            project.getPlugins().apply("idea");
        }

        project.getPlugins().withId("idea", plugin -> {
            var idea = project.getExtensions().getByType(IdeaModel.class);
            var module = idea.getModule();
            module.setDownloadSources(getDownloadSources().get());
            module.setDownloadJavadoc(getDownloadJavadoc().get());
        });
    }

    private void applyApiJar() {
        var javaPlugin = project.getExtensions().getByType(JavaPluginExtension.class);
        var mainTask = project.getTasks().named("jar", Jar.class);
        var main = javaPlugin.getSourceSets().getByName("main");
        var apiPath = getPackage().replace('.', '/') + "/api/**";

        var apiJar = project.getTasks().register("apiJar", Jar.class, jar -> {
            jar.getArchiveClassifier().set("api");
            jar.dependsOn(mainTask);
            jar.from(main.getOutput());
            jar.include(apiPath);
        });

        var apiSources = project.getTasks().register("apiSources", Jar.class, jar -> {
            jar.getArchiveClassifier().set("api-sources");
            jar.dependsOn(mainTask);
            jar.from(main.getAllJava());
            jar.include(apiPath);
        });

        project.artifacts(a -> {
            a.add("archives", apiJar);
            a.add("archives", apiSources);
        });
    }

    private void applyBuildConfig() {
        Object o = getBuildConfig().get();

        String fileName;
        if (o instanceof Boolean b) {
            if (!b) return;
            fileName = "BuildConfig";
        } else if (o instanceof String s) {
            fileName = s;
        } else {
            throw new GradleException("Invalid value for BuildConfig property: " + o);
        }

        try {
            String buildConfigPluginId = "com.github.gmazzo.buildconfig";
            if (!project.getPlugins().hasPlugin(buildConfigPluginId)) {
                project.getPlugins().apply(buildConfigPluginId);
            }

            var buildConfig = project.getExtensions().getByType(BuildConfigExtension.class);
            buildConfig.useJavaOutput();
            buildConfig.buildConfigField("String", "MOD_ID", "\"" + getModId() + "\"");
            buildConfig.buildConfigField("String", "MOD_NAME", "\"" + getModName() + "\"");
            buildConfig.buildConfigField("String", "MOD_VERSION", "\"" + project.getVersion() + "\"");

            buildConfig.className(fileName);
            var modPackage = getPackage();
            buildConfig.packageName(modPackage);

            log("📕Applied buildconfig output under: " + modPackage + "." + fileName);
        } catch (Exception e) {
            project.getLogger().error("... Failed to apply buildconfig", e);
        }
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
