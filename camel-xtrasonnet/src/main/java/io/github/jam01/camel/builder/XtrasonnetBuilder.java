package io.github.jam01.camel.builder;

/*-
 * Copyright 2022-2023 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

/* camel copyright/notice, per Apache-2.0 § 4.c */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Work covered:
 * - 4f70839b985464fce64ffb8532e436920d86cc80:  CAMEL-17075: Deprecate DataSonnetBuilder to not use it, and prepare f
 *  ...or deletion.
 */

import io.github.jam01.camel.model.language.XtrasonnetExpression;
import org.apache.camel.Expression;
import org.apache.camel.builder.ValueBuilder;
import org.apache.camel.reifier.language.ExpressionReifier;
import org.apache.camel.reifier.language.XtrasonnetExpressionReifier;

public final class XtrasonnetBuilder extends ValueBuilder {
    static {
        ExpressionReifier.registerReifier(XtrasonnetExpression.class, (XtrasonnetExpressionReifier::new));
    }
    public XtrasonnetBuilder(Expression expression) {
        super(expression);
    }

    /**
     * Returns a xtrasonnet expression value builder
     */
    public static XtrasonnetBuilder xtrasonnet(String value) {
        return xtrasonnet(value, null);
    }

    /**
     * Returns a xtrasonnet expression value builder
     */
    public static XtrasonnetBuilder xtrasonnet(String value, Class<?> resultType) {
        XtrasonnetExpression exp = new XtrasonnetExpression(value);
        exp.setResultType(resultType);
        return new XtrasonnetBuilder(exp);
    }

    /**
     * Returns a xtrasonnet expression value builder
     */
    public static ValueBuilder xtrasonnet(String value, Class<?> resultType, String bodyMediaType, String outputMediaType) {
        XtrasonnetExpression exp = new XtrasonnetExpression(value);
        exp.setResultType(resultType);
        exp.setBodyMediaType(bodyMediaType);
        exp.setOutputMediaType(outputMediaType);
        return new ValueBuilder(exp);
    }
}
