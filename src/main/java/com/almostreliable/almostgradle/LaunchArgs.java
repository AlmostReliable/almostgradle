package com.almostreliable.almostgradle;


import net.neoforged.moddevgradle.dsl.RunModel;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;

import javax.inject.Inject;

public abstract class LaunchArgs {

    @Inject
    public LaunchArgs(Project project) {
        var providers = project.getProviders();
        var prefix = AlmostGradleExtension.NAME + ".launchArgs.";
        getResizeClient().convention(providers
                .gradleProperty(prefix + "resizeClient")
                .map(Boolean::parseBoolean)
                .orElse(false));
        getAutoWorldJoin().set(providers.gradleProperty(prefix + "autoWorldJoin").map(s -> {
            if (s.equals("true")) return true;
            if (s.equals("false")) return false;
            return s;
        }).orElse(false));
        getLoggingLevel().convention(providers
                .gradleProperty(prefix + "loggingLevel")
                .map(String::valueOf)
                .orElse("DEBUG"));
        getMixinDebugOutput().convention(providers
                .gradleProperty(prefix + "mixinDebugOutput")
                .map(Boolean::parseBoolean)
                .orElse(false));
    }

    public abstract Property<Boolean> getResizeClient();

    public abstract Property<Object> getAutoWorldJoin();

    public abstract Property<String> getLoggingLevel();

    public abstract Property<Boolean> getMixinDebugOutput();

    @Internal
    public void applyRunArguments(RunModel run) {
        run.jvmArgument("-XX:+IgnoreUnrecognizedVMOptions");
        run.jvmArgument("-XX:+AllowEnhancedClassRedefinition");

        var sysArgs = run.getSystemProperties();
        sysArgs.put("forge.logging.console.level", getLoggingLevel().get());
        if (getMixinDebugOutput().get()) {
            sysArgs.put("mixin.debug.export", "true");
        }

        if (run.getType().get().equals("client")) {
            addResizeClient(run);
            addAutoWorldJoin(run);
        }
    }

    private void addResizeClient(RunModel run) {
        if (getResizeClient().get()) {
            run.getProgramArguments().addAll("--width", "1920", "--height", "1080");
        }
    }

    private void addAutoWorldJoin(RunModel run) {
        Object o = getAutoWorldJoin().get();
        if (o instanceof Boolean b && b) {
            run.getProgramArguments().addAll("--quickPlaySingleplayer", "New World");
        } else if (o instanceof String s) {
            run.getProgramArguments().addAll("--quickPlaySingleplayer", s);
        }
    }
}
