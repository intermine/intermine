package org.intermine.webservice.server.query.result;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * XMLValidator is class that validates xml string according an XML Schema at specified url.
 * @author Jakub Kulaviak
 **/
public class XMLValidator
{

    private XMLValidatorErrorHandler errorHandler = new XMLValidatorErrorHandler();

    /**
     * Validates an XML string according to an XML Schema at a given URL.
     * @param xml an XML string.
     * @param xmlSchemaUrl the URL of an XML Schema.
     */
    public void validate(String xml, String xmlSchemaUrl) {
        // Made according to http://www.javaworld.com/javaworld/jw-08-2005/jw-0808-xml.html?page=5
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            factory.setNamespaceAware(false);
            factory.setValidating(true);
            factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                    "http://www.w3.org/2001/XMLSchema");
            // Specify our own schema - this overrides the schemaLocation in the xml file
            factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource",
                    xmlSchemaUrl);

            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(errorHandler);
            try {
                builder.parse(new InputSource(new StringReader(xml)));
            } catch (SAXParseException ex) {
                // Ignore this exception - error appears in errorHandler and
                // it is displayed to  user
            }
        } catch (Exception ex) {
            throw new InternalErrorException("XML validation failed.", ex);
        }
    }

    /**
     * Returns errors occurred during parsing xml.
     * @return errors
     */
    public List<String> getErrors() {
        return errorHandler.getErrors();
    }

    /**
     * Returns warnings occurred during parsing xml.
     * @return warning
     */
    public List<String> getWarnings() {
        return errorHandler.getWarnings();
    }

    /**
     * Returns errors and warning occurred during parsing xml.
     * @return errors and warning
     */
    public List<String> getErrorsAndWarnings() {
        List<String> ret = new ArrayList<String>();
        for (String s : getErrors()) {
            ret.add(s);
        }
        for (String s : getWarnings()) {
            ret.add(s);
        }
        return ret;
    }
}
