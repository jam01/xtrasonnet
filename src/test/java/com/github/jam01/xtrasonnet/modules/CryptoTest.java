package com.github.jam01.xtrasonnet.modules;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import com.github.jam01.xtrasonnet.TestUtils;
import com.github.jam01.xtrasonnet.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CryptoTest {

    @Test
    public void hash() {
        Assertions.assertEquals(TestUtils.transform("'8cca0e965edd0e223b744f9cedf8e141'"), TestUtils.transform("xtr.crypto.hash('Hello, world!', 'MD2')"));
        Assertions.assertEquals(TestUtils.transform("'6cd3556deb0da54bca060b4c39479839'"), TestUtils.transform("xtr.crypto.hash('Hello, world!', 'MD5')"));
        Assertions.assertEquals(TestUtils.transform("'943a702d06f34599aee1f8da8ef9f7296031d699'"), TestUtils.transform("xtr.crypto.hash('Hello, world!', 'SHA-1')"));
        Assertions.assertEquals(TestUtils.transform("'315f5bdb76d078c43b8ac0064e4a0164612b1fce77c869345bfc94c75894edd3'"), TestUtils.transform("xtr.crypto.hash('Hello, world!', 'SHA-256')"));
        Assertions.assertEquals(TestUtils.transform("'55bc556b0d2fe0fce582ba5fe07baafff035653638c7ac0d5494c2a64c0bea1cc57331c7c12a45cdbca7f4c34a089eeb'"), TestUtils.transform("xtr.crypto.hash('Hello, world!', 'SHA-384')"));
        Assertions.assertEquals(TestUtils.transform("'c1527cd893c124773d811911970c8fe6e857d6df5dc9226bd8a160614c0cd963a4ddea2b94bb7d36021ef9d865d5cea294a82dd49a0bb269f51f6e7a57f79421'"), TestUtils.transform("xtr.crypto.hash('Hello, world!', 'SHA-512')"));
    }

    @Test
    public void hmac() {
        Assertions.assertEquals(TestUtils.transform("'30fb9bd1cb7fca6925c027da4e692b579dbf3880'"), TestUtils.transform("xtr.crypto.hmac('Hello, world!', 'secretKey', 'HMACSHA1')"));
        Assertions.assertEquals(TestUtils.transform("'a6dab5f5a3be3d565a35b142d3aed373edc46216524eb9e4ed322398dbcb65ec'"), TestUtils.transform("xtr.crypto.hmac('Hello, world!', 'secretKey', 'HMACSHA256')"));
        Assertions.assertEquals(TestUtils.transform("'5f5c70c80c230c09c7f0c84501aa8fbcf69eb74ea40d9b84bd6e3bf9db5f3a5a0ca6ab98e1d66397a8f59e694f9432577e0271ff23b9b398d571b5ee6878c2b6'"), TestUtils.transform("xtr.crypto.hmac('Hello, world!', 'secretKey', 'HMACSHA512')"));
    }

    @Test
    public void encryption() {
        var data = "Hello, world!";
        var eight = "eightchk";
        var sixteen = "$sixteencharkey$";
        var twentyfour = "twentyfourcharkeytwenty4";
        var thirtytwo = "thirtytwocharkeythirtytwocharkey";

        round_trip(data, sixteen, "AES/ECB/PKCS5Padding");
        round_trip(data, thirtytwo, "AES/CBC/PKCS5Padding");
        round_trip(data, twentyfour, "AES/CBC/PKCS5Padding");

        round_trip(data, eight, "DES/ECB/PKCS5Padding");
        round_trip(data, eight, "DES/CBC/PKCS5Padding");

        round_trip(data, twentyfour, "DESede/ECB/PKCS5Padding");
        round_trip(data, twentyfour, "DESede/CBC/PKCS5Padding");

        // round_trip(data, sixteen, "RSA/ECB/PKCS1Padding");
    }

    private void round_trip(String data, String key, String transformation) {
        var encrypted = TestUtils.transform("xtr.crypto.encrypt('%s', '%s', '%s')".formatted(data, key, transformation))
                .replace("\"", "");
        Assertions.assertEquals(data, TestUtils.transform("xtr.crypto.decrypt('%s', '%s', '%s')".formatted(encrypted, key, transformation))
                .replace("\"", ""));
    }
}
