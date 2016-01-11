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
import org.intermine.api.bag.operations.RelativeComplement;

/**
 * A service for subtracting one group of lists from another group of lists to produce a new
 * list.
 * @author Alexis Kalderimis
 *
 */
public class ListSubtractionService extends ListOperationService
{

    /**
     * Constructor
     * @param im A reference to the main settings bundle.
     */
    public ListSubtractionService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected ListInput getInput() {
        return new AsymmetricOperationInput(request, bagManager, getPermission().getProfile());
    }

    @Override
    protected RelativeComplement getOperation(ListInput input) {
        AsymmetricOperationInput aoi = (AsymmetricOperationInput) input;
        return new RelativeComplement(im.getModel(), getPermission().getProfile(),
                aoi.getReferenceLists(), aoi.getLists());
    }

}
