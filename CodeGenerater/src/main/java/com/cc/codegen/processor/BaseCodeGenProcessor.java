package com.cc.codegen.processor;

import com.cc.codegen.context.ProcessingEnvironmentHolder;
import com.cc.codegen.processor.vo.FieldDesc;
import com.cc.codegen.spi.CodeGenProcessor;
import com.squareup.javapoet.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author gim
 */
public abstract class BaseCodeGenProcessor implements CodeGenProcessor {

    public static final String PREFIX = "Base";

    @Override
    public void generate(TypeElement typeElement, RoundEnvironment roundEnvironment)
            throws Exception {
        //添加其他逻辑扩展
        generateClass(typeElement, roundEnvironment);
    }

    /**
     * 生成class 类
     *
     * @param typeElement
     * @param roundEnvironment
     * @return
     */
    protected abstract void generateClass(TypeElement typeElement,
                                          RoundEnvironment roundEnvironment);

    /**
     * 过滤属性
     *
     * @param typeElement
     * @param predicate
     * @return
     */
    public Set<VariableElement> findFields(TypeElement typeElement,
                                           Predicate<VariableElement> predicate) {
        List<? extends Element> fieldTypes = typeElement.getEnclosedElements();
        Set<VariableElement> variableElements = new LinkedHashSet<>();
        for (VariableElement e : ElementFilter.fieldsIn(fieldTypes)) {
            if (predicate.test(e)) {
                variableElements.add(e);
            }
        }
        return variableElements;
    }

    /**
     * 获取父类
     *
     * @param element
     * @return
     */
    public TypeElement getSuperClass(TypeElement element) {
        TypeMirror parent = element.getSuperclass();
        if (parent instanceof DeclaredType) {
            Element elt = ((DeclaredType) parent).asElement();
            if (elt instanceof TypeElement) {
                return (TypeElement) elt;
            }
        }
        return null;
    }

    public void addSetterAndGetterMethod(TypeSpec.Builder builder,
                                         Set<VariableElement> variableElements) {
        for (VariableElement ve : variableElements) {
            TypeName typeName = TypeName.get(ve.asType());
            FieldSpec.Builder fieldSpec = FieldSpec
                    .builder(typeName, ve.getSimpleName().toString(), Modifier.PRIVATE)
                    .addAnnotation(AnnotationSpec.builder(Schema.class)
                            .addMember("title", "$S", getFieldDesc(ve))
                            .build());
            builder.addField(fieldSpec.build());
            String fieldName = getFieldDefaultName(ve);
            MethodSpec.Builder getMethod = MethodSpec.methodBuilder("get" + fieldName)
                    .returns(typeName)
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("return $L", ve.getSimpleName().toString());
            MethodSpec.Builder setMethod = MethodSpec.methodBuilder("set" + fieldName)
                    .returns(void.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(typeName, ve.getSimpleName().toString())
                    .addStatement("this.$L = $L", ve.getSimpleName().toString(),
                            ve.getSimpleName().toString());
            builder.addMethod(getMethod.build());
            builder.addMethod(setMethod.build());
        }
    }

    protected void addIdSetterAndGetter(TypeSpec.Builder builder) {
        MethodSpec.Builder getMethod = MethodSpec.methodBuilder("getId")
                .returns(ClassName.get(Long.class))
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return $L", "id");
        MethodSpec.Builder setMethod = MethodSpec.methodBuilder("setId")
                .returns(void.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.LONG, "id")
                .addStatement("this.$L = $L", "id", "id");
        builder.addMethod(getMethod.build());
        builder.addMethod(setMethod.build());
    }

    protected String getFieldDefaultName(VariableElement ve) {
        return ve.getSimpleName().toString().substring(0, 1).toUpperCase() + ve.getSimpleName()
                .toString().substring(1);
    }

    protected String getFieldDesc(VariableElement ve) {
        return Optional.ofNullable(ve.getAnnotation(FieldDesc.class))
                .map(FieldDesc::name).orElse(ve.getSimpleName().toString());
    }


    public void genJavaSourceFile(String packageName, String pathStr,
                                  TypeSpec.Builder typeSpecBuilder) {
        TypeSpec typeSpec = typeSpecBuilder.build();
        JavaFile javaFile = JavaFile
                .builder(packageName, typeSpec)
                .addFileComment("---Auto Generated by CC-CodeGen ---")
                .build();
//    System.out.println(javaFile);
        String packagePath =
                packageName.replace(".", File.separator) + File.separator + typeSpec.name + ".java";
        try {
            Path path = Paths.get(pathStr);
            File file = new File(path.toFile().getAbsolutePath());
            if (!file.exists()) {
                return;
            }
            String sourceFileName = path.toFile().getAbsolutePath() + File.separator + packagePath;
            File sourceFile = new File(sourceFileName);
            if (!sourceFile.exists()) {
                javaFile.writeTo(file);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public TypeSpec.Builder getSourceType(String sourceName, String packageName,
                                          String superClassName) {
        TypeSpec.Builder sourceBuilder = TypeSpec.classBuilder(sourceName)
                .superclass(ClassName.get(packageName, superClassName))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Schema.class)
                .addAnnotation(Data.class);
        return sourceBuilder;
    }

    public TypeSpec.Builder getSourceTypeWithConstruct(TypeElement e, String sourceName,
                                                       String packageName, String superClassName) {
        MethodSpec.Builder constructorSpecBuilder = MethodSpec.constructorBuilder()
                .addParameter(TypeName.get(e.asType()), "source")
                .addModifiers(Modifier.PUBLIC);
        constructorSpecBuilder.addStatement("super(source)");
        TypeSpec.Builder sourceBuilder = TypeSpec.classBuilder(sourceName)
                .superclass(ClassName.get(packageName, superClassName))
                .addModifiers(Modifier.PUBLIC)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .build())
                .addMethod(constructorSpecBuilder.build())
                .addAnnotation(Schema.class)
                .addAnnotation(Data.class);
        return sourceBuilder;
    }


    protected void genJavaFile(String packageName, TypeSpec.Builder typeSpecBuilder) {
        JavaFile javaFile = JavaFile.builder(packageName, typeSpecBuilder.build())
                .addFileComment("---Auto Generated by CC-CodeGen ---").build();
        try {
            javaFile.writeTo(ProcessingEnvironmentHolder.getEnvironment().getFiler());
        } catch (IOException e) {
            ProcessingEnvironmentHolder.getEnvironment().getMessager()
                    .printMessage(Kind.ERROR, e.getMessage());
        }
    }

}
