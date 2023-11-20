package io.github.jam01.xtrasonnet.plugins;

/*-
 * Copyright 2022-2023 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import io.github.jam01.xtrasonnet.document.Document;
import io.github.jam01.xtrasonnet.document.MediaType;
import io.github.jam01.xtrasonnet.document.MediaTypes;
import io.github.jam01.xtrasonnet.spi.BasePlugin;
import io.github.jam01.xtrasonnet.spi.PluginException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import sjsonnet.EvalScope;
import sjsonnet.Position;
import sjsonnet.Val;
import upickle.core.Visitor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class DefaultExcelPlugin extends BasePlugin {
    public DefaultExcelPlugin() {
        supportedTypes.add(MediaTypes.APPLICATION_EXCEL);
        supportedTypes.add(MediaTypes.APPLICATION_OOXML_SPREADSHEET_SHEET);

        readerSupportedClasses.add(InputStream.class);
        readerSupportedClasses.add(File.class);
    }

    @Override
    public boolean canWrite(MediaType requestedType, Class<?> clazz) {
        return false;
    }

    @Override
    public Val.Literal read(Document<?> doc, Position pos) throws PluginException {
        if (doc.getContent() == null) {
            return new Val.Null(new Position(null, 0));
        }

        var bVisitor = new LiteralVisitor(pos).visitObject(-1, -1).narrow();
        Class<?> targetType = doc.getContent().getClass();

        try (Workbook book = File.class.isAssignableFrom(targetType) ? WorkbookFactory.create((File) doc.getContent()) : WorkbookFactory.create((InputStream) doc.getContent())) {
            var sheets = book.iterator();

            while (sheets.hasNext()) {
                var sheet = sheets.next();
                var rows = sheet.iterator();
                var bKeyVisitor = bVisitor.visitKey(-1);
                bVisitor.visitKeyValue(bKeyVisitor.visitString(sheet.getSheetName(), -1));
                var sVisitor = bVisitor.subVisitor().visitArray(-1, -1).narrow();

                while (rows.hasNext()) {
                    var row = rows.next();
                    var cells = row.iterator();
                    var rVisitor = sVisitor.subVisitor().visitObject(-1, -1).narrow();

                    while (cells.hasNext()) {
                        var cell = cells.next();
                        var rKeyVisitor = rVisitor.visitKey(-1);
                        rVisitor.visitKeyValue(rKeyVisitor.visitString(CellReference.convertNumToColString(cell.getAddress().getColumn()), -1));
                        var cVisitor = rVisitor.subVisitor();

                        var type = cell.getCellType();
                        Val.Literal val = literalOf(type, cell, cVisitor);
                        rVisitor.visitValue(val, -1);
                    }
                    sVisitor.visitValue(rVisitor.visitEnd(-1), -1);
                }
                bVisitor.visitValue(sVisitor.visitEnd(-1), -1);
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        return bVisitor.visitEnd(-1);
    }

    private static Val.Literal literalOf(CellType type, Cell cell, Visitor<?, ?> cVisitor) {
        if (CellType.BOOLEAN == type) {
            if (cell.getBooleanCellValue()) return (Val.Literal) cVisitor.visitTrue(-1);
            else return (Val.Literal) cVisitor.visitFalse(-1);
        } else if (CellType.NUMERIC == type) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return (Val.Literal) cVisitor.visitString(cell.getLocalDateTimeCellValue().toString(), -1);
            } else {
                return (Val.Literal) cVisitor.visitFloat64(cell.getNumericCellValue(), -1);
            }
        } else if (CellType.STRING == type || CellType.BLANK == type) {
            return (Val.Literal) cVisitor.visitString(cell.getStringCellValue(), -1);
        } else if (CellType.FORMULA == type) {
            return literalOf(cell.getCachedFormulaResultType(), cell, cVisitor);
        } else {
            throw new IllegalArgumentException("Cannot represent type: " + type.toString() + " as a jsonnet element");
        }
    }

    @Override
    public <T> Document<T> write(Val input, MediaType mediaType, Class<T> targetType, EvalScope ev) throws PluginException {
        throw new UnsupportedOperationException("Writing excel files is unsupported");
    }
}
