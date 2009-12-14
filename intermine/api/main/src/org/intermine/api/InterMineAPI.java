package org.intermine.api;

/*
 * Copyright (C) 2002-2009 FlyMine
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
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplateSummariser;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;

public class InterMineAPI {

    private ObjectStore objectStore;
    private Model model;
    private Map<String, List<FieldDescriptor>> classKeys;
    private BagQueryConfig bagQueryConfig;
    private ProfileManager profileManager;
    private TemplateManager templateManager;
    private BagManager bagManager;
    private TemplateSummariser templateSummariser;
    
    protected InterMineAPI(ObjectStore objectStore, ObjectStoreWriter userProfileWriter,
            Map<String, List<FieldDescriptor>> classKeys, BagQueryConfig bagQueryConfig) {
        this.objectStore = objectStore;
        this.model = objectStore.getModel();
        this.classKeys = classKeys;
        this.bagQueryConfig = bagQueryConfig;
        this.profileManager = new ProfileManager(objectStore, userProfileWriter);
        this.bagManager = new BagManager(profileManager.getSuperuserProfile(), model);
        this.templateManager = new TemplateManager(profileManager.getSuperuserProfile(), model);
        this.templateSummariser = new TemplateSummariser(objectStore, 
                profileManager.getProfileObjectStoreWriter());
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
        return new WebResultsExecutor(objectStore, classKeys, bagQueryConfig, profile,
                templateManager.getConversionTemplates(), bagManager);
    }
    
    /**
     * @param profile the user that is executing the query
     * @return the pathQueryExecutor
     */
    public PathQueryExecutor getPathQueryExecutor(Profile profile) {
        return new PathQueryExecutor(objectStore, classKeys, bagQueryConfig, profile,
                templateManager.getConversionTemplates(), bagManager);
    }

    /**
     * @return the bagQueryRunner
     */
    public BagQueryRunner getBagQueryRunner() {
        return new BagQueryRunner(objectStore, classKeys, bagQueryConfig,
                templateManager.getConversionTemplates());
    }
    
    

}
