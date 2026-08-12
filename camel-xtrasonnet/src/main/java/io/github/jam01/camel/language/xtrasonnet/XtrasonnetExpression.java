package io.github.jam01.camel.language.xtrasonnet;

/*-
 * Copyright 2022-2026 Jose Montoya.
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

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.jam01.xtrasonnet.Transformer;
import io.github.jam01.xtrasonnet.TransformerBuilder;
import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.MediaType;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import io.github.jam01.xtrasonnet.plugins.DefaultJavaPlugin;
import io.github.jam01.xtrasonnet.spi.Library;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeExpressionException;
import org.apache.camel.spi.ExpressionResultTypeAware;
import org.apache.camel.support.ExchangeHelper;
import org.apache.camel.support.ExpressionAdapter;

import java.util.Collections;
import java.util.Queue;
import java.util.Set;

/**
 * Xtrasonnet expression for Apache Camel.
 * <p>
 * Evaluates a datasonnet/xtrasonnet transformation within a Camel exchange.
 * Supports configurable body media type, output media type, and result type.
 * </p>
 */
public class XtrasonnetExpression extends ExpressionAdapter implements ExpressionResultTypeAware {
    private final String expression;
    // volatile: configured while the route is built, read from worker threads afterwards
    private volatile MediaType bodyMediaType;
    private volatile MediaType outputMediaType;
    private volatile Class<?> resultType;
    private transient volatile XtrasonnetLanguage language;
    // borrowed from for the duration of each evaluation; see XtrasonnetLanguage.poolFor
    private transient volatile Queue<Transformer> pool;

    /**
     * Constructs a new xtrasonnet expression.
     *
     * @param expression the xtrasonnet expression string
     */
    public XtrasonnetExpression(String expression) {
        this.expression = expression;
    }

    @Override
    public boolean matches(Exchange exchange) {
        // as an override for this evaluation only. Assigning this.outputMediaType made a predicate
        // evaluation permanently change the output type of every later exchange on the route.
        return evaluate(exchange, Boolean.class, MediaTypes.APPLICATION_JAVA);
    }

    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {
        return evaluate(exchange, type, null);
    }

    @SuppressWarnings("unchecked")
    private <T> T evaluate(Exchange exchange, Class<T> type, MediaType outputOverride) {
        if (language == null) {
            throw new IllegalStateException("xtrasonnet expression not initialized");
        }

        try {
            // pass exchange to CML lib using thread as context
            CML.getInstance().getExchange().set(exchange);
            Class<?> effectiveResultType = effectiveResultType(exchange);
            Document<?> result = doEvaluate(exchange, outputOverride, effectiveResultType);

            if (type.equals(Document.class)) {
                return (T) result;
            } else if (!type.equals(Object.class)) {
                return ExchangeHelper.convertToType(exchange, type, result.getContent());
            } else if (effectiveResultType == null || effectiveResultType.equals(Document.class)) {
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

    /**
     * The configured result type, or failing that the one this exchange asks for. Deliberately not
     * written back to the field: this instance is shared by every exchange on the route, so caching
     * one exchange's header there applied it to all the later ones.
     */
    private Class<?> effectiveResultType(Exchange exchange) {
        Class<?> configured = resultType;
        if (configured != null) {
            return configured;
        }

        return exchange.getProperty(XtrasonnetConstants.RESULT_TYPE,
                exchange.getMessage().getHeader(XtrasonnetConstants.RESULT_TYPE), Class.class);
    }

    private Document<?> doEvaluate(Exchange exchange, MediaType outputOverride, Class<?> effectiveResultType) {
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

        MediaType outMT = outputOverride != null ? outputOverride : outputMediaType;
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

        // borrow for the duration of this evaluation, then hand it back, so concurrent exchanges
        // never share one. A miss builds a transformer rather than recompiling per message.
        Transformer mapper = pool.poll();
        if (mapper == null) {
            mapper = createTransformer(language.getCamelContext());
        }

        try {
            if (effectiveResultType == null || effectiveResultType.equals(Document.class)) {
                return mapper.transform(body, Collections.emptyMap(), outMT, Object.class);
            } else {
                return mapper.transform(body, Collections.emptyMap(), outMT, effectiveResultType);
            }
        } finally {
            pool.add(mapper);
        }
    }

    private Transformer createTransformer(CamelContext context) {
        TransformerBuilder builder = new TransformerBuilder(expression)
                .withLibrary(CML.getInstance())
                .withPreserveOrder(true)
                .withDefaultInput(MediaTypes.APPLICATION_JAVA)
                .withDefaultOutput(MediaTypes.APPLICATION_JAVA);

        Set<Library> additionalLibraries = context.getRegistry().findByType(Library.class);
        for (Library lib : additionalLibraries) {
            builder = builder.withLibrary(lib);
        }

        JsonMapper mapper = context.getRegistry().lookupByNameAndType("xtrasonnet", JsonMapper.class);
        if (mapper != null) {
            builder.extendPlugins(plugins -> {
                plugins.removeIf(plugin -> plugin instanceof DefaultJavaPlugin);
                plugins.add(0, new DefaultJavaPlugin(mapper));
            });
        }

        return builder.build();
    }

    @Override
    public void init(CamelContext context) {
        super.init(context);
        if (language != null) return;

        // pool is published before language, because evaluate() guards on language alone: a thread
        // that passed that guard while pool was still null would dereference it
        var resolved = (XtrasonnetLanguage) context.resolveLanguage("xtrasonnet");
        var resolvedPool = resolved.poolFor(expression);
        if (resolvedPool.isEmpty()) {
            resolvedPool.add(createTransformer(context)); // compile eagerly, as before
        }

        pool = resolvedPool;
        language = resolved;
    }

    // Getter/Setter methods
    // -------------------------------------------------------------------------
    /**
     * Gets the body media type (programmatic).
     * @return the body media type
     */
    public MediaType getBodyMediaType() {
        return bodyMediaType;
    }

    /**
     * Set the message's body MediaType
     * @param inputMimeType the media type
     */
    public void setBodyMediaType(MediaType inputMimeType) {
        this.bodyMediaType = inputMimeType;
    }

    /**
     * The MediaType to output
     * @return the output MediaType
     */
    public MediaType getOutputMediaType() {
        return outputMediaType;
    }

    /**
     * Set the MediaType to output
     * @param outputMimeType the media type
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
     * Sets the type of the result object.
     * <p>
     * The default result type is {@link Document}
     * </p>
     * @param targetType the object type
     */
    public void setResultType(Class<?> targetType) {
        this.resultType = targetType;
    }

    @Override
    public String toString() {
        return "xtrasonnet: " + expression;
    }

}
