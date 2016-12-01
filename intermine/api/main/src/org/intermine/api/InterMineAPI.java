package org.intermine.api;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.IdentityHashMap;
import java.util.Map;

import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.mines.FriendlyMineManager;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManager;
import org.intermine.api.query.MemoryQueryStore;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.query.QueryStore;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplateSummariser;
import org.intermine.api.tracker.TrackerDelegate;
import org.intermine.api.types.ClassKeys;
import org.intermine.api.util.AnonProfile;
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
    protected ClassKeys classKeys;
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
    protected QueryStore queryStore;

    // query executors are cached per profile
    private final Map<Profile, WebResultsExecutor> wreCache =
        new IdentityHashMap<Profile, WebResultsExecutor>();
    private final Map<Profile, PathQueryExecutor> pqeCache =
        new IdentityHashMap<Profile, PathQueryExecutor>();
    private ObjectStoreWriter userProfile;

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
            ClassKeys classKeys, BagQueryConfig bagQueryConfig,
            ObjectStoreSummary oss, TrackerDelegate trackerDelegate, LinkRedirectManager
            linkRedirector) {
        this.objectStore = objectStore;
        this.userProfile = userProfileWriter;
        this.model = objectStore.getModel();
        this.classKeys = classKeys;
        this.bagQueryConfig = bagQueryConfig;
        this.oss = oss;
        this.trackerDelegate = trackerDelegate;
        this.linkRedirector = linkRedirector;
        this.queryStore = new MemoryQueryStore(1024);
        initUserProfileResources(userProfileWriter);
    }

    /**
     * Initialise parts of this object that require connection to a user-profile
     * object store.
     * @param userProfileWriter The object store for the users and their stuff.
     */
    protected void initUserProfileResources(ObjectStoreWriter userProfileWriter) {
        this.profileManager = new ProfileManager(objectStore, userProfileWriter);
        Profile superUser = profileManager.getSuperuserProfile(classKeys);
        this.bagManager = new BagManager(superUser, model);
        this.templateManager =
                new TemplateManager(superUser, model, trackerDelegate.getTemplateTracker());
        this.templateSummariser =
                new TemplateSummariser(objectStore, userProfileWriter, oss);
        this.bagQueryRunner =
                new BagQueryRunner(objectStore, classKeys, bagQueryConfig, templateManager);
    }

    /**
     * @return the objectStore
     */
    public ObjectStore getObjectStore() {
        return objectStore;
    }

    /**
     * @return The ObjectStore that represents a connection to the userprofile store.
     */
    public ObjectStoreWriter getUserProfile() {
        return userProfile;
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
     * @return the pathQueryExecutor
     */
    public PathQueryExecutor getPathQueryExecutor() {
        return getPathQueryExecutor(new AnonProfile());
    }

    /**
     * @param profile the user that is executing the query
     * @return the pathQueryExecutor
     */
    public PathQueryExecutor getPathQueryExecutor(Profile profile) {
        synchronized (pqeCache) {
            PathQueryExecutor retval = pqeCache.get(profile);
            if (retval == null) {
                retval = new PathQueryExecutor(objectStore, profile,
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
    public ClassKeys getClassKeys() {
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
     * The Link-Redirector generates links to objects within the database. If an external redirect
     * has been configured, then a non-null string will be generated.
     */
    public LinkRedirectManager getLinkRedirector() {
        return linkRedirector;
    }

    /**
     * @return The query store, which associates queries to ids.
     */
    public QueryStore getQueryStore() {
        return queryStore;
    }
}
