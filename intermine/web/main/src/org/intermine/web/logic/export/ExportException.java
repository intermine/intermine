package org.intermine.web.logic.export;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Exception thrown if the there is a problem while exporting (eg. from FieldExporter)
 *
 * @author Kim Rutherford
 */
public class ExportException extends RuntimeException
{
    /**
     * default serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Create a new ExportException.
     * @param message the Exception description
     */
    public ExportException(String message) {
        super(message);
    }

    /**
     * Create a new ExportException.
     * @param message the Exception description
     * @param e the nested Exception
     */
    public ExportException(String message, Exception e) {
        super(message, e);
    }
}
