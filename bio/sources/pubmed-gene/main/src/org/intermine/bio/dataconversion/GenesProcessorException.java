package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Exception that may be thrown from GeneConvertor.
 * @author Jakub Kulaviak
 **/
public class GenesProcessorException extends RuntimeException
{

    private static final long serialVersionUID = 1L;

    /**
     * @param msg message
     */
    public GenesProcessorException(String msg) {
        super(msg);
    }

    /**
     * @param cause cause
     */
    public GenesProcessorException(Throwable cause) {
        super(cause);
    }
}
