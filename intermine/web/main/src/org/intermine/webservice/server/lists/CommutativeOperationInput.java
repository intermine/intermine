package org.intermine.webservice.server.lists;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.bag.BagManager;
import org.intermine.webservice.server.exceptions.BadRequestException;

public class CommutativeOperationInput extends ListInput {

    public CommutativeOperationInput(HttpServletRequest request, BagManager bagManager) {
        super(request, bagManager);
    }

    @Override
    protected void validateRequiredParams() {
        super.validateRequiredParams();
        if (getLists().isEmpty()) {
            throw new BadRequestException("Required parameter " + LISTS_PARAMETER + " is missing");
        }
    }
}
