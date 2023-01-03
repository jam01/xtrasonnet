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
 * - de45553bada3b1cabd0aacd520684c042d85480c: CAMEL-18731: Add result type and different sources of input data to l
 *  ...anguages (#8778)
 */

import io.github.jam01.xtrasonnet.Transformer;
import io.github.jam01.xtrasonnet.document.MediaType;
import org.apache.camel.Expression;
import org.apache.camel.Predicate;
import org.apache.camel.spi.annotations.Language;
import org.apache.camel.support.LRUCacheFactory;
import org.apache.camel.support.LanguageSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Language("xtrasonnet")
public class XtrasonnetLanguage extends LanguageSupport {
    private static final Logger LOG = LoggerFactory.getLogger(XtrasonnetLanguage.class);

    // Cache used to stores the Mappers
    // See: {@link GroovyLanguage}
    private final Map<String, Transformer> mapperCache = LRUCacheFactory.newLRUSoftCache(16, 1000, true);

    @Override
    public Predicate createPredicate(String expression) {
        return createPredicate(expression, null);
    }

    @Override
    public Expression createExpression(String expression) {
        return createExpression(expression, null);
    }

    @Override
    public Predicate createPredicate(String expression, Object[] properties) {
        return (Predicate) createExpression(expression, properties);
    }

    @Override
    public Expression createExpression(String expression, Object[] properties) {
        expression = loadResource(expression);
        XtrasonnetExpression answer = new XtrasonnetExpression(expression);

        answer.setResultType(property(Class.class, properties, 0, null));
        MediaType bodyMediaType = property(MediaType.class, properties, 1, null);
        answer.setBodyMediaType(bodyMediaType);
        MediaType outputMediaType = property(MediaType.class, properties, 2, null);
        answer.setOutputMediaType(outputMediaType);

        return answer;
    }

    Optional<Transformer> lookup(String script) {
        return Optional.ofNullable(mapperCache.get(script));
    }

    Transformer computeIfMiss(String script, Supplier<Transformer> mapperSupplier) {
        return mapperCache.computeIfAbsent(script, k -> mapperSupplier.get());
    }
}
