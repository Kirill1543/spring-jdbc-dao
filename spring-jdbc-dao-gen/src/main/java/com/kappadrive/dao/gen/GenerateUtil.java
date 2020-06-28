package com.kappadrive.dao.gen;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.JavaFile;

import javax.annotation.Nonnull;
import javax.annotation.processing.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementKindVisitor9;
import javax.lang.model.util.TypeKindVisitor9;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GenerateUtil {

    private GenerateUtil() {
    }

    @Nonnull
    public static AnnotationSpec createGeneratedAnnotation() {
        return AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", DaoGenProcessor.class.getName())
                .addMember("date", "$S", DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now()))
                .build();
    }

    @Nonnull
    public static DeclaredType resolveVarType(@Nonnull final TypeMirror varType, @Nonnull final List<DeclaredType> hierarchy, int index) {
        switch (varType.getKind()) {
            case DECLARED:
                return (DeclaredType) varType;
            case TYPEVAR:
                if (hierarchy.size() <= index) {
                    throw new IllegalArgumentException();
                }
                DeclaredType type = hierarchy.get(index);
                TypeVariable typeVariable = (TypeVariable) varType;
                return typeVariable.asElement().getEnclosingElement()
                        .accept(new ElementKindVisitor9<DeclaredType, TypeVariable>() {
                            @Override
                            public DeclaredType visitType(TypeElement e, TypeVariable o) {
                                DeclaredType childType = (DeclaredType) e.asType();
                                TypeMirror newVarType = type.getTypeArguments().get(lookUpTypeVarId(childType, o));
                                return resolveVarType(newVarType, hierarchy, index + 1);
                            }
                        }, typeVariable);
            default:
                throw new IllegalArgumentException("Unsupported: " + varType.getKind());
        }
    }

    public static int lookUpTypeVarId(@Nonnull final DeclaredType declaredType, @Nonnull final TypeVariable typeVariable) {
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        for (int i = 0; i < typeArguments.size(); i++) {
            TypeMirror typeArgument = typeArguments.get(i);
            if (typeArgument == typeVariable)
                return i;
        }
        throw new IllegalArgumentException("Not found: " + typeVariable);
    }

    public static void writeSafe(@Nonnull final JavaFile javaFile, @Nonnull final ProcessingEnvironment processingEnv) {
        try {

            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error writing java file: " + e.getMessage());
        }
    }

    @Nonnull
    public static List<DeclaredType> getInterfaceHierarchy(@Nonnull final TypeElement typeElement,
                                                           @Nonnull final Class<?> interfaceType) {
        return typeElement.getInterfaces().stream().map(
                i -> i.accept(new TypeKindVisitor9<List<DeclaredType>, List<DeclaredType>>() {
                    @Override
                    public List<DeclaredType> visitDeclared(DeclaredType t, List<DeclaredType> o) {
                        TypeElement typeElement = (TypeElement) t.asElement();
                        if (typeElement.getQualifiedName().contentEquals(interfaceType.getName())) {
                            List<DeclaredType> hierarchy = new ArrayList<>();
                            hierarchy.add(t);
                            return hierarchy;
                        }
                        List<DeclaredType> hierarchy = getInterfaceHierarchy(typeElement, interfaceType);
                        if (!hierarchy.isEmpty()) {
                            hierarchy.add(t);
                        }
                        return hierarchy;
                    }
                }, null))
                .filter(Predicate.not(Collection::isEmpty))
                .findAny().orElse(Collections.emptyList());
    }

    @Nonnull
    public static Collection<ExecutableElement> getAllAbstractMethods(@Nonnull final TypeElement typeElement) {
        return typeElement.getInterfaces().stream().flatMap(i -> i.accept(new TypeKindVisitor9<Stream<ExecutableElement>, Object>() {
            @Override
            public Stream<ExecutableElement> visitDeclared(DeclaredType t, Object o) {
                TypeElement element = (TypeElement) t.asElement();
                return Stream.concat(element.getEnclosedElements()
                                .stream().filter(e -> e.getKind() == ElementKind.METHOD)
                                .map(e -> (ExecutableElement) e)
                                .filter(e -> !e.isDefault()),
                        getAllAbstractMethods(element).stream());
            }
        }, null))
                .collect(Collectors.toList());
    }

    @Nonnull
    public static Collection<FieldMeta> getAllFields(@Nonnull final DeclaredType declaredType) {
        return declaredType.asElement()
                .getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .map(e -> (VariableElement) e)
                .map(e -> FieldMeta.builder().field(e).build())
                .collect(Collectors.toList());
    }

    @Nonnull
    public static List<AnnotationSpec> getAnnotationSpecs(@Nonnull final Element element) {
        return element.getAnnotationMirrors().stream().map(AnnotationSpec::get).collect(Collectors.toList());
    }
}
