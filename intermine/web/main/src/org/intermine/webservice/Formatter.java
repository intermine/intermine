package org.intermine.webservice;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Formatter is abstract class for printing formatted output. Its subclasses prints
 * xml formatted output or tab separated values output. 
 * @author Jakub Kulaviak
 **/
public abstract class Formatter 
{

    protected PrintWriter out;
    
    /**
     * Prints header. 
     * @param attributes attributes of the header element
     */
    public void printHeader(Map<String, String> attributes) { }
 
    /**
     * Prints footer. 
     */
    public void printFooter() { }

    /**
     * Prints result item. Format depends at subclass implementation.
     * @param item printed item
     */
    public abstract void printResultItem(List<String> item);

    /**
     * Prints errors as simple xml like string.
     * Example:
     * &lt;error&gt;&lt;message&gt; invalid format parameter    &lt;/message&gt;
        &lt;/error&gt;
     * @param errors errors to be printed
     */
    public void printErrors(List<String> errors) {
        out.write("<error>");
        for (String error : errors) {
            out.write("    <message>");
            out.write(error);
            out.write("    </message>\n");
        }
        out.write("</error>");
    }

    /**
     * Prints error
     * @param error error to be printed
     */
    public void printError(String error) {
        List<String> errors = new ArrayList<String>();
        errors.add(error);
        printErrors(errors);
    }

    /**
     * Returns associated output stream writer.
     * @return associated writer
     */
    public PrintWriter getOut() {
        return out;
    }

    /**
     * Sets associated output stream writer.
     * @param out associated writer
     */
    public void setOut(PrintWriter out) {
        this.out = out;
    }
}
