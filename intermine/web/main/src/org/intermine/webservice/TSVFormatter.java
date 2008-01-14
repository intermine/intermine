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

/** Formatter that prints each result as tab separated values row.
 * @author Jakub Kulaviak
 **/
public class TSVFormatter extends Formatter 
{

    /**
     * TSVFormatter constructor.
     * @param out associated writer
     */
    public TSVFormatter(PrintWriter out) {
        this.out = out;
    }

    /**
     * {@inheritDoc}}
     */
    public void printResultItem(List<String> result) {
        for (int i = 0; i < result.size(); i++) {
            out.print(result.get(i));
            if (i != result.size() - 1) {
                out.print("\t");
            }
        }
        out.println();
    }
}
