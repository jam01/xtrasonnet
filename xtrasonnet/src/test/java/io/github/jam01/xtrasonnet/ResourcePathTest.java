package io.github.jam01.xtrasonnet;

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResourcePathTest {

    @Test
    public void classpath() {
        assertEquals("", new ResourcePath("(main)").parent().toString());
        assertEquals("", ResourcePath.root().last());
        assertEquals("(main)", new ResourcePath("(main)").last());

        assertEquals("garnish.txt", new ResourcePath("imports/garnish.txt").last());
        assertEquals("imports/garnish.txt", ResourcePath.root().$div("imports/garnish.txt").toString());
        assertEquals("imports", new ResourcePath("imports/garnish.txt").parent().toString());
        assertEquals("imports/garnish.txt", new ResourcePath("imports").$div("garnish.txt").toString());

        assertEquals("classpath:imports", new ResourcePath("classpath:imports/garnish.txt").parent().toString());
        assertEquals("classpath:imports/garnish.txt", new ResourcePath("classpath:imports").$div("garnish.txt").toString());

        assertEquals("(main)", new ResourcePath("(main)").relativeToString(ResourcePath.root()));
        assertEquals("(main)", new ResourcePath("(main)").relativeToString(new ResourcePath("imports/")));
        assertEquals("(main)", new ResourcePath("(main)").relativeToString(new ResourcePath("imports")));
        assertEquals("(main)", new ResourcePath("(main)").relativeToString(new ResourcePath("classpath:imports/")));
        assertEquals("(main)", new ResourcePath("(main)").relativeToString(new ResourcePath("classpath:imports")));

        assertEquals("garnish.txt", new ResourcePath("imports/garnish.txt").relativeToString(new ResourcePath("imports/")));
        assertEquals("garnish.txt", new ResourcePath("imports/garnish.txt").relativeToString(new ResourcePath("imports")));
        assertEquals("garnish.txt", new ResourcePath("classpath:imports/garnish.txt").relativeToString(new ResourcePath("classpath:imports/")));
        assertEquals("garnish.txt", new ResourcePath("classpath:imports/garnish.txt").relativeToString(new ResourcePath("classpath:imports")));

        assertEquals("exports/garnish.txt", new ResourcePath("exports/garnish.txt").relativeToString(new ResourcePath("imports/")));
        assertEquals("file:/parent/child", new ResourcePath("file:/parent/child").relativeToString(new ResourcePath("classpath:imports/")));
        assertEquals("http://localhost:8080/parent/child", new ResourcePath("http://localhost:8080/parent/child").relativeToString(new ResourcePath("classpath:imports/")));
    }

    @Test
    public void file() {
        assertEquals("child", new ResourcePath("file:/child").last());
        assertEquals("child", new ResourcePath("file:/parent/child").last());

        assertEquals("file:", new ResourcePath("file:/child").parent().toString());
        assertEquals("file:/parent", new ResourcePath("file:/parent/child").parent().toString());

        assertEquals("file:/child", new ResourcePath("file:").$div("child").toString());

        assertEquals("(main)", new ResourcePath("(main)").relativeToString(new ResourcePath("file:/parent/")));
        assertEquals("(main)", new ResourcePath("(main)").relativeToString(new ResourcePath("file:/parent")));
        assertEquals("(main)", new ResourcePath("(main)").relativeToString(new ResourcePath("file:/")));

        assertEquals("child", new ResourcePath("file:/parent/child").relativeToString(new ResourcePath("file:/parent/")));
        assertEquals("child", new ResourcePath("file:/parent/child").relativeToString(new ResourcePath("file:/parent")));
        assertEquals("parent/child", new ResourcePath("file:/parent/child").relativeToString(new ResourcePath("file:/")));

        assertEquals("file:/another/child", new ResourcePath("file:/another/child").relativeToString(new ResourcePath("file:/parent/")));
        assertEquals("classpath:parent/child", new ResourcePath("classpath:parent/child").relativeToString(new ResourcePath("file:/parent/")));
        assertEquals("http://localhost:8080/parent/child", new ResourcePath("http://localhost:8080/parent/child").relativeToString(new ResourcePath("file:/parent/")));
    }

    @Test
    public void http() {
        assertEquals("child", new ResourcePath("http://localhost:8080/child").last());
        assertEquals("child", new ResourcePath("http://localhost:8080/parent/child").last());

        assertEquals("http://localhost:8080", new ResourcePath("http://localhost:8080/child").parent().toString());
        assertEquals("http://localhost:8080/parent", new ResourcePath("http://localhost:8080/parent/child").parent().toString());

        assertEquals("http://localhost:8080/child", new ResourcePath("http://localhost:8080").$div("child").toString());


        assertEquals("http://localhost:8080", new ResourcePath("http://localhost:8080/child?query=string#fragment").parent().toString());
        assertEquals("http://localhost:8080/parent", new ResourcePath("http://localhost:8080/parent/child?query=string#fragment").parent().toString());

        assertEquals("(main)", new ResourcePath("(main)").relativeToString(new ResourcePath("http://localhost:8080/parent/")));
        assertEquals("(main)", new ResourcePath("(main)").relativeToString(new ResourcePath("http://localhost:8080/parent")));
        assertEquals("(main)", new ResourcePath("(main)").relativeToString(new ResourcePath("http://localhost:8080/")));
        assertEquals("(main)", new ResourcePath("(main)").relativeToString(new ResourcePath("http://localhost:8080")));

        assertEquals("child", new ResourcePath("http://localhost:8080/parent/child").relativeToString(new ResourcePath("http://localhost:8080/parent/")));
        assertEquals("child", new ResourcePath("http://localhost:8080/parent/child").relativeToString(new ResourcePath("http://localhost:8080/parent")));
        assertEquals("parent/child", new ResourcePath("http://localhost:8080/parent/child").relativeToString(new ResourcePath("http://localhost:8080/")));
        assertEquals("parent/child", new ResourcePath("http://localhost:8080/parent/child").relativeToString(new ResourcePath("http://localhost:8080")));

        assertEquals("child?query=string#fragment", new ResourcePath("http://localhost:8080/parent/child?query=string#fragment").relativeToString(new ResourcePath("http://localhost:8080/parent/")));
        assertEquals("parent/child?query=string#fragment", new ResourcePath("http://localhost:8080/parent/child?query=string#fragment").relativeToString(new ResourcePath("http://localhost:8080/")));

        assertEquals("http://example:8080/parent/child", new ResourcePath("http://example:8080/parent/child").relativeToString(new ResourcePath("http://localhost:8080/parent/")));
        assertEquals("classpath:parent/child", new ResourcePath("classpath:parent/child").relativeToString(new ResourcePath("http://localhost:8080/parent/")));
        assertEquals("file:/parent/child", new ResourcePath("file:/parent/child").relativeToString(new ResourcePath("http://localhost:8080/parent/")));
    }
}
