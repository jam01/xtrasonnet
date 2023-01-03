package io.github.jam01.camel.builder;

/*-
 * Copyright 2022-2023 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.camel.model.language.XtrasonnetExpression;
import io.github.jam01.xtrasonnet.document.MediaType;
import org.apache.camel.Expression;
import org.apache.camel.builder.ValueBuilder;

public final class XtrasonnetBuilder extends ValueBuilder {

    public XtrasonnetBuilder(Expression expression) {
        super(expression);
    }

    /**
     * Returns an xtrasonnet expression value builder
     */
    public static XtrasonnetBuilder xtrasonnet(String value) {
        return xtrasonnet(value, null);
    }

    /**
     * Returns an xtrasonnet expression value builder
     */
    public static XtrasonnetBuilder xtrasonnet(String value, Class<?> resultType) {
        XtrasonnetExpression exp = new XtrasonnetExpression(value);
        exp.setResultType(resultType);
        return new XtrasonnetBuilder(exp);
    }

    /**
     * Adds the given body media type to the value builder
     */
    public XtrasonnetBuilder bodyMediaType(MediaType bodyMediaType) {
        ((XtrasonnetExpression) getExpression()).setBodyMediaType(bodyMediaType);
        return this;
    }

    /**
     * Adds the given output media type to the value builder
     */
    public XtrasonnetBuilder outputMediaType(MediaType outputMediaType) {
        ((XtrasonnetExpression) getExpression()).setOutputMediaType(outputMediaType);
        return this;
    }
}
