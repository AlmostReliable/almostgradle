package com.almostreliable.almostgradle;

import net.neoforged.moddevgradle.dsl.RunModel;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;

public class Utils {

    public static void writeRunArguments(RunModel run) {
        run.jvmArgument("-XX:+IgnoreUnrecognizedVMOptions");
        run.jvmArgument("-XX:+AllowEnhancedClassRedefinition");

        if (run.getType().get().equals("client")) {
            run.getProgramArguments().addAll("--width", "1920", "--height", "1080");
        }
    }

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

    public static Configuration createLocalRuntimeOnly(Project project, String classPathConfigName, @Nullable String prefix) {
        String name = prefix == null ? "localRuntimeOnly" : prefix + "LocalRuntimeOnly";
        return project.getConfigurations().create(name, c -> {
            c.setVisible(true);
            c.setCanBeResolved(true);
            c.setCanBeConsumed(false);
            project.getConfigurations().getByName(classPathConfigName).extendsFrom(c);
        });
    }

    public static void log(Project project, String key, Object value) {
        project.getLogger().lifecycle(String.format("%-25s -> %s", key, value));
    }
}
