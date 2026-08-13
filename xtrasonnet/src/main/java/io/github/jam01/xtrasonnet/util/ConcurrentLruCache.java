package io.github.jam01.xtrasonnet.util;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

/* spring-framework copyright/notice, per Apache-2.0 § 4.c */
/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Changes made:
 * - moved out of MediaTypeUtils into its own file, so other callers can reuse it
 */

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * Simple Least Recently Used cache, bounded by the maximum size given
 * to the class constructor.
 * <p>This implementation is backed by a {@code ConcurrentHashMap} for storing
 * the cached values and a {@code ConcurrentLinkedQueue} for ordering the keys
 * and choosing the least recently used key when the cache is at full capacity.
 *
 * @param <K> the type of the key used for caching
 * @param <V> the type of the cached values
 */
public class ConcurrentLruCache<K, V> {

    private final int maxSize;

    private final ConcurrentLinkedDeque<K> queue = new ConcurrentLinkedDeque<>();

    private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();

    private final ReadWriteLock lock;

    private final Function<K, V> generator;

    private volatile int size;

    public ConcurrentLruCache(int maxSize, Function<K, V> generator) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("LRU max size should be positive");
        }

        Objects.requireNonNull(generator, "Generator function should not be null");
        this.maxSize = maxSize;
        this.generator = generator;
        this.lock = new ReentrantReadWriteLock();
    }

    public V get(K key) {
        V cached = this.cache.get(key);
        if (cached != null) {
            if (this.size < this.maxSize) {
                return cached;
            }
            this.lock.readLock().lock();
            try {
                if (this.queue.removeLastOccurrence(key)) {
                    this.queue.offer(key);
                }
                return cached;
            } finally {
                this.lock.readLock().unlock();
            }
        }
        this.lock.writeLock().lock();
        try {
            // Retrying in case of concurrent reads on the same key
            cached = this.cache.get(key);
            if (cached != null) {
                if (this.queue.removeLastOccurrence(key)) {
                    this.queue.offer(key);
                }
                return cached;
            }
            // Generate value first, to prevent size inconsistency
            V value = this.generator.apply(key);
            int cacheSize = this.size;
            if (cacheSize == this.maxSize) {
                K leastUsed = this.queue.poll();
                if (leastUsed != null) {
                    this.cache.remove(leastUsed);
                    cacheSize--;
                }
            }
            this.queue.offer(key);
            this.cache.put(key, value);
            this.size = cacheSize + 1;
            return value;
        } finally {
            this.lock.writeLock().unlock();
        }
    }
}
