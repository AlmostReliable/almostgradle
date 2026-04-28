package com.almostreliable.almostgradle.dependency;

import com.almostreliable.almostgradle.AlmostGradleExtension;
import com.almostreliable.almostgradle.TestSettings;
import com.almostreliable.almostgradle.Utils;
import net.neoforged.moddevgradle.dsl.ModModel;
import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public abstract class RecipeViewers {

    private final RecipeViewerOptions emi;
    private final RecipeViewerOptions jei;
    private final RecipeViewerOptions rei;
    private final Project project;

    @Inject
    public RecipeViewers(Project project) {
        emi = project.getObjects().newInstance(RecipeViewerOptions.class, project, ModDependency.EMI);
        jei = project.getObjects().newInstance(RecipeViewerOptions.class, project, ModDependency.JEI);
        rei = project.getObjects().newInstance(RecipeViewerOptions.class, project, ModDependency.REI);
        this.project = project;
    }

    @Input
    public abstract Property<String> getDefaultViewer();

    public RecipeViewerOptions getEmi() {
        return emi;
    }

    public void emi(Action<? super RecipeViewerOptions> action) {
        action.execute(emi);
    }

    public RecipeViewerOptions getJei() {
        return jei;
    }

    public void jei(Action<? super RecipeViewerOptions> action) {
        action.execute(jei);
    }

    public RecipeViewerOptions getRei() {
        return rei;
    }

    public void rei(Action<? super RecipeViewerOptions> action) {
        action.execute(rei);
    }

    @Internal
    public void createRuns() {
        createRun(emi, ModDependency.EMI);
        createRun(jei, ModDependency.JEI);
        createRun(rei, ModDependency.REI);
    }

    private void createRun(RecipeViewerOptions settings, ModDependency mod) {
        if (!settings.getVersion().isPresent()) {
            return;
        }

        var logger = project.getLogger();
        logger.lifecycle("📕Start initializing RecipeViewer " + mod.shortId());

        Utils.log(project, "\t* Version", settings.getVersion().get());
        Utils.log(project, "\t* Mode", settings.getMode().get().toString());
        Utils.log(project, "\t* Run Config Enabled", settings.getRunConfig().get());
        Utils.log(project, "\t* Test Mod Enabled", settings.getTestMod().get());
        Utils.log(project, "\t* Minecraft Version", settings.getMinecraftVersion().orElse("NOT_DEFINED").get());
        Utils.log(project, "\t* Repository", settings.getMavenRepository().get());

        var repo = settings.getMavenRepository().get();
        if (Utils.isMissingMavenRepository(this.project, repo)) {
            logger.lifecycle("\t* Repository missing, will be added automatically");
            this.project.getRepositories().maven((m) -> {
                m.setUrl(repo);
            });
        }

        var neoForge = this.project.getExtensions().getByType(NeoForgeExtension.class);
        var java = this.project.getExtensions().getByType(JavaPluginExtension.class);
        var almostGradle = this.project.getExtensions().getByType(AlmostGradleExtension.class);
        var mainMod = neoForge.getMods().maybeCreate(almostGradle.getModId());
        var mainSourceSet = java.getSourceSets().getByName("main");

        var deps = settings.getDependencies();
        var apiDeps = settings.getApiDependencies();

        if (settings.getRunConfig().isPresent() && settings.getRunConfig().get()) {
            var sourceSet = java.getSourceSets().create("client_" + mod.shortId().toLowerCase(Locale.ROOT));

            var compileClasspath = sourceSet.getCompileClasspath()
                    .plus(mainSourceSet.getCompileClasspath());
            var runtimeClasspath = sourceSet.getRuntimeClasspath()
                    .plus(mainSourceSet.getRuntimeClasspath());

            Set<ModModel> loadedMods = new HashSet<>();
            loadedMods.add(mainMod);

            if (almostGradle.getTestSettings().getTestMod().get() && settings.getTestMod().get()) {
                var testMod = neoForge.getMods().maybeCreate(TestSettings.TESTMOD_ID);
                var testSourceSet = java.getSourceSets().getByName("test");

                compileClasspath = compileClasspath.plus(testSourceSet.getCompileClasspath());
                runtimeClasspath = runtimeClasspath.plus(testSourceSet.getRuntimeClasspath());
                loadedMods.add(testMod);
            }

            sourceSet.setCompileClasspath(compileClasspath);
            sourceSet.setRuntimeClasspath(runtimeClasspath);

            neoForge.getRuns().create(sourceSet.getName(), run -> {
                run.getIdeName().set("Client (" + mod.shortId() + ")");
                run.client();
                run.getSourceSet().set(sourceSet);
                run.getLoadedMods().set(loadedMods);
            });

            var config = Utils.createLocalRuntime(project,
                    sourceSet.getRuntimeClasspathConfigurationName(),
                    mod.shortId().toLowerCase(Locale.ROOT));
            config.withDependencies(d -> d.addAllLater(deps));
        }

        var runtimeOnly = project.getConfigurations().getByName("localRuntime");
        runtimeOnly.resolutionStrategy(ResolutionStrategy::failOnVersionConflict);
        var compileOnly = project.getConfigurations().getByName(mainSourceSet.getCompileOnlyConfigurationName());
        compileOnly.resolutionStrategy(ResolutionStrategy::failOnVersionConflict);

        switch (settings.getMode().get()) {
            case API -> {
                compileOnly.withDependencies(d -> d.addAllLater(apiDeps));
            }
            case FULL -> {
                compileOnly.withDependencies(d -> d.addAllLater(deps));
            }
        }
    }
}
