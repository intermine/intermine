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
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.Logger;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.xml.sax.SAXParseException;

/**
 * XMLValidator is class that validates xml string according an XML Schema at specified url.
 * @author Radek Štěpán after Jakub Kulaviak
 **/
public class XMLValidator
{

    private XMLValidatorErrorHandler errorHandler = null;
    
    private static final Logger LOG = Logger.getLogger(XMLValidator.class);

    /**
     * Validates an XML string according to an XML Schema at a given URL.
     * @param xml an XML string.
     * @param xmlSchemaUrl the URL of an XML Schema.
     */
    public void validate(String xml, String xmlSchemaUrl) {
        
    	errorHandler = new XMLValidatorErrorHandler();
        
    	try {
        	// `query.xsd` had to be edited to allow the `QueryType` to be exported. But this means
        	//  that now a `<query>` has to have a namespace associated. Here we do a simply
        	//  replacement to fake in the namespace of queries that are un-namespaced.
    		String[] parts = xmlSchemaUrl.split(Pattern.quote("/"));
        	if ("query.xsd".equals(parts[parts.length - 1])) {
        		xml = xml.replaceAll(Pattern.quote("<query"), "<xsq:query xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsq=\"http://intermine.org/query/1.0\" xsi:schemaLocation=\"http://intermine.org/query/1.0 query.xsd\"");
        		xml = xml.replaceAll(Pattern.quote("</query>"), "</xsq:query>");
        	}
        	
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = factory.newSchema(new StreamSource(new StringReader(xmlSchemaUrl)));

			Validator validator = schema.newValidator();
			validator.setErrorHandler(errorHandler);
			validator.validate(new StreamSource(new StringReader(xml)));
			
    	} catch (SAXParseException e) {
    		// Ignore.
		} catch (Exception e) {
			throw new InternalErrorException("XML validation failed.", e);
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
