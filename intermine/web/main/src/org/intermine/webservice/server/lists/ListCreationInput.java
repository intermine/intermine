package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.Profile;

/**
 * Class representing the input to a list creation request.
 * @author ajk59
 *
 */
public class ListCreationInput extends ListInput
{

    private ArrayList<String> addIssues;

    /**
     * Constructor.
     * @param request The request we are responding to.
     * @param bagManager The manager for requesting bags from.
     * @param profile The profile of the the current user.
     */
    public ListCreationInput(HttpServletRequest request, BagManager bagManager, Profile profile) {
        super(request, bagManager, profile);
        this.addIssues = new ArrayList<String>();
        this.populateNormedList(this.addIssues, "add");
    }

    /** @return the set of issue types the user wants to add. **/
    public Collection<String> getAddIssues() {
        // Return a subclass of set that does case insensitive matching.
        return new HashSet<String>(this.addIssues) {
            private static final long serialVersionUID = -3419390541449198412L;

            public boolean contains(Object o) {
                if (o instanceof String) {
                    if (super.contains(":all")) {
                        return true;
                    }
                    return super.contains(String.valueOf(o).toLowerCase());
                }
                return super.contains(o);
            }
        };
    }

}
