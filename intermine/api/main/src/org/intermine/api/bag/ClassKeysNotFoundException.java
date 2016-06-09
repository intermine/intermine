package org.intermine.api.bag;

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
 * Exception thrown when given attempting to create a bag for a type that isn't in the model, this
 * can happen when reading bags from a userprofile database and the data model has changed.
 * types
 * @author Richard Smith
 *
 */
public class ClassKeysNotFoundException extends Exception
{
    private static final long serialVersionUID = 7566185607950016514L;

    /**
     * Constructs an IncompatibleBagTypesException with the specified detail message.
     *
     * @param msg the detail message
     */
    public ClassKeysNotFoundException(String msg) {
        super(msg);
    }

    /**
     * Constructs an IncompatibleBagTypesException with the specified nested throwable.
     *
     * @param t the nested throwable
     */
    public ClassKeysNotFoundException(Throwable t) {
        super(t);
    }

    /**
     * Constructs an IncompatibleBagTypesException with the specified detail message and nested
     * throwable.
     *
     * @param msg the detail message
     * @param t the nested throwable
     */
    public ClassKeysNotFoundException(String msg, Throwable t) {
        super(msg, t);
    }
}

