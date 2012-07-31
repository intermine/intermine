package org.intermine.web.logic.config;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.model.InterMineObject;

/**
 * An individual InterMineObject wrapped in an InlineList Set
 * @author radek
 *
 */
public class InlineListObject
{

    private InterMineObject object = null;
    private Object value = null;
    private Object id = null;

    /**
     * Initialize
     * @param object InterMine object received from ReportObject
     * @param value represents the values of a column specified in webconfig-model.xml
     * @param id represents an object ID so we can link to its report page
     */
    public InlineListObject(InterMineObject object, Object value, Object id) {
        this.object = object;
        this.value = value;
        this.id = id;
    }

    /**
     * Fetch the InterMine object (if needed)
     * @return InterMine object
     */
    public Object getObject() {
        return object;
    }

    /**
     * Fetch the column value, ie "Zerknullt" for Synonyms of a Gene
     * @return value
     */
    public Object getValue() {
        return value;
    }

    /**
     *
     * @return InterMine id
     */
    public Object getId() {
        return id;
    }

}
