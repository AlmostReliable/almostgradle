package com.almostreliable.almostgradle;

import com.almostreliable.almostgradle.dependency.RecipeViewerOptions;
import com.almostreliable.almostgradle.dependency.RecipeViewers;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.language.jvm.tasks.ProcessResources;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

public class ProcessResourceHandler implements Action<ProcessResources> {

    public static final String USER_KEY = "githubUser";
    public static final String REPO_KEY = "githubRepo";
    public static final Map<String, Function<RecipeViewers, RecipeViewerOptions>> RECIPE_VIEWER_VERSIONS = Map.of(
            "jeiVersion", RecipeViewers::getJei,
            "emiVersion", RecipeViewers::getEmi,
            "reiVersion", RecipeViewers::getRei
    );

    private static final List<String> DEFAULT_TARGETS = List.of("META-INF/neoforge.mods.toml", "pack.mcmeta");

    private final Project project;
    private final RecipeViewers recipeViewers;
    private final List<String> targets;

    public ProcessResourceHandler(Project project, RecipeViewers recipeViewers, Iterable<String> customTargets) {
        this.project = project;
        this.recipeViewers = recipeViewers;
        this.targets = createTargets(customTargets);
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

        Set<String> commentedProperties = new HashSet<>();
        for (File file : files) {
            try (var reader = new Scanner(file)) {
                while (reader.hasNextLine()) {
                    String line = reader.nextLine();
                    Matcher matcher = pattern.matcher(line);
                    while (matcher.find()) {
                        var match = matcher.group(1);
                        if (line.trim().startsWith("#") && !keys.contains(match)) {
                            commentedProperties.add(match);
                            continue;
                        }
                        keys.add(match);
                        commentedProperties.remove(match);
                    }
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        if (!commentedProperties.isEmpty()) {
            throw new IllegalStateException("Found commented properties! Gradle will still try to resolve these. To exclude them from Gradle property expansion, remove the '$' symbols. " + commentedProperties);
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

    private List<String> createTargets(Iterable<String> customTargets) {
        Set<String> result = new LinkedHashSet<>(DEFAULT_TARGETS);
        for (String target : customTargets) {
            if (new File(target).isAbsolute()) {
                throw new GradleException("Process resource target paths must be relative to the resources directory: " + target);
            }

            result.add(target);
        }

        return result.stream().toList();
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

            for (var entry : RECIPE_VIEWER_VERSIONS.entrySet()) {
                var viewerVersion = getRecipeViewerVersion(
                        key,
                        entry.getKey(),
                        () -> entry.getValue().apply(recipeViewers)
                );
                if (viewerVersion.isPresent()) {
                    return viewerVersion;
                }
            }
        }

        return Optional.ofNullable(property).map(Object::toString);
    }

    private Optional<String> getRecipeViewerVersion(String key, String viewerKey, Supplier<RecipeViewerOptions> recipeViewerOptions) {
        if (key.equals(viewerKey)) {
            var version = recipeViewerOptions.get().getVersion();
            if (version.isPresent()) {
                return Optional.of(version.get());
            }

            var logger = project.getLogger();
            logger.lifecycle("\t* Property '" + viewerKey + "' found in target but the recipe viewer is not enabled");
        }

        return Optional.empty();
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
