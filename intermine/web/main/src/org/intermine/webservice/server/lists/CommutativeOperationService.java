package org.intermine.webservice.server.lists;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;

public abstract class CommutativeOperationService extends ListOperationService
{

    public CommutativeOperationService(InterMineAPI api) {
        super(api);
    }

    @Override
    protected ListInput getInput(HttpServletRequest request) {
        return new CommutativeOperationInput(request, bagManager);
    }

    @Override
    protected String getNewListType(ListInput input) {
        return ListServiceUtils.findCommonSuperTypeOf(getClassesForBags(input.getLists()));
    }

}
