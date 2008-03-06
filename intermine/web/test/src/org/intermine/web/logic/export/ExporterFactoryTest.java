package org.intermine.web.logic.export;

import java.io.OutputStream;

import junit.framework.TestCase;

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
public class ExporterFactoryTest extends TestCase
{

    public void testCreateExporter() {
        Exporter exporter = ExporterFactory.createExporter(System.out, ExporterFactory.CSV);
        assertTrue(exporter instanceof ExporterImpl);
        exporter = ExporterFactory.createExporter(System.out, ExporterFactory.TAB);
        assertTrue(exporter instanceof ExporterImpl);
        exporter = ExporterFactory.createExporter(System.out, ExporterFactory.EXCEL);
        assertTrue(exporter instanceof ExcelExporter);
    }
}
