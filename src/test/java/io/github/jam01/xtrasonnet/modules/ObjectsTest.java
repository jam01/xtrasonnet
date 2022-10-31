package io.github.jam01.xtrasonnet.modules;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static io.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectsTest {
    @Test
    public void all() {
        assertEquals(transform("false"), transform("""
                local languages = {
                    scala: { version: '3.1.3', isJvm: true },
                    java: { version: '19', isJvm: true },
                    python: { version: '3.10.4', isJvm: false }
                };

                xtr.objects.all(languages, function(lang, name) lang.isJvm)"""));
    }

    @Test
    public void any() {
        assertEquals(transform("true"), transform("""
                local languages = {
                    scala: { version: '3.1.3', isJvm: true },
                    java: { version: '19', isJvm: true },
                    python: { version: '3.10.4', isJvm: false }
                };

                xtr.objects.any(languages, function(lang, name) lang.isJvm)"""));
    }

    @Test
    public void distinctBy() {
        assertEquals(transform("""
                {
                    first: { name: 'scala', version: '3.1.3', isJvm: true },
                    second: { name: 'java', version: '19', isJvm: true }
                }"""), transform("""
                local languages = {
                    first: { name: 'scala', version: '3.1.3', isJvm: true },
                    second: { name: 'java', version: '19', isJvm: true },
                    third: { name: 'java', version: '18', isJvm: true }
                };

                xtr.objects.distinctBy(languages, function(lang) lang.name)"""));
        assertEquals(transform("""
                {
                    first: { name: 'scala', version: '3.1.3', isJvm: true },
                    second: { name: 'java', version: '19', isJvm: true },
                    third: { name: 'java', version: '18', isJvm: true }
                }"""), transform("""
                local languages = {
                    first: { name: 'scala', version: '3.1.3', isJvm: true },
                    second: { name: 'java', version: '19', isJvm: true },
                    third: { name: 'java', version: '18', isJvm: true }
                };

                xtr.objects.distinctBy(languages, function(lang, ordinal)
                    if (lang.name == 'java') then lang.version
                    else ordinal
                )"""));
    }

    @Test
    public void fromArray() {
        assertEquals(transform("""
                {
                    java: { name: 'java', version: '19', isPreferred: false },
                    python: { name: 'python', version: '3.1.14', isPreferred: false },
                    default: { name: 'scala', version: '3.1.3', isPreferred: true }
                }"""), transform("""
                local languages = [
                    { name: 'java', version: '19', isPreferred: false },
                    { name: 'python', version: '3.1.14', isPreferred: false },
                    { name: 'scala', version: '3.1.3', isPreferred: true }
                ];

                xtr.objects.fromArray(languages, function(lang) if (lang.isPreferred) then 'default' else lang.name)"""));
    }

    @Test
    public void innerEqJoin() throws JSONException {
        JSONAssert.assertEquals(transform("""
                [
                    { id: 2, email: 'joe@example.com', joined: '2021-07-30',
                        orderId: 10308, customerId: 2, date: '2022-07-30' },
                    { id: 2, email: 'joe@example.com', joined: '2021-07-30',
                        orderId: 10309, customerId: 2, date: '2022-07-30' },
                    { id: 77, email: 'jane@example.com', joined: '2019-07-30',
                        orderId: 10310, customerId: 77, date: '2022-07-03' }
                ]"""), transform("""
                local customers = [
                    { id: 2, email: 'joe@example.com', joined: '2021-07-30' },
                    { id: 77, email: 'jane@example.com', joined: '2019-07-30' },
                    { id: 17, email: 'john@example.com', joined: '2002-07-03' }
                ];

                local orders = [
                    { orderId: 10308, customerId: 2, date: '2022-07-30' },
                    { orderId: 10309, customerId: 2, date: '2022-07-30' },
                    { orderId: 10310, customerId: 77, date: '2022-07-03' },
                    { orderId: 10311, customerId: 93, date: '2021-05-03' }
                ];

                xtr.objects.innerEqJoin(customers, orders,
                    function(cust) cust.id, function(order) order.customerId)"""), true);
        assertEquals(transform("""
                [
                    { id: 2, oId: 10308 },
                    { id: 2, oId: 10309 },
                    { id: 77, oId: 10310 }
                ]"""), transform("""
                local customers = [
                    { id: 2, email: 'joe@example.com', joined: '2021-07-30' },
                    { id: 77, email: 'jane@example.com', joined: '2019-07-30' },
                    { id: 17, email: 'john@example.com', joined: '2002-07-03' }
                ];

                local orders = [
                    { orderId: 10308, customerId: 2, date: '2022-07-30' },
                    { orderId: 10309, customerId: 2, date: '2022-07-30' },
                    { orderId: 10310, customerId: 77, date: '2022-07-03' },
                    { orderId: 10311, customerId: 93, date: '2021-05-03' }
                ];

                xtr.objects.innerEqJoin(customers, orders,
                    function(cust) cust.id, function(order) order.customerId,
                    function(cust, order) { id: cust.id, oId: order.orderId })"""));
    }

    @Test
    public void leftEqJoin() throws JSONException {
        JSONAssert.assertEquals(transform("""
                [
                    { id: 2, email: 'joe@example.com', joined: '2021-07-30',
                        orderId: 10308, customerId: 2, date: '2022-07-30' },
                    { id: 2, email: 'joe@example.com', joined: '2021-07-30',
                        orderId: 10309, customerId: 2, date: '2022-07-30' },
                    { id: 77, email: 'jane@example.com', joined: '2019-07-30',
                        orderId: 10310, customerId: 77, date: '2022-07-03' },
                    { id: 17, email: 'john@example.com', joined: '2002-07-03' }
                ]"""), transform("""
                local customers = [
                    { id: 2, email: 'joe@example.com', joined: '2021-07-30' },
                    { id: 77, email: 'jane@example.com', joined: '2019-07-30' },
                    { id: 17, email: 'john@example.com', joined: '2002-07-03' }
                ];

                local orders = [
                    { orderId: 10308, customerId: 2, date: '2022-07-30' },
                    { orderId: 10309, customerId: 2, date: '2022-07-30' },
                    { orderId: 10310, customerId: 77, date: '2022-07-03' },
                    { orderId: 10311, customerId: 93, date: '2021-05-03' }
                ];

                xtr.objects.leftEqJoin(customers, orders,
                    function(cust) cust.id, function(order) order.customerId)"""), true);
        assertEquals(transform("""
                [
                    { id: 2, oId: 10308 },
                    { id: 2, oId: 10309 },
                    { id: 77, oId: 10310 },
                    { id: 17, oId: null }
                ]"""), transform("""
                local customers = [
                    { id: 2, email: 'joe@example.com', joined: '2021-07-30' },
                    { id: 77, email: 'jane@example.com', joined: '2019-07-30' },
                    { id: 17, email: 'john@example.com', joined: '2002-07-03' }
                ];

                local orders = [
                    { orderId: 10308, customerId: 2, date: '2022-07-30' },
                    { orderId: 10309, customerId: 2, date: '2022-07-30' },
                    { orderId: 10310, customerId: 77, date: '2022-07-03' },
                    { orderId: 10311, customerId: 93, date: '2021-05-03' }
                ];

                xtr.objects.leftEqJoin(customers, orders,
                    function(cust) cust.id, function(order) order.customerId,
                    function(cust, order) { id: cust.id, oId: order?.orderId })"""));
    }

    @Test
    public void fullEqJoin() throws JSONException {
        JSONAssert.assertEquals(transform("""
                [
                    { id: 2, email: 'joe@example.com', joined: '2021-07-30',
                        orderId: 10308, customerId: 2, date: '2022-07-30' },
                    { id: 2, email: 'joe@example.com', joined: '2021-07-30',
                        orderId: 10309, customerId: 2, date: '2022-07-30' },
                    { id: 77, email: 'jane@example.com', joined: '2019-07-30',
                        orderId: 10310, customerId: 77, date: '2022-07-03' },
                    { id: 17, email: 'john@example.com', joined: '2002-07-03' },
                    { orderId: 10311, customerId: 93, date: '2021-05-03' }
                ]"""), transform("""
                local customers = [
                    { id: 2, email: 'joe@example.com', joined: '2021-07-30' },
                    { id: 77, email: 'jane@example.com', joined: '2019-07-30' },
                    { id: 17, email: 'john@example.com', joined: '2002-07-03' }
                ];

                local orders = [
                    { orderId: 10308, customerId: 2, date: '2022-07-30' },
                    { orderId: 10309, customerId: 2, date: '2022-07-30' },
                    { orderId: 10310, customerId: 77, date: '2022-07-03' },
                    { orderId: 10311, customerId: 93, date: '2021-05-03' }
                ];

                xtr.objects.fullEqJoin(customers, orders,
                    function(cust) cust.id, function(order) order.customerId)"""), false);

        JSONAssert.assertEquals(transform("""
                [
                    { id: 2, oId: 10308 },
                    { id: 2, oId: 10309 },
                    { id: 77, oId: 10310 },
                    { id: 17, oId: null },
                    { id: null, oId: 10311 }
                ]"""), transform("""
                local customers = [
                    { id: 2, email: 'joe@example.com', joined: '2021-07-30' },
                    { id: 77, email: 'jane@example.com', joined: '2019-07-30' },
                    { id: 17, email: 'john@example.com', joined: '2002-07-03' }
                ];
                                
                local orders = [
                    { orderId: 10308, customerId: 2, date: '2022-07-30' },
                    { orderId: 10309, customerId: 2, date: '2022-07-30' },
                    { orderId: 10310, customerId: 77, date: '2022-07-03' },
                    { orderId: 10311, customerId: 93, date: '2021-05-03' }
                ];
                                
                xtr.objects.fullEqJoin(customers, orders,
                    function(cust) cust.id, function(order) order.customerId,
                    function(cust, order) { id: cust?.id, oId: order?.orderId })"""), false);
    }
}
