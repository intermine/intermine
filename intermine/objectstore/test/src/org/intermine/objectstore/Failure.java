package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.util.Util;

/**
 * Object to be put in results maps, as representing a failure, with a given exception class and
 * error message.
 *
 * @author Matthew Wakeling
 */
public class Failure
{
    private Class exceptionClass;
    private String message;

    public Failure(Class exceptionClass, String message) {
        this.exceptionClass = exceptionClass;
        this.message = message;
    }

    public Failure(Exception e) {
        this.exceptionClass = e.getClass();
        this.message = e.getMessage();
    }

    public boolean equals(Object o) {
        if (o instanceof Failure) {
            Failure f = (Failure) o;
            if (exceptionClass.equals(f.exceptionClass) && Util.equals(message, f.message)) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return exceptionClass.getName() + ": " + message;
    }
}
