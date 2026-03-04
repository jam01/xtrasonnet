package io.github.jam01.camel.model.language;

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
 * - 0a6a5767eb42ef072ff3ad6b1876c481fd57bea0: CAMEL-18697: camel-core - Propose a DSL for languages (#8688)
 */

import io.github.jam01.xtrasonnet.document.MediaType;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import org.apache.camel.Expression;
import org.apache.camel.model.language.TypedExpressionDefinition;
import org.apache.camel.reifier.language.ExpressionReifier;
import org.apache.camel.reifier.language.XtrasonnetExpressionReifier;
import org.apache.camel.spi.Metadata;

/**
 * To use xtrasonnet transformations.
 */
@Metadata(firstVersion = "3.20.0", label = "language,transformation", title = "xtrasonnet")
@XmlRootElement(name = "xtrasonnet")
@XmlAccessorType(XmlAccessType.FIELD)
public class XtrasonnetExpression extends TypedExpressionDefinition {

    static {
        ExpressionReifier.registerReifier(XtrasonnetExpression.class, (XtrasonnetExpressionReifier::new));
    }

    /**
     * Media type of the message body as a string (for XML serialization).
     */
    @XmlAttribute(name = "bodyMediaType")
    private String bodyMediaTypeString;
    /**
     * Media type of the output as a string (for XML serialization).
     */
    @XmlAttribute(name = "outputMediaType")
    private String outputMediaTypeString;
    /**
     * Media type of the message body (programmatic).
     */
    @XmlTransient
    private MediaType bodyMediaType;
    /**
     * Media type of the output (programmatic).
     */
    @XmlTransient
    private MediaType outputMediaType;

    /**
     * Default constructor.
     */
    public XtrasonnetExpression() {
    }

    /**
     * Constructs an expression with the given xtrasonnet transformation.
     * @param expression the xtrasonnet transformation
     */
    public XtrasonnetExpression(String expression) {
        super(expression);
    }

    /**
     * Constructs an expression with the given Camel expression.
     * @param expression the Camel expression
     */
    public XtrasonnetExpression(Expression expression) {
        super(expression);
    }

    /**
     * Private constructor used by the builder.
     * @param builder the builder instance
     */
    private XtrasonnetExpression(Builder builder) {
        super(builder);
        this.bodyMediaType = builder.bodyMediaType;
        this.outputMediaType = builder.outputMediaType;
    }

    /**
     * Returns the language name "xtrasonnet".
     * @return "xtrasonnet"
     */
    @Override
    public String getLanguage() {
        return "xtrasonnet";
    }

    /**
     * Gets the body media type as a string (for XML serialization).
     * @return the body media type string
     */
    public String getBodyMediaTypeString() {
        return bodyMediaTypeString;
    }

    /**
     * Sets the body media type as a string (for XML serialization).
     * @param bodyMediaTypeString the body media type string
     */
    public void setBodyMediaTypeString(String bodyMediaTypeString) {
        this.bodyMediaTypeString = bodyMediaTypeString;
    }

    /**
     * Gets the output media type as a string (for XML serialization).
     * @return the output media type string
     */
    public String getOutputMediaTypeString() {
        return outputMediaTypeString;
    }

    /**
     * Gets the body media type (programmatic).
     * @return the body media type
     */
    public MediaType getBodyMediaType() {
        return bodyMediaType;
    }

    /**
     * Sets the body media type (programmatic).
     * @param bodyMediaType the body media type
     */
    public void setBodyMediaType(MediaType bodyMediaType) {
        this.bodyMediaType = bodyMediaType;
    }

    /**
     * Gets the output media type (programmatic).
     * @return the output media type
     */
    public MediaType getOutputMediaType() {
        return outputMediaType;
    }

    /**
     * Sets the output media type (programmatic).
     * @param outputMediaType the output media type
     */
    public void setOutputMediaType(MediaType outputMediaType) {
        this.outputMediaType = outputMediaType;
    }

    /**
     * Sets the output media type as a string (for XML serialization).
     * @param outputMediaTypeString the output media type string
     */
    public void setOutputMediaTypeString(String outputMediaTypeString) {
        this.outputMediaTypeString = outputMediaTypeString;
    }

    /**
     * {@code Builder} is a specific builder for {@link XtrasonnetExpression}.
     */
    @XmlTransient
    public static class Builder extends AbstractBuilder<Builder, XtrasonnetExpression> {
        /**
         * The message's body MediaType (programmatic).
         */
        private MediaType bodyMediaType;
        /**
         * The MediaType to output (programmatic).
         */
        private MediaType outputMediaType;

        /**
         * The message's body MediaType
         * @param bodyMediaType the media type
         * @return fluent Builder
         */
        public Builder bodyMediaType(MediaType bodyMediaType) {
            this.bodyMediaType = bodyMediaType;
            return this;
        }

        /**
         * The MediaType to output
         * @param outputMediaType the media type
         * @return fluent Builder
         */
        public Builder outputMediaType(MediaType outputMediaType) {
            this.outputMediaType = outputMediaType;
            return this;
        }

        /**
         * Builds the XtrasonnetExpression instance.
         * @return a new XtrasonnetExpression
         */
        @Override
        public XtrasonnetExpression end() {
            return new XtrasonnetExpression(this);
        }
    }
}
