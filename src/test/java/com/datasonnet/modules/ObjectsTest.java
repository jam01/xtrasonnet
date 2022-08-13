package com.datasonnet.modules;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.datasonnet.TestUtils.transform;
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

                tro.objects.all(languages, function(lang, name) lang.isJvm)"""));
    }

    @Test
    public void any() {
        assertEquals(transform("true"), transform("""
                local languages = {
                    scala: { version: '3.1.3', isJvm: true },
                    java: { version: '19', isJvm: true },
                    python: { version: '3.10.4', isJvm: false }
                };

                tro.objects.any(languages, function(lang, name) lang.isJvm)"""));
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

                tro.objects.distinctBy(languages, function(lang) lang.name)"""));
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

                tro.objects.distinctBy(languages, function(lang, ordinal)
                    if (lang.name == 'java') then lang.version
                    else ordinal
                )"""));
    }

    @Disabled
    @Test
    public void innerJoin() {
        assertEquals(transform("""
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

                tro.objects.innerJoin(customers, orders
                    function(cust) cust.id, function(order) order.customerId)"""));
        assertEquals(transform("""
                [
                    { id: 2, orderId: 10308 },
                    { id: 2, orderId: 10309 },
                    { id: 77, orderId: 10310 }
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

                tro.objects.innerJoin(customers, orders
                    function(cust) cust.id, function(order) order.customerId,
                    function(cust, order) { id: cust.id, oId: order.id })"""));
    }

    @Disabled
    @Test
    public void leftJoin() {
        assertEquals(transform("""
                [
                    { id: 2, email: 'joe@example.com', joined: '2021-07-30',
                        orderId: 10308, customerId: 2, date: '2022-07-30' },
                    { id: 2, email: 'joe@example.com', joined: '2021-07-30',
                        orderId: 10309, customerId: 2, date: '2022-07-30' },
                    { id: 77, email: 'jane@example.com', joined: '2019-07-30'
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

                tro.objects.leftJoin(customers, orders
                    function(cust) cust.id, function(order) order.customerId)"""));
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

                tro.objects.leftJoin(customers, orders
                    function(cust) cust.id, function(order) order.customerId,
                    function(cust, order) { id: cust.id, oId: order?.id })"""));
    }

    @Disabled
    @Test
    public void fullJoin() {
        assertEquals(transform("""
                [
                    { id: 2, email: 'joe@example.com', joined: '2021-07-30',
                        orderId: 10308, customerId: 2, date: '2022-07-30' },
                    { id: 2, email: 'joe@example.com', joined: '2021-07-30',
                        orderId: 10309, customerId: 2, date: '2022-07-30' },
                    { id: 77, email: 'jane@example.com', joined: '2019-07-30'
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

                tro.objects.fullJoin(customers, orders
                    function(cust) cust.id, function(order) order.customerId)"""));
    }
}
