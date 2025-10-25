package com.almostreliable.almostgradle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public abstract class InterfaceInjectionGenerator extends DefaultTask {

    @InputDirectory
    public abstract DirectoryProperty getMixinDir();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @TaskAction
    public void generate() throws IOException {
        Path mixinPath = getMixinDir().get().getAsFile().toPath();
        Path outputFilePath = getOutputFile().get().getAsFile().toPath();
        if (!Files.exists(mixinPath)) {
            getLogger().warn("Mixin directory does not exist: {}", mixinPath);
            return;
        }

        List<Path> mixinFiles;
        try (Stream<Path> s = Files.walk(mixinPath)) {
            mixinFiles = s.filter(p -> p.toString().endsWith(".class")).toList();
        }

        Map<String, List<String>> mapping = new LinkedHashMap<>();
        for (Path mixinFile : mixinFiles) {
            byte[] bytes = Files.readAllBytes(mixinFile);
            ClassReader reader = new ClassReader(bytes);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);

            if (
                    node.invisibleAnnotations == null ||
                    node.interfaces == null ||
                    node.interfaces.isEmpty() ||
                    node.invisibleAnnotations
                            .stream()
                            .noneMatch(a -> a.desc.contains("Lorg/spongepowered/asm/mixin/Mixin;"))
            ) {
                continue;
            }

            for (String iface : node.interfaces) {
                if (iface.startsWith("com/almostreliable/summoningrituals/extension")) {
                    // Get target class from @Mixin value
                    String target = extractTarget(node);
                    if (target != null) {
                        mapping.computeIfAbsent(target, k -> new ArrayList<>()).add(iface);
                    }
                }
            }
        }

        if (mapping.isEmpty()) return;

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer w = Files.newBufferedWriter(outputFilePath)) {
            gson.toJson(mapping, w);
        }

        getLogger().lifecycle("Generated interface metadata → {}", outputFilePath);

        var neoForge = getProject().getExtensions().getByType(NeoForgeExtension.class);
        var outputPath = getProject().file("src/main/resources/interfaces.json");
        neoForge.setInterfaceInjectionData(outputPath);
    }

    // Extract target class from @Mixin annotation descriptor
    @Nullable
    private static String extractTarget(ClassNode node) {
        return node.invisibleAnnotations.stream()
                .filter(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;"))
                .flatMap(a -> a.values == null ? Stream.empty() : a.values.stream())
                .filter(v -> v instanceof List<?> list && !list.isEmpty())
                .flatMap(v -> ((Collection<?>) v).stream())
                .filter(v -> v instanceof Type)
                .map(v -> ((Type) v).getInternalName())
                .findFirst()
                .orElse(null);
    }
}
