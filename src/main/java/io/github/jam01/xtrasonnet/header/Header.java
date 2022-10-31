package io.github.jam01.xtrasonnet.header;

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
/*-
 * Adopted:
 * - 0b946df9667c64ac631934a77d2444700889accf: fix system-dependent header parsing; allow any standard line separators
 * - f3e2584ce6d2bf68f49d762503b876255cb29ac4: convert opaque array index exceptions to descriptive exceptions and i...
 *      mprove locating of invalid media format problems in commit
 * - d94c7e337e77a358dd3f8944b81b2425c0235d43: improve resilience of header parsing, including unknown header lines...
 *      and unterminated headers
 * - ad59b61447ad423a66c2e51b3ec79914aaed28bb: Merge pull request #67 from datasonnet/header-comments
 */

import io.github.jam01.xtrasonnet.document.InvalidMediaTypeException;
import io.github.jam01.xtrasonnet.document.MediaType;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import io.github.jam01.xtrasonnet.document.InvalidMediaTypeException;
import io.github.jam01.xtrasonnet.document.MediaType;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Header {
    public static final String HEADER = "/** xtrasonnet";
    public static final String COMMENT_PREFIX = "//";
    public static final String INPUT = "input";
    public static final Pattern INPUT_LINE = Pattern.compile("^(?:input (?<name>\\w+)|input (?<all>\\*)) (?<mediatype>\\S.*)$");
    public static final String OUTPUT = "output";
    public static final Pattern OUTPUT_LINE = Pattern.compile("^output (?<mediatype>\\S.*)$");
    public static final String PRESERVE_ORDER = "preserveOrder";
    public static final String DATAFORMAT_PREFIX = "dataformat";
    private final boolean preserveOrder;
    private final Map<String, MediaType> inputs;
    private final MediaType output;

    public Header(boolean preserveOrder,
                  Map<String, MediaType> inputs,
                  MediaType output) {
        this.preserveOrder = preserveOrder;
        this.inputs = inputs;
        this.output = output;
    }

    private static final Header EMPTY =
            new Header(true, Collections.emptyMap(), MediaTypes.ANY);

    public static Header parseHeader(String script) throws HeaderParseException {
        if (!script.trim().startsWith(HEADER)) {
            return EMPTY;
        }

        String headerSection = extractHeader(script);
        return doParseHeader(headerSection);
    }

    @NotNull
    private static String extractHeader(String script) throws HeaderParseException {
        int terminus = script.indexOf("*/");
        if (terminus == -1) {
            throw new HeaderParseException("Unterminated header. Headers must end with */");
        }

        return script
                .substring(0, terminus)
                .replace(HEADER, "")
                .trim();
    }

    @NotNull
    private static Header doParseHeader(String headerSection) throws HeaderParseException {
        boolean preserve = true;
        MediaType output = null;
        Map<String, MediaType> inputs = new HashMap<>(8);

        List<MediaType> allInputs = new ArrayList<>(8);
        List<MediaType> dataformats = new ArrayList<>(8);

        for (String line : headerSection.split("\\r?\\n")) {
            line = line.trim();  // we never care about leading or trailing whitespace
            try {
                if (line.startsWith(PRESERVE_ORDER)) {
                    String[] tokens = line.split("=", 2);
                    preserve = Boolean.parseBoolean(tokens[1]);
                } else if (line.startsWith(INPUT)) {
                    Matcher matcher = INPUT_LINE.matcher(line);
                    if (!matcher.matches()) {
                        throw new HeaderParseException("Unable to parse header line " + line + ", it must follow the input line format");
                    }

                    String name = matcher.group("name");
                    MediaType mediaType = MediaType.valueOf(matcher.group("mediatype"));

                    if (matcher.group("all") != null) {  // there's a *. This also means it can't be a default.
                        allInputs.add(mediaType);
                    } else {
                        var prev = inputs.get(name);
                        if (prev == null) {
                            inputs.put(name, mediaType);
                        } else if (mediaType.equalsTypeAndSubtype(prev)) {
                            var params = new HashMap<String, String>(prev.getParameters().size() + mediaType.getParameters().size());
                            params.putAll(prev.getParameters());
                            params.putAll(mediaType.getParameters());
                            inputs.put(name, mediaType.withParameters(params));
                        }
                    }
                } else if (line.startsWith(OUTPUT)) {
                    Matcher matcher = OUTPUT_LINE.matcher(line);
                    if (!matcher.matches()) {
                        throw new HeaderParseException("Unable to parse header line " + line + ", it must follow the output line format");
                    }

                    output = MediaType.valueOf(matcher.group("mediatype"));
                } else if (line.startsWith(DATAFORMAT_PREFIX)) {
                    String[] tokens = line.split(" ", 2);
                    MediaType toAdd = MediaType.valueOf(tokens[1]);
                    dataformats.add(toAdd);
                } else if (line.isEmpty() || line.startsWith(COMMENT_PREFIX)) {
                    // deliberately do nothing
                } else {
                    throw new HeaderParseException("Unable to parse header line: " + line);
                }
            } catch (InvalidMediaTypeException exc) {
                throw new HeaderParseException("Could not parse media type from header in line " + line, exc);
            } catch (ArrayIndexOutOfBoundsException exc) {
                throw new HeaderParseException("Problem with header formatting in line " + line);
            }
        }

        for (Map.Entry<String, MediaType> in : inputs.entrySet()) {
            Map<String, String> params = new HashMap<>(8);
            for (MediaType dataformat : dataformats) {
                if (dataformat.includes(in.getValue())) {
                    params.putAll(dataformat.getParameters());
                }
            }

            for (MediaType allin : allInputs) {
                if (allin.includes(in.getValue())) {
                    params.putAll(allin.getParameters());
                }
            }

            params.putAll(in.getValue().getParameters());
            inputs.put(in.getKey(), in.getValue().withParameters(params));
        }

        if (output != null) {
            Map<String, String> params = new HashMap<>(8);
            for (MediaType dataformat : dataformats) {
                if (dataformat.includes(output)) {
                    params.putAll(dataformat.getParameters());
                }
            }

            params.putAll(output.getParameters());
            output = output.withParameters(params);
        }

        return new Header(preserve, Collections.unmodifiableMap(inputs), output);
    }

    public Map<String, MediaType> getInputs() {
        return Collections.unmodifiableMap(inputs);
    }

    public Optional<MediaType> getInput(String name) {
        return Optional.ofNullable(inputs.get(name));
    }

    public Optional<MediaType> getOutput() {
        return Optional.ofNullable(output);
    }

    public Optional<MediaType> getPayloadInput() {
        return Optional.ofNullable(inputs.get("payload"));
    }

    public boolean isPreserveOrder() {
        return preserveOrder;
    }
}
