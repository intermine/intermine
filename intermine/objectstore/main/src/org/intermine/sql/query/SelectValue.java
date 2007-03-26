package org.intermine.sql.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Represents an AbstractValue with an alias, suitable for use in the SELECT portion of an SQL
 * query.
 *
 * @author Matthew Wakeling
 */
public class SelectValue implements SQLStringable
{
    protected AbstractValue v;
    protected String alias;

    /**
     * Construct a new SelectValue.
     *
     * @param v the AbstractValue that this SelectValue holds
     * @param alias the alias that the value is renamed to
     */
    public SelectValue(AbstractValue v, String alias) {
        if (v == null) {
            throw (new NullPointerException("v cannot be null"));
        }
        if (alias == null) {
            if (v instanceof Field) {
                alias = ((Field) v).name;
            } else {
                throw (new NullPointerException("alias cannot be null unless v is a Field"));
            }
        }
        this.alias = alias;
        this.v = v;
    }

    /**
     * Returns a String representation of this Field object, suitable for forming part of an SQL
     * SELECT list.
     *
     * @return the String representation
     */
    public String getSQLString() {
        if ((v instanceof Field) && ((Field) v).name.equals(alias)) {
            return v.getSQLString();
        }
        return v.getSQLString() + " AS " + alias;
    }

    /**
     * Overrides Object.equals().
     *
     * @param obj an Object to compare to
     * @return true if the object is equal
     */
    public boolean equals(Object obj) {
        if (obj instanceof SelectValue) {
            SelectValue objSV = (SelectValue) obj;
            return v.equals(objSV.v) && alias.equals(objSV.alias);
        }
        return false;
    }

    /**
     * Overrides Object.hashCode().
     *
     * @return an arbitrary integer based on the contents of the SelectField
     */
    public int hashCode() {
        return (3 * v.hashCode()) + (5 * alias.hashCode());
    }

    /**
     * Gets the AbstractValue from this object.
     *
     * @return the AbstractValue of this object
     */
    public AbstractValue getValue() {
        return v;
    }

    /**
     * Gets the alias from this object.
     *
     * @return the alias of this object
     */
    public String getAlias() {
        return alias;
    }
}
