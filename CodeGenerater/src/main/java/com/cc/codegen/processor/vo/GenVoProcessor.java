package com.cc.codegen.processor.vo;

import com.cc.codegen.processor.BaseCodeGenProcessor;
import com.cc.codegen.spi.CodeGenProcessor;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Set;

@AutoService(CodeGenProcessor.class)
public class GenVoProcessor extends BaseCodeGenProcessor {

    public static final String SUFFIX = "VO";

    @Override
    protected void generateClass(TypeElement typeElement, RoundEnvironment roundEnvironment) {
        // 找出未标记IgnoreVo注解的元素（属性）
        Set<VariableElement> fields = findFields(typeElement,
                ve -> Objects.isNull(ve.getAnnotation(IgnoreVo.class)));
        // 生成class名称
        String className = PREFIX + typeElement.getSimpleName() + SUFFIX;
        String sourceClassName = typeElement.getSimpleName() + SUFFIX;
        // 生成class
        TypeSpec.Builder builder = TypeSpec.classBuilder(sourceClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Data.class)
                .addAnnotation(Schema.class);
        // 生成getter、setter
        addSetterAndGetterMethod(builder, fields);
        // 生成构造方法
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addParameter(TypeName.get(typeElement.asType()), "source")
                .addModifiers(Modifier.PUBLIC);
        fields.forEach(f -> constructorBuilder.addStatement("this.set$L(source.get$L())", getFieldDefaultName(f),
                getFieldDefaultName(f)));
        // 生成无参构造
        builder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PROTECTED)
                .build());
        builder.addMethod(constructorBuilder.build());
        // 包路径
        String pkgName = generatePackage(typeElement);
//        genJavaFile(pkgName,builder);
//        genJavaFile(pkgName, getSourceTypeWithConstruct(typeElement,sourceClassName, pkgName, className));
        genJavaSourceFile(pkgName, typeElement.getAnnotation(GenVo.class).sourcePath(), builder);
    }

    @Override
    public Class<? extends Annotation> getAnnotation() {
        return GenVo.class;
    }

    @Override
    public String generatePackage(TypeElement typeElement) {
        return typeElement.getAnnotation(GenVo.class).pkgName();
    }
}
