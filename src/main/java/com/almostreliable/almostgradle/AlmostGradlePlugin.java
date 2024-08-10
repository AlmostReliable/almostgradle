package com.almostreliable.almostgradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;

public class AlmostGradlePlugin implements Plugin<Project> {

    public void apply(Project project) {
        project.getPlugins().apply(JavaPlugin.class);
        project.getPlugins().apply(MavenPublishPlugin.class);


        Utils.createLocalRuntimeOnly(project, JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME, null);
        Utils.createLocalRuntimeOnly(project, JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME, "test");

        var javaPlugin = project.getExtensions().getByType(JavaPluginExtension.class);
        javaPlugin.withSourcesJar();
        project.getTasks().withType(JavaCompile.class).whenTaskAdded(javaCompile -> {
            javaCompile.getOptions().setEncoding("UTF-8");
        });

        project.getExtensions().create(AlmostGradleExtension.NAME, AlmostGradleExtension.class);
        project
                .getTasks()
                .named("processResources", ProcessResources.class)
                .configure(new ProcessResourceHandler(project));
    }
}
