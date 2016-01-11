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

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.operations.Union;

/**
 * A class for exposing creating unions of lists as a resource.
 * @author Alex Kalderimis.
 */
public class ListUnionService extends ListOperationService
{
    /**
     * Constructor.
     * @param im The InterMine magic sauce.
     */
    public ListUnionService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected Union getOperation(ListInput input) {
        return new Union(im.getModel(), getPermission().getProfile(), input.getLists());
    }
}
