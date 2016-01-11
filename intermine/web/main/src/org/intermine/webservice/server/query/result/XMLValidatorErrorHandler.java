package org.intermine.webservice.server.query.result;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * @author Jakub Kulaviak
 **/
public class XMLValidatorErrorHandler implements ErrorHandler
{
    private List<String> errors = new ArrayList<String>();
    private List<String> warnings = new ArrayList<String>();

    /**
     * Returns errors occurred during parsing xml.
     * @return errors
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Returns  warning occurred during  parsing xml.
     * @return warnings
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * {@inheritDoc}}
     */
    public void error(SAXParseException exception) {
        errors.add(exception.getMessage());
    }

    /**
     * {@inheritDoc}}
     */
    public void fatalError(SAXParseException exception) {
        errors.add(exception.getMessage());
    }

    /**
     * {@inheritDoc}}
     */
    public void warning(SAXParseException exception) {
        warnings.add(exception.getMessage());
    }
}
