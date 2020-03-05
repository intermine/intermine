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

import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;

public class MockInterMineLUIConverter extends InterMineLUIConverter {
    private ObjectStore os = null;
    private Profile suProfile = null;

    @Override
    public ObjectStore getObjectStore() {
        return os;
    }

    @Override
    public Model getModel() {
        return Model.getInstanceByName("testmodel");
    }

    /**
     * Set the object store for testing
     * @param os th eobject store
     */
    public void setObjectStore(ObjectStore os) {
        this.os = os;
    }

    /**
     * Set the user profile
     * @param suProfile
     */
    public void setSUProfile(Profile suProfile) {
        this.suProfile = suProfile;
    }

    @Override
    public PathQueryExecutor getPathQueryExecutor() {
        return new PathQueryExecutor(os, suProfile,null,
                new BagManager(suProfile, Model.getInstanceByName("testmodel")));
    }

}
