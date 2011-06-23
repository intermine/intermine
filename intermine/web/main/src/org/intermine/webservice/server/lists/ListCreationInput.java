package org.intermine.webservice.server.lists;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.bag.BagManager;
import org.intermine.webservice.server.exceptions.BadRequestException;

public class ListCreationInput extends ListInput {

    public ListCreationInput(HttpServletRequest request, BagManager bagManager) {
        super(request, bagManager);
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
