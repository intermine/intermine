package org.intermine.modelproduction;

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
 * An Exception that may be thrown by ModelMerger methods.
 *
 * @author Thomas Riley
 */
public class ModelMergerException extends Exception
{
    /**
	 * Serial ID demanded by His Highness Lord Eclipse
	 */
	private static final long serialVersionUID = 7068567716197037627L;

	/**
     * Constructs a ModelMergerException
     */
    public ModelMergerException() {
        super();
    }

    /**
     * Constructs a ModelMergerException with the specified detail message.
     *
     * @param msg the detail message
     */
    public ModelMergerException(String msg) {
        super(msg);
    }

    /**
     * Constructs a ModelMergerException with the specified nested throwable.
     *
     * @param t the nested throwable
     */
    public ModelMergerException(Throwable t) {
        super(t);
    }

    /**
     * Constructs a ModelMergerException with the specified detail message and nested throwable.
     *
     * @param msg the detail message
     * @param t the nested throwable
     */
    public ModelMergerException(String msg, Throwable t) {
        super(msg, t);
    }
}
