package io.github.jam01.camel.language.xtrasonnet;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.document.MediaType;
import org.apache.camel.Expression;
import org.apache.camel.builder.ValueBuilder;

public final class XtrasonnetBuilder extends ValueBuilder {
    public XtrasonnetBuilder(Expression expression) {
        super(expression);
    }

    public static XtrasonnetBuilder xtrasonnet(String value) {
        return xtrasonnet(value, null);
    }

    /**
     * Returns a datasonnet expression value builder
     */
    public static XtrasonnetBuilder xtrasonnet(String value, Class<?> resultType) {
        XtrasonnetExpression exp = new XtrasonnetExpression(value);
        exp.setResultType(resultType);
        return new XtrasonnetBuilder(exp);
    }

    public XtrasonnetBuilder bodyMediaType(MediaType bodyMediaType) {
        ((XtrasonnetExpression) this.getExpression()).bodyMediaType(bodyMediaType);
        return this;
    }

    public XtrasonnetBuilder outputMediaType(MediaType outputMediaType) {
        ((XtrasonnetExpression) this.getExpression()).outputMediaType(outputMediaType);
        return this;
    }
}
