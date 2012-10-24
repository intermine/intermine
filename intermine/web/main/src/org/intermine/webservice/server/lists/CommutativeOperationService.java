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

import org.intermine.api.InterMineAPI;

/**
 * A base class for services that perform commutative operations.
 * @author Alex Kalderimis.
 *
 */
public abstract class CommutativeOperationService extends ListOperationService
{

    /**
     * Constructor.
     * @param api The InterMine application object.
     */
    public CommutativeOperationService(InterMineAPI api) {
        super(api);
    }

    @Override
    protected ListInput getInput(HttpServletRequest request) {
        return new CommutativeOperationInput(request, bagManager, getPermission().getProfile());
    }

    @Override
    protected String getNewListType(ListInput input) {
        return ListServiceUtils.findCommonSuperTypeOf(getClassesForBags(input.getLists()));
    }

}
