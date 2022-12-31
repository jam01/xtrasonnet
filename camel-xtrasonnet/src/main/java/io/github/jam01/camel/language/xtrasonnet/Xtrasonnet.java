package io.github.jam01.camel.language.xtrasonnet;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.camel.support.language.LanguageAnnotation;

/**
 * Used to inject a xtrasonnet expression into a field, property, method or parameter when using
 * <a href="http://camel.apache.org/bean-integration.html">Bean Integration</a>.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@LanguageAnnotation(language = "xtrasonnet")
public @interface Xtrasonnet {
    String value();
}
