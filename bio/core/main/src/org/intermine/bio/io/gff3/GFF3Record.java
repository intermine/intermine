package org.intermine.bio.io.gff3;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.intermine.util.StringUtil;

/**
 * A class that represents one line of a GFF3 file.  Some of this code is
 * derived from BioJava.
 *
 * @author Kim Rutherford
 */

public class GFF3Record
{
    private String sequenceID;
    private String source;
    private String type;
    private int    start;
    private int    end;
    private Double score;
    private String strand;
    private String phase;
    private Map    attributes   = new LinkedHashMap();
    private static Map replacements;

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

        sequenceID = fixEntityNames(URLDecoder.decode(st.nextToken(), "UTF-8"));

        source = st.nextToken();

        if (source.equals("") || source.equals(".")) {
            source = null;
        }

        type = st.nextToken();

        String startString = st.nextToken();
        try {
            if (startString.equals(".")) {
                start = -1;
            } else {
                start = Integer.parseInt(startString);
            }
        } catch (NumberFormatException nfe) {
            throw new IOException("can not parse integer for start position: " + startString);
        }

        String endString = st.nextToken();
        try {
            if (endString.equals(".")) {
                end = -1;
            } else {
                end = Integer.parseInt(endString);
            }
        } catch (NumberFormatException nfe) {
            throw new IOException("can not parse integer for end position: " + endString);
        }

        String scoreString = st.nextToken();

        if (scoreString.equals("") || scoreString.equals(".")) {
            score = null;
        } else {
            try {
                score = new Double(scoreString);
            } catch (NumberFormatException nfe) {
                throw new IOException("can not parse score: " + scoreString);
            }
        }

        strand = st.nextToken();

        if (strand.equals("") || strand.equals(".")) {
            strand = null;
        }

        phase = st.nextToken();

        if (phase.equals("") || phase.equals(".")) {
            phase = null;
        }

        if (st.hasMoreTokens()) {
            parseAttribute(st.nextToken(), line);
        }
    }

    /**
     * Create a new GFF3Record
     * @param sequenceID the sequence name
     * @param source the source
     * @param type the feature type
     * @param start the start coordinate on the sequence given by sequenceID
     * @param end the end coordinate on the sequence
     * @param score the feature score or null if there is no score
     * @param strand the feature strand or null
     * @param phase the phase or null
     * @param attributes a Map from attribute name to a List of attribute values
     */
    public GFF3Record(String sequenceID, String source, String type, int start, int end,
                      Double score, String strand, String phase, Map attributes) {
        this.sequenceID = sequenceID;
        this.source = source;
        this.type = type;
        this.start = start;
        this.end = end;
        this.score = score;
        this.strand = strand;
        this.phase = phase;
        this.attributes = attributes;
    }

    private void parseAttribute(String attributeString, String line) throws IOException {
        StringTokenizer sTok = new StringTokenizer(attributeString, ";", false);

        while (sTok.hasMoreTokens()) {
            String attVal = sTok.nextToken().trim();

            if (attVal.length() == 0) {
                continue;
            }
            
            String attName;
            List valList = new ArrayList();
            int spaceIndx = attVal.indexOf("=");
            if (spaceIndx == -1) {
                throw new IOException("the attributes section must contain name=value pairs, "
                                      + "while parsing: " + line);
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
            // Decode values
            for (int i = 0; i < valList.size(); i++) {
                String value = (String) valList.get(i);
                if (!attName.equals("Target") && !attName.equals("Gap")) {
                    value = URLDecoder.decode(value, "UTF-8");
                }
                value = fixEntityNames(value);
                valList.set(i, value);
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
     * Set the type of this record.
     * @param type the new type
     */
    public void setType(String type) {
        this.type = type;
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
    public Double getScore () {
        return score;
    }

    /**
     * Return the strand field of this record.
     * @return returns null if the strand is unset (ie. with an empty field or contained "." in the
     * original GFF3 file)
     */
    public String getStrand () {
        return strand;
    }

    /**
     * Return the phase field of this record.
     * @return returns null if the phase is unset (ie. with an empty field or contained "." in the
     * original GFF3 file)
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
     * Set the Id of this GFF3Record.
     * @param id the new id
     */
    public void setId(String id) {
        attributes.put("ID", Collections.singletonList(id));
    }

    /**
     * Return the list of the Name field from the attributes of this record.
     * @return the Name from the attributes of this record or null of there isn't a value
     */
    public List<String> getNames() {
        if (getAttributes().containsKey("Name")) {
            return (List) getAttributes().get("Name");
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
     * Return the list of the Parent field from the attributes of this record.
     * @return the Parent from the attributes of this record or null of there isn't a value
     */
    public List<String> getParents () {
        if (getAttributes().containsKey("Parent")) {
            return (List) getAttributes().get("Parent");
        } else {
            return null;
        }
    }

    /**
     * Return the first value of the Target field from the attributes of this record.
     * @return the Target from the attributes of this record or null of there isn't a value
     */
    public String getTarget() {
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
    public String getGap() {
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
    public String getNote() {
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
    public List<String> getDbxrefs() {
        if (getAttributes().containsKey("Dbxref")) {
            return (List) getAttributes().get("Dbxref");
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
     * {@inheritDoc}
     */
    public String toString() {
        return "<GFF3Record: sequenceID: " + sequenceID + " source: " + source + " type: "
            + type + " start: " + start + " end: " + end + " score: " + score + " strand: "
            + strand + " phase: " + phase + " attributes: " + attributes + ">";
    }

    /**
     * Return this record in GFF format.  The String is suitable for output to a GFF file.
     * @return a GFF line
     */
    public String toGFF3() {
        try {
            return URLEncoder.encode(sequenceID, "UTF-8") + "\t"
                + ((source == null) ? "." : source) + "\t"
                + type + "\t" + start + "\t" + end + "\t"
                + ((score == null) ? "." : score.toString()) + "\t"
                + ((strand == null) ? "." : strand) + "\t"
                + ((phase == null) ? "." : phase) + "\t"
                + writeAttributes();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("error while encoding: " + sequenceID, e);
        }
    }

    private String writeAttributes() {
        StringBuffer sb = new StringBuffer();
        boolean first = true;
        Iterator iter = attributes.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            if (!first) {
                sb.append(";");
            }
            first = false;
            String listValue;
            if (entry.getValue() instanceof List) {
                List oldList = (List) entry.getValue();
                List encodedList = new ArrayList(oldList);

                for (int i = 0; i < encodedList.size(); i++) {
                    Object oldValue = encodedList.get(i);
                    String newValue;
                    try {
                        newValue = URLEncoder.encode("" + oldValue, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException("error while encoding: " + oldValue, e);
                    }
                    encodedList.set(i, newValue);
                }

                listValue = StringUtil.join(encodedList, ",");
            } else {
                try {
                    listValue = URLEncoder.encode("" + entry.getValue(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("error while encoding: " + entry.getValue(), e);
                }
            }
            sb.append(entry.getKey() + "=" + listValue);
        }
        return sb.toString();
    }

    /**
     * Replace greek character entity names with entity names that work in HTML.
     * @param value input string
     * @return string with replacements
     */
    protected static String fixEntityNames(String value) {
        synchronized (GFF3Record.class) {
            if (replacements == null) {
                replacements = new HashMap();
                replacements.put("agr", "alpha");
                replacements.put("Agr", "Alpha");
                replacements.put("bgr", "beta");
                replacements.put("Bgr", "Beta");
                replacements.put("ggr", "gamma");
                replacements.put("Ggr", "Gamma");
                replacements.put("dgr", "delta");
                replacements.put("Dgr", "Delta");
                replacements.put("egr", "epsilon");
                replacements.put("Egr", "Epsilon");
                replacements.put("zgr", "zeta");
                replacements.put("Zgr", "Zeta");
                replacements.put("eegr", "eta");
                replacements.put("EEgr", "Eta");
                replacements.put("thgr", "theta");
                replacements.put("THgr", "Theta");
                replacements.put("igr", "iota");
                replacements.put("Igr", "Iota");
                replacements.put("kgr", "kappa");
                replacements.put("Kgr", "Kappa");
                replacements.put("lgr", "lambda");
                replacements.put("Lgr", "Lambda");
                replacements.put("mgr", "mu");
                replacements.put("Mgr", "Mu");
                replacements.put("ngr", "nu");
                replacements.put("Ngr", "Nu");
                replacements.put("xgr", "xi");
                replacements.put("Xgr", "Xi");
                replacements.put("ogr", "omicron");
                replacements.put("Ogr", "Omicron");
                replacements.put("pgr", "pi");
                replacements.put("Pgr", "Pi");
                replacements.put("rgr", "rho");
                replacements.put("Rgr", "Rho");
                replacements.put("sgr", "sigma");
                replacements.put("Sgr", "Sigma");
                replacements.put("sfgr", "sigmaf");
                replacements.put("tgr", "tau");
                replacements.put("Tgr", "Tau");
                replacements.put("ugr", "upsilon");
                replacements.put("Ugr", "Upsilon");
                replacements.put("phgr", "phi");
                replacements.put("PHgr", "Phi");
                replacements.put("khgr", "chi");
                replacements.put("KHgr", "Chi");
                replacements.put("psgr", "psi");
                replacements.put("PSgr", "Psi");
                replacements.put("ohgr", "omega");
                replacements.put("OHgr", "Omega");
            }
        }

        for (Iterator iter = replacements.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            if (value.indexOf('&') != -1) {
                value = value.replaceAll("&" + entry.getKey() + ";", "&" + entry.getValue() + ";");
            }
        }

        return value;
    }
}
