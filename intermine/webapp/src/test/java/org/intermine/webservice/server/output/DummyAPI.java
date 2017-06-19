package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;

import org.intermine.api.InterMineAPI;
import org.intermine.api.LinkRedirectManager;
import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManager;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplateSummariser;
import org.intermine.api.tracker.TrackerDelegate;
import org.intermine.api.types.ClassKeys;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;

/**
 * A dummy version of the API for testing against.
 * @author Alex Kalderimis
 *
 */
public class DummyAPI extends InterMineAPI {

    boolean returnALinkRedirector = true;
    /**
     * Empty constructor
     */
    public DummyAPI() {
        // empty constructor
    }

    /**
     * Optionally turn off the Link Redirector
     */
    public DummyAPI(boolean giveMeALinkRedirector) {
        this.returnALinkRedirector = giveMeALinkRedirector;
    }

    @Override
    public ObjectStore getObjectStore() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Model getModel() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProfileManager getProfileManager() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TemplateManager getTemplateManager() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BagManager getBagManager() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TagManager getTagManager() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TemplateSummariser getTemplateSummariser() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WebResultsExecutor getWebResultsExecutor(Profile profile) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PathQueryExecutor getPathQueryExecutor(Profile profile) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BagQueryRunner getBagQueryRunner() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ObjectStoreSummary getObjectStoreSummary() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ClassKeys getClassKeys() {
        return null;
    }

    @Override
    public BagQueryConfig getBagQueryConfig() {
        return null;
    }

    @Override
    public TrackerDelegate getTrackerDelegate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LinkRedirectManager getLinkRedirector() {
        if (returnALinkRedirector) {
            return new MockRedirector(null);
        } else {
            return null;
        }
    }

    private class MockRedirector extends LinkRedirectManager
    {
        public MockRedirector(Properties webProperties) {
            super(webProperties);
        }

        @Override
        public String generateLink(InterMineAPI im, InterMineObject imo) {
            return "Link for:" + imo.toString();
        }
    }
}
