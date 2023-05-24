package com.cc.codegen.processor.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.codegen.processor.BaseCodeGenProcessor;
import com.cc.codegen.processor.vo.GenVo;
import com.cc.codegen.spi.CodeGenProcessor;
import com.cc.codegen.util.StringUtils;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Service;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.Optional;

@AutoService(CodeGenProcessor.class)
public class GenMPProcessor extends BaseCodeGenProcessor {

    public static final String DAO_SUFFIX = "Dao";

    public static final String SERVICE_SUFFIX = "Service";

    public static final String SERVICE_IMPL_SUFFIX = "ServiceImpl";
    public static final String DAO_PKG_SUFFIX = ".dao";
    public static final String SERVICE_PKG_SUFFIX = ".service";
    public static final String SERVICE_IMPL_PKG_SUFFIX = ".service.impl";

    @Override
    protected void generateClass(TypeElement typeElement, RoundEnvironment roundEnvironment) {
        // 指定生成的类名称
        String daoName = Optional.of(typeElement.getAnnotation(GenMP.class).daoName())
                .filter(StringUtils::isNotEmpty)
                .orElse(typeElement.getSimpleName() + DAO_SUFFIX);
        String serviceName = Optional.of(typeElement.getAnnotation(GenMP.class).serviceName())
                .filter(StringUtils::isNotEmpty)
                .orElse(typeElement.getSimpleName() + SERVICE_SUFFIX);
        String serviceImplName = Optional.of(typeElement.getAnnotation(GenMP.class).serviceName())
                .filter(StringUtils::isNotEmpty)
                .orElse(typeElement.getSimpleName() + SERVICE_IMPL_SUFFIX);
        // 生成dao
        TypeSpec.Builder daoBuilder = TypeSpec.interfaceBuilder(daoName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Mapper.class)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get(BaseMapper.class),   // raw type
                        ClassName.get(typeElement) //generic type
                ));
        String pkgName = generatePackage(typeElement);
        String daoPkg = pkgName + DAO_PKG_SUFFIX;
        String servicePkg = pkgName + SERVICE_PKG_SUFFIX;
        String implPkg = pkgName + SERVICE_IMPL_PKG_SUFFIX;
        // 生成Service接口
        TypeSpec.Builder serviceBuilder = TypeSpec.interfaceBuilder(serviceName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get(IService.class),   // raw type
                        ClassName.get(typeElement) //generic type
                ));
        // 生成ServiceImpl实现类
        TypeSpec.Builder serviceImplBuilder = TypeSpec.classBuilder(serviceImplName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Service.class)
                .superclass(ParameterizedTypeName.get(
                        ClassName.get(ServiceImpl.class),
                        ClassName.get(daoPkg,daoName),
                        ClassName.get(typeElement)
                ))
                .addSuperinterface(ClassName.get(servicePkg,serviceName));
        String sourcePath = typeElement.getAnnotation(GenVo.class).sourcePath();
        genJavaSourceFile(daoPkg, sourcePath, daoBuilder);
        genJavaSourceFile(servicePkg, sourcePath, serviceBuilder);
        genJavaSourceFile(implPkg, sourcePath, serviceImplBuilder);
    }

    @Override
    public Class<? extends Annotation> getAnnotation() {
        return GenMP.class;
    }

    @Override
    public String generatePackage(TypeElement typeElement) {
        return typeElement.getAnnotation(GenMP.class).pckName();
    }
}
