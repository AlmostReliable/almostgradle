package com.almostreliable.almostgradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.compile.JavaCompile;

@SuppressWarnings("unused")
public class AlmostGradlePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPlugins().apply(JavaPlugin.class);
        project.getPlugins().apply(MavenPublishPlugin.class);

        Utils.createLocalRuntime(project, JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME, null);
        Utils.createLocalRuntime(project, JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME, "test");

        project.getTasks().withType(JavaCompile.class).whenTaskAdded(javaCompile -> {
            javaCompile.getOptions().setEncoding("UTF-8");
        });

        project.getExtensions().create(AlmostGradleExtension.NAME, AlmostGradleExtension.class);
    }
}
