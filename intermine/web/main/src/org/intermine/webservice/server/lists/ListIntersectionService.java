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
import org.intermine.api.bag.operations.Intersection;

/**
 * A service that intersects one or more lists.
 * @author Alex Kalderimis
 *
 */
public class ListIntersectionService extends ListOperationService
{

    /** @param im The InterMine state object **/
    public ListIntersectionService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected Intersection getOperation(ListInput input) {
        return new Intersection(im.getModel(), getPermission().getProfile(), input.getLists());
    }

}
