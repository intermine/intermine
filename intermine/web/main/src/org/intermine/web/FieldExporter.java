package org.intermine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletResponse;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;

/**
 * An interface implemented by objects that can export the value of a particular. 
 *
 * @author Kim Rutherford
 */

public interface FieldExporter
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
                            HttpServletResponse response)
       throws ExportException;
}
