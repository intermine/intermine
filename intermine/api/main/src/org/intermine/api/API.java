package org.intermine.api;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;

import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManager;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.template.TemplateSummariser;
import org.intermine.api.tracker.TrackerDelegate;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.api.template.TemplateManager;

/**
 * The interface of the API, abstracted for testing purposes.
 * @author Alex Kalderimis
 *
 */
public interface API
{

    /**
     * @return the objectStore
     */
    ObjectStore getObjectStore();

    /**
     * @return the model
     */
    Model getModel();

    /**
     * @return the profileManager
     */
    ProfileManager getProfileManager();
    /**
     * @return the templateManager
     */
    TemplateManager getTemplateManager();

    /**
     * @return the bagManager
     */
    BagManager getBagManager();

    /**
     * @return the TagManager
     */
    TagManager getTagManager();

    /**
     * @return the templateSummariser
     */
    TemplateSummariser getTemplateSummariser();

    /**
     * @param profile the user that is executing the query
     * @return the webResultsExecutor
     */
    WebResultsExecutor getWebResultsExecutor(Profile profile);

    /**
     * @param profile the user that is executing the query
     * @return the pathQueryExecutor
     */
    PathQueryExecutor getPathQueryExecutor(Profile profile);

    /**
     * @return the bagQueryRunner
     */
    BagQueryRunner getBagQueryRunner();

    /**
     * @return the oss
     */
    ObjectStoreSummary getObjectStoreSummary();

    /**
     * @return the classKeys
     */
    Map<String, List<FieldDescriptor>> getClassKeys();

    /**
     * @return the bagQueryConfig
     */
    BagQueryConfig getBagQueryConfig();

    /**
     * @return the trackers delegate
     */
    TrackerDelegate getTrackerDelegate();

    /**
     * @return the linkRedirector
     */
    LinkRedirectManager getLinkRedirector();

}
