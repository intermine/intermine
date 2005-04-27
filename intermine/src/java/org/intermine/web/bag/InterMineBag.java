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
import java.util.LinkedHashSet;

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStore;

/**
 * A LinkedHashSet with a getSize() method.
 *
 * @author Kim Rutherford
 */

public abstract class InterMineBag extends LinkedHashSet
{
    protected static final Logger LOG = Logger.getLogger(InterMineBag.class);
    
    /** 
     * Constructs a new, empty InterMineBag.
     */
    public InterMineBag() {
        super();
    }

    /** 
     * Constructs a new InterMineBag with the same contents as the argument.
     * @param c the new bag contents
     */
    public InterMineBag(Collection c) {
        super(c);
    }

    /**
     * @see LinkedHashSet#size
     */
    public int getSize() {
        return size();
    }
    
    /**
     * Return a collection of actual objects represented by this bag rather than any
     * intermediate form (such as intermine object id numbers).
     * @param os object store to load objects from
     * @return collection of objects
     */
    public abstract Collection toObjectCollection(ObjectStore os);
}
