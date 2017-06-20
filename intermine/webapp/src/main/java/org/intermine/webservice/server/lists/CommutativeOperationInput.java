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

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 * An input class for operations that are commutative.
 * @author Alex Kalderimis
 *
 */
public class CommutativeOperationInput extends ListInput
{

    /**
     * Constructor.
     * @param request The web service request.
     * @param bagManager A bag manager.
     * @param profile The current user's profile.
     */
    public CommutativeOperationInput(HttpServletRequest request,
            BagManager bagManager, Profile profile) {
        super(request, bagManager, profile);
    }

    @Override
    protected void validateRequiredParams() {
        super.validateRequiredParams();
        if (getLists().isEmpty()) {
            throw new BadRequestException("Required parameter " + LISTS_PARAMETER + " is missing");
        }
    }
}
