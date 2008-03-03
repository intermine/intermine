package org.intermine.webservice.lists;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.webservice.WebServiceInput;

/**
 * ListsServiceInput is parameter object representing parameters for ListService web service.   
 * @author Jakub Kulaviak
 **/
public class ListsServiceInput extends WebServiceInput
{

    private Integer objectId;
    
    /** Returns id of object for which the public lists that contain this object 
     * are found. 
     * @return id of object
     * **/
    public Integer getObjectId() {
        return objectId;
    }

    /** Sets id of object.
     * @param objectId object id
     * @see #getObjectId()
     * **/
    public void setObjectId(Integer objectId) {
        this.objectId = objectId;
    }
}
