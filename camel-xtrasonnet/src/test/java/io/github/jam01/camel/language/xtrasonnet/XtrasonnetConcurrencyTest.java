package io.github.jam01.camel.language.xtrasonnet;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.Documents;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A route-scoped XtrasonnetExpression is shared by every exchange that passes through it, and the
 * file's own header cites CAMEL-16918 "Fix concurrency issue and other thread-safety problems" --
 * but the ported fix had been undone, and nothing tested it.
 */
public class XtrasonnetConcurrencyTest extends CamelTestSupport {

    /**
     * doEvaluate cached the exchange's requested result type into the instance field, so the first
     * exchange to ask for one imposed it on every exchange after it.
     */
    @Test
    public void resultType_doesNotLeakBetweenExchanges() {
        var exp = new XtrasonnetExpression("{ root: payload }");
        exp.init(context());

        var first = createExchangeWithBody("{\"foo\": \"bar\"}");
        first.setProperty(XtrasonnetConstants.BODY_MEDIATYPE, "application/json");
        first.setProperty(XtrasonnetConstants.OUTPUT_MEDIATYPE, "application/xml");
        first.setProperty(XtrasonnetConstants.RESULT_TYPE, "java.lang.String");
        assertEquals("<?xml version='1.0' encoding='UTF-8'?><root><foo>bar</foo></root>",
                exp.evaluate(first, Object.class));

        // this exchange asked for nothing, so it must get the default Document back
        var second = createExchangeWithBody("{\"foo\": \"bar\"}");
        second.setProperty(XtrasonnetConstants.BODY_MEDIATYPE, "application/json");
        var result = exp.evaluate(second, Object.class);

        assertInstanceOf(Document.class, result,
                "the previous exchange's result type was applied to this one");
    }

    /**
     * matches() assigned the output media type to the instance field, so evaluating the expression
     * as a predicate once permanently pinned every later exchange's output to application/java.
     */
    @Test
    public void matches_doesNotLeakOutputMediaType() {
        var exp = new XtrasonnetExpression("2 + 2 == 4");
        exp.init(context());

        assertTrue(exp.matches(createExchangeWithBody(Documents.Null())));

        var after = createExchangeWithBody(Documents.Null());
        after.setProperty(XtrasonnetConstants.OUTPUT_MEDIATYPE, "application/json");
        var result = (Document<?>) exp.evaluate(after, Object.class);

        // JSON, as this exchange asked for -- not the java Boolean that matches() pinned
        assertEquals("true", result.getContent());
    }

    /**
     * The transformers are pooled, so concurrent exchanges each get one to themselves.
     */
    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    public void concurrentExchanges_eachGetTheirOwnResult() throws Exception {
        var exp = new XtrasonnetExpression("{ doubled: payload.n * 2 }");
        exp.init(context());

        int threads = 8;
        int iterations = 50;
        var pool = Executors.newFixedThreadPool(threads);
        var start = new CountDownLatch(1);
        var failures = new AtomicReference<Throwable>();

        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                final int base = t * iterations;
                futures.add(pool.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < iterations; i++) {
                            int n = base + i;
                            var ex = createExchangeWithBody("{\"n\": %d}".formatted(n));
                            ex.setProperty(XtrasonnetConstants.BODY_MEDIATYPE, "application/json");
                            ex.setProperty(XtrasonnetConstants.OUTPUT_MEDIATYPE, "application/json");

                            var result = (Document<?>) exp.evaluate(ex, Object.class);
                            assertEquals("{\"doubled\":%d}".formatted(n * 2), result.getContent());
                        }
                    } catch (Throwable e) {
                        failures.compareAndSet(null, e);
                    }
                }));
            }

            start.countDown();
            for (Future<?> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertNull(failures.get(), () -> "concurrent evaluation failed: " + failures.get());
    }
}
