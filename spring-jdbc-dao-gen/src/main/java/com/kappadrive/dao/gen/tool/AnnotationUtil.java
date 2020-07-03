package com.kappadrive.dao.gen.tool;

import com.squareup.javapoet.AnnotationSpec;

import javax.annotation.Nonnull;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class AnnotationUtil {

    private AnnotationUtil() {
    }

    @Nonnull
    public static List<AnnotationSpec> getAnnotationSpecs(@Nonnull final Element element) {
        return element.getAnnotationMirrors().stream().map(AnnotationSpec::get).collect(Collectors.toList());
    }

    @Nonnull
    public static Optional<AnnotationMirror> getAnnotationMirror(
            @Nonnull final Element element,
            @Nonnull final Class<? extends Annotation> annotationClass
    ) {
        return element.getAnnotationMirrors()
                .stream()
                .filter(a -> a.getAnnotationType().toString()
                        .equals(annotationClass.getName()))
                .map(a -> (AnnotationMirror) a)
                .findAny();
    }

    @Nonnull
    public static Optional<AnnotationValue> getAnnotationValue(
            @Nonnull final AnnotationMirror annotationMirror,
            @Nonnull final String key
    ) {
        return annotationMirror.getElementValues().entrySet().stream()
                .filter(e -> e.getKey().getSimpleName().toString().equals(key))
                .map(e -> (AnnotationValue) e.getValue())
                .findAny();
    }

    @Nonnull
    public static <T> Optional<T> getAnnotationValue(
            @Nonnull final AnnotationMirror annotationMirror,
            @Nonnull final String key,
            @Nonnull final Class<? extends T> type
    ) {
        return getAnnotationValue(annotationMirror, key)
                .map(AnnotationValue::getValue)
                .map(type::cast);
    }

    @Nonnull
    public static <T> Optional<T> getAnnotationValue(
            @Nonnull final Element element,
            @Nonnull final Class<? extends Annotation> annotationClass,
            @Nonnull final String key,
            @Nonnull final Class<? extends T> type
    ) {
        return getAnnotationMirror(element, annotationClass)
                .flatMap(a -> getAnnotationValue(a, key, type));
    }
}
