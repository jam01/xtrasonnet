package io.github.jam01.xtrasonnet;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.Documents;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import io.github.jam01.xtrasonnet.plugins.DefaultCSVPlugin;
import io.github.jam01.xtrasonnet.spi.JLibrary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import sjsonnet.Val;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * There were no concurrency tests at all, which is why every problem below survived.
 */
public class ConcurrencyTest {
    private static final int THREADS = 8;
    private static final int ITERATIONS = 150;

    /**
     * The plugin caches were {@code static HashMap}s mutated through {@code computeIfAbsent}, shared
     * by every Transformer in the JVM. That broke even the correct usage this test models -- one
     * Transformer per thread -- because the plugin instances behind
     * {@link DataFormatService#DEFAULT} are shared regardless. Many distinct parameter maps are used
     * so the tables resize while under contention.
     */
    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS) // a corrupted HashMap can spin rather than fail
    public void pluginCaches_surviveConcurrentUse() throws Exception {
        var separators = new char[] { ',', '|', ';', ':', '\t', '#', '~', '^' };
        var pool = Executors.newFixedThreadPool(THREADS);
        var start = new CountDownLatch(1);
        var failures = new AtomicReference<Throwable>();

        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                futures.add(pool.submit(() -> {
                    try {
                        start.await();
                        // one Transformer per thread: the documented, correct usage
                        var transformer = new Transformer("payload");

                        for (int i = 0; i < ITERATIONS; i++) {
                            char sep = separators[i % separators.length];
                            var csv = "a%sb\n1%s2\n".formatted(sep, sep);
                            var result = transformer.transform(
                                    Document.of(csv, MediaTypes.TEXT_CSV.withParameter(
                                            DefaultCSVPlugin.PARAM_SEPARATOR_CHAR, String.valueOf(sep))),
                                    Map.of(), MediaTypes.APPLICATION_JSON);

                            assertEquals("[{\"a\":\"1\",\"b\":\"2\"}]", result.getContent());
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

        assertNull(failures.get(), () -> "concurrent plugin use failed: " + failures.get());
    }

    /**
     * A Transformer cannot be shared, because the state evaluation mutates lives inside sjsonnet
     * (see the class doc on {@link Transformer}). The point of the guard is that overlapping use
     * fails here, where the mistake is, rather than silently corrupting a cache and surfacing much
     * later as a wrong result or a spinning thread.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void sharedTransformer_rejectsOverlappingUse() throws Exception {
        var lib = new BlockingLib();
        var transformer = Transformer.builder("{ a: blocklib.block('x') }").withLibrary(lib).build();

        var holder = Executors.newSingleThreadExecutor();
        try {
            Future<String> held = holder.submit(() -> transformer.transform(Documents.Null()).getContent());

            // the holding thread is now inside transform, blocked in the library function
            assertTrue(lib.entered.await(10, TimeUnit.SECONDS), "library function was never reached");

            var ex = assertThrows(XtrasonnetException.class,
                    () -> transformer.transform(Documents.Null()));
            assertTrue(ex.getMessage().contains("already in use by thread"), ex.getMessage());
            assertTrue(ex.getMessage().contains("one per thread"), ex.getMessage());

            lib.release.countDown();
            assertEquals("{\"a\":\"blocked\"}", held.get(10, TimeUnit.SECONDS));
        } finally {
            lib.release.countDown();
            holder.shutdownNow();
        }
    }

    /**
     * The guard must not reject a thread that already released, or reuse would be a one-shot.
     */
    @Test
    public void sameTransformer_isReusableSequentially() {
        var transformer = new Transformer("{ a: payload }");

        for (int i = 0; i < 5; i++) {
            assertEquals("{\"a\":%d}".formatted(i),
                    transformer.transform(Document.of(String.valueOf(i), MediaTypes.APPLICATION_JSON),
                            Map.of(), MediaTypes.APPLICATION_JSON).getContent());
        }
    }

    /**
     * And a failed evaluation must release it too, or one bad payload would wedge the Transformer
     * for good.
     */
    @Test
    public void failedTransform_releasesTheTransformer() {
        var transformer = new Transformer("{ a: payload.missing.deeper }");

        assertThrows(RuntimeException.class,
                () -> transformer.transform(Document.of("{}", MediaTypes.APPLICATION_JSON),
                        Map.of(), MediaTypes.APPLICATION_JSON));

        // still usable: the guard was released on the way out
        assertEquals("{\"a\":\"here\"}",
                new Transformer("{ a: payload.missing }").transform(
                        Document.of("{\"missing\": \"here\"}", MediaTypes.APPLICATION_JSON),
                        Map.of(), MediaTypes.APPLICATION_JSON).getContent());
        assertThrows(RuntimeException.class,
                () -> transformer.transform(Document.of("{}", MediaTypes.APPLICATION_JSON),
                        Map.of(), MediaTypes.APPLICATION_JSON));
    }

    /** A library function that parks inside evaluation, so two threads can be made to overlap. */
    public static class BlockingLib extends JLibrary {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        @Override
        public String name() {
            return "blocklib";
        }

        @Override
        public Map<String, Val.Func> functions() {
            var res = new HashMap<String, Val.Func>();
            res.put("block", jbuiltin(new String[]{"param"}, (vals, pos, ev) -> {
                entered.countDown();
                try {
                    release.await(20, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return new Val.Str(position(), "blocked");
            }));
            return res;
        }
    }
}
