package org.intermine.web.logic.export;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.intermine.api.results.ResultElement;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * @author Jakub Kulaviak
 **/
public class ExcelExporterTest extends TestCase
{

    public void testExport() throws IOException {
        List<List<ResultElement>> input = new ArrayList<List<ResultElement>>();
        input.add(ExportTestUtil.getRow(10, "1", true, "EmployeeA1"));
        input.add(ExportTestUtil.getRow(20, "2", false, "EmployeeA2"));
        Date date = new Date();
        input.add(getDateRow(date));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Exporter exporter = new ExcelExporter(out);
        exporter.export(input.iterator());
        InputStream is = new ByteArrayInputStream(out.toByteArray());
        HSSFWorkbook wb = new HSSFWorkbook(is);
        HSSFSheet sheet = wb.getSheet("results");

        HSSFRow row = sheet.getRow(0);
        assertEquals(10.0, row.getCell((short) 0).getNumericCellValue());
        assertEquals(HSSFCell.CELL_TYPE_NUMERIC, row.getCell((short) 0).getCellType());
        assertEquals("1", row.getCell((short) 1).getStringCellValue());
        assertEquals("true", row.getCell((short) 2).getStringCellValue());
        assertEquals("EmployeeA1", row.getCell((short) 3).getStringCellValue());

        row = sheet.getRow(1);
        assertEquals(20.0, row.getCell((short) 0).getNumericCellValue());
        assertEquals("2", row.getCell((short) 1).getStringCellValue());
        assertEquals("false", row.getCell((short) 2).getStringCellValue());
        assertEquals("EmployeeA2", row.getCell((short) 3).getStringCellValue());

        // test that date was added with exporter as date - it is numeric format in excel
        row = sheet.getRow(2);
        String expected = date.toString();
        String returned = row.getCell((short) 0).getDateCellValue().toString();
        System.out.println(expected);
        System.out.println(returned);
        assertEquals(expected, returned);
        assertEquals(HSSFCell.CELL_TYPE_NUMERIC, row.getCell((short) 0).getCellType());
    }

    private List<ResultElement> getDateRow(Date date) {
        List<ResultElement> ret = new ArrayList<ResultElement>();
        ret.add(new ResultElement(date));
        return ret;
    }
}
