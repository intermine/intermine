/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.util;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;

/**
 * A wrapper for java.util.ArrayList that has getter and setter methods for the
 * the list contents so it appears bean-like.  Used for marshalling/unmarsahlling
 * data with Castor so it appears that a FlyMine xml simply contains a List of
 * business objects.  Castor can only deal with bean-like objects
 *
 * @author Richard Smith
 */



public class ListBean extends ArrayList
{

    /**
     * Set fictional items field to be a given List, will clear any existing elements
     *
     * @param c collection of objects to become list
     */
    public void setItems(Collection c) {
        this.clear();
        this.addAll(c);
    }

    /**
     * Retrieve items from list, simply returns this.
     *
     * @return this list
     */
    public List getItems() {
        return this;
    }

}
