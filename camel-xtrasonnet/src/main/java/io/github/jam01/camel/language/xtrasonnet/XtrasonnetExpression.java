package io.github.jam01.camel.language.xtrasonnet;

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
 * - c798e1b00be671057274429794197d121c5ad172: CAMEL-15804: Polished
 * - b482498205ffd53b6f7046cf8c3698e3f53809ca: CAMEL-16918: camel-datasonnet - Fix concurrency issue and other threa
 *  ...d-safety problems.
 * - 92138ec1b4796ae6f1fe8cb6f75e6cb4a8517c3e: Datasonnet libraries autodiscovery (#7374)
 */

import java.util.Collections;
import java.util.Set;

import io.github.jam01.xtrasonnet.Transformer;
import io.github.jam01.xtrasonnet.TransformerBuilder;
import io.github.jam01.xtrasonnet.TransformerSettings;
import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.MediaType;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import io.github.jam01.xtrasonnet.spi.Library;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeExpressionException;
import org.apache.camel.spi.ExpressionResultTypeAware;
import org.apache.camel.support.ExchangeHelper;
import org.apache.camel.support.ExpressionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XtrasonnetExpression extends ExpressionAdapter implements ExpressionResultTypeAware {
    private static final Logger LOG = LoggerFactory.getLogger(XtrasonnetExpression.class);

    private final String expression;
    private MediaType bodyMediaType;
    private MediaType outputMediaType;
    private Class<?> resultType;
    private transient XtrasonnetLanguage language;

    public XtrasonnetExpression(String expression) {
        this.expression = expression;
    }

    @Override
    public boolean matches(Exchange exchange) {
        this.outputMediaType = MediaTypes.APPLICATION_JAVA;
        return evaluate(exchange, Boolean.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {
        try {
            // pass exchange to CML lib using thread as context
            CML.getInstance().getExchange().set(exchange);

            Document<?> result = doEvaluate(exchange);

            if (type.equals(Document.class)) {
                return (T) result;
            } else if (!type.equals(Object.class)) {
                return ExchangeHelper.convertToType(exchange, type, result.getContent());
            } else if (resultType == null || resultType.equals(Document.class)) {
                return (T) result;
            } else {
                return (T) result.getContent();
            }
        } catch (Exception e) {
            throw new RuntimeExpressionException("Unable to evaluate xtrasonnet expression: " + expression, e);
        } finally {
            CML.getInstance().getExchange().remove();
        }
    }

    private Document<?> doEvaluate(Exchange exchange) {
        if (resultType == null) {
            resultType = exchange.getProperty(XtrasonnetConstants.RESULT_TYPE,
                    exchange.getMessage().getHeader(XtrasonnetConstants.RESULT_TYPE), Class.class);
        }

        MediaType bodyMT = bodyMediaType;
        if (bodyMT == null) {
            //Try to auto-detect input mime type if it was not explicitly set
            String typeHeader = exchange.getProperty(XtrasonnetConstants.BODY_MEDIATYPE,
                    exchange.getMessage().getHeader(XtrasonnetConstants.BODY_MEDIATYPE,
                            exchange.getMessage().getHeader(Exchange.CONTENT_TYPE)), String.class);
            if (typeHeader != null) {
                bodyMT = MediaType.valueOf(typeHeader);
            }
        }

        Document<?> body;
        if (exchange.getMessage().getBody() instanceof Document) {
            body = (Document<?>) exchange.getMessage().getBody();
        } else {
            body = Document.of(exchange.getMessage().getBody(), bodyMT);
        }

        // the mapper is pre initialized
        Transformer mapper = language.lookup(expression)
                .orElseThrow(() -> new IllegalStateException("xtrasonnet expression not initialized"));

        MediaType outMT = outputMediaType;
        if (outMT == null) {
            //Try to auto-detect output mime type if it was not explicitly set
            String typeHeader = exchange.getProperty(XtrasonnetConstants.OUTPUT_MEDIATYPE,
                    exchange.getMessage().getHeader(XtrasonnetConstants.OUTPUT_MEDIATYPE), String.class);
            if (typeHeader != null) {
                outMT = MediaType.valueOf(typeHeader);
            } else {
                outMT = MediaTypes.ANY;
            }
        }

        if (resultType == null || resultType.equals(Document.class)) {
            return mapper.transform(body, Collections.emptyMap(), outMT, Object.class);
        } else {
            return mapper.transform(body, Collections.emptyMap(), outMT, resultType);
        }
    }

    @Override
    public void init(CamelContext context) {
        super.init(context);
        if (language != null) return;

        language = (XtrasonnetLanguage) context.resolveLanguage("xtrasonnet");
        // initialize mapper eager
        language.computeIfMiss(expression, () -> {
            TransformerBuilder builder = new TransformerBuilder(expression)
                    .withLibrary(CML.getInstance())
                    .withSettings(new TransformerSettings(true, false, false,
                            MediaTypes.APPLICATION_JAVA, MediaTypes.APPLICATION_JAVA));

            Set<Library> additionalLibraries = context.getRegistry().findByType(Library.class);
            for (Library lib : additionalLibraries) {
                builder = builder.withLibrary(lib);
            }
            return builder.build();
        });
    }

    // Getter/Setter methods
    // -------------------------------------------------------------------------
    public MediaType getBodyMediaType() {
        return bodyMediaType;
    }

    /**
     * The message's body MediaType
     */
    public void setBodyMediaType(MediaType inputMimeType) {
        this.bodyMediaType = inputMimeType;
    }

    public MediaType getOutputMediaType() {
        return outputMediaType;
    }

    /**
     * The MediaType to output
     */
    public void setOutputMediaType(MediaType outputMimeType) {
        this.outputMediaType = outputMimeType;
    }

    @Override
    public String getExpressionText() {
        return this.expression;
    }

    @Override
    public Class<?> getResultType() {
        return this.resultType;
    }

    /**
     * Sets the class of the result type (type from output).
     * <p/>
     * The default result type is io.github.jam01.xtrasonnet.document.Document
     */
    public void setResultType(Class<?> targetType) {
        this.resultType = targetType;
    }

    @Override
    public String toString() {
        return "xtrasonnet: " + expression;
    }

}
