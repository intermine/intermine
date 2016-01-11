package org.intermine.template;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Exception thrown when errors occur populating a template query with values.
 * @author Richard Smith
 *
 */
public class TemplatePopulatorException extends RuntimeException
{

    private static final long serialVersionUID = -2453270391799250660L;

    /**
     * Constructs an TemplatePopulatorException with the specified detail message.
     *
     * @param msg the detail message
     */
    public TemplatePopulatorException(String msg) {
        super(msg);
    }

    /**
     * Constructs an TemplatePopulatorException with the specified nested throwable.
     *
     * @param t the nested throwable
     */
    public TemplatePopulatorException(Throwable t) {
        super(t);
    }

    /**
     * Constructs an TemplatePopulatorException with the specified detail message and
     * nested throwable.
     *
     * @param msg the detail message
     * @param t the nested throwable
     */
    public TemplatePopulatorException(String msg, Throwable t) {
        super(msg, t);
    }
}

