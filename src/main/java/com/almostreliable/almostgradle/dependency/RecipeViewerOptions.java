package com.almostreliable.almostgradle.dependency;


import com.almostreliable.almostgradle.AlmostGradleExtension;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

import javax.inject.Inject;

@SuppressWarnings("UnstableApiUsage")
public abstract class RecipeViewerOptions {

    private final Project project;
    private final ModDependency mod;

    @Inject
    public RecipeViewerOptions(Project project, ModDependency mod) {
        this.project = project;
        this.mod = mod;
        ProviderFactory providers = project.getProviders();
        var propPrefix = AlmostGradleExtension.NAME + ".recipeViewers." + mod.id();
        getMavenRepository().convention(providers
                .gradleProperty(propPrefix + ".maven")
                .orElse(mod.defaultMavenRepo()));
        getVersion().convention(providers.gradleProperty(propPrefix + ".version"));
        getMode().convention(providers
                .gradleProperty(propPrefix + ".mode")
                .map(LoadingMode::fromString)
                .orElse(LoadingMode.NONE));
        getRunConfig().convention(providers
                .gradleProperty(propPrefix + ".runConfig")
                .map(s -> s.equals("true"))
                .orElse(false));
        getMinecraftVersion().convention(providers.gradleProperty(propPrefix + ".minecraftVersion"));
    }

    public abstract Property<String> getMavenRepository();

    public abstract Property<String> getVersion();

    public abstract Property<LoadingMode> getMode();

    public abstract Property<Boolean> getRunConfig();

    public abstract Property<String> getMinecraftVersion();

    public Provider<ModuleDependency> getDependency() {
        var almostGradle = project.getExtensions().getByType(AlmostGradleExtension.class);
        var mcv = getMinecraftVersion().orElse(almostGradle.getMinecraftVersion()).get();
        return getVersion().map(v -> mod.createDependency(mcv, v, project.getDependencyFactory()));
    }

    public Provider<ModuleDependency> getApiDependency() {
        var almostGradle = project.getExtensions().getByType(AlmostGradleExtension.class);
        var mcv = getMinecraftVersion().orElse(almostGradle.getMinecraftVersion()).get();
        return getVersion().map(v -> mod.createApiDependency(mcv, v, project.getDependencyFactory()));
    }
}
