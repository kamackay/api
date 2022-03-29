package com.keithmackay.api.benchmark;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Benchmark {
    /**
     * @return Whether to get the first parameter as a String and append to the Benchmark name
     */
    boolean paramName() default false;

    long limit() default 0;
}
