package org.intermine.webservice.server.lists;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.exceptions.BadRequestException;

public class CommutativeOperationInput extends ListInput {

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
