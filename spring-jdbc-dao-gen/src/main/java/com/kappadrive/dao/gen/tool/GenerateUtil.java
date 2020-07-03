package com.kappadrive.dao.gen.tool;

import com.kappadrive.dao.api.ColumnType;
import com.kappadrive.dao.gen.DaoGenProcessor;
import com.kappadrive.dao.gen.FieldMeta;
import com.kappadrive.dao.gen.ImplData;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.processing.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementKindVisitor9;
import javax.lang.model.util.TypeKindVisitor9;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GenerateUtil {

    private final ProcessingEnvironment processingEnv;

    public GenerateUtil(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    @Nonnull
    public AnnotationSpec createGeneratedAnnotation() {
        return AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", DaoGenProcessor.class.getName())
                .addMember("date", "$S", DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now()))
                .build();
    }

    @Nonnull
    public DeclaredType resolveVarType(@Nonnull final TypeMirror varType, @Nonnull final List<DeclaredType> hierarchy, int index) {
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

    public int lookUpTypeVarId(@Nonnull final DeclaredType declaredType, @Nonnull final TypeVariable typeVariable) {
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        for (int i = 0; i < typeArguments.size(); i++) {
            TypeMirror typeArgument = typeArguments.get(i);
            if (typeArgument == typeVariable)
                return i;
        }
        throw new IllegalArgumentException("Not found: " + typeVariable);
    }

    public void writeSafe(@Nonnull final JavaFile javaFile) {
        try {

            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error writing java file: " + e.getMessage());
        }
    }

    @Nonnull
    public List<DeclaredType> getInterfaceHierarchy(@Nonnull final TypeElement typeElement,
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
    public Collection<ExecutableElement> getAllAbstractMethods(@Nonnull final TypeElement typeElement) {
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
    public Optional<ExecutableElement> findGetter(
            @Nonnull final Collection<ExecutableElement> methods,
            @Nonnull final VariableElement field
    ) {
        final String getterName = "get" + StringUtils.capitalize(field.getSimpleName().toString());
        return methods.stream()
                .filter(m -> m.getSimpleName().contentEquals(getterName))
                .filter(m -> m.getParameters().isEmpty())
                .findAny();
    }

    @Nonnull
    public Optional<ExecutableElement> findSetter(
            @Nonnull final Collection<ExecutableElement> methods,
            @Nonnull final VariableElement field
    ) {
        final String setterName = "set" + StringUtils.capitalize(field.getSimpleName().toString());
        return methods.stream()
                .filter(m -> m.getSimpleName().contentEquals(setterName))
                .filter(m -> m.getParameters().size() == 1)
                .filter(m -> m.getParameters().get(0).asType().equals(field.asType()))
                .findAny();
    }

    @Nonnull
    public Collection<FieldMeta> getAllFields(@Nonnull final DeclaredType declaredType) {
        List<ExecutableElement> methods = declaredType.asElement()
                .getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .map(e -> (ExecutableElement) e)
                .collect(Collectors.toList());

        return declaredType.asElement()
                .getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .map(e -> (VariableElement) e)
                .map(v -> {
                    ExecutableElement getter = findGetter(methods, v)
                            .orElseThrow(IllegalArgumentException::new);
                    ExecutableElement setter = findSetter(methods, v)
                            .orElseThrow(IllegalArgumentException::new);
                    return createFieldMeta(v, getter, setter);
                })
                .collect(Collectors.toList());
    }

    @Nonnull
    public FieldMeta createFieldMeta(
            @Nonnull final VariableElement field,
            @Nonnull final ExecutableElement getter,
            @Nonnull final ExecutableElement setter
    ) {
        String name = field.getSimpleName().toString();
        return FieldMeta.builder()
                .field(field)
                .name(name)
                .type(field.asType())
                .getter(getter)
                .setter(setter)
                .sqlType(AnnotationUtil.getAnnotationValue(field, ColumnType.class, "value", Integer.class)
                        .orElse(null))
                .isKey(AnnotationUtil.getAnnotationMirror(field, Id.class).isPresent())
                .isGenerated(AnnotationUtil.getAnnotationMirror(field, GeneratedValue.class).isPresent())
                .columnName(AnnotationUtil.getAnnotationValue(field, Column.class, "name", String.class)
                        .orElseGet(() -> DbNameUtil.convertToDbName(name)))
                .build();
    }

    @Nonnull
    public Optional<FieldMeta> findFieldByName(
            @Nonnull final ImplData implData,
            @Nonnull final String fieldName
    ) {
        return implData.getEntityMeta().getFields().stream()
                .filter(f -> f.getField().getSimpleName().contentEquals(fieldName))
                .findAny();
    }

    public boolean hasType(@Nonnull final TypeMirror typeMirror, @Nonnull Class<?> typeClass) {
        return processingEnv.getTypeUtils().isSameType(
                typeMirror,
                processingEnv.getElementUtils().getTypeElement(typeClass.getName()).asType());
    }

    @Nonnull
    public Optional<String> getNumberMethodPrefix(@Nonnull final TypeMirror type) {
        if (hasType(type, Long.class) || type.getKind() == TypeKind.LONG) {
            return Optional.of("long");
        } else if (hasType(type, Integer.class) || type.getKind() == TypeKind.INT) {
            return Optional.of("int");
        } else if (hasType(type, Short.class) || type.getKind() == TypeKind.SHORT) {
            return Optional.of("short");
        } else if (hasType(type, Byte.class) || type.getKind() == TypeKind.BYTE) {
            return Optional.of("byte");
        } else if (hasType(type, Double.class) || type.getKind() == TypeKind.DOUBLE) {
            return Optional.of("double");
        } else if (hasType(type, Float.class) || type.getKind() == TypeKind.FLOAT) {
            return Optional.of("float");
        } else {
            return Optional.empty();
        }
    }

    @Nonnull
    public String createCondition(@Nonnull final Collection<FieldMeta> fields) {
        return fields.stream()
                .map(f -> String.format("%s = :%s", f.getColumnName(), f.getName()))
                .collect(Collectors.joining(" AND "));
    }

    @Nonnull
    public String createCondition(
            @Nonnull final ExecutableElement executableElement,
            @Nonnull final ImplData implData
    ) {
        return executableElement.getParameters().stream()
                .map(v -> {
                    String varName = v.getSimpleName().toString();
                    return findFieldByName(implData, varName)
                            .map(f -> String.format("%s = :%s", f.getColumnName(), f.getName()))
                            .orElseThrow(() -> new IllegalArgumentException("No field found with name " + varName));
                })
                .collect(Collectors.joining(" AND "));
    }

    @Nonnull
    public String createIdCondition(@Nonnull final ImplData implData) {
        return createCondition(implData.getEntityMeta().getKeyFields());
    }

    @Nonnull
    public CodeBlock createParamSourceFromComplexType(
            @Nonnull final ExecutableElement executableElement,
            @Nonnull final String paramSource,
            @Nonnull final ImplData implData,
            @Nonnull final Predicate<? super FieldMeta> filter
    ) {
        VariableElement param = executableElement.getParameters().get(0);
        CodeBlock.Builder builder = CodeBlock.builder()
                .add("final var $N = new $T()", paramSource, MapSqlParameterSource.class);
        implData.getEntityMeta().getFields().stream()
                .filter(filter)
                .forEach(f -> {
                    String paramName = f.getColumnName();
                    if (f.getSqlType() == null) {
                        builder.add("\n.addValue($S, $L.$L)", paramName, param, f.getGetter());
                    } else {
                        builder.add("\n.addValue($S, $L.$L, $L)", paramName, param, f.getGetter(), f.getSqlType());
                    }
                });
        return builder.build();
    }

    @Nonnull
    public CodeBlock createParamSourceFromSimpleType(
            @Nonnull final ExecutableElement executableElement,
            @Nonnull final String paramSource,
            @Nonnull final ImplData implData,
            @Nonnull final Predicate<? super FieldMeta> filter
    ) {
        CodeBlock.Builder builder = CodeBlock.builder()
                .add("final var $N = new $T()", paramSource, MapSqlParameterSource.class);
        VariableElement param = executableElement.getParameters().get(0);
        implData.getEntityMeta().getFields().stream()
                .filter(filter)
                .forEach(f -> {
                    String paramName = f.getColumnName();
                    builder.add("\n.addValue($S, $L)", paramName, param);
                });
        return builder.build();
    }

    @Nonnull
    public CodeBlock createParamSourceFromParameters(
            @Nonnull final ExecutableElement executableElement,
            @Nonnull final String paramSource,
            @Nonnull final ImplData implData
    ) {
        CodeBlock.Builder builder = CodeBlock.builder()
                .add("final var $N = new $T()", paramSource, MapSqlParameterSource.class);
        executableElement.getParameters().forEach(v -> {
            String varName = v.getSimpleName().toString();
            FieldMeta f = findFieldByName(implData, varName)
                    .orElseThrow(() -> new IllegalArgumentException("No field found with name " + varName));
            String paramName = f.getColumnName();
            String paramValue = v.getSimpleName().toString();
            builder.add("\n.addValue($S, $L)", paramName, paramValue);
        });
        return builder.build();
    }

    @Nonnull
    public CodeBlock createParamSourceFromEntity(
            @Nonnull final ExecutableElement executableElement,
            @Nonnull final String paramSource,
            @Nonnull final ImplData implData,
            @Nonnull final Predicate<? super FieldMeta> filter
    ) {
        List<? extends VariableElement> parameters = executableElement.getParameters();
        if (parameters.size() == 1
                && processingEnv.getTypeUtils().isSubtype(
                resolveGenericTypes(executableElement, implData.getInterfaceType()).get(0),
                implData.getEntityType())) {
            return createParamSourceFromComplexType(executableElement, paramSource, implData, filter);
        } else {
            return createParamSourceFromParameters(executableElement, paramSource, implData);
        }
    }

    @Nonnull
    public CodeBlock createParamSourceFromId(
            @Nonnull final ExecutableElement executableElement,
            @Nonnull final String paramSource,
            @Nonnull final ImplData implData,
            @Nonnull final Predicate<? super FieldMeta> filter
    ) {
        List<? extends VariableElement> parameters = executableElement.getParameters();
        if (parameters.size() == 1
                && processingEnv.getTypeUtils().isSubtype(
                resolveGenericTypes(executableElement, implData.getInterfaceType()).get(0),
                implData.getIdType())) {
            return createParamSourceFromSimpleType(executableElement, paramSource, implData, filter);
        } else {
            return createParamSourceFromParameters(executableElement, paramSource, implData);
        }
    }

    @Nonnull
    public CodeBlock createEntityOnInsert(
            @Nonnull final ExecutableElement executableElement,
            @Nonnull final String keysVar,
            @Nonnull final ImplData implData
    ) {
        CodeBlock.Builder builder = CodeBlock.builder();
        List<? extends VariableElement> parameters = executableElement.getParameters();
        String entityVar;
        if (parameters.size() == 1
                && processingEnv.getTypeUtils().isSubtype(
                resolveGenericTypes(executableElement, implData.getInterfaceType()).get(0),
                implData.getEntityType())) {
            entityVar = parameters.get(0).getSimpleName().toString();
        } else {
            entityVar = "entity";
            builder.addStatement("final var $L = new $T()", entityVar, implData.getEntityType());
            parameters.forEach(v -> {
                String varName = v.getSimpleName().toString();
                FieldMeta f = findFieldByName(implData, varName)
                        .orElseThrow(() -> new IllegalArgumentException("No field found with name " + varName));
                builder.add("$L.$L($L)", entityVar, f.getSetter().getSimpleName(), v.getSimpleName());
            });
        }

        implData.getEntityMeta().getFields().stream()
                .filter(FieldMeta::isGenerated).forEach(f -> {
            ExecutableElement setter = f.getSetter();
            TypeMirror type = f.getType();
            String prefix = getNumberMethodPrefix(type)
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported generated field type: " + type));
            builder.addStatement("$L.$L((($T) $L.get($S)).$LValue())",
                    entityVar, setter.getSimpleName(), Number.class, keysVar, f.getColumnName(), prefix);
        });

        builder.addStatement("return $L", entityVar);
        return builder.build();
    }

    @Nonnull
    public List<? extends TypeMirror> resolveGenericTypes(
            @Nonnull final ExecutableElement executableElement,
            @Nonnull final DeclaredType type
    ) {
        ExecutableType executableType = (ExecutableType) processingEnv.getTypeUtils().asMemberOf(type, executableElement);
        return executableType.getParameterTypes();
    }
}
