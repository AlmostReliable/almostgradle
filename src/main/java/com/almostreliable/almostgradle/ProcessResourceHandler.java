package com.almostreliable.almostgradle;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.language.jvm.tasks.ProcessResources;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

public class ProcessResourceHandler implements Action<ProcessResources> {

    public static final String USER_KEY = "githubUser";
    public static final String REPO_KEY = "githubRepo";

    private final List<String> targets = List.of("META-INF/neoforge.mods.toml", "pack.mcmeta");

    private final Project project;

    public ProcessResourceHandler(Project project) {
        this.project = project;
    }

    @Override
    public void execute(ProcessResources processResources) {
        project.getLogger().lifecycle("📕Start process resource handling...");
        var keys = getPossibleKeys();
        project.getLogger().lifecycle("\t* Found keys: " + keys);
        var properties = createProperties(keys);
        project.getLogger().lifecycle("\t* Properties:");
        properties.forEach((k, v) -> {
            Utils.log(project, "\t\t* " + k, v);
        });

        processResources.getInputs().properties(properties);
        processResources.filesMatching(targets, fileCopyDetails -> fileCopyDetails.expand(properties));
    }

    private Collection<String> getPossibleKeys() {
        Set<String> keys = new HashSet<>();
        Pattern pattern = Pattern.compile("\\$\\{(.+?)}");

        SourceSet mainSourceSet = project
                .getExtensions()
                .getByType(JavaPluginExtension.class)
                .getSourceSets()
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        List<File> files = getTargetFilesInSourceSet(mainSourceSet);

        for (File file : files) {
            try (var reader = new Scanner(file)) {
                while (reader.hasNextLine()) {
                    String line = reader.nextLine();
                    Matcher matcher = pattern.matcher(line);
                    while (matcher.find()) {
                        keys.add(matcher.group(1));
                    }
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        return keys.stream().sorted().toList();
    }

    private List<File> getTargetFilesInSourceSet(SourceSet mainSourceSet) {
        return StreamSupport
                .stream(mainSourceSet.getResources().getSourceDirectories().spliterator(), false)
                .flatMap(dir -> targets.stream().map(target -> new File(dir, target)))
                .filter(File::exists)
                .toList();
    }

    private Optional<String> getPropertyValue(String key) {
        Object property = project.findProperty(key);
        if (property == null) {
            var logger = project.getLogger();
            if (key.equals(REPO_KEY)) {
                logger.lifecycle("\t* Property '" + REPO_KEY + "' found in target  but not set, defaulting to mod id");
                var almostGradle = project.getExtensions().getByType(AlmostGradleExtension.class);
                return Optional.of(almostGradle.getModId());
            }

            if (key.equals(USER_KEY)) {
                logger.lifecycle(
                        "\t* Property '" + USER_KEY + "' found in target but not set, defaulting to 'AlmostReliable'");
                return Optional.of("AlmostReliable");
            }
        }

        return Optional.ofNullable(property).map(Object::toString);
    }

    private Map<String, String> createProperties(Collection<String> keys) {
        Set<String> missingProperties = new HashSet<>();
        Map<String, String> properties = new LinkedHashMap<>();

        for (var key : keys) {
            getPropertyValue(key).ifPresentOrElse(s -> properties.put(key, s), () -> missingProperties.add(key));
        }

        if (!missingProperties.isEmpty()) {
            throw new IllegalStateException("Missing properties: " + missingProperties);
        }

        return properties;
    }
}
