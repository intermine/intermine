package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/** @author Alex Kalderimis **/
public class TriageBagForm extends ModifyBagForm
{
    protected String newBagType;

    /**
     * Set the new bag name.
     * @param name the new bag name
     */
    public void setNewBagType(String name) {
        newBagType = name;
    }

    /**
     * Get the new bag name.
     * @return the new bag name
     */
    public String getNewBagType() {
        return newBagType;
    }

    @Override
    public void initialise() {
        super.initialise();
        newBagType = "";
    }

}
