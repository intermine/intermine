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

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 * Class representing the input to a list creation request.
 * @author ajk59
 *
 */
public class ListCreationInput extends ListInput
{

    /**
     * Constructor.
     * @param request The request we are responding to.
     * @param bagManager The manager for requesting bags from.
     */
    public ListCreationInput(HttpServletRequest request, BagManager bagManager, Profile profile) {
        super(request, bagManager, profile);
    }

    @Override
    protected void validateRequiredParams() {
        if (getType() == null) {
            throw new BadRequestException("Required parameter '" + TYPE_PARAMETER
                + "' is missing");
        }
        super.validateRequiredParams();
    }

}
