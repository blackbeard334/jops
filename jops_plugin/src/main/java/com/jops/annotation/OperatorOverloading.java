package com.jops.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface OperatorOverloading {
    String ANNOTATION = "@OperatorOverloading";
    String NAME       = "OperatorOverloading";
}
