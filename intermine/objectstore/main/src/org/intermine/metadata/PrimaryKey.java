package org.intermine.metadata;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * Class represing a primary key as a list of field names
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class PrimaryKey
{
    String name;
    Set<String> fieldNames = new LinkedHashSet<String>();

    /**
     * Constructor
     * @param name the name to use for the primary key
     * @param fields a comma-delimited list of field names
     */
    public PrimaryKey(String name, String fields) {
        this.name = name;
        if (fields == null) {
            throw new NullPointerException("fields parameter cannot be null");
        }
        String[] tokens = fields.split(",");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i].trim();
            fieldNames.add(token);
        }
    }


    /**
     * Return the name
     *
     * @return name of this primary key
     */
    public String getName() {
        return name;
    }


    /**
     * Return the Set of field names
     *
     * @return the Set of field names
     */
    public Set<String> getFieldNames() {
        return fieldNames;
    }

    /**
     * @see Object#equals(Object)
     */
    public boolean equals(Object o) {
        if (o instanceof PrimaryKey) {
            return fieldNames.equals(((PrimaryKey) o).fieldNames);
        }
        return false;
    }

    /**
     * @see Object#hashCode()
     */
    public int hashCode() {
        return fieldNames.hashCode();
    }

    /**
     * @see Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<fields=\"");
        for (Iterator iter = getFieldNames().iterator(); iter.hasNext();) {
            sb.append(iter.next());
            if (iter.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("\">");

        return sb.toString();
    }
}
