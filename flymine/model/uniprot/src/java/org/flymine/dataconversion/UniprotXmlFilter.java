package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Extract only entries for organisms of interest from UniProt XML dump files.  A
 * list of organism full names can be provided.  Currently ignores any entries for
 * more than one organism.
 *
 * @author Richard Smith
 */
public class UniprotXmlFilter
{
    private Set organisms;

    /**
     * Construct a filter with a set of organism full names to extract
     * entries for.
     * @param organisms organism full name Strings
     */
    public UniprotXmlFilter(Set organisms) {
        this.organisms = organisms;
    }

    /**
     * Filter a UniProt XML dump file, write entries for organisms of interest to
     * a new BufferedWriter.  Currently ignores entries for more than one organism.
     * @param in the UniProt XML dump
     * @param out to write output to
     * @throws IOException if problem with input or output
     */
    public void filter(BufferedReader in, BufferedWriter out) throws IOException {
        StringBuffer sb = new StringBuffer();
        String line = null;
        boolean keep = true;
        boolean inOrganism = false;

        while ((line = in.readLine()) != null) {
            // quicker to trim whole file first?
            String trimmed = trimLeft(line);
            if (trimmed.startsWith("<entry")) {
                // make sure opening element is included
                if (keep) {
                    out.write(sb.toString());
                }
                sb = new StringBuffer();
                keep = true;
                inOrganism = false;
            } else if (trimmed.startsWith("<organism")) {
                inOrganism = true;
            } else if (inOrganism && trimmed.startsWith("<dbReference type=\"NCBI Taxonomy")) {
                // ignores the possibility of a protein being linked to multiple organisms
                int start = trimmed.indexOf("id=\"") + 4;
                String taxonId = trimmed.substring(start, trimmed.indexOf('"', start));
                if (!(organisms.contains(taxonId))) {
                    keep = false;
                }
            }
            if (keep) {
                sb.append(line + System.getProperty("line.separator"));
            }
        }
        // catch final entry
        if (keep) {
            out.write(sb.toString());
        } else {
            out.write("</uniprot>");
        }
        out.flush();
    }


    // should move to InterMine StringUtil
    private String trimLeft(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return s.substring(i);
            }
        }
        return s;
    }
}
