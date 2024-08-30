package com.almostreliable.almostgradle.dependency;

import com.almostreliable.almostgradle.Utils;
import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

import javax.inject.Inject;

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
        project.getLogger().lifecycle("EMI: " + emi.getVersion().get());
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
        logger.lifecycle("📕Start initializing RecipeViewer for " + mod.id().toUpperCase());

        Utils.log(project, "\t* Version", settings.getVersion().get());
        Utils.log(project, "\t* Mode", settings.getMode().get().toString());
        Utils.log(project, "\t* Run Config Enabled", settings.getRunConfig().get());
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
        var mainSourceSet = java.getSourceSets().getByName("main");

        var dep = settings.getDependency();
        var apiDep = settings.getApiDependency();

        if (settings.getRunConfig().isPresent() && settings.getRunConfig().get()) {
            var sourceSet = java.getSourceSets().create(mod.id() + "Run");
            sourceSet.setCompileClasspath(sourceSet.getCompileClasspath().plus(mainSourceSet.getCompileClasspath()));
            sourceSet.setRuntimeClasspath(sourceSet.getRuntimeClasspath().plus(mainSourceSet.getRuntimeClasspath()));

            neoForge.getRuns().create(sourceSet.getName(), (run) -> {
                run.getIdeName().set("RecipeViewer (" + mod.id().toUpperCase() + ")");
                run.client();
                run.getSourceSet().set(sourceSet);
            });

            var config = Utils.createLocalRuntime(project,
                    sourceSet.getRuntimeClasspathConfigurationName(),
                    mod.id());
            config.withDependencies(d -> d.addLater(dep));
        }

        var runtimeOnly = project.getConfigurations().getByName("localRuntime");
        runtimeOnly.resolutionStrategy(ResolutionStrategy::failOnVersionConflict);
        var compileOnly = project.getConfigurations().getByName(mainSourceSet.getCompileOnlyConfigurationName());
        compileOnly.resolutionStrategy(ResolutionStrategy::failOnVersionConflict);

        switch (settings.getMode().get()) {
            case API -> {
                compileOnly.withDependencies(d -> d.addLater(apiDep));
            }
            case FULL -> {
                compileOnly.withDependencies(d -> d.addLater(dep));
            }
        }
    }
}
