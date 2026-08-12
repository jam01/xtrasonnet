package io.github.jam01.xtrasonnet;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.Documents;
import io.github.jam01.xtrasonnet.document.MediaType;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static io.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Expectations are written as explicit UTF-8 octets rather than as round-trips, so they pin the
 * encoding instead of merely agreeing with whatever the platform happens to use. A default charset
 * creeping back in fails these only where the default is not UTF-8, which no modern host provides on
 * its own -- so the surefire execution {@code charset-tests-on-a-non-utf8-host} reruns this class
 * with {@code -Dfile.encoding=ISO-8859-1} on every build.
 */
public class CharsetTest {
    private static final String N_TILDE = "ñ";                     // C3 B1 in UTF-8, F1 in Latin-1
    private static final String EURO = "€";                        // 3 UTF-8 octets, absent from Latin-1
    // combining marks, a euro sign and an emoji (a surrogate pair, 4 UTF-8 octets)
    private static final String NON_ASCII = "ñaïve café € 🎉";
    private static final String LATIN1_TEXT = "café";              // representable in both encodings
    private static final Charset LATIN_1 = StandardCharsets.ISO_8859_1;

    private static MediaType withCharset(MediaType type, Charset charset) {
        return new MediaType(type, charset);
    }

    @Test
    public void url_encodesAsUtf8() {
        // Latin-1 would give %F1; percent-encoding is defined over UTF-8 octets (RFC 3986 s2.5)
        assertEquals("\"%C3%B1\"", transform("xtr.url.encode('" + N_TILDE + "')"));
        assertEquals("\"%E2%82%AC\"", transform("xtr.url.encode('" + EURO + "')"));
    }

    @Test
    public void url_decodesAsUtf8() {
        assertEquals('"' + N_TILDE + '"', transform("xtr.url.decode('%C3%B1')"));
        assertEquals('"' + EURO + '"', transform("xtr.url.decode('%E2%82%AC')"));
    }

    @Test
    public void url_roundTrips() {
        assertEquals(transform("'" + NON_ASCII + "'"),
                transform("xtr.url.decode(xtr.url.encode('" + NON_ASCII + "'))"));
    }

    @Test
    public void base64_encodesUtf8Octets() {
        // "n-tilde" is C3 B1 in UTF-8 -> "w7E="; as the single Latin-1 octet F1 it would be "8Q=="
        assertEquals("\"w7E=\"", transform("xtr.base64.encode('" + N_TILDE + "')"));
        assertEquals("\"4oKs\"", transform("xtr.base64.encode('" + EURO + "')"));
    }

    @Test
    public void base64_decodesUtf8Octets() {
        assertEquals('"' + N_TILDE + '"', transform("xtr.base64.decode('w7E=')"));
        assertEquals('"' + EURO + '"', transform("xtr.base64.decode('4oKs')"));
    }

    @Test
    public void base64_roundTrips() {
        assertEquals(transform("'" + NON_ASCII + "'"),
                transform("xtr.base64.decode(xtr.base64.encode('" + NON_ASCII + "'))"));
    }

    @Test
    public void crypto_hashesUtf8Octets() {
        // sha256 of the two octets C3 B1; the single Latin-1 octet F1 hashes to something else
        assertEquals("\"024bb90888ca89a15a19e9bdd8c712bfb070465fce1ef25e43c170ea44fc5e5f\"",
                transform("xtr.crypto.hash('" + N_TILDE + "', 'SHA-256')"));
    }

    @Test
    public void crypto_roundTripsNonAscii() {
        // both the value and the secret cross the String/bytes boundary, so with the platform default
        // encrypting on a UTF-8 host and decrypting on a Latin-1 one silently corrupted the plaintext,
        // and the same secret string produced a different key
        var secret = "$sixteencharkey$";
        var encrypted = transform("xtr.crypto.encrypt('%s', '%s', 'AES/CBC/PKCS5Padding')"
                .formatted(NON_ASCII, secret)).replace("\"", "");

        assertEquals(transform("'" + NON_ASCII + "'"),
                transform("xtr.crypto.decrypt('%s', '%s', 'AES/CBC/PKCS5Padding')".formatted(encrypted, secret)));
    }

    @Test
    public void imports_areDecodedAsUtf8() {
        // ResourcePath.importer calls ResourceResolver.asString with a null charset for every import,
        // so imported sources were decoded with the platform default
        assertEquals(transform("'" + NON_ASCII + "'"),
                transform("(import 'imports/utf8.libsonnet').greeting"));
    }

    @Test
    public void xml_declarationDefaultsToUtf8AndMatchesTheBytes() {
        var doc = new Transformer("{ root: '" + NON_ASCII + "' }")
                .transform(Documents.Null(), Collections.emptyMap(), MediaTypes.APPLICATION_XML, OutputStream.class);
        var bytes = ((ByteArrayOutputStream) doc.getContent()).toByteArray();
        var asUtf8 = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(asUtf8.contains("encoding='UTF-8'"), asUtf8);
        assertTrue(asUtf8.contains(NON_ASCII), asUtf8);
    }

    @Test
    public void xml_declarationHonoursRequestedCharsetAndMatchesTheBytes() {
        // the declaration hardcoded the platform default while DefaultXMLPlugin encoded the bytes with
        // the media type's charset, so asking for Latin-1 produced Latin-1 bytes declared as UTF-8
        var doc = new Transformer("{ root: '" + LATIN1_TEXT + "' }")
                .transform(Documents.Null(), Collections.emptyMap(),
                        withCharset(MediaTypes.APPLICATION_XML, LATIN_1), OutputStream.class);
        var bytes = ((ByteArrayOutputStream) doc.getContent()).toByteArray();

        var asLatin1 = new String(bytes, LATIN_1);
        assertTrue(asLatin1.contains("encoding='ISO-8859-1'"), asLatin1);
        // and the bytes really are in the encoding the declaration names: decoding them as anything
        // else would not yield the original text
        assertTrue(asLatin1.contains(LATIN1_TEXT), asLatin1);
        assertArrayEquals("<root>%s</root>".formatted(LATIN1_TEXT).getBytes(LATIN_1),
                asLatin1.substring(asLatin1.indexOf("<root>")).getBytes(LATIN_1));
    }

    @Test
    public void xml_stringOutputDeclaresUtf8RegardlessOfTheRequestedCharset() {
        // a String carries no encoding, so this branch cannot honour a requested charset; naming it
        // would hand back a document declaring an encoding nothing has applied
        var doc = new Transformer("{ root: '" + LATIN1_TEXT + "' }")
                .transform(Documents.Null(), Collections.emptyMap(),
                        withCharset(MediaTypes.APPLICATION_XML, LATIN_1), String.class);

        assertTrue(doc.getContent().contains("encoding='UTF-8'"), doc.getContent());
        assertFalse(doc.getContent().contains("ISO-8859-1"), doc.getContent());
    }

    @Test
    public void xml_readHonoursDeclaredInputCharset() {
        // no XML declaration in the document, so the parser would otherwise assume UTF-8 and mis-decode
        var in = new ByteArrayInputStream("<root>%s</root>".formatted(LATIN1_TEXT).getBytes(LATIN_1));
        var doc = new Transformer("payload")
                .transform(Document.of(in, withCharset(MediaTypes.APPLICATION_XML, LATIN_1)),
                        Collections.emptyMap(), MediaTypes.APPLICATION_JSON);

        assertTrue(doc.getContent().contains(LATIN1_TEXT), doc.getContent());
    }

    @Test
    public void xml_readDefaultsToTheDocumentsOwnDeclaration() {
        // with no charset on the media type the document's own declaration must win -- setting an
        // encoding on the InputSource unconditionally would override it
        var xml = "<?xml version='1.0' encoding='ISO-8859-1'?><root>%s</root>".formatted(LATIN1_TEXT);
        var in = new ByteArrayInputStream(xml.getBytes(LATIN_1));
        var doc = new Transformer("payload")
                .transform(Document.of(in, MediaTypes.APPLICATION_XML), Collections.emptyMap(),
                        MediaTypes.APPLICATION_JSON);

        assertTrue(doc.getContent().contains(LATIN1_TEXT), doc.getContent());
    }

    @Test
    public void json_writesUtf8ByDefault() {
        var doc = new Transformer("{ a: '" + NON_ASCII + "' }")
                .transform(Documents.Null(), Collections.emptyMap(), MediaTypes.APPLICATION_JSON, byte[].class);

        assertArrayEquals("{\"a\":\"%s\"}".formatted(NON_ASCII).getBytes(StandardCharsets.UTF_8),
                (byte[]) doc.getContent());
    }

    @Test
    public void json_honoursRequestedCharset() {
        // the charset was computed in DefaultJSONPlugin.write and then never read, so every
        // byte-producing target emitted the renderer's UTF-8 regardless of what was asked for
        var doc = new Transformer("{ a: '" + LATIN1_TEXT + "' }")
                .transform(Documents.Null(), Collections.emptyMap(),
                        withCharset(MediaTypes.APPLICATION_JSON, LATIN_1), byte[].class);

        assertArrayEquals("{\"a\":\"%s\"}".formatted(LATIN1_TEXT).getBytes(LATIN_1), (byte[]) doc.getContent());
    }
}
