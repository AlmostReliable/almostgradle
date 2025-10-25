package com.almostreliable.annotation_processor;


import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

@AutoService(Processor.class)
@SupportedAnnotationTypes("org.spongepowered.asm.mixin.Mixin")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class MixinAnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) return false;

        TypeElement mixinAnnotation = annotations.iterator().next();
        Element valueSymbol = mixinAnnotation
                .getEnclosedElements()
                .stream()
                .filter(e -> e.getSimpleName().contentEquals("value"))
                .findFirst()
                .orElseThrow();

        Map<String, List<String>> jsonEntries = new HashMap<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(mixinAnnotation)) {
            if (element.getKind() != ElementKind.CLASS || !(element instanceof TypeElement mixinClass)) {
                continue;
            }

            Set<String> keys = new HashSet<>();

            List<? extends AnnotationMirror> classAnnotations = mixinClass.getAnnotationMirrors();
            for (AnnotationMirror classAnnotation : classAnnotations) {
                if (classAnnotation.getAnnotationType() == mixinAnnotation.asType()) {
                    Object rawAnnotationValue = classAnnotation.getElementValues().get(valueSymbol).getValue();
                    if (rawAnnotationValue instanceof List<?> annotationValues) {
                        for (var annotationValue : annotationValues) {
                            keys.add(annotationValue.toString());
                        }
                    }
                    break;
                }
            }

            for (var iface : mixinClass.getInterfaces()) {
                var interfaceName = iface.toString();
                if (interfaceName.endsWith("Extension")) {
                    for (var key : keys) {
                        jsonEntries.computeIfAbsent(key, k -> new ArrayList<>()).add(interfaceName);
                    }
                }
            }
        }

        if (jsonEntries.isEmpty()) return false;

        StringBuilder sb = new StringBuilder().append("{");
        boolean firstKey = true;

        for (var entryJson : jsonEntries.entrySet()) {
            if (!firstKey) sb.append(",");

            var keyName = entryJson.getKey().replace(".class", "").replace(".", "/");
            sb.append("\"").append(keyName).append("\":[");

            List<String> values = entryJson.getValue();
            boolean firstValue = true;
            for (var value : values) {
                if (!firstValue) sb.append(",");
                var valueName = value.replace(".", "/");
                sb.append("\"").append(valueName).append("\"");
                firstValue = false;
            }

            firstKey = false;
            sb.append("]");
        }
        sb.append("}");

        try {
            var resource = processingEnv
                    .getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "", "interfaces.json");

            try (OutputStream stream = resource.openOutputStream()) {
                stream.write(sb.toString().getBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create interface injection metadata file", e);
        }

        return false;
    }
}
