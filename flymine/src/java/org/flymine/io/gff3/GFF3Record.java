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
    private static final double NO_SCORE = Double.NEGATIVE_INFINITY;

    /**
     * Create a GFF3Record from a line of a GFF3 file
     * @param line the String to parse
     * @throws IOException if there is an error during parsing the line
     */
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

        if (scoreString.equals("") || scoreString.equals(".") || scoreString.equals("0")) {
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

        while (sTok.hasMoreTokens()) {
            String attVal = sTok.nextToken().trim();
            String attName;
            List valList = new ArrayList();
            int spaceIndx = attVal.indexOf("=");
            if (spaceIndx == -1) {
                attName = attVal;
            } else {
                attName = attVal.substring(0, spaceIndx);
                attributeString = attVal.substring(spaceIndx + 1).trim();
                while (attributeString.length() > 0) {
                    if (attributeString.startsWith("\"")) {
                        attributeString = attributeString.substring(1);
                        int quoteIndx = attributeString.indexOf("\"");
                        if (quoteIndx > 0) {
                            valList.add(attributeString.substring(0, quoteIndx));
                            attributeString = attributeString.substring(quoteIndx + 1).trim();
                            if (attributeString.startsWith(",")) {
                                attributeString = attributeString.substring(1).trim();
                            }
                        } else {
                            throw new IOException("unmatched quote in this line: " + line);
                        }
                    } else {
                        int commaIndx = attributeString.indexOf(",");
                        if (commaIndx == -1) {
                            valList.add(attributeString);
                            attributeString = "";
                        } else {
                            valList.add(attributeString.substring(0, commaIndx));
                            attributeString = attributeString.substring(commaIndx + 1).trim();
                        }
                    }
                }
            }
            attributes.put(attName, valList);
        }
    }

    /**
     * Return the sequenceID field of this record.
     * @return the sequenceID field of this record
     */
    public String getSequenceID () {
        return sequenceID;
    }

    /**
     * Return the source field of this record.
     * @return the source field of this record
     */
    public String getSource () {
        return source;
    }

    /**
     * Return the type field of this record.
     * @return the type field of this record
     */
    public String getType () {
        return type;
    }

    /**
     * Return the start field of this record.
     * @return the start field of this record
     */
    public int getStart () {
        return start;
    }

    /**
     * Return the end field of this record.
     * @return the end field of this record
     */
    public int getEnd () {
        return end;
    }

    /**
     * Return the score field of this record.
     * @return the score field of this record
     */
    public double getScore () {
        return score;
    }

    /**
     * Return the strand field of this record.
     * @return the strand field of this record
     */
    public String getStrand () {
        return strand;
    }

    /**
     * Return the phase field of this record.
     * @return the phase field of this record
     */
    public String getPhase () {
        return phase;
    }

    /**
     * Return the first value of the Id field from the attributes of this record.
     * @return the Id from the attributes of this record or null of there isn't a value
     */
    public String getId () {
        if (getAttributes().containsKey("ID")) {
            return (String) ((List) getAttributes().get("ID")).get(0);
        } else {
            return null;
        }
    }

    /**
     * Return the first value of the Name field from the attributes of this record.
     * @return the Name from the attributes of this record or null of there isn't a value
     */
    public String getName () {
        if (getAttributes().containsKey("Name")) {
            return (String) ((List) getAttributes().get("Name")).get(0);
        } else {
            return null;
        }
    }

    /**
     * Return the first value of the Alias field from the attributes of this record.
     * @return the Alias from the attributes of this record or null of there isn't a value
     */
    public String getAlias () {
        if (getAttributes().containsKey("Alias")) {
            return (String) ((List) getAttributes().get("Alias")).get(0);
        } else {
            return null;
        }
    }

    /**
     * Return the first value of the Parent field from the attributes of this record.
     * @return the Parent from the attributes of this record or null of there isn't a value
     */
    public String getParent () {
        if (getAttributes().containsKey("Parent")) {
            return (String) ((List) getAttributes().get("Parent")).get(0);
        } else {
            return null;
        }
    }

    /**
     * Return the first value of the Target field from the attributes of this record.
     * @return the Target from the attributes of this record or null of there isn't a value
     */
    public String getTarget () {
        if (getAttributes().containsKey("Target")) {
            return (String) ((List) getAttributes().get("Target")).get(0);
        } else {
            return null;
        }
    }

    /**
     * Return the first value of the Gap field from the attributes of this record.
     * @return the Gap from the attributes of this record or null of there isn't a value
     */
    public String getGap () {
        if (getAttributes().containsKey("Gap")) {
            return (String) ((List) getAttributes().get("Gap")).get(0);
        } else {
            return null;
        }
    }

    /**
     * Return the first value of the Note field from the attributes of this record.
     * @return the Note from the attributes of this record or null of there isn't a value
     */
    public String getNote () {
        if (getAttributes().containsKey("Note")) {
            return (String) ((List) getAttributes().get("Note")).get(0);
        } else {
            return null;
        }
    }

    /**
     * Return the first value of the Dbxref field from the attributes of this record.
     * @return the Dbxref from the attributes of this record or null of there isn't a value
     */
    public String getDbxref () {
        if (getAttributes().containsKey("Dbxref")) {
            return (String) ((List) getAttributes().get("Dbxref")).get(0);
        } else {
            return null;
        }
    }

    /**
     * Return the first value of the OntologyTerm field from the attributes of this record.
     * @return the OntologyTerm from the attributes of this record or null of there isn't a value
     */
    public String getOntologyTerm () {
        if (getAttributes().containsKey("Ontology_term")) {
            return (String) ((List) getAttributes().get("Ontology_term")).get(0);
        } else {
            return null;
        }
    }

    /**
     * Return the attributes of this record as a Map from attribute key to Lists of attribute
     * values.
     * @return the attributes of this record
     */
    public Map getAttributes () {
        return attributes;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "<GFF3Record: sequenceID: " + sequenceID + " source: " + source + " type: "
            + type + " start: " + start + " end: " + end + " score: " + score + " strand: "
            + strand + " phase: " + phase + " attributes: " + attributes + ">";
    }
}
