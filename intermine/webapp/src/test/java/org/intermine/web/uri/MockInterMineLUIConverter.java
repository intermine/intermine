package org.intermine.web.uri;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;

public class MockInterMineLUIConverter extends InterMineLUIConverter {
    private ObjectStore os = null;
    private InterMineAPI im = null;

    public MockInterMineLUIConverter() {
        super();
    }

    @Override
    public Model getModel() {
        return Model.getInstanceByName("testmodel");
    }

    @Override
    public InterMineAPI getInterMineAPI() {
        return im;
    }

    /**
     * Set the os for testing
     * @param os the objectstore
     */
    public void setObjectStore(ObjectStore os) {
        this.os = os;
    }

    /**
     * Set the InterMineAPI for testing
     * @param im the interMineAPI
     */
    public void setInterMineAPI(InterMineAPI im) {
        this.im = im;
    }
}
