package com.kappadrive.dao.gen;

import com.google.auto.service.AutoService;
import com.kappadrive.dao.api.GenerateDao;
import com.kappadrive.dao.api.JdbcDao;
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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
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
import javax.persistence.Id;
import javax.persistence.Table;
import javax.tools.Diagnostic;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@SupportedAnnotationTypes("com.kappadrive.dao.api.GenerateDao")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@AutoService(Processor.class)
public class DaoGenProcessor extends AbstractProcessor {

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
        MethodSpec paramSource = createParamSourceMethod(implData);

        List<MethodSpec> daoMethods = implData.getDaoMethods()
                .stream()
                .map(e -> createDaoMethod(e, implData, rowMapper, paramSource))
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
                .addMethod(paramSource)
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
                List<DeclaredType> daoHierarchy = generateUtil.getInterfaceHierarchy(e, JdbcDao.class);
                if (daoHierarchy.isEmpty()) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("@%s must implement %s", GenerateDao.class.getName(), JdbcDao.class.getName()), e);
                    return null;
                }
                PackageElement packageElement = (PackageElement) e.getEnclosingElement();
                List<? extends TypeMirror> typeArguments = daoHierarchy.get(0).getTypeArguments();
                Collection<ExecutableElement> allMethods = generateUtil.getAllAbstractMethods(e);
                DeclaredType entityType = generateUtil.resolveVarType(typeArguments.get(0), daoHierarchy, 0);
                return ImplData.builder()
                        .packageName(packageElement.getQualifiedName().toString())
                        .interfaceElement(e)
                        .interfaceType((DeclaredType) e.asType())
                        .entityType(entityType)
                        .idType(generateUtil.resolveVarType(typeArguments.get(1), daoHierarchy, 0))
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
        if (keyFields.size() != 1) {
            String exception = String.format("Exact 1 field in %s should be annotated with @%s", entityType, Id.class.getName());
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, exception);
            throw new IllegalStateException(exception);
        }
        String tableName = AnnotationUtil.getAnnotationValue(entityType.asElement(), Table.class, "name", String.class)
                .orElseGet(() -> entityType.asElement().getSimpleName().toString().toLowerCase());
        return EntityMeta.builder()
                .tableName(tableName)
                .fields(allFields)
                .keyFields(keyFields)
                .build();
    }

    @Nullable
    public MethodSpec createDaoMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData, FieldSpec rowMapper, MethodSpec paramSource) {
        switch (executableElement.getSimpleName().toString()) {
            case "findAll":
                return createFindAllMethod(executableElement, implData, rowMapper, paramSource);
            case "findById":
                return createFindByIdMethod(executableElement, implData, rowMapper);
            case "insert":
                return createInsertMethod(executableElement, implData);
            case "insertAll":
                return createInsertAllMethod(executableElement, implData);
            case "update":
                return createUpdateMethod(executableElement, implData);
            case "updateAll":
                return createUpdateAllMethod(executableElement, implData);
            case "delete":
                return createDeleteMethod(executableElement, implData);
            case "deleteAllById":
                return createDeleteAllByIdMethod(executableElement, implData);
            case "deleteAll":
                return createDeleteAll(executableElement, implData);
            case "exists":
                return createExists(executableElement, implData);
            default:
                return null;
        }
    }

    @Nonnull
    private MethodSpec createParamSourceMethod(@Nonnull ImplData implData) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("createParamSource")
                .returns(SqlParameterSource.class)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addAnnotation(Nonnull.class)
                .addStatement("final var params = new $T()", MapSqlParameterSource.class);
        implData.getEntityMeta().getFields()
                .forEach(f -> {
                    if (f.getSqlType() != null) {
                        builder.addStatement("params.registerSqlType($S, $L)",
                                f.getName(),
                                f.getSqlType());
                    }
                });
        return builder.addStatement("return params").build();
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
                            builder.addStatement("entity.$L(Optional.ofNullable($L.getString($S)).map($T::valueOf).orElse(null))",
                                    f.getSetter().getSimpleName(), "rs", f.getColumnName(), f.getType());
                        } else {
                            throw new IllegalArgumentException();
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
    private MethodSpec createFindAllMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData,
                                           @Nonnull FieldSpec rowMapper, @Nonnull MethodSpec paramSource) {
        String query = String.format("SELECT * FROM %s",
                implData.getEntityMeta().getTableName());
        return methodBuilder(executableElement, implData)
                .addStatement("return this.$N.query($S, $N(), this.$N)",
                        TEMPLATE_NAME, query, paramSource, rowMapper)
                .build();
    }

    @Nonnull
    private MethodSpec createFindByIdMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData, @Nonnull FieldSpec rowMapper) {
        String query = String.format("SELECT * FROM %s WHERE %s",
                implData.getEntityMeta().getTableName(),
                generateUtil.createIdCondition(implData));
        return methodBuilder(executableElement, implData)
                .addStatement(generateUtil.createParamSourceFromId(executableElement, PARAM_SOURCE, implData, FieldMeta::isKey))
                .addStatement("return $T.ofNullable($T.singleResult(this.$N.query($S, $N, this.$N)))",
                        TypeName.get(Optional.class), TypeName.get(DataAccessUtils.class),
                        TEMPLATE_NAME, query, PARAM_SOURCE, rowMapper)
                .build();
    }

    @Nonnull
    private MethodSpec createInsertMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        CodeBlock paramSource = generateUtil.createParamSourceFromEntity(executableElement, PARAM_SOURCE, implData, Predicate.not(FieldMeta::isGenerated));
        Collection<String> generatedKeys = implData.getEntityMeta().getFields().stream()
                .filter(FieldMeta::isGenerated)
                .map(FieldMeta::getColumnName)
                .collect(Collectors.toList());
        String generatedKeysLiteral = generatedKeys.stream()
                .collect(Collectors.joining("\", \"", "\"", "\""));
        MethodSpec.Builder builder = methodBuilder(executableElement, implData)
                .addStatement(paramSource)
                .addStatement(CodeBlock.builder()
                        .add("final var keys = new $T(this.$N.getJdbcTemplate())", SimpleJdbcInsert.class, TEMPLATE_NAME)
                        .add("\n.withTableName($S)", implData.getEntityMeta().getTableName())
                        .add("\n.usingGeneratedKeyColumns($L)", generatedKeysLiteral)
                        .add("\n.executeAndReturnKeyHolder($N)", PARAM_SOURCE)
                        .add("\n.getKeys()")
                        .build())
                .addCode(generateUtil.createEntityOnInsert(executableElement, "keys", implData));
        TypeVariableName typeVariableName = builder.typeVariables.get(0);
        builder.typeVariables.set(0, TypeVariableName.get(typeVariableName.name, TypeName.get(implData.getEntityType())));
        return builder.build();
    }

    @Nonnull
    private MethodSpec createInsertAllMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        MethodSpec.Builder builder = methodBuilder(executableElement, implData)
                .addStatement(CodeBlock.builder()
                        .add("return $T.stream($L.spliterator(), false)\n", StreamSupport.class, executableElement.getParameters().get(0))
                        .add(".map(this::insert)\n")
                        .add(".collect($T.toList())", Collectors.class)
                        .build());
        TypeVariableName typeVariableName = builder.typeVariables.get(0);
        builder.typeVariables.set(0, TypeVariableName.get(typeVariableName.name, TypeName.get(implData.getEntityType())));
        return builder.build();
    }

    @Nonnull
    private MethodSpec createUpdateMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        CodeBlock paramSource = generateUtil.createParamSourceFromEntity(executableElement, PARAM_SOURCE, implData, Predicate.not(FieldMeta::isGenerated));
        String query = String.format("UPDATE %s SET %s WHERE %s",
                implData.getEntityMeta().getTableName(),
                generateUtil.createCondition(implData.getEntityMeta().getFields()
                        .stream().filter(f -> !f.isGenerated() && !f.isKey()).collect(Collectors.toList())),
                generateUtil.createCondition(implData.getEntityMeta().getKeyFields()));
        return methodBuilder(executableElement, implData)
                .addStatement(paramSource)
                .addStatement("this.$N.update($S, $N)", TEMPLATE_NAME, query, PARAM_SOURCE)
                .build();
    }

    @Nonnull
    private MethodSpec createUpdateAllMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        return methodBuilder(executableElement, implData)
                .addStatement("$L.forEach(this::update)", executableElement.getParameters().get(0))
                .build();
    }

    @Nonnull
    private MethodSpec createDeleteMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        CodeBlock paramSource = generateUtil.createParamSourceFromId(executableElement, PARAM_SOURCE, implData, FieldMeta::isKey);
        String query = String.format("DELETE FROM %s WHERE %s",
                implData.getEntityMeta().getTableName(),
                generateUtil.createIdCondition(implData));
        return methodBuilder(executableElement, implData)
                .addStatement(paramSource)
                .addStatement("this.$N.update($S, $N)", TEMPLATE_NAME, query, PARAM_SOURCE)
                .build();
    }

    @Nonnull
    private MethodSpec createDeleteAllByIdMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        return methodBuilder(executableElement, implData)
                .addStatement("$L.forEach(this::delete)", executableElement.getParameters().get(0))
                .build();
    }

    @Nonnull
    private MethodSpec createDeleteAll(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        return methodBuilder(executableElement, implData)
                .addStatement("this.$N.update($S, $T.emptyMap())",
                        TEMPLATE_NAME, "DELETE FROM " + implData.getEntityMeta().getTableName(), Collections.class)
                .build();
    }

    @Nonnull
    private MethodSpec createExists(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        return methodBuilder(executableElement, implData)
                .addStatement("return findById($L).isPresent()", executableElement.getParameters().get(0))
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
