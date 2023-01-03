package io.github.jam01.camel.model.language;

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
 * - 0a6a5767eb42ef072ff3ad6b1876c481fd57bea0: CAMEL-18697: camel-core - Propose a DSL for languages (#8688)
 */

import io.github.jam01.xtrasonnet.document.MediaType;
import org.apache.camel.Expression;
import org.apache.camel.model.language.TypedExpressionDefinition;
import org.apache.camel.reifier.language.ExpressionReifier;
import org.apache.camel.reifier.language.XtrasonnetExpressionReifier;
import org.apache.camel.spi.Metadata;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * To use xtrasonnet scripts for message transformations.
 */
@Metadata(firstVersion = "3.20.0", label = "language,transformation", title = "xtrasonnet")
@XmlRootElement(name = "xtrasonnet")
@XmlAccessorType(XmlAccessType.FIELD)
public class XtrasonnetExpression extends TypedExpressionDefinition {

    static {
        ExpressionReifier.registerReifier(XtrasonnetExpression.class, (XtrasonnetExpressionReifier::new));
    }

    @XmlAttribute(name = "bodyMediaType")
    private String bodyMediaTypeString;
    @XmlAttribute(name = "outputMediaType")
    private String outputMediaTypeString;
    @XmlTransient
    private MediaType bodyMediaType;
    @XmlTransient
    private MediaType outputMediaType;

    public XtrasonnetExpression() {
    }

    public XtrasonnetExpression(String expression) {
        super(expression);
    }

    public XtrasonnetExpression(Expression expression) {
        super(expression);
    }

    private XtrasonnetExpression(Builder builder) {
        super(builder);
        this.bodyMediaType = builder.bodyMediaType;
        this.outputMediaType = builder.outputMediaType;
    }

    @Override
    public String getLanguage() {
        return "xtrasonnet";
    }

    public String getBodyMediaTypeString() {
        return bodyMediaTypeString;
    }

    public void setBodyMediaTypeString(String bodyMediaTypeString) {
        this.bodyMediaTypeString = bodyMediaTypeString;
    }

    public String getOutputMediaTypeString() {
        return outputMediaTypeString;
    }

    public MediaType getBodyMediaType() {
        return bodyMediaType;
    }

    public void setBodyMediaType(MediaType bodyMediaType) {
        this.bodyMediaType = bodyMediaType;
    }

    public MediaType getOutputMediaType() {
        return outputMediaType;
    }

    public void setOutputMediaType(MediaType outputMediaType) {
        this.outputMediaType = outputMediaType;
    }

    public void setOutputMediaTypeString(String outputMediaTypeString) {
        this.outputMediaTypeString = outputMediaTypeString;
    }

    /**
     * {@code Builder} is a specific builder for {@link XtrasonnetExpression}.
     */
    @XmlTransient
    public static class Builder extends AbstractBuilder<Builder, XtrasonnetExpression> {
        private MediaType bodyMediaType;
        private MediaType outputMediaType;

        /**
         * The message's body MediaType
         */
        public Builder bodyMediaType(MediaType bodyMediaType) {
            this.bodyMediaType = bodyMediaType;
            return this;
        }

        /**
         * The MediaType to output
         */
        public Builder outputMediaType(MediaType outputMediaType) {
            this.outputMediaType = outputMediaType;
            return this;
        }

        @Override
        public XtrasonnetExpression end() {
            return new XtrasonnetExpression(this);
        }
    }
}
