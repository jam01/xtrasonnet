package io.github.jam01.camel.builder;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.camel.model.language.XtrasonnetExpression;
import io.github.jam01.xtrasonnet.document.MediaType;
import org.apache.camel.Expression;
import org.apache.camel.builder.ValueBuilder;

/**
 * Builder for creating Xtrasonnet expressions in Apache Camel routes.
 * <p>
 * Provides static methods to create xtrasonnet expressions with optional result type,
 * body media type, and output media type.
 * </p>
 */
public final class XtrasonnetBuilder extends ValueBuilder {

    /**
     * Constructs a new XtrasonnetBuilder with the given expression.
     *
     * @param expression the expression to wrap
     */
    public XtrasonnetBuilder(Expression expression) {
        super(expression);
    }

    /**
     * An xtrasonnet expression value builder
     * @param value the xtrasonnet transformation
     * @return fluent Builder
     */
    public static XtrasonnetBuilder xtrasonnet(String value) {
        return xtrasonnet(value, null);
    }

    /**
     * An xtrasonnet expression value builder
     * @param value the xtrasonnet transformation
     * @param resultType the expression result object type
     * @return fluent Builder
     */
    public static XtrasonnetBuilder xtrasonnet(String value, Class<?> resultType) {
        XtrasonnetExpression exp = new XtrasonnetExpression(value);
        exp.setResultType(resultType);
        return new XtrasonnetBuilder(exp);
    }

    /**
     * Adds the given body MediaTupe to the value builder
     * @param bodyMediaType the media type
     * @return fluent Builder
     */
    public XtrasonnetBuilder bodyMediaType(MediaType bodyMediaType) {
        ((XtrasonnetExpression) getExpression()).setBodyMediaType(bodyMediaType);
        return this;
    }

    /**
     * Adds the given output MediaType to the value builder
     * @param outputMediaType the media type
     * @return fluent Builder
     */
    public XtrasonnetBuilder outputMediaType(MediaType outputMediaType) {
        ((XtrasonnetExpression) getExpression()).setOutputMediaType(outputMediaType);
        return this;
    }
}
