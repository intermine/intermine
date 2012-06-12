package org.intermine.util;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * General-purpose methods for manipulating URIs and XML schema types
 * @author Mark Woodbridge
 */
public final class XmlUtil
{
    private XmlUtil() {
    }

    /**
     * Return the fragment portion of a URI string (i.e. everything after a #).
     * @param uri a uri string
     * @return the fragment or original uri if no # present
     */
    public static String getFragmentFromURI(String uri) {
        if (uri.indexOf('#') > 0) {
            return uri.substring(uri.indexOf('#') + 1);
        }
        return uri;
    }

    /**
     * Apply some indentiation to some XML. This method is not very sophisticated and will
     * not cope well with anything but the simplest XML (no CDATA etc). The algorithm used does
     * not look at element names and does not actually parse the XML. It also assumes that the
     * forward slash and greater-than at the end of a self-terminating tag and not seperated by
     * ant whitespace.
     *
     * @param xmlString input XML fragment
     * @return indented XML fragment
     */
    public static String indentXmlSimple(String xmlString) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int indent = 0;
        byte[] bytes = xmlString.getBytes();
        int i = 0;
        while (i < bytes.length) {
            if (bytes[i] == '<' && bytes[i + 1] == '/') {
                os.write('\n');
                writeIndentation(os, --indent);
            } else if (bytes[i] == '<') {
                if (i > 0) {
                    os.write('\n');
                }
                writeIndentation(os, indent++);
            } else if (bytes[i] == '/' && bytes[i + 1] == '>') {
                indent--;
            } else if (bytes[i] == '>') {

            }
            os.write(bytes[i++]);
        }
        return os.toString();
    }

    private static void writeIndentation(ByteArrayOutputStream os, int indent) {
        for (int j = 0; j < indent; j++) {
            os.write(' ');
            os.write(' ');
        }
    }

    private static Map<String, String> replacements = new HashMap<String, String>();

    static {
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

    /**
     * Replace greek character entity names with entity names that work in HTML.
     * @param value input string
     * @return string with replacements
     */
    public static String fixEntityNames(String value) {
        String retVal = value;

        if (retVal.indexOf('&') != -1) {
            for (Map.Entry<String, String> entry: replacements.entrySet()) {
                String orig = entry.getKey();
                String replacement = entry.getValue();
                retVal = retVal.replaceAll("&" + orig + ";", "&" + replacement + ";");
                if (retVal.indexOf('&') == -1) {
                    break;
                }
            }
        }

        return retVal;
    }
}
