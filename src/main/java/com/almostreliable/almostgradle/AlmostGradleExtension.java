package com.almostreliable.almostgradle;

import com.almostreliable.almostgradle.dependency.RecipeViewers;
import com.github.gmazzo.buildconfig.BuildConfigExtension;
import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;

public abstract class AlmostGradleExtension {
    public static final String DEFAULT_PACKAGE_DOMAIN = "com.almostreliable";
    public static final String NAME = "almostgradle";

    private final Project project;
    private final RecipeViewers recipeViewers;

    @Inject
    public AlmostGradleExtension(Project project) {
        this.project = project;
        this.recipeViewers = project.getObjects().newInstance(RecipeViewers.class);
        getTestMod().convention(false);
        getDataGen().convention(false);
        getBuildConfig().convention(false);
    }

    public abstract Property<Boolean> getTestMod();

    public abstract Property<Boolean> getBuildConfig();

    public abstract Property<Boolean> getDataGen();

    public RecipeViewers getRecipeViewers() {
        return recipeViewers;
    }

    public void recipeViewers(Action<RecipeViewers> action) {
        action.execute(recipeViewers);
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
        log("📕Setup project through AlmostGradle Plugin...");

        defineBasics();
        Utils.log(project, "\t* Project Version", project.getVersion());
        Utils.log(project, "\t* Project Group", project.getGroup());
        Utils.log(project, "\t* Minecraft Version", getMinecraftVersion());
        Utils.log(project, "\t* Mod Id", getModId());
        Utils.log(project, "\t* Mod Name", getModName());
        Utils.log(project, "\t* Mod Version", getModVersion());

        applyBuildConfig();
        applyBasicMod();
        applyTestMod();
        getRecipeViewers().createRuns();
    }

    private void applyBasicMod() {
        var neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        var javaPlugin = project.getExtensions().getByType(JavaPluginExtension.class);
        neoForge.getVersion().set(getNeoforgeVersion());
        var mainMod = neoForge.getMods().maybeCreate(getModId());
        var mainSourceSet = javaPlugin.getSourceSets().getByName("main");

        mainMod.sourceSet(mainSourceSet);
        neoForge.getRuns().create("client", (run) -> {
            run.client();
            run.getMods().set(Set.of(mainMod));
            Utils.writeRunArguments(run);
        });
        neoForge.getRuns().create("server", (run) -> {
            run.server();
            run.getMods().set(Set.of(mainMod));
            Utils.writeRunArguments(run);
        });

        if (getDataGen().get()) {
            neoForge.getRuns().create("datagen", (run) -> {
                run.data();
                run.getMods().set(Set.of(mainMod));
                Utils.writeRunArguments(run);
                run.getProgramArguments().addAll(
                        "--mod", getModId(),
                        "--all",
                        "--output", project.file("src/generated/resources").getAbsolutePath(),
                        "--existing", project.file("src/main/resources").getAbsolutePath()
                );
            });
        }
    }

    private void defineBasics() {
        if (project.getGroup().toString().isEmpty()) {
            project.setGroup(DEFAULT_PACKAGE_DOMAIN + ".mods");
        }

        String v = project.getVersion().toString();
        if (v.isEmpty() || v.equals("unspecified")) {
            project.setVersion(getMinecraftVersion() + "-" + getModVersion());
        }

        BasePluginExtension base = project.getExtensions().getByType(BasePluginExtension.class);
        base.getArchivesName().set(getModId() + "-neoforge");
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
                packageName = DEFAULT_PACKAGE_DOMAIN + "." + this.getModId();
            }

            buildConfig.packageName(packageName.toString());
            log("📕Applied buildconfig output under package name: " + packageName);
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
                Utils.writeRunArguments(run);
            });
            runs.create("testmod", (run) -> {
                run.client();
                run.getSourceSet().set(testSourceSet);
                run.systemProperty("neoforge.gameTestServer", "true");
                run.systemProperty("neoforge.enabledGameTestNamespaces", modId);
                run.systemProperty(modId + ".example_scripts", exampleScripts);
                Utils.writeRunArguments(run);
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
