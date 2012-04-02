package org.intermine.webservice.server.lists;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.exceptions.BadRequestException;

public class AsymmetricOperationInput extends ListInput {

    /**
     * Constructor.
     * @param request The web service request.
     * @param bagManager A bag manager.
     * @param profile The current user's profile.
     */
    public AsymmetricOperationInput(HttpServletRequest request,
            BagManager bagManager, Profile profile) {
        super(request, bagManager, profile);
    }

    @Override
    public Set<InterMineBag> getLists() {
        if (!getSubtractLists().isEmpty()) {
            return getSubtractLists();
        } else {
            return super.getLists();
        }
    }

    @Override
    protected void validateRequiredParams() {
        super.validateRequiredParams();
        List<String> errors = new ArrayList<String>();
        if (getLists().isEmpty()) {
            errors.add("Both '" + LISTS_PARAMETER + "' and '" + SUBTRACT_PARAM
                    + "' are missing, at least one is required");
        }
        if (!getSubtractLists().isEmpty() && !super.getLists().isEmpty()) {
            errors.add("Values have been supplied for both '" +
                LISTS_PARAMETER + "' and '" + SUBTRACT_PARAM + "'. At most" +
                " one is permitted");
        }
        if (getReferenceLists().isEmpty()) {
            errors.add("Required parameter '" + REFERENCE_PARAMETER + "' is missing");
        }
        if (!errors.isEmpty()) {
            String message = StringUtils.join(errors, ", ");
            throw new BadRequestException(message + ". PARAMS:" + request.getQueryString());
        }
    }
}
