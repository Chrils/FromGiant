package com.cc.codegen.processor.mybatis;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GenMP {

    String pckName();

    String sourcePath() default "src/main/java";

    String daoName() default "";

    String serviceName() default "";

}
