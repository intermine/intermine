package org.intermine.util;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Basic SAX Parser
 *
 * @author Mark Woodbridge
 */
public class SAXParser
{
    /**
     * Parse the an xml file
     * @param is the inputsource to parse
     * @param handler the SAX event handler to use
     * @throws Exception if an error occuring during parsing
     */
    public static void parse(InputSource is, DefaultHandler handler) throws Exception {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(true);
            factory.newSAXParser().parse(is, handler);
        } catch (ParserConfigurationException e) {
            throw new Exception("The underlying parser does not support "
                                + " the requested features", e);
        } catch (SAXException e) {
            throw new Exception("Error parsing XML document", e);
        }
    }
}
