package org.intermine.modelproduction;

/*
 * Copyright (C) 2002-2007 FlyMine
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
 * @author Thomas Riley
 */
public class ModelMergerException extends Exception
{
    /**
     * Constructs an ModelMergerException
     */
    public ModelMergerException() {
        super();
    }

    /**
     * Constructs an ModelMergerException with the specified detail message.
     *
     * @param msg the detail message
     */
    public ModelMergerException(String msg) {
        super(msg);
    }

    /**
     * Constructs an ModelMergerException with the specified nested throwable.
     *
     * @param t the nested throwable
     */
    public ModelMergerException(Throwable t) {
        super(t);
    }

    /**
     * Constructs an ModelMergerException with the specified detail message and nested throwable.
     *
     * @param msg the detail message
     * @param t the nested throwable
     */
    public ModelMergerException(String msg, Throwable t) {
        super(msg, t);
    }
}
