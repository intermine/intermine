package org.intermine.webservice.output;

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
 * Abstract class representing output of web service or something else.
 * Data written to output can be streamed to user via http or saved in memory or something else.
 * It depends at implementation.
 * If the data are saved in memory they can be retrieved later. 
 * @author jakub
 * 
 */
public abstract class Output  
{
    /**
     * Adds data to output.
     * @param item data
     */
    public abstract void addResultItem(List<String> item);

    /**
     * Adds error messages to output.  
     * @param errors error messages
     */
    public abstract void addErrors(List<String> errors);

    /**
     * Adds error message to output.
     * @param error error message
     */
    public void addError(String error) {
        List<String> errors = new ArrayList<String>();
        errors.add(error);
        addErrors(errors);
    }

    /**
     * Flushes output. What it actually does depends at implementation. 
     */
    public abstract void flush();
    
}
