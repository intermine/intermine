package org.intermine.xml.full;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Representation of a field in an object.
 *
 * @author Andrew Varley
 */
public class ReferenceList
{
    protected String name;
    protected List<String> refIds = new ArrayList<String>();

    /**
     * Constructor
     */
    public ReferenceList() {
        // empty
    }

    /**
     * Constructor
     * @param name the name
     */
    public ReferenceList(String name) {
        this.name = name;
    }

    /**
     * Constructor
     * @param name the name
     * @param refIds the refIds
     */
    public ReferenceList(String name, List<String> refIds) {
        this(name);
        Iterator<String> refIdsIter = refIds.iterator();
        while (refIdsIter.hasNext()) {
            // we do this rather this calling this.refIds.addAll(refIds) so that the type of the
            // elements is checked immediately
            String thisId = refIdsIter.next();
            addRefId(thisId);
        }
    }

    /**
     * Set the name of this field.
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the name of this field.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Add a value to the list of references.
     *
     * @param refId the value
     */
    public void addRefId(String refId) {
        refIds.add(refId);
    }

    /**
     * Get the references in this collection.
     *
     * @return the list of references
     */
    public List<String> getRefIds() {
        return refIds;
    }

    /**
    * Set the references in this collection
    *
    * @param refIds the refIds
    */
    public void setRefIds(List<String> refIds) {
        this.refIds = refIds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof ReferenceList) {
            ReferenceList r = (ReferenceList) o;
            return name.equals(r.name)
                && refIds.equals(r.refIds);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return name.hashCode()
        + 3 * refIds.hashCode();
    }
}
