package com.datasonnet.modules;

import org.junit.jupiter.api.Test;

import static com.datasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CryptoTest {

    @Test
    public void hash() {
        assertEquals(transform("'8cca0e965edd0e223b744f9cedf8e141'"), transform("tro.crypto.hash('Hello, world!', 'MD2')"));
        assertEquals(transform("'6cd3556deb0da54bca060b4c39479839'"), transform("tro.crypto.hash('Hello, world!', 'MD5')"));
        assertEquals(transform("'943a702d06f34599aee1f8da8ef9f7296031d699'"), transform("tro.crypto.hash('Hello, world!', 'SHA-1')"));
        assertEquals(transform("'315f5bdb76d078c43b8ac0064e4a0164612b1fce77c869345bfc94c75894edd3'"), transform("tro.crypto.hash('Hello, world!', 'SHA-256')"));
        assertEquals(transform("'55bc556b0d2fe0fce582ba5fe07baafff035653638c7ac0d5494c2a64c0bea1cc57331c7c12a45cdbca7f4c34a089eeb'"), transform("tro.crypto.hash('Hello, world!', 'SHA-384')"));
        assertEquals(transform("'c1527cd893c124773d811911970c8fe6e857d6df5dc9226bd8a160614c0cd963a4ddea2b94bb7d36021ef9d865d5cea294a82dd49a0bb269f51f6e7a57f79421'"), transform("tro.crypto.hash('Hello, world!', 'SHA-512')"));
    }

    @Test
    public void hmac() {
        assertEquals(transform("'30fb9bd1cb7fca6925c027da4e692b579dbf3880'"), transform("tro.crypto.hmac('Hello, world!', 'secretKey', 'HMACSHA1')"));
        assertEquals(transform("'a6dab5f5a3be3d565a35b142d3aed373edc46216524eb9e4ed322398dbcb65ec'"), transform("tro.crypto.hmac('Hello, world!', 'secretKey', 'HMACSHA256')"));
        assertEquals(transform("'5f5c70c80c230c09c7f0c84501aa8fbcf69eb74ea40d9b84bd6e3bf9db5f3a5a0ca6ab98e1d66397a8f59e694f9432577e0271ff23b9b398d571b5ee6878c2b6'"), transform("tro.crypto.hmac('Hello, world!', 'secretKey', 'HMACSHA512')"));
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
        var encrypted = transform("tro.crypto.encrypt('%s', '%s', '%s')".formatted(data, key, transformation))
                .replace("\"", "");
        assertEquals(data, transform("tro.crypto.decrypt('%s', '%s', '%s')".formatted(encrypted, key, transformation))
                .replace("\"", ""));
    }
}
