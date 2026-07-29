package com.almostreliable.almostgradle.dependency;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyFactory;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public interface ModDependency {
    ModDependency EMI = new Emi();
    ModDependency REI = new Rei();
    ModDependency JEI = new Jei();

    String modId();

    String shortId();

    String defaultMavenRepo();

    Iterable<Dependency> createApiDependencies(String minecraftVersion, String depVersion, DependencyFactory factory);

    Iterable<Dependency> createDependencies(String minecraftVersion, String depVersion, DependencyFactory factory);

    class Emi implements ModDependency {

        @Override
        public String modId() {
            return "emi";
        }

        @Override
        public String shortId() {
            return "EMI";
        }

        @Override
        public String defaultMavenRepo() {
            return "https://repo.sleeping.town/";
        }

        @Override
        public Iterable<Dependency> createApiDependencies(String minecraftVersion, String depVersion, DependencyFactory factory) {
            return List.of(
                    factory.create("dev.emi", "emi-neoforge", depVersion + "+" + minecraftVersion, "api", null)
            );
        }

        @Override
        public Iterable<Dependency> createDependencies(String minecraftVersion, String depVersion, DependencyFactory factory) {
            return List.of(
                    factory.create("dev.emi", "emi-neoforge", depVersion + "+" + minecraftVersion)
            );
        }
    }

    class Rei implements ModDependency {


        @Override
        public String modId() {
            return "roughlyenoughitems";
        }

        @Override
        public String shortId() {
            return "REI";
        }

        @Override
        public String defaultMavenRepo() {
            return "https://maven.shedaniel.me";
        }

        @Override
        public Iterable<Dependency> createApiDependencies(String minecraftVersion, String depVersion, DependencyFactory factory) {
            return List.of(
                    factory.create("me.shedaniel", "RoughlyEnoughItems-api-neoforge", depVersion)
            );
        }

        @Override
        public Iterable<Dependency> createDependencies(String minecraftVersion, String depVersion, DependencyFactory factory) {
            return List.of(
                    factory.create("me.shedaniel", "RoughlyEnoughItems-neoforge", depVersion)
            );
        }
    }

    class Jei implements ModDependency {

        @Override
        public String modId() {
            return "jei";
        }

        @Override
        public String shortId() {
            return "JEI";
        }

        @Override
        public String defaultMavenRepo() {
            return "https://maven.blamejared.com/";
        }

        @Override
        public Iterable<Dependency> createApiDependencies(String minecraftVersion, String depVersion, DependencyFactory factory) {
            return List.of(
                    factory.create("mezz.jei", "jei-" + minecraftVersion + "-neoforge-api", depVersion)
                            .setTransitive(false),
                    factory.create("mezz.jei", "jei-" + minecraftVersion + "-common-api", depVersion)
                            .setTransitive(false)
            );
        }

        @Override
        public Iterable<Dependency> createDependencies(String minecraftVersion, String depVersion, DependencyFactory factory) {
            return List.of(
                    factory.create("mezz.jei", "jei-" + minecraftVersion + "-neoforge", depVersion).setTransitive(false)
            );
        }
    }
}
