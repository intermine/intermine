package org.intermine.webservice;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;


/**
 * Base class for input classes used with web services.
 * @author Jakub Kulaviak
 **/
public class WebServiceInput
{

    private List<String> errors = new ArrayList<String>();

    /**
     * Returns errors messages
     * @return errors
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Set error messages.
     * @param errors errors
     */
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
    
    /**
     * Finds out if input is valid.
     * @return true if input is valid
     */
    public boolean isValid() {
        return errors.size() == 0;
    }
    
    /**
     * Add error message
     * @param error error
     */
    public void addError(String error) {
        errors.add(error);
    }
    
}
