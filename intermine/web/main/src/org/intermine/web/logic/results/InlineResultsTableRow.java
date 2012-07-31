package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

/**
 * Provides a wrapper for a row of InlineTableResultElement
 *
 * @author radek
 */
public class InlineResultsTableRow
{

    @SuppressWarnings("unchecked")
    List columnList = new ArrayList<Object>();
    String className = null;
    Integer imObjId;

    /**
     * Add an InlineTableResultElement or an empty String
     * @param re InlineTableResultElement or String
     */
    @SuppressWarnings("unchecked")
    public void add(Object re) {
        columnList.add(re);
    }

    /**
     *
     * @return list of objects
     */
    @SuppressWarnings("unchecked")
    public List<Object> getItems() {
        return columnList;
    }

    /**
     *
     * @param className String resolved on imObj
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     *
     * @return className of the overlaying imObj to JSP
     */
    public String getClassName() {
        return className;
    }

    /**
     * Set imObj ID for the object (taken from the first valid imObj id for this row)
     * @param id imObj id
     */
    public void setObjectId(Integer id) {
        this.imObjId  = id;
    }

    /**
     * Used from JSP
     *
     * @see getResultElementRows() in InlineResultsTable as to how this is set
     * @return InterMine Object id
     */
    public Object getObjectId() {
        return (imObjId != null) ? imObjId : "Not implemented";
    }

}
