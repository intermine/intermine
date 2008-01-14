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
import java.util.List;
import java.util.Map;


/**
 * XMLFormatter is class that prints out xml formatted output.  
 * @author Jakub Kulaviak
 **/
public class XMLFormatter extends Formatter 
{

    private boolean headerPrinted = false;

    /**
     * XMLFormatter constructor.
     * @param out associated writer that prints to its stream
     */
    public XMLFormatter(PrintWriter out) {
        this.out = out;
    }
        
    /**
     * {@inheritDoc}}
     */
    public void printHeader(Map<String, String> attributes) {
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.print("<ResultSet ");
        for (String key : attributes.keySet()) {
            out.print(key + "=\"" + attributes.get(key) + "\" ");
        }
        out.println(">");
        headerPrinted = true;
    }

    /**
     * {@inheritDoc}}
     */
    public void printFooter() {
        if (headerPrinted) {
            out.println("</ResultSet>");    
        }
    }
    
    /**
     * {@inheritDoc}}
     */
    public void printResultItem(List<String> result) {
        out.print("<Result>");
        for (String s : result) {
            out.print("<i>");
            out.print(s);
            out.print("</i>");
        }
        out.println("</Result>");
    }
}
