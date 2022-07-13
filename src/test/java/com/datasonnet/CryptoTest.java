package com.datasonnet;
/*-
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.junit.jupiter.api.Test;

import static com.datasonnet.util.TestUtils.stacktraceFrom;
import static com.datasonnet.util.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class CryptoTest {
    @Test
    void testHash() {
        assertEquals("4227ce10dca49dd2d0ba3f438d1ea9f3", transform("ds.crypto.hash('HelloWorld', 'MD2')"));
        assertEquals("68e109f0f40ca72a15e05cc22786f8e6", transform("ds.crypto.hash('HelloWorld', 'MD5')"));
        assertEquals("db8ac1c259eb89d4a131b253bacfca5f319d54f2", transform("ds.crypto.hash('HelloWorld', 'SHA-1')"));
        assertEquals("872e4e50ce9990d8b041330c47c9ddd11bec6b503ae9386a99da8584e9bb12c4", transform("ds.crypto.hash('HelloWorld', 'SHA-256')"));
        assertEquals("293cd96eb25228a6fb09bfa86b9148ab69940e68903cbc0527a4fb150eec1ebe0f1ffce0bc5e3df312377e0a68f1950a", transform("ds.crypto.hash('HelloWorld', 'SHA-384')"));
        assertEquals("8ae6ae71a75d3fb2e0225deeb004faf95d816a0a58093eb4cb5a3aa0f197050d7a4dc0a2d5c6fbae5fb5b0d536a0a9e6b686369fa57a027687c3630321547596", transform("ds.crypto.hash('HelloWorld', 'SHA-512')"));
        try {
            transform("ds.crypto.hash('HelloWorld', 'DUMMY')");
            fail("This should fail with NoSuchAlgorithmException");
        } catch (Exception e) {
            String stacktrace = stacktraceFrom(e);
            assertTrue(stacktrace.contains("Caused by: java.security.NoSuchAlgorithmException: DUMMY MessageDigest not available"),
                    "Stacktrace does not indicate the issue");
        }
    }

    @Test
    void testHMAC() {
        String hmac = transform("ds.crypto.hmac('HelloWorld', 'PortX rules!', 'HmacSHA1')");
        assertEquals("91f27e2a84a9804b101257a2edfd121b02917e1a", hmac);

        hmac = transform("ds.crypto.hmac('HelloWorld', 'PortX rules!', 'HmacSHA256')");
        assertEquals("7854220ef827b07529509f68f391a80bf87fff328dbda140ed582520a1372dc1", hmac);

        hmac = transform("ds.crypto.hmac('HelloWorld', 'PortX rules!', 'HmacSHA512')");
        assertEquals("0acbc9c5d0828f3bc9c30e311311073089f969d7ccbfacc457c8da289bc163fee911f75b3b018b2d0c7a09e758b970fcdc6b6488118fd52dbbf31b9ee0415d4c", hmac);

        try {
            transform("ds.crypto.hash('HelloWorld', 'DUMMY')");
            fail("This should fail with NoSuchAlgorithmException");
        } catch (Exception e) {
            String stacktrace = stacktraceFrom(e);
            assertTrue(stacktrace.contains("Caused by: java.security.NoSuchAlgorithmException: DUMMY MessageDigest not available"),
                    "Stacktrace does not indicate the issue");
        }
    }

    @Test
    void testEncryptDecrypt() {
        String alg = "AES", mode = "CBC", padding = "PKCS5Padding";
        String encrypted = transform("ds.crypto.encrypt('Hello World', 'DataSonnet123456', '" + alg + "/" + mode + "/" + padding + "')");
        assertEquals("Hello World", transform("ds.crypto.decrypt('" + encrypted + "', 'DataSonnet123456', '" + alg + "/" + mode + "/" + padding + "')"));

        // 32 bits long
        encrypted = transform("ds.crypto.encrypt('Hello World', 'DataSonnet123456DataSonnet123456', '" + alg + "/" + mode + "/" + padding + "')");
        assertEquals("Hello World", transform("ds.crypto.decrypt('" + encrypted + "', 'DataSonnet123456DataSonnet123456', '" + alg + "/" + mode + "/" + padding + "')"));

        //=============================ECB
        mode = "ECB";
        encrypted = transform("ds.crypto.encrypt('Hello World', 'DataSonnet123456', '" + alg + "/" + mode + "/" + padding + "')");
        assertEquals("Hello World", transform("ds.crypto.decrypt('" + encrypted + "', 'DataSonnet123456', '" + alg + "/" + mode + "/" + padding + "')"));

        //========================================================================================
        alg = "DES";
        mode = "CBC";
        encrypted = transform("ds.crypto.encrypt('Hello World', 'DataSonn', '" + alg + "/" + mode + "/" + padding + "')");
        assertEquals("Hello World", transform("ds.crypto.decrypt('" + encrypted + "', 'DataSonn', '" + alg + "/" + mode + "/" + padding + "')"));

        //=============================ECB
        mode = "ECB";
        encrypted = transform("ds.crypto.encrypt('Hello World', 'DataSonn', '" + alg + "/" + mode + "/" + padding + "')");
        assertEquals("Hello World", transform("ds.crypto.decrypt('" + encrypted + "', 'DataSonn', '" + alg + "/" + mode + "/" + padding + "')"));

        //========================================================================================
        alg = "DESede";
        mode = "CBC";
        encrypted = transform("ds.crypto.encrypt('Hello World', 'Datasonnet123456XDatason', '" + alg + "/" + mode + "/" + padding + "')");
        assertEquals("Hello World", transform("ds.crypto.decrypt('" + encrypted + "', 'Datasonnet123456XDatason', '" + alg + "/" + mode + "/" + padding + "')"));

        //=============================ECB
        mode = "ECB";
        encrypted = transform("ds.crypto.encrypt('Hello World', 'Datasonnet123456XDatason', '" + alg + "/" + mode + "/" + padding + "')");
        assertEquals("Hello World", transform("ds.crypto.decrypt('" + encrypted + "', 'Datasonnet123456XDatason', '" + alg + "/" + mode + "/" + padding + "')"));

        //========================================================================================
        /*alg ="RSA";
        mode="ECB";
        padding="PKCS1Padding";
        encrypted = transform("ds.crypto.encrypt('Hello World', 'DataSonnet123456', '" + alg + "/" + mode + "/" + padding + "')");
		assertEquals("Hello World", transform("ds.crypto.decrypt('" + encrypted + "', 'DataSonnet123456', '" + alg + "/" + mode + "/" + padding + "')"));*/

        //========================================================================================
        alg = "DUMMY";
        mode = "CBC";
        padding = "PKCS5Padding";
        try {
            transform("ds.crypto.encrypt('Hello World', 'DataSonnet123456', '" + alg + "/" + mode + "/" + padding + "')");
            fail("This should fail with NoSuchAlgorithmException");
        } catch (Exception e) {
            String stacktrace = stacktraceFrom(e);
            assertTrue(stacktrace.contains("Caused by: java.security.NoSuchAlgorithmException: Cannot find any provider supporting DUMMY/CBC/PKCS5Padding"),
                    "Stacktrace does not indicate the issue");
        }

        alg = "AES";
        mode = "CBC";
        padding = "PKCS5Padding";
        try {
            transform("ds.crypto.encrypt('Hello World', 'not-long-enough', '" + alg + "/" + mode + "/" + padding + "')");
            fail("This should fail with NoSuchAlgorithmException");
        } catch (Exception e) {
            String stacktrace = stacktraceFrom(e);
            assertTrue(stacktrace.contains("Caused by: java.security.InvalidKeyException: Invalid AES key length"),
                    "Stacktrace does not indicate the issue");
        }
    }
}