package com.kappadrive.dao.gen;

import com.google.auto.service.AutoService;
import com.kappadrive.dao.api.GenerateDao;
import com.kappadrive.dao.api.JdbcDao;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementKindVisitor9;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("com.kappadrive.dao.api.GenerateDao")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@AutoService(Processor.class)
public class DaoGenProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(GenerateDao.class).forEach(this::processRootElement);
        return true;
    }

    private void processRootElement(@Nonnull Element rootElement) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Starting DAO generation for " + rootElement);
        ImplData implData = rootElement.accept(createElementVisitor(), null);
        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(NamedParameterJdbcTemplate.class), "template")
                .addStatement("this.$N = $N", "template", "template")
                .build();

        List<MethodSpec> daoMethods = implData.getDaoMethods()
                .stream()
                .map(e -> createDaoMethod(e, implData))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        TypeSpec impl = TypeSpec.classBuilder(implData.getInterfaceElement().getSimpleName() + "Impl")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(GenerateUtil.createGeneratedAnnotation())
                .addAnnotation(Repository.class)
                .addSuperinterface(implData.getInterfaceElement().asType())
                .addField(TypeName.get(NamedParameterJdbcTemplate.class), "template",
                        Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(constructor)
                .addMethods(daoMethods)
                .build();

        JavaFile javaFile = JavaFile.builder(implData.getPackageName(), impl)
                .build();

        GenerateUtil.writeSafe(javaFile, processingEnv);
    }

    @Nonnull
    private ElementKindVisitor9<ImplData, Object> createElementVisitor() {
        // do not optimize generic - jdk11 has unfixed bug
        return new ElementKindVisitor9<ImplData, Object>() {
            @Override
            public ImplData visitType(TypeElement e, Object o) {
                List<DeclaredType> daoHierarchy = GenerateUtil.getInterfaceHierarchy(e, JdbcDao.class);
                if (daoHierarchy.isEmpty()) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("@%s must implement %s", GenerateDao.class.getName(), JdbcDao.class.getName()), e);
                    return null;
                }
                PackageElement packageElement = (PackageElement) e.getEnclosingElement();
                List<? extends TypeMirror> typeArguments = daoHierarchy.get(0).getTypeArguments();
                Collection<ExecutableElement> allMethods = GenerateUtil.getAllAbstractMethods(e);
                DeclaredType entityType = GenerateUtil.resolveVarType(typeArguments.get(0), daoHierarchy, 0);
                Collection<FieldMeta> allFields = GenerateUtil.getAllFields(entityType);
                return ImplData.builder()
                        .packageName(packageElement.getQualifiedName().toString())
                        .interfaceElement(e)
                        .interfaceType((DeclaredType) e.asType())
                        .entityType(entityType)
                        .idType(GenerateUtil.resolveVarType(typeArguments.get(1), daoHierarchy, 0))
                        .fields(allFields)
                        .daoMethods(allMethods)
                        .build();
            }
        };
    }

    @Nullable
    public MethodSpec createDaoMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        switch (executableElement.getSimpleName().toString()) {
            case "findAll":
                return createFindAllMethod(executableElement, implData);
            case "findById":
                return createFindByIdMethod(executableElement, implData);
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
    private MethodSpec createFindAllMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        return methodBuilder(executableElement, implData)
                .addStatement("return null")
                .build();
    }

    @Nonnull
    private MethodSpec createFindByIdMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        return methodBuilder(executableElement, implData)
                .addStatement("return null")
                .build();
    }

    @Nonnull
    private MethodSpec createInsertMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        MethodSpec.Builder builder = methodBuilder(executableElement, implData)
                .addStatement("return null");
        TypeVariableName typeVariableName = builder.typeVariables.get(0);
        builder.typeVariables.set(0, TypeVariableName.get(typeVariableName.name, TypeName.get(implData.getEntityType())));
        return builder.build();
    }

    @Nonnull
    private MethodSpec createInsertAllMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        MethodSpec.Builder builder = methodBuilder(executableElement, implData)
                .addStatement("return null");
        TypeVariableName typeVariableName = builder.typeVariables.get(0);
        builder.typeVariables.set(0, TypeVariableName.get(typeVariableName.name, TypeName.get(implData.getEntityType())));
        return builder.build();
    }

    @Nonnull
    private MethodSpec createUpdateMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        return methodBuilder(executableElement, implData)
                .build();
    }

    @Nonnull
    private MethodSpec createUpdateAllMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        return methodBuilder(executableElement, implData)
                .build();
    }

    @Nonnull
    private MethodSpec createDeleteMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        return methodBuilder(executableElement, implData)
                .build();
    }

    @Nonnull
    private MethodSpec createDeleteAllByIdMethod(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        return methodBuilder(executableElement, implData)
                .build();
    }

    @Nonnull
    private MethodSpec createDeleteAll(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        return methodBuilder(executableElement, implData)
                .build();
    }

    @Nonnull
    private MethodSpec createExists(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        return methodBuilder(executableElement, implData)
                .addStatement("return false")
                .build();
    }

    @Nonnull
    private MethodSpec.Builder methodBuilder(@Nonnull ExecutableElement executableElement, @Nonnull ImplData implData) {
        Types types = processingEnv.getTypeUtils();
        MethodSpec.Builder builder = MethodSpec.overriding(executableElement, implData.getInterfaceType(), types)
                .addAnnotations(GenerateUtil.getAnnotationSpecs(executableElement));
        ExecutableType executableType = (ExecutableType) types.asMemberOf(implData.getInterfaceType(), executableElement);
        List<? extends TypeMirror> resolvedParameterTypes = executableType.getParameterTypes();
        List<? extends VariableElement> declaredParameters = executableElement.getParameters();
        for (int i = 0, size = builder.parameters.size(); i < size; i++) {
            ParameterSpec parameter = builder.parameters.get(i);
            TypeName type = TypeName.get(resolvedParameterTypes.get(i));
            ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(type, parameter.name);
            parameterBuilder.modifiers.addAll(parameter.modifiers);
            parameterBuilder.annotations.addAll(GenerateUtil.getAnnotationSpecs(declaredParameters.get(0)));
            builder.parameters.set(i, parameterBuilder.build());
        }
        return builder;
    }

}
