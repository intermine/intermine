package org.intermine.model.testmodel.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.servlet.http.HttpServletResponse;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.ExportException;
import org.intermine.web.logic.FieldExporter;

/**
 * CommaFieldExporter class
 *
 * @author Kim Rutherford
 */

public class CommaFieldExporter implements FieldExporter
{
   /** 
     * Example FieldExporter that splits the a String field at the first comma and outputs each bit
     * on a new line.
     * @param o the object of interest 
     * @param fieldName the field of the object
     * @param os the ObjectStore that contains the object
     * @param response The HTTP response we are creating - used to get the OutputStream to write to
     * @throws ExportException if the application business logic throws an exception
     */
    public void exportField(InterMineObject o, String fieldName, ObjectStore os,
                            HttpServletResponse response) throws ExportException {
        try {
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition ", "inline; filename=" + fieldName + ".txt");

            OutputStream outputStream = response.getOutputStream();

            PrintStream printStream = new PrintStream(outputStream);

            String fieldValue = (String) TypeUtil.getFieldValue(o, fieldName);
            
            int commaPos = fieldValue.indexOf(",");

            String firstPart = fieldValue.substring(0, commaPos + 1);
            String secondPart = fieldValue.substring(commaPos + 1).trim();

            printStream.println(firstPart);
            printStream.println(secondPart);

            printStream.close();
            outputStream.close();
        } catch (IllegalAccessException e) {
            throw new ExportException("unexpected IO error while exporting", e);
        } catch (IOException e) {
            throw new ExportException("unexpected IO error while exporting", e);
        }

    }
}
