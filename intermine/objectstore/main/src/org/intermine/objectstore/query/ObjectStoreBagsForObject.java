package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import org.intermine.util.Util;

/**
 * Object representing a query to fetch ObjectStoreBag IDs for ObjectStoreBags that contain a
 * certain value.
 *
 * @author Matthew Wakeling
 */
public class ObjectStoreBagsForObject implements QuerySelectable
{
    private final Integer value;
    private final Collection<ObjectStoreBag> bags;

    /**
     * Constructs a new ObjectStoreBagsForObject with no limit on the ObjectStoreBags returned.
     *
     * @param value the value to look for in the bags
     */
    public ObjectStoreBagsForObject(Integer value) {
        this.value = value;
        this.bags = null;
    }

    /**
     * Constructs a new ObjectStoreBagsForObject with a limited set of ObjectStoreBags that can be
     * present in the results.
     *
     * @param value the value to look for in the bags
     * @param bags a Collection of ObjectStoreBag objects which may be present in the results
     */
    public ObjectStoreBagsForObject(Integer value, Collection<ObjectStoreBag> bags) {
        this.value = value;
        this.bags = bags;
    }

    /**
     * Returns the value.
     *
     * @return an Integer
     */
    public Integer getValue() {
        return value;
    }

    /**
     * Returns the Collection of bags.
     *
     * @return a Collection of ObjectStoreBags
     */
    public Collection<ObjectStoreBag> getBags() {
        return bags;
    }

    /**
     * {@inheritDoc}
     */
    public Class getType() {
        return Integer.class;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (o instanceof ObjectStoreBagsForObject) {
            return Util.equals(bags, ((ObjectStoreBagsForObject) o).bags)
                && Util.equals(value, ((ObjectStoreBagsForObject) o).value);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return (bags == null ? 0 : bags.hashCode()) + (value == null ? 0 : value.intValue());
    }
}

