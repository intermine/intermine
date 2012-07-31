package org.intermine.biomart.retrieve;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

public class BioMartField
{
    private String field;
    private String displayName;
    private String internalName;
    private String tableConstraint;
    private String key;

    public BioMartField() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * @param field the field to set
     */
    public void setField(String field) {
        this.field = field;
    }

    /**
     * @return the displayName
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @param displayName the displayName to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return the internalName
     */
    public String getInternalName() {
        return internalName;
    }

    /**
     * @param internalName the internalName to set
     */
    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }

    /**
     * @return the tableConstraint
     */
    public String getTableConstraint() {
        return tableConstraint;
    }

    /**
     * @param tableConstraint the tableConstraint to set
     */
    public void setTableConstraint(String tableConstraint) {
        this.tableConstraint = tableConstraint;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(String key) {
        this.key = key;
    }

}
