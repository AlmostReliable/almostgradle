package com.almostreliable.almostgradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.compile.JavaCompile;

@SuppressWarnings("unused")
public class AlmostGradlePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPlugins().apply(JavaPlugin.class);
        project.getPlugins().apply(MavenPublishPlugin.class);

        // Add the annotation processor automatically
//        addAnnotationProcessor(project);

        Utils.createLocalRuntime(project, JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME, null);
        Utils.createLocalRuntime(project, JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME, "test");

        Utils.createLocalImplementation(project,
                JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME,
                JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME,
                null);
        Utils.createLocalImplementation(project,
                JavaPlugin.TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME,
                JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME,
                "test");

        project.getTasks().withType(JavaCompile.class).whenTaskAdded(javaCompile -> {
            javaCompile.getOptions().setEncoding("UTF-8");
        });

        project.getExtensions().create(AlmostGradleExtension.NAME, AlmostGradleExtension.class);
    }

    private void addAnnotationProcessor(Project project) {
        // Get the plugin JAR that contains our annotation processor
        Configuration pluginConfiguration = project.getBuildscript().getConfigurations().getByName("classpath");

        // Add our plugin JAR to the annotationProcessor configuration
        project.afterEvaluate(p -> {
            // Find our plugin in the buildscript classpath
            pluginConfiguration.getResolvedConfiguration().getFirstLevelModuleDependencies().forEach(dep -> {
                if (dep.getModuleGroup().equals("com.almostreliable") &&
                    dep.getModuleName().equals("gradle-plugin")) {

                    p.getDependencies().add("annotationProcessor",
                            dep.getModuleGroup() + ":" + dep.getModuleName() + ":" + dep.getModuleVersion());
                }
            });

            // Alternative approach: add the plugin coordinates directly
            // This is simpler but requires knowing the version
            // p.getDependencies().add("annotationProcessor", "com.almostreliable:gradle-plugin:1.3.2");
        });
    }
}
