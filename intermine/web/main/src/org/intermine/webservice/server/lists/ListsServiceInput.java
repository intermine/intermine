package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.webservice.server.WebServiceInput;

/**
 * ListsServiceInput is parameter object representing parameters for ListService web service.
 * @author Jakub Kulaviak
 **/
public class ListsServiceInput extends WebServiceInput
{

    private String publicId;
    private Integer mineId;
    private String type;
    private String extraValue;

    /**
     * @return object type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets object type.
     * @param type object type
     */
    public void setType(String type) {
        this.type = type;
    }

    /** Returns id of object. It can be for example primaryIdentifier,
     * secondaryIdentifier or some other identifier.
     * @return id of object
     */
    public String getPublicId() {
        return publicId;
    }

    /** Sets id of object.
     * @param publicId object public id
     * @see #getPublicId()
     */
    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    /**
     * @return Intermine unique object id
     */
    public Integer getMineId() {
        return mineId;
    }

    /**
     * Sets Intermine unique object id
     * @param mineId object id
     */
    public void setMineId(Integer mineId) {
        this.mineId = mineId;
    }

    /**
     * An extra value, such as an organism, for use in lookup constraints.
     * @param extra The extra value.
     */
    public void setExtraValue(String extra) {
        this.extraValue = extra;
    }

    /**
     * @return The extra value, such as an organism name.
     */
    public String getExtraValue() {
        return this.extraValue;
    }
}

