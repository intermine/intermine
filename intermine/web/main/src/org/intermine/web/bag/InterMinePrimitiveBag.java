package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;

import org.intermine.objectstore.ObjectStore;

/**
 * Bag of primitive wrapper objects.
 * 
 * @author tom
 */
public class InterMinePrimitiveBag extends InterMineBag
{
    /**
     * Construct an instance of InterMinePrimitiveBag.
     * @param bag bag to copy
     */
    public InterMinePrimitiveBag(InterMinePrimitiveBag bag) {
        super(bag);
    }
    
    /**
     * Construct an instance of InterMinePrimitiveBag.
     */
    public InterMinePrimitiveBag() {
        super();
    }

    /**
     * @see InterMineBag#toObjectCollection
     */
    public Collection toObjectCollection(ObjectStore os) {
        return this;
    }
}
