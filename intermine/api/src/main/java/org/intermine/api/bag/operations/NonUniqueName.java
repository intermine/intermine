package org.intermine.api.bag.operations;

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
 * Exception for non-unique name
 *
 * @author julie
 */
public class NonUniqueName extends BagOperationException
{

    private NonUniqueName() {
        // don't
    }

    /**
     * @param name the list name
     */
    public NonUniqueName(String name) {
        super("A bag called " + name + " already exists");
    }
}
