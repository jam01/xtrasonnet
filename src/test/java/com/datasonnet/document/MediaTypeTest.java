package com.datasonnet.document;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class MediaTypeTest {
    @Test
    public void parse_simple() {
        var type = MediaType.parseMediaType("type/subtype;attr=val");
        assertEquals(1, type.getParameters().size());
        assertEquals("val", type.getParameter("attr"));
    }

    @Test
    public void parse_should_not_be_empty() {
        try {
            var type = MediaType.parseMediaType("type/subtype;attr=");
            fail("Empty value is invalid!");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("'value' must not be empty"));
        }
    }

    @Test
    public void parse_quoted_simple() {
        var type = MediaType.parseMediaType("type/subtype;attr=\"val\"");
        assertEquals(1, type.getParameters().size());
        assertEquals("val", type.getParameter("attr"));
    }

    @Test
    public void parse_quoted_escaped() {
        var type = MediaType.parseMediaType("type/subtype;attr=\"pre\\\"post\"");
        assertEquals(1, type.getParameters().size());
        assertEquals("pre\"post", type.getParameter("attr"));
    }

    @Test
    public void parse_quoted_escaped_earlysemicolon() {
        var type = MediaType.parseMediaType("type/subtype;attr=\"pre\\\";post\"");
        assertEquals(1, type.getParameters().size());
        assertEquals("pre\";post", type.getParameter("attr"));
    }

    @Test
    public void parse_quoted_escaped_earlysemicolon_and_second_param() {
        var type = MediaType.parseMediaType("type/subtype;attr=\"pre\\\";post\";attr1=val1");
        assertEquals(2, type.getParameters().size());
        assertEquals("pre\";post", type.getParameter("attr"));
        assertEquals("val1", type.getParameter("attr1"));
    }

    @Test
    public void parse_should_be_quoted() {
        try {
            var type = MediaType.parseMediaType("type/subtype;attr=pre=post");
            fail("Unquoted special char is invalid!");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("Invalid token character '=' in token \"pre=post\""));
        }
    }

    @Test
    public void parse_should_be_quoted_and_second_param() {
        try {
            var type = MediaType.parseMediaType("type/subtype;attr=pre=post;attr1=val1");
            fail("Unquoted special char is invalid!");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("Invalid token character '=' in token \"pre=post\""));
        }
    }

    @Test
    public void parse_unfinished_quoted() {
        try {
            var type = MediaType.parseMediaType("type/subtype;attr=\"pre=post");
            fail("Unfinished quoted string is invalid!");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("Invalid token character '\"' in token \"\"pre=post\""));
        }
    }

    @Test
    public void parse_quoted_mid_value() {
        try {
            var type = MediaType.parseMediaType("type/subtype;attr=pre\"=\"post");
            fail("Quotes mid value are invalid!");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("Invalid token character '\"' in token \"pre\"=\"post\""));
        }
    }

    @Test
    public void instantiate_simple() {
        var type = new MediaType("type", "subtype", Map.of("attr", "val"));
        assertEquals(1, type.getParameters().size());
        assertEquals("val", type.getParameter("attr"));
        assertEquals("type/subtype;attr=val", type.toString());
    }

    @Test
    public void instantiate_quoted() {
        var type = new MediaType("type", "subtype", Map.of("attr", "pre\"post"));
        assertEquals(1, type.getParameters().size());
        assertEquals("pre\"post", type.getParameter("attr"));
        assertEquals("type/subtype;attr=\"pre\\\"post\"", type.toString()); // type/subtype;attr="pre\"post"
    }

    @Test
    public void instantiate_should_not_be_empty() {
        try {
            var type = new MediaType("type", "subtype", Map.of("attr", ""));
            fail("Param values should not be empty!");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("'value' must not be empty"));
        }
    }

    @Test
    public void instantiate_should_not_be_null() {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("attr", null);
            var type = new MediaType("type", "subtype", params);
            fail("Param values should not be empty!");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("'value' must not be empty"));
        }
    }
}
