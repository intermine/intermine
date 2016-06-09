package org.intermine.util;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Basic SAX Parser
 *
 * @author Mark Woodbridge
 */
public final class SAXParser
{
    private SAXParser() {
    }

    /**
     * Parse the an xml file
     * @param is the inputsource to parse
     * @param handler the SAX event handler to use
     * @throws SAXException if an error occurs during parsing
     * @throws IOException if an error occurs reading from the InputSource
     * @throws ParserConfigurationException if there is an error in the config
     */
    public static void parse(InputSource is, DefaultHandler handler)
        throws SAXException, IOException, ParserConfigurationException {
        parse(is, handler, true);
    }

    /**
     * Parse the an xml file
     * @param is the inputsource to parse
     * @param handler the SAX event handler to use
     * @param validate if true, validate before parsing
     * @throws SAXException if an error occurs during parsing
     * @throws IOException if an error occurs reading from the InputSource
     * @throws ParserConfigurationException if there is an error in the config
     */
    public static void parse(InputSource is, DefaultHandler handler, boolean validate)
        throws SAXException, IOException, ParserConfigurationException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(validate);
            factory.newSAXParser().parse(is, handler);
        } catch (ParserConfigurationException e) {
            ParserConfigurationException e2 = new ParserConfigurationException("The underlying "
                    + "parser does not support the requested features");
            e2.initCause(e);
            throw e2;
        } catch (SAXException e) {
            throw e;
        }
    }
}
