package org.intermine.metadata;

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
 * An Exception that may be thrown by ModelMerger methods.
 *
 * @author Alex Kalderimis, from Thomas Riley
 */
public class ModelParserException extends Exception
{
    /**
     * Serial ID demanded by Commissar Eclipse
     */
    private static final long serialVersionUID = 5556567365374538414L;

    /**
     * Constructs a ModelParserException
     */
    public ModelParserException() {
        super();
    }

    /**
     * Constructs a ModelParserException with the specified detail message.
     *
     * @param msg the detail message
     */
    public ModelParserException(String msg) {
        super(msg);
    }

    /**
     * Constructs a ModelParserException with the specified nested throwable.
     *
     * @param t the nested throwable
     */
    public ModelParserException(Throwable t) {
        super(t);
    }

    /**
     * Constructs a ModelParserException with the specified detail message and nested throwable.
     *
     * @param msg the detail message
     * @param t the nested throwable
     */
    public ModelParserException(String msg, Throwable t) {
        super(msg, t);
    }
}
