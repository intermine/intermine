package org.intermine.xml.full;

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
 * Representation of an Reference in an object
 *
 * @author Andrew Varley
 */

public class Reference
{
    private String name = "";
    private String refId = "";

    /**
     * Constructor
     */
    public Reference() {
    }

    /**
     * Construnctor
     * @param name the name
     * @param refId the refId
     */
    public Reference(String name, String refId) {
        this.name = name;
        this.refId = refId;
    }

    /**
     * Set the name of this field
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the name of this field
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the refId of this field
     *
     * @param refId the refId
     */
    public void setRefId(String refId) {
        this.refId = refId;
    }

    /**
     * Get the refId of this field
     *
     * @return the refId
     */
    public String getRefId() {
        return refId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Reference) {
            Reference r = (Reference) o;
            return name.equals(r.name) && refId.equals(r.refId);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return name.hashCode() + 3 * refId.hashCode();
    }
}
