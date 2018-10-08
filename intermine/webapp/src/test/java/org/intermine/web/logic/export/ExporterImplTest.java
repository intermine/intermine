package org.intermine.web.logic.export;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

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
public class ExporterImplTest extends TestCase
{

    public void testExport() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<List<ResultElement>> input = ExporterImplTest.getInput();
        RowFormatter formatter = new RowFormatterImpl(",", true);
        ExporterImpl exporter = new ExporterImpl(out, formatter);
        exporter.export(input.iterator(), null, null);
        assertEquals(getExpected(), out.toString());
    }

    private String getExpected() {
        return
        "10,\"1\",\"true\",\"EmployeeA1\"\n" +
        "20,\"2\",\"true\",\"EmployeeA2\"\n" +
        "30,\"3\",\"false\",\"EmployeeA3\"\n" +
        "40,\"4\",\"true\",\"EmployeeB1\"\n" +
        "50,\"5\",\"true\",\"EmployeeB2\"\n" +
        "60,\"6\",\"true\",\"EmployeeB3\"\n";
    }

    static List<List<ResultElement>> getInput() {
        List<List<ResultElement>> input = new ArrayList<List<ResultElement>>();
        input.add(ExportTestUtil.getRow(10, "1", true, "EmployeeA1"));
        input.add(ExportTestUtil.getRow(20, "2", true, "EmployeeA2"));
        input.add(ExportTestUtil.getRow(30, "3", false, "EmployeeA3"));
        input.add(ExportTestUtil.getRow(40, "4", true, "EmployeeB1"));
        input.add(ExportTestUtil.getRow(50, "5", true, "EmployeeB2"));
        input.add(ExportTestUtil.getRow(60, "6", true, "EmployeeB3"));
        return input;
    }
}
