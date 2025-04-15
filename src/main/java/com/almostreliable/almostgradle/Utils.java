package com.almostreliable.almostgradle;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.regex.Pattern;

public class Utils {

    private static final Pattern VERSION_SUFFIX_PATTERN = Pattern.compile("-.*$");

    public static boolean isMissingMavenRepository(Project project, String repository) {
        try {
            URI uri = new URI(repository);
            for (ArtifactRepository repo : project.getRepositories()) {
                if (repo instanceof MavenArtifactRepository maven && maven.getUrl().equals(uri)) {
                    return false;
                }
            }
        } catch (URISyntaxException e) {
            return true;
        }

        return true;
    }

    public static Configuration createLocalRuntime(Project project, String classPathConfigName, @Nullable String prefix) {
        String name = prefix == null ? "localRuntime" : prefix + "LocalRuntime";
        return project.getConfigurations().create(name, c -> {
            c.setVisible(true);
            c.setCanBeResolved(true);
            c.setCanBeConsumed(false);
            project.getConfigurations().getByName(classPathConfigName).extendsFrom(c);
        });
    }

    public static void createLocalImplementation(Project project, String compileClassPathConfigName, String runtimeClassPathConfigName, @Nullable String prefix) {
        String name = prefix == null ? "localImplementation" : prefix + "LocalImplementation";
        project.getConfigurations().create(name, c -> {
            c.setVisible(true);
            c.setCanBeResolved(true);
            c.setCanBeConsumed(false);
            project.getConfigurations().getByName(compileClassPathConfigName).extendsFrom(c);
            project.getConfigurations().getByName(runtimeClassPathConfigName).extendsFrom(c);
        });
    }

    public static void ensureMinimalPluginVersion(Project project, String pluginId, String minimumVersion) {
        Set<ResolvedDependency> dependencies = project
                .getBuildscript()
                .getConfigurations()
                .getByName("classpath")
                .getResolvedConfiguration()
                .getFirstLevelModuleDependencies();

        for (ResolvedDependency dependency : dependencies) {
            if (!dependency.getModuleGroup().equals(pluginId)) {
                continue;
            }

            String dependencyVersion = dependency.getModuleVersion();
            if (!isVersionAtLeast(dependencyVersion, minimumVersion)) {
                throw new GradleException("Plugin '" + pluginId + "' version " + dependencyVersion +
                                          " is less than the minimum required version " + minimumVersion + "!");
            }

            return;
        }

        throw new GradleException("Required plugin '" + pluginId + "' with minimum version " + minimumVersion +
                                  " not found!");
    }

    private static boolean isVersionAtLeast(String version, String minimumVersion) {
        String[] versionParts = VERSION_SUFFIX_PATTERN.matcher(version).replaceAll("").split("\\.");
        String[] minimumParts = VERSION_SUFFIX_PATTERN.matcher(minimumVersion).replaceAll("").split("\\.");

        int length = Math.max(versionParts.length, minimumParts.length);
        for (int i = 0; i < length; i++) {
            int vPart = i < versionParts.length ? Integer.parseInt(versionParts[i]) : 0;
            int mPart = i < minimumParts.length ? Integer.parseInt(minimumParts[i]) : 0;

            if (vPart < mPart) {
                return false;
            }
            if (vPart > mPart) {
                return true;
            }
        }

        return true;
    }

    public static void log(Project project, String key, Object value) {
        project.getLogger().lifecycle(String.format("%-25s -> %s", key, value));
    }
}
