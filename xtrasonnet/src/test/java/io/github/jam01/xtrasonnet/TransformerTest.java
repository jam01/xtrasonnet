package io.github.jam01.xtrasonnet;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

/* datasonnet-mapper copyright/notice, per Apache-2.0 § 4.c */
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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ssl.SSLContextBuilder;
import com.github.tomakehurst.wiremock.http.ssl.TrustEverythingStrategy;
import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.Documents;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import io.github.jam01.xtrasonnet.spi.LibraryTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

import javax.net.ssl.HttpsURLConnection;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.github.jam01.xtrasonnet.TestUtils.resourceAsString;
import static io.github.jam01.xtrasonnet.TestUtils.stacktraceFrom;
import static io.github.jam01.xtrasonnet.TestUtils.transform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TransformerTest {

    @ParameterizedTest
    @MethodSource("simpleProvider")
    void simple(String jsonnet, String json, String expected) {
        Transformer transformer = new Transformer(jsonnet);
        assertEquals(expected, transformer.transform(Document.of(json, MediaTypes.APPLICATION_JSON)).getContent());
    }

    static Stream<String[]> simpleProvider() {
        return Stream.of(
                new String[] { "{ \"uid\": payload.user_id }", "{ \"user_id\": 7 }", "{\"uid\":7}"},
                new String[] { "{ \"uid\": payload.user_id }", "{ \"user_id\": 8 }", "{\"uid\":8}"},
                new String[] { "xtr.datetime.plus(\"2019-07-22T21:00:00Z\", \"P1Y1D\")", "{}", "\"2020-07-23T21:00:00Z\""}
                );
    }

    @ParameterizedTest
    @MethodSource("variableProvider")
    void variables(String jsonnet, String json, String variable, String value, String expected) {
        Map<String, Document<?>> variables = Collections.singletonMap(variable, Document.of(value, MediaTypes.APPLICATION_JSON));
        Transformer transformer = new Transformer(jsonnet, variables.keySet());
        assertEquals(expected, transformer.transform(Document.of(json, MediaTypes.APPLICATION_JSON), variables, MediaTypes.APPLICATION_JSON).getContent());
    }

    static Stream<String[]> variableProvider() {
        return Stream.of(
                new String[] { "{ [name]: payload.user_id }", "{ \"user_id\": 7 }", "name", "\"variable\"", "{\"variable\":7}"},
                new String[] { "{ \"uid\": payload.user_id + offset }", "{ \"user_id\": 8 }", "offset", "3", "{\"uid\":11}"}
        );
    }

    @Test
    void parseErrorLineNumberWhenWrapped() {
        try {
            Transformer transformer = new Transformer("xtr.time.now() a");
            fail("Must fail to parse");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getCause().getMessage().contains("Expected end-of-input:1:16"), "Found message: " + e.getCause().getMessage());
        }
    }

    @Test
    void executeErrorLineNumberWhenWrapped() {
        try {
            transform("payload.foo");
            fail("Must fail to execute");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getCause().getMessage().contains("attempted to index a null with string foo"), "Found message: " + e.getCause().getMessage());
            assertTrue(stacktraceFrom(e).contains("(main):1:8"), "Stacktrace does not indicate the issue");
        }
    }

    @Test
    void nonJsonArguments() {
        Transformer transformer = new Transformer("argument", Set.of("argument"));


        Map<String, Document<?>> map = Collections.singletonMap("argument", Document.of("value", MediaTypes.TEXT_PLAIN));

        Document<String> mapped = transformer.transform(Document.of("{}", MediaTypes.APPLICATION_JSON), map, MediaTypes.TEXT_PLAIN);

        //assertEquals(new DefaultDocument<String>("value", MediaTypes.TEXT_PLAIN), mapped);
        assertEquals("value", mapped.getContent());
        assertEquals(MediaTypes.TEXT_PLAIN, mapped.getMediaType());

    }

    @Test
    void testFieldsOrder() throws Exception {
        String jsonData = TestUtils.resourceAsString("fieldOrder.json");
        String datasonnet = TestUtils.resourceAsString("fieldOrder.ds");

        Map<String, Document<?>> variables = new HashMap<>();
        variables.put("v2", Document.of("v2value", MediaTypes.TEXT_PLAIN));
        variables.put("v1", Document.of("v1value", MediaTypes.TEXT_PLAIN));

        Transformer transformer = new Transformer(datasonnet, variables.keySet());


        String mapped = transformer.transform(Document.of(jsonData, MediaTypes.APPLICATION_JSON), variables, MediaTypes.APPLICATION_JSON).getContent();

        assertEquals("{\"z\":\"z\",\"a\":\"a\",\"v2\":\"v2value\",\"v1\":\"v1value\",\"y\":\"y\",\"t\":\"t\"}", mapped.trim());

        datasonnet = "/** xtrasonnet\n" +
                     "preserveOrder=false\n*/\n" + datasonnet;

        transformer = new Transformer(datasonnet, variables.keySet());


        mapped = transformer.transform(Document.of(jsonData, MediaTypes.APPLICATION_JSON), variables, MediaTypes.APPLICATION_JSON).getContent();

        assertEquals("{\"a\":\"a\",\"t\":\"t\",\"v1\":\"v1value\",\"v2\":\"v2value\",\"y\":\"y\",\"z\":\"z\"}", mapped.trim());
    }

    @Test
    public void imports() throws JSONException {
        var imports = """
                local martinis = import 'imports/martinis.libsonnet';
                                
                {
                  'Vodka Martini': martinis['Vodka Martini'],
                  Manhattan: {
                    ingredients: [
                      { kind: 'Rye', qty: 2.5 },
                      { kind: 'Sweet Red Vermouth', qty: 1 },
                      { kind: 'Angostura', qty: 'dash' },
                    ],
                    garnish: importstr 'imports/garnish.txt',
                    served: 'Straight Up',
                  },
                }""";

        var res = transform(imports);
        JSONAssert.assertEquals(resourceAsString("imports/output.json"), res, true);
    }

    @Test
    public void http_import() throws JSONException {
        var srv = new WireMockServer(options().port(8080));
        srv.start();
        srv.addStubMapping(WireMock.get("/imports/garnish.txt")
                .willReturn(okForContentType("text/plain", "Maraschino Cherry")).build());

        try {
            var res = transform("importstr 'http://localhost:8080/imports/garnish.txt'");
            JSONAssert.assertEquals("\"Maraschino Cherry\"", res, true);
        } finally {
            srv.stop();
        }
    }

    @Test
    public void https_import() throws NoSuchAlgorithmException, KeyManagementException, JSONException {
        var context = SSLContextBuilder.create().loadTrustMaterial(new TrustEverythingStrategy()).build();
        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());

        var srv = new WireMockServer(options().httpsPort(8443));
        srv.start();
        srv.addStubMapping(WireMock.get("/imports/garnish.txt")
                .willReturn(okForContentType("text/plain", "Maraschino Cherry")).build());

        try {
            var res = transform("importstr 'https://localhost:8443/imports/garnish.txt'");
            JSONAssert.assertEquals("\"Maraschino Cherry\"", res, true);
        } finally {
            srv.stop();
        }
    }

    @Test
    public void file_import() throws JSONException, URISyntaxException {
        var res = transform("importstr 'file:%s'".formatted(getClass().getClassLoader().getResource("imports/garnish.txt").toURI().getPath()));
        JSONAssert.assertEquals("\"Maraschino Cherry\"", res, true);
    }

    @Test
    public void resource_script() throws JSONException {
        var res = transform("import 'resource.xtr'");
        JSONAssert.assertEquals("{\"hello\": \"world!\"}", res, true);
    }

    @Test
    public void imports_nested_customLib() throws JSONException {
        var imports = """
                local lib1 = import 'imports/lib-1.libsonnet';
                local lib2 = import 'imports/nested/lib-2.libsonnet';

                {
                    lib1: {
                        xtr: lib1.xtr('Hello'),
                        std: lib1.std('Hello'),
                        echo: lib1.echo('Hello')
                    },
                    lib2: {
                        xtr: lib2.xtr('Hello'),
                        std: lib2.std('Hello'),
                        echo: lib2.echo('Hello')
                    },
                    lib3: lib2.lib3
                }
                """;

        var res = Transformer.builder(imports).withLibrary(new LibraryTest.TestLib()).build().transform(Documents.Null());
        JSONAssert.assertEquals(resourceAsString("imports/extended-output.json"), res.getContent(), true);
    }
}
