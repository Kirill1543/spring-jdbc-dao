package com.kappadrive.dao.gen;

import com.google.auto.service.AutoService;
import com.kappadrive.dao.api.GenerateDao;
import com.kappadrive.dao.api.Id;
import com.kappadrive.dao.api.Table;
import com.kappadrive.dao.gen.tool.AnnotationUtil;
import com.kappadrive.dao.gen.tool.GenerateUtil;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementKindVisitor9;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("com.kappadrive.dao.api.GenerateDao")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@AutoService(Processor.class)
public class GenerateDaoProcessor extends AbstractProcessor {

    private static final String TEMPLATE_NAME = "template";
    private static final String PARAM_SOURCE = "paramSource";
    private GenerateUtil generateUtil;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        generateUtil = new GenerateUtil(processingEnv);
        roundEnv.getElementsAnnotatedWith(GenerateDao.class).forEach(this::processRootElement);
        return true;
    }

    private void processRootElement(@Nonnull Element rootElement) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Starting DAO generation for " + rootElement);
        ImplData implData = rootElement.accept(createElementVisitor(), null);
        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(NamedParameterJdbcTemplate.class), TEMPLATE_NAME)
                .addStatement("this.$N = $N", TEMPLATE_NAME, TEMPLATE_NAME)
                .build();

        TypeSpec rowMapperClass = createRowMapperInnerClass(implData);
        FieldSpec rowMapper = FieldSpec.builder(
                createRowMapperType(implData),
                "rowMapper", Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $N()", rowMapperClass)
                .build();
        MethodSpec paramSourceMethod = createParamSourceMethod(implData);

        List<MethodSpec> daoMethods = implData.getDaoMethods()
                .stream()
                .map(e -> createDaoMethod(e, implData, rowMapper, paramSourceMethod))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        TypeSpec impl = TypeSpec.classBuilder(implData.getInterfaceElement().getSimpleName() + "Impl")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generateUtil.createGeneratedAnnotation())
                .addAnnotation(Repository.class)
                .addSuperinterface(implData.getInterfaceElement().asType())
                .addField(TypeName.get(NamedParameterJdbcTemplate.class), TEMPLATE_NAME, Modifier.PRIVATE, Modifier.FINAL)
                .addField(rowMapper)
                .addMethod(constructor)
                .addMethods(daoMethods)
                .addType(rowMapperClass)
                .addMethod(paramSourceMethod)
                .build();

        JavaFile javaFile = JavaFile.builder(implData.getPackageName(), impl)
                .build();

        generateUtil.writeSafe(javaFile);
    }

    @Nonnull
    private static ParameterizedTypeName createRowMapperType(@Nonnull ImplData implData) {
        return ParameterizedTypeName.get(ClassName.get(RowMapper.class), TypeName.get(implData.getEntityType()));
    }

    @Nonnull
    private ElementKindVisitor9<ImplData, Object> createElementVisitor() {
        // do not optimize generic - jdk11 has unfixed bug
        return new ElementKindVisitor9<ImplData, Object>() {
            @Override
            public ImplData visitType(TypeElement e, Object o) {
                PackageElement packageElement = (PackageElement) e.getEnclosingElement();
                Collection<ExecutableElement> allMethods = generateUtil.getAllAbstractMethods(e);
                DeclaredType entityType = AnnotationUtil.getAnnotationValue(e, GenerateDao.class, "value", DeclaredType.class)
                        .orElseThrow(IllegalStateException::new);
                return ImplData.builder()
                        .packageName(packageElement.getQualifiedName().toString())
                        .interfaceElement(e)
                        .interfaceType((DeclaredType) e.asType())
                        .entityType(entityType)
                        .entityMeta(createEntityMeta(entityType))
                        .daoMethods(allMethods)
                        .build();
            }
        };
    }

    @Nonnull
    private EntityMeta createEntityMeta(@Nonnull DeclaredType entityType) {
        Collection<FieldMeta> allFields = generateUtil.getAllFields(entityType);
        Collection<FieldMeta> keyFields = allFields.stream().filter(FieldMeta::isKey).collect(Collectors.toList());
        String tableName = AnnotationUtil.getAnnotationValue(entityType.asElement(), Table.class, "name", String.class)
                .orElseGet(() -> entityType.asElement().getSimpleName().toString().toLowerCase());
        return EntityMeta.builder()
                .tableName(tableName)
                .fields(allFields)
                .keyFields(keyFields)
                .build();
    }

    @Nullable
    public MethodSpec createDaoMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData,
                                      @Nonnull FieldSpec rowMapper, @Nonnull MethodSpec paramSourceMethod) {
        String s = executableElement.getSimpleName().toString();
        if (s.startsWith("find")) {
            return createFindMethod(executableElement, implData, rowMapper);
        } else if (s.startsWith("insert")) {
            return createInsertMethod(executableElement, implData, paramSourceMethod);
        } else if (s.startsWith("update")) {
            return createUpdateMethod(executableElement, implData, paramSourceMethod);
        } else if (s.startsWith("delete")) {
            return createDeleteMethod(executableElement, implData);
        }
        throw new IllegalArgumentException("Unsupported method: " + executableElement);
    }

    @Nonnull
    private MethodSpec createParamSourceMethod(@Nonnull ImplData implData) {
        String paramName = implData.getEntityType().asElement().getSimpleName().toString().toLowerCase().substring(0, 1);
        ParameterSpec entityParameter = ParameterSpec.builder(TypeName.get(implData.getEntityType()), paramName)
                .addAnnotation(Nonnull.class)
                .build();
        CodeBlock paramSource = generateUtil.createParamSourceFromComplexType(paramName, PARAM_SOURCE, implData);
        return MethodSpec.methodBuilder("createParamSource")
                .returns(SqlParameterSource.class)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addParameter(entityParameter)
                .addAnnotation(Nonnull.class)
                .addStatement(paramSource)
                .addStatement("return $N", PARAM_SOURCE)
                .build();
    }

    @Nonnull
    private TypeSpec createRowMapperInnerClass(@Nonnull ImplData implData) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("mapRow")
                .addAnnotation(Override.class)
                .addAnnotation(org.springframework.lang.Nullable.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ResultSet.class, "rs")
                .addParameter(TypeName.INT, "rowNum")
                .returns(TypeName.get(implData.getEntityType()))
                .addException(SQLException.class)
                .addStatement("final var entity = new $T()", implData.getEntityType());

        implData.getEntityMeta().getFields().forEach(f -> {
            generateUtil.getResultSetGetter(f).ifPresentOrElse(getter ->
                            builder.addStatement("entity.$L($L." + getter + ")",
                                    f.getSetter().getSimpleName(), "rs", f.getColumnName()),
                    () -> {
                        if (generateUtil.isEnum(f.getType())) {
                            builder.addStatement("entity.$L($T.ofNullable($L.getString($S)).map($T::valueOf).orElse(null))",
                                    f.getSetter().getSimpleName(), Optional.class, "rs", f.getColumnName(), f.getType());
                        } else {
                            throw new IllegalArgumentException("Unsupported entity field type: " + f.getType());
                        }
                    });
        });
        return TypeSpec.classBuilder(implData.getEntityType().asElement().getSimpleName().toString() + "RowMapper")
                .addSuperinterface(createRowMapperType(implData))
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addMethod(builder.addStatement("return entity").build())
                .build();
    }

    @Nonnull
    private MethodSpec createFindMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData, @Nonnull FieldSpec rowMapper) {
        final String query = String.format("SELECT * FROM %s%s",
                implData.getEntityMeta().getTableName(),
                generateUtil.createCondition(executableElement, implData));
        MethodSpec.Builder builder = methodBuilder(executableElement, implData)
                .addStatement(generateUtil.createParamSourceFromParameters(executableElement, PARAM_SOURCE, implData));
        TypeMirror returnType = executableElement.getReturnType();
        if (generateUtil.isAssignableGeneric(returnType, List.class)
                || generateUtil.isAssignableGeneric(returnType, Collection.class)
                || generateUtil.isAssignableGeneric(returnType, Iterable.class)) {
            builder.addStatement("return this.$N.query($S, $N, this.$N)",
                    TEMPLATE_NAME, query, PARAM_SOURCE, rowMapper);
        } else {
            if (!generateUtil.isAssignableGeneric(returnType, Optional.class)) {
                throw new IllegalArgumentException("find* method return should be Optional or List/Collection/Iterable");
            }
            builder.addStatement("return $T.ofNullable($T.singleResult(this.$N.query($S, $N, this.$N)))",
                    Optional.class, DataAccessUtils.class,
                    TEMPLATE_NAME, query, PARAM_SOURCE, rowMapper);
        }
        return builder.build();
    }

    @Nonnull
    private MethodSpec createInsertMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData,
                                          @Nonnull MethodSpec paramSourceMethod) {
        CodeBlock paramSource = generateUtil.createParamSourceFromEntity(executableElement, PARAM_SOURCE, implData, paramSourceMethod);
        String insertFields = implData.getEntityMeta().getFields().stream()
                .filter(Predicate.not(FieldMeta::isGenerated)).map(FieldMeta::getColumnName)
                .collect(Collectors.joining(", "));
        String insertValues = implData.getEntityMeta().getFields().stream()
                .filter(Predicate.not(FieldMeta::isGenerated)).map(f -> ":" + f.getName())
                .collect(Collectors.joining(", "));
        String query = String.format("INSERT INTO %s (%s) VALUES (%s)",
                implData.getEntityMeta().getTableName(), insertFields, insertValues);
        MethodSpec.Builder builder = methodBuilder(executableElement, implData)
                .addStatement(paramSource)
                .addStatement("final var keyHolder = new $T()", GeneratedKeyHolder.class)
                .addStatement("this.$N.update($S, $N, $L)", TEMPLATE_NAME, query, PARAM_SOURCE, "keyHolder")
                .addStatement("final var keys = keyHolder.getKeys()", GeneratedKeyHolder.class)
                .addCode(generateUtil.createEntityOnInsert(executableElement, "keys", implData));
        if (!builder.typeVariables.isEmpty()) {
            TypeVariableName typeVariableName = builder.typeVariables.get(0);
            builder.typeVariables.set(0, TypeVariableName.get(typeVariableName.name, TypeName.get(implData.getEntityType())));
        }
        return builder.build();
    }

    @Nonnull
    private MethodSpec createUpdateMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData,
                                          @Nonnull MethodSpec paramSourceMethod) {
        CodeBlock paramSource = generateUtil.createParamSourceFromEntity(executableElement, PARAM_SOURCE, implData, paramSourceMethod);
        String query = String.format("UPDATE %s SET %s%s",
                implData.getEntityMeta().getTableName(),
                generateUtil.createStatement(implData.getEntityMeta().getFields()
                        .stream().filter(f -> !f.isGenerated() && !f.isKey()).collect(Collectors.toList()), ", "),
                generateUtil.createCondition(implData.getEntityMeta().getKeyFields()));
        return methodBuilder(executableElement, implData)
                .addStatement(paramSource)
                .addStatement("this.$N.update($S, $N)", TEMPLATE_NAME, query, PARAM_SOURCE)
                .build();
    }

    @Nonnull
    private MethodSpec createDeleteMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        CodeBlock paramSource = generateUtil.createParamSourceFromParameters(executableElement, PARAM_SOURCE, implData);
        String query = String.format("DELETE FROM %s%s",
                implData.getEntityMeta().getTableName(),
                generateUtil.createCondition(executableElement, implData));
        return methodBuilder(executableElement, implData)
                .addStatement(paramSource)
                .addStatement("this.$N.update($S, $N)", TEMPLATE_NAME, query, PARAM_SOURCE)
                .build();
    }

    @Nonnull
    private MethodSpec.Builder methodBuilder(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        Types types = processingEnv.getTypeUtils();
        MethodSpec.Builder builder = MethodSpec.overriding(executableElement, implData.getInterfaceType(), types)
                .addAnnotations(AnnotationUtil.getAnnotationSpecs(executableElement));
        List<? extends TypeMirror> resolvedParameterTypes = generateUtil.resolveGenericTypes(executableElement, implData.getInterfaceType());
        List<? extends VariableElement> declaredParameters = executableElement.getParameters();
        for (int i = 0, size = builder.parameters.size(); i < size; i++) {
            ParameterSpec parameter = builder.parameters.get(i);
            TypeName type = TypeName.get(resolvedParameterTypes.get(i));
            VariableElement declared = declaredParameters.get(i);
            ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(type, declared.getSimpleName().toString());
            parameterBuilder.modifiers.addAll(parameter.modifiers);
            parameterBuilder.annotations.addAll(AnnotationUtil.getAnnotationSpecs(declared));
            builder.parameters.set(i, parameterBuilder.build());
        }
        return builder;
    }

}
