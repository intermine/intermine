package org.flymine.objectstore.webservice.ser;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.flymine.objectstore.query.fql.FqlQuery;

/**
 * Bean that represents a proxy (reference or collection), used in serialization
*
 * @author Mark Woodbridge
 */
public class ProxyBean
{
    String type;
    FqlQuery fqlQuery;
    Integer id;

    /**
     * No-arg constructor (for deserialization)
     */
    public ProxyBean() {
    }

    /**
     * Constructor
     * @param type the type of the underlying object
     * @param fqlQuery the query to retrieve the object
     * @param id the internal id of the underlying object
     */
    public ProxyBean(String type, FqlQuery fqlQuery, Integer id) {
        this.type = type;
        this.fqlQuery = fqlQuery;
        this.id = id;
    }

    /**
     * Returns the type of the underlying object
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the underlying object.
     *
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the query to retrieve the object
     * @return the query
     */
    public FqlQuery getFqlQuery() {
        return fqlQuery;
    }

    /**
     * Sets the query to retrieve the object.
     *
     * @param fqlQuery the FqlQuery
     */
    public void setFqlQuery(FqlQuery fqlQuery) {
        this.fqlQuery = fqlQuery;
    }
    
    /**
     * Returns the internal id of the underlying object
     * @return the id
     */
    public Integer getId() {
        return id;
    }

    /**
     * Sets the internal id of the underlying object.
     *
     * @param id the id
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return "ProxyBean: type=" + type + " fqlQuery='" + fqlQuery + "' id=" + id;
    }

    /**
     * @see Object#equals
     */
    public boolean equals(Object o) {
        if (!(o instanceof ProxyBean)) {
            return false;
        }
        ProxyBean p = (ProxyBean) o;
        return p.type.equals(type)
            && p.fqlQuery.equals(fqlQuery)
            && p.id.equals(id);
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return 2 * type.hashCode()
            + 3 * fqlQuery.hashCode()
            + 5 * id.hashCode();
    }
}
