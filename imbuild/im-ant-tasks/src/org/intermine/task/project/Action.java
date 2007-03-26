package org.intermine.task.project;

import java.util.ArrayList;
import java.util.List;

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
 * Base class for actions in a project.xml files
 * @author Kim Rutherford
 */
public abstract class Action
{
    private List properties = new ArrayList();

    /**
     * Add a UserProperty to 
     * @param userProperty
     */
    public void addUserProperty(UserProperty userProperty) {
        properties.add(userProperty);
    }

    /**
     * Return a list of UserProperty objects.
     * @return the UserPropertys
     */
    public List getUserProperties() {
        return properties;
    }
}
