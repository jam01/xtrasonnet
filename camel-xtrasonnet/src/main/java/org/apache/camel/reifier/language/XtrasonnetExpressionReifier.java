package org.apache.camel.reifier.language;

/*-
 * Copyright 2022-2023 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.camel.model.language.XtrasonnetExpression;
import org.apache.camel.CamelContext;
import org.apache.camel.model.language.ExpressionDefinition;

public class XtrasonnetExpressionReifier extends TypedExpressionReifier<XtrasonnetExpression> {
    public XtrasonnetExpressionReifier(CamelContext camelContext, ExpressionDefinition definition) {
        super(camelContext, definition);
    }

    @Override
    protected Object[] createProperties() {
        Object[] properties = new Object[3];
        properties[0] = definition.getResultType();
        properties[1] = parseString(definition.getBodyMediaType());
        properties[2] = parseString(definition.getOutputMediaType());
        return properties;
    }
}
