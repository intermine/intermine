package org.flymine.io.gff3;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import java.io.IOException;

/**
 * A class that represents one line of a GFF3 file.
 *
 * @author Kim Rutherford
 */

public class GFF3Record
{
    private String sequenceID;
    private String source;
    private String type;
    private int start;
    private int end;
    private double score;
    private String strand;
    private String phase;
    private Map attributes = new LinkedHashMap();

    /**
     * Flag to indicate that there is no score info.
     */
    public static double NO_SCORE = Double.NEGATIVE_INFINITY;

    public GFF3Record(String line) throws IOException {
        StringTokenizer st = new StringTokenizer(line, "\t", false);

        if (st.countTokens() < 8) {
            throw new IOException("GFF line too short (" + st.countTokens() + " fields): " + line);
        }

        sequenceID = st.nextToken();
        source = st.nextToken();
        type = st.nextToken();

        String startString = st.nextToken();
        try {
            start = Integer.parseInt(startString);
        } catch (NumberFormatException nfe) {
            throw new IOException("can not parse integer for start position: " + startString);
        }

        String endString = st.nextToken();
        try {
            end = Integer.parseInt(endString);
        } catch (NumberFormatException nfe) {
            throw new IOException("can not parse integer for end position: " + endString);
        }

        String scoreString = st.nextToken();

        if(scoreString.equals("") || scoreString.equals(".") || scoreString.equals("0")) {
            score = NO_SCORE;
        } else {
            try {
                score = Double.parseDouble(scoreString);
            } catch (NumberFormatException nfe) {
                throw new IOException("can not parse score: " + scoreString);
            }
        }

        strand = st.nextToken();

        phase = st.nextToken();

        if (st.hasMoreTokens()) {
            parseAttribute(st.nextToken(), line);
        }
    }

    private void parseAttribute(String attributeString, String line) throws IOException {
        StringTokenizer sTok = new StringTokenizer(attributeString, ";", false);

        while(sTok.hasMoreTokens()) {
            String attVal = sTok.nextToken().trim();
            String attName;
            List valList = new ArrayList();
            int spaceIndx = attVal.indexOf("=");
            if(spaceIndx == -1) {
                attName = attVal;
            } else {
                attName = attVal.substring(0, spaceIndx);
                attributeString = attVal.substring(spaceIndx + 1).trim();
                while(attributeString.length() > 0) {
                    if(attributeString.startsWith("\"")) {
                        attributeString = attributeString.substring(1);
                        int quoteIndx = attributeString.indexOf("\"");
                        if(quoteIndx > 0){
                            valList.add(attributeString.substring(0, quoteIndx));
                            attributeString = attributeString.substring(quoteIndx+1).trim();
                            if (attributeString.startsWith(",")) {
                                attributeString = attributeString.substring(1).trim();
                            }
                        } else {
                            throw new IOException("unmatched quote in this line: " + line);
                        }
                    } else {
                        int commaIndx = attributeString.indexOf(",");
                        if(commaIndx == -1) {
                            valList.add(attributeString);
                            attributeString = "";
                        } else {
                            valList.add(attributeString.substring(0, commaIndx));
                            attributeString = attributeString.substring(commaIndx+1).trim();
                        }
                    }
                }
            }
            attributes.put(attName, valList);
        }
    }


    public String getSequenceID() {
        return sequenceID;
    }

    public String getSource() {
        return source;
    }

    public String getType() {
        return type;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public double getScore() {
        return score;
    }

    public String getStrand() {
        return strand;
    }

    public String getPhase() {
        return phase;
    }

    public String getId() {
        return (String) getAttributes().get("ID");
    }

    public String getName () {
        return (String) getAttributes().get("Name");
    }

    public String getAlias () {
        return (String) getAttributes().get("Alias");
    }

    public String getParent () {
        return (String) getAttributes().get("Parent");
    }

    public String getTarget () {
        return (String) getAttributes().get("Target");
    }

    public String getGap () {
        return (String) getAttributes().get("Gap");
    }

    public String getNote () {
        return (String) getAttributes().get("Note");
    }

    public String getDbxref () {
        return (String) getAttributes().get("Dbxref");
    }

    public String getOntologyTerm () {
        return (String) getAttributes().get("Ontology_term");
    }

    public Map getAttributes() {
        return attributes;
    }

    public String toString() {
        return "<GFF3Record: sequenceID: " + sequenceID + " source: " + source + " type: "
            + type + " start: " + start + " end: " + end + " score: " + score + " strand: "
            + strand + " phase: " + phase + " attributes: " + attributes + ">";
    }
}
