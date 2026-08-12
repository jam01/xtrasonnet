package io.github.jam01.xtrasonnet.plugins;

/*-
 * Copyright 2022-2026 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.TestUtils;
import io.github.jam01.xtrasonnet.Transformer;
import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class ExcelPluginTest {
    private final String simple_xlsx_json = """
            {
                "Sheet1": [
                    {"A":"a1","B":"b1","C":"c1","D":"d1","E":"e1"},
                    {"A":"a2","B":"b2","C":"c2","D":"d2","E":"e2"},
                    {"A":"a3","B":"b3","C":"c3","D":"d3","E":"e3"},
                    {"A":"a4","B":"b4","C":"c4","D":"d4","E":"e4"},
                    {"A":"a5","B":"b5","C":"c5","D":"d5","E":"e5"}
                ],
                "Sheet2": [
                    {"A":"a1","B":"b1","C":"c1","D":"d1","E":"e1"},
                    {"A":"a2","B":"b2","C":"c2","D":"d2","E":"e2"},
                    {"A":"a3","B":"b3","C":"c3","D":"d3","E":"e3"},
                    {"A":"a4","B":"b4","C":"c4","D":"d4","E":"e4"},
                    {"A":"a5","B":"b5","C":"c5","D":"d5","E":"e5"}
                ],
                "Sheet3": [
                    {"A":"a1","B":"b1","C":"c1","D":"d1","E":"e1"},
                    {"A":"a2","B":"b2","C":"c2","D":"d2","E":"e2"},
                    {"A":"a3","B":"b3","C":"c3","D":"d3","E":"e3"},
                    {"A":"a4","B":"b4","C":"c4","D":"d4","E":"e4"},
                    {"A":"a5","B":"b5","C":"c5","D":"d5","E":"e5"}
                ]
            }""";

    @Test
    public void read_xlsx_simple() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(TestUtils.resourceAsFile("simple.xlsx"), MediaTypes.APPLICATION_EXCEL));

        JSONAssert.assertEquals(simple_xlsx_json, doc.getContent(), true);
        Assertions.assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    @Test
    public void read_xls_simple() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(TestUtils.resourceAsFile("simple.xls"), MediaTypes.APPLICATION_OOXML_SPREADSHEET_SHEET));

        JSONAssert.assertEquals(simple_xlsx_json, doc.getContent(), true);
        Assertions.assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    public final String simple_empty_cells = """
            {
                "Sheet1":[
                    {"B":"b2","C":"c2","D":"d2","E":"e2"},
                    {"B":"b3","C":"c3","D":"d3","E":"e3"},
                    {"B":"b4","C":"c4","D":"d4","E":"e4"},
                    {"B":"b5","C":"c5","D":"d5","E":"e5"}
                ],
                "Sheet2":[
                    {"A":"a1","C":"c1","D":"d1","E":"e1"},
                    {"A":"a3","C":"c3","D":"d3","E":"e3"},
                    {"A":"a4","C":"c4","D":"d4","E":"e4"},
                    {"A":"a5","C":"c5","D":"d5","E":"e5"}
                ],
                "Sheet3":[
                        {"A":"a1","B":"b1","D":"d1","E":"e1"},
                        {"A":"a2","B":"b2","D":"d2","E":"e2"},
                        {"A":"a4","B":"b4","D":"d4","E":"e4"},
                        {"A":"a5","B":"b5","D":"d5","E":"e5"}
                ]
            }
            """;

    @Test
    public void read_xlsx_simple_empty_cells() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(TestUtils.resourceAsFile("simple-empty-cells.xlsx"), MediaTypes.APPLICATION_EXCEL));

        JSONAssert.assertEquals(simple_empty_cells, doc.getContent(), true);
        Assertions.assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    public String dates_xlsx_json = """
            { "Sheet1": [
                {"A":123,"B":456,"C":789},
                {"A":123.456,"B":789.123,"C":456.789},
                {"A":"2023-03-29T00:00","B":"2023-03-30T00:00","C":"2023-03-31T00:00"}
            ]}""";

    @Test
    public void read_xlsx_dates() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(TestUtils.resourceAsFile("dates.xlsx"), MediaTypes.APPLICATION_EXCEL));

        JSONAssert.assertEquals(dates_xlsx_json, doc.getContent(), true);
        Assertions.assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    public String formula_xlsx_json = """
            {"Sheet3":[{"A":"","B":"","C":"","D":"","E":""},{"A":"","B":60,"C":60,"D":3600,"E":""}]}""";

    @Test
    public void read_xlsx_formula() throws JSONException {
        var doc = new Transformer("payload")
                .transform(Document.of(TestUtils.resourceAsFile("formula.xlsx"), MediaTypes.APPLICATION_EXCEL));

        JSONAssert.assertEquals(formula_xlsx_json, doc.getContent(), true);
        Assertions.assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }

    /**
     * The whole cell-type range through MatrixExcelPlugin's shared conversion: boolean false must
     * survive (not turn into a null value), and a date-formatted numeric renders as an ISO string
     * rather than Excel's serial number.
     */
    @Test
    public void read_xlsx_matrix_booleansAndDates() throws Exception {
        byte[] bytes;
        try (var wb = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var row = wb.createSheet("s1").createRow(0);
            row.createCell(0).setCellValue(true);
            row.createCell(1).setCellValue(false);
            row.createCell(2).setCellValue("text");
            row.createCell(3).setCellValue(2.5);

            var dateCell = row.createCell(4);
            dateCell.setCellValue(LocalDateTime.of(2019, 7, 10, 0, 0));
            var style = wb.createCellStyle();
            style.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
            dateCell.setCellStyle(style);

            wb.write(out);
            bytes = out.toByteArray();
        }

        var doc = Transformer.builder("payload")
                .configurePlugins(plugins -> {
                    plugins.add(new MatrixExcelPlugin());
                    plugins.add(new DefaultJSONPlugin());
                })
                .build()
                .transform(Document.of(new ByteArrayInputStream(bytes), MediaTypes.APPLICATION_EXCEL));

        JSONAssert.assertEquals("""
                [[[true, false, "text", 2.5, "2019-07-10T00:00"]]]""", doc.getContent(), true);
    }

    @Test
    public void read_unreadableContentFailsWithPluginException() {
        var garbage = new ByteArrayInputStream("this is not a spreadsheet".getBytes(StandardCharsets.UTF_8));

        var thrown = Assertions.assertThrows(Exception.class, () -> new Transformer("payload")
                .transform(Document.of(garbage, MediaTypes.APPLICATION_EXCEL)));

        var chain = new StringBuilder();
        for (Throwable t = thrown; t != null; t = t.getCause()) {
            chain.append(t.getClass().getSimpleName()).append(": ").append(t.getMessage()).append(" | ");
        }
        Assertions.assertTrue(chain.toString().contains("Could not read spreadsheet"),
                "expected the declared PluginException naming the failure, got: " + chain);
    }

    /**
     * MatrixExcelPlugin is not part of DataFormatService.DEFAULT and registers the same media type
     * as DefaultExcelPlugin, so it has to be configured as the only Excel reader to be reachable.
     */
    @Test
    public void read_xlsx_formula_matrix() throws JSONException {
        var doc = Transformer.builder("payload")
                .configurePlugins(plugins -> {
                    plugins.add(new MatrixExcelPlugin());
                    plugins.add(new DefaultJSONPlugin());
                })
                .build()
                .transform(Document.of(TestUtils.resourceAsFile("formula.xlsx"), MediaTypes.APPLICATION_EXCEL));

        JSONAssert.assertEquals("""
                [[["","","","",""],["",60,60,3600,""]]]""", doc.getContent(), true);
        Assertions.assertEquals(MediaTypes.APPLICATION_JSON, doc.getMediaType());
    }
}
