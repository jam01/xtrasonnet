package com.github.jam01.xtrasonnet.modules;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import org.junit.jupiter.api.Test;

import static com.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonPathTest {
    @Test
    public void eval() {
        assertEquals(transform("""
                [
                  "Herman Melville",
                  "J. R. R. Tolkien"
                ]"""), transform("""
                local store = {
                    store: {
                        book: [
                            {
                                category: 'reference',
                                author: 'Nigel Rees',
                                title: 'Sayings of the Century',
                                price: 8.95
                            },
                            {
                                category: 'fiction',
                                author: 'Evelyn Waugh',
                                title: 'Sword of Honour',
                                price: 12.99
                            },
                            {
                                category: 'fiction',
                                author: 'Herman Melville',
                                title: 'Moby Dick',
                                isbn: '0-553-21311-3',
                                price: 8.99
                            },
                            {
                                category: 'fiction',
                                author: 'J. R. R. Tolkien',
                                title: 'The Lord of the Rings',
                                isbn: '0-395-19395-8',
                                price: 22.99
                            }
                        ]
                    }
                };
                            
                xtr.jsonpath.eval(store, '$..book[-2:]..author')"""));
    }
}