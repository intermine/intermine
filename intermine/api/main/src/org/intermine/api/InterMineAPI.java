package org.intermine.api;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.mines.FriendlyMineManager;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManager;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplateSummariser;
import org.intermine.api.tracker.TrackerDelegate;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.ObjectStoreWriter;

/**
 * InterMineAPI provides access to manager objects for the main parts of an InterMine application:
 * the production ObjectStore, the userprofile (via ProfileManager), template queries, bags and
 * tags.  There should be one InterMineAPI object per application.
 *
 * @author Richard Smith
 */
public class InterMineAPI
{
    protected ObjectStore objectStore;
    protected Model model;
    protected Map<String, List<FieldDescriptor>> classKeys;
    protected BagQueryConfig bagQueryConfig;
    protected ProfileManager profileManager;
    protected TemplateManager templateManager;
    protected BagManager bagManager;
    protected TemplateSummariser templateSummariser;
    protected ObjectStoreSummary oss;
    protected BagQueryRunner bagQueryRunner;
    protected TrackerDelegate trackerDelegate;
    protected LinkRedirectManager linkRedirector;
    protected FriendlyMineManager friendlyMineManager;

    // query executors are cached per profile
    private final Map<Profile, WebResultsExecutor> wreCache =
        new IdentityHashMap<Profile, WebResultsExecutor>();
    private final Map<Profile, PathQueryExecutor> pqeCache =
        new IdentityHashMap<Profile, PathQueryExecutor>();

    /**
     * Protected no-argument constructor only used for building test implementations of this class.
     */
    protected InterMineAPI() {
        // don't instantiate
    }

    /**
     * Construct an InterMine API object.
     *
     * @param objectStore the production database
     * @param userProfileWriter a writer for the userprofile database
     * @param classKeys the class keys
     * @param bagQueryConfig configured bag queries used by BagQueryRunner
     * @param oss summary information for the ObjectStore
     * @param trackerDelegate the trackers delegate
     * @param linkRedirector class that builds URLs that replace report links
     */
    public InterMineAPI(ObjectStore objectStore, ObjectStoreWriter userProfileWriter,
            Map<String, List<FieldDescriptor>> classKeys, BagQueryConfig bagQueryConfig,
            ObjectStoreSummary oss, TrackerDelegate trackerDelegate, LinkRedirectManager
            linkRedirector) {
        this.objectStore = objectStore;
        this.model = objectStore.getModel();
        this.classKeys = classKeys;
        this.bagQueryConfig = bagQueryConfig;
        this.oss = oss;
        this.profileManager = new ProfileManager(objectStore, userProfileWriter);
        Profile superUser = profileManager.getSuperuserProfile(classKeys);
        this.bagManager = new BagManager(superUser, model);
        this.templateManager = new TemplateManager(superUser, model,
                trackerDelegate.getTemplateTracker());
        this.templateSummariser = new TemplateSummariser(objectStore,
                profileManager.getProfileObjectStoreWriter(), oss);
        this.bagQueryRunner =
            new BagQueryRunner(objectStore, classKeys, bagQueryConfig, templateManager);
        this.trackerDelegate = trackerDelegate;
        this.linkRedirector = linkRedirector;
    }

    /**
     * @return the objectStore
     */
    public ObjectStore getObjectStore() {
        return objectStore;
    }

    /**
     * @return the model
     */
    public Model getModel() {
        return model;
    }
    /**
     * @return the profileManager
     */
    public ProfileManager getProfileManager() {
        return profileManager;
    }
    /**
     * @return the templateManager
     */
    public TemplateManager getTemplateManager() {
        return templateManager;
    }
    /**
     * @return the bagManager
     */
    public BagManager getBagManager() {
        return bagManager;
    }

    /**
     * @return the TagManager
     */
    public TagManager getTagManager() {
        return profileManager.getTagManager();
    }

    /**
     * @return the templateSummariser
     */
    public TemplateSummariser getTemplateSummariser() {
        return templateSummariser;
    }

    /**
     * @param profile the user that is executing the query
     * @return the webResultsExecutor
     */
    public WebResultsExecutor getWebResultsExecutor(Profile profile) {
        synchronized (wreCache) {
            WebResultsExecutor retval = wreCache.get(profile);
            if (retval == null) {
                retval = new WebResultsExecutor(this, profile);
                wreCache.put(profile, retval);
            }
            return retval;
        }
    }

    /**
     * @param profile the user that is executing the query
     * @return the pathQueryExecutor
     */
    public PathQueryExecutor getPathQueryExecutor(Profile profile) {
        synchronized (pqeCache) {
            PathQueryExecutor retval = pqeCache.get(profile);
            if (retval == null) {
                retval = new PathQueryExecutor(objectStore, classKeys, profile,
                        bagQueryRunner, bagManager);
                pqeCache.put(profile, retval);
            }
            return retval;
        }
    }

    /**
     * The bag-query runner is the object that performs look-up queries used when
     * constructing bags from lists of identifiers, and in path-queries when
     * LOOKUP constraints are used.
     * @return the bagQueryRunner
     */
    public BagQueryRunner getBagQueryRunner() {
        return bagQueryRunner;
    }

    /**
     * @return the oss
     */
    public ObjectStoreSummary getObjectStoreSummary() {
        return oss;
    }

    /**
     * The class keys are the list of fields used to identify objects in the data
     * base, so for Employee it would include "name", and for Gene it would include
     * "symbol".
     * @return the classKeys
     */
    public Map<String, List<FieldDescriptor>> getClassKeys() {
        return classKeys;
    }

    /**
     * @return the bagQueryConfig
     */
    public BagQueryConfig getBagQueryConfig() {
        return bagQueryConfig;
    }

    /**
     * @return the trackers delegate
     */
    public TrackerDelegate getTrackerDelegate() {
        return trackerDelegate;
    }

    /**
     * @return the linkRedirector
     */
    public LinkRedirectManager getLinkRedirector() {
        return linkRedirector;
    }
}
