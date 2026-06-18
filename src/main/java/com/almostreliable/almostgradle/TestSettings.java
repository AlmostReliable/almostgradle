package com.almostreliable.almostgradle;

import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.testing.Test;

import javax.inject.Inject;

public abstract class TestSettings {

    public static final String TESTMOD_ID = "testmod";
    public static final String JUNIT_VERSION = "5.14.1";
    public static final String GAME_TEST_PROPERTY = "neoforge.enableGameTest";

    private final Project project;
    private boolean testsEnabled;

    @Inject
    public TestSettings(Project project) {
        this.project = project;
        getTestMod().convention(false);
        getGameTests().convention(false);
        getTestFramework().convention(false);
        getJUnit().convention(false);
    }

    public abstract Property<Boolean> getTestMod();

    public abstract Property<Boolean> getGameTests();

    public abstract Property<Boolean> getTestFramework();

    public abstract Property<Boolean> getJUnit();

    public void apply() {
        if (!testsEnabled) return;

        var logger = project.getLogger();
        logger.lifecycle("📕Applying test configurations:");
        Utils.log(project, "\t* Test Mod", getTestMod().get());
        Utils.log(project, "\t* Game Tests", getGameTests().get());
        Utils.log(project, "\t* Test Framework", getTestFramework().get());
        Utils.log(project, "\t* JUnit", getJUnit().get());

        applyTestMod();
        applyGameTests();
        applyTestFramework();
        applyJUnit();
    }

    private void applyTestMod() {
        if (!getTestMod().get()) {
            return;
        }

        var java = project.getExtensions().getByType(JavaPluginExtension.class);
        var neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        var testSourceSet = java.getSourceSets().getByName("test");

        neoForge.mods(mods ->
                mods.create(TESTMOD_ID, mod -> mod.sourceSet(testSourceSet))
        );
        neoForge.addModdingDependenciesTo(testSourceSet);

        var gameTestsEnabled = (boolean) getGameTests().get();
        neoForge.runs(runs -> {
            runs.create(TESTMOD_ID, run -> {
                run.client();
                run.getSourceSet().set(testSourceSet);
                // for clients, game tests are enabled by default, so this needs to be stateful
                run.systemProperty(GAME_TEST_PROPERTY, String.valueOf(gameTestsEnabled));
            });
        });
    }

    private void applyGameTests() {
        if (!getGameTests().get()) {
            return;
        }

        if (!getTestMod().get()) {
            var logger = project.getLogger();
            logger.error("Game tests can only be enabled with a test mod!");
            return;
        }

        var java = project.getExtensions().getByType(JavaPluginExtension.class);
        var neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        var testSourceSet = java.getSourceSets().getByName("test");

        neoForge.runs(runs ->
                runs.create("gametest", run -> {
                    run.getType().set("gameTestServer");
                    run.getGameDirectory()
                            .set(project.getLayout().getBuildDirectory().get().dir("tmp").dir("gametestRuns"));
                    run.getSourceSet().set(testSourceSet);
                    run.systemProperty(GAME_TEST_PROPERTY, "true");
                })
        );
    }

    @SuppressWarnings("UnstableApiUsage")
    private void applyTestFramework() {
        if (!getTestFramework().get()) {
            return;
        }

        var java = project.getExtensions().getByType(JavaPluginExtension.class);
        var almostGradle = project.getExtensions().getByType(AlmostGradleExtension.class);
        var testSourceSet = java.getSourceSets().getByName("test");

        var testImpl = project.getConfigurations().getByName(testSourceSet.getImplementationConfigurationName());
        testImpl.resolutionStrategy(ResolutionStrategy::failOnVersionConflict);

        var neoVersion = almostGradle.getNeoforgeVersion();
        var dep = project.getDependencyFactory().create("net.neoforged", "testframework", neoVersion);
        testImpl.withDependencies(d -> d.add(dep));
    }

    @SuppressWarnings("UnstableApiUsage")
    private void applyJUnit() {
        if (!getJUnit().get()) {
            return;
        }

        var java = project.getExtensions().getByType(JavaPluginExtension.class);
        var neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        var almostGradle = project.getExtensions().getByType(AlmostGradleExtension.class);
        var testSourceSet = java.getSourceSets().getByName("test");

        var testImpl = project.getConfigurations().getByName(testSourceSet.getImplementationConfigurationName());
        testImpl.resolutionStrategy(ResolutionStrategy::failOnVersionConflict);
        var testRuntime = project.getConfigurations().getByName(testSourceSet.getRuntimeOnlyConfigurationName());
        testRuntime.resolutionStrategy(ResolutionStrategy::failOnVersionConflict);

        var jupiterDep = project.getDependencyFactory().create("org.junit.jupiter", "junit-jupiter", JUNIT_VERSION);
        testImpl.withDependencies(d -> d.add(jupiterDep));
        var launcherDep = project.getDependencyFactory().create("org.junit.platform:junit-platform-launcher");
        testRuntime.withDependencies(d -> d.add(launcherDep));

        project.getTasks().named("test", Test.class, test -> {
            test.useJUnitPlatform();
            test.exclude(TESTMOD_ID + "/mixin/**");
        });

        var mainMod = neoForge.getMods().maybeCreate(almostGradle.getModId());
        var testMod = neoForge.getMods().maybeCreate(TESTMOD_ID);
        var testModEnabled = (boolean) getTestMod().get();
        var testedMod = testModEnabled ? testMod : mainMod;

        neoForge.unitTest(unitTest -> {
            unitTest.enable();
            unitTest.getTestedMod().set(testedMod);
        });
    }

    void enableTests() {
        testsEnabled = true;
    }
}
