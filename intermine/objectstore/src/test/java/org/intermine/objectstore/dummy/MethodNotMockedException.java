package org.intermine.objectstore.dummy;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * @author Alexis Kalderimis
 *
 */

public class MethodNotMockedException extends UnsupportedOperationException {

	/**
	 * Default serial id
	 */
	private static final long serialVersionUID = 1L;

	public MethodNotMockedException() {
		// empty constructor
	}

	public MethodNotMockedException(String message) {
		super(message);
	}

	public MethodNotMockedException(String message, Throwable cause) {
		super(message, cause);
	}

	public MethodNotMockedException(Throwable cause) {
		super(cause);
	}
}
