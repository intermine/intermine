package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.FileWriter;
import java.util.GregorianCalendar;
import java.text.DateFormat;

/**
 * Class to log stuff
 * @author Kim Rutherford
 */
public class Logger
{
    protected static final String FILENAME = "/home/mark/cvs/intermine/inter.log";

    /**
     * Log a message
     * @param message the message to log
     */
    public static void log(String message) {
        try {
            PrintWriter f = new PrintWriter(new FileWriter (FILENAME, true));
            f.println(DateFormat.getDateTimeInstance().format(new GregorianCalendar().getTime())
                      + ": " + message);
            f.close();
        } catch (Exception e) {
        }
    }

    /**
     * Log an exception
     * @param e the exception to log
     */
    public static void log(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        log(sw.toString());
    }
}
