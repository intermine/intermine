package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.LinkedHashSet;

/**
 * A LinkedHashSet with a getSize() method.
 *
 * @author Kim Rutherford
 */

public class InterMineBag extends LinkedHashSet
{
    /** 
     * Constructs a new, empty InterMineBag.
     */
    public InterMineBag() {
        super();
    }

    /**
     * @see LinkedHashSet#size
     */
    public int getSize() {
        return size();
    }
}
