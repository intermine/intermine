package org.intermine.web.struts;
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
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.template.TemplateSummariser;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.api.template.TemplateManager;

/**
 * InterMineAPITest is a class created only to allow some tests.
 * The contrusctor method receives in input the ProfileManager object
 * created by the test class.
 */
public class TestInterMineAPI extends InterMineAPI
{
     /**
     * Construct an InterMine API object.
     * @param objectStore the production database
     * @param pm profilemanager
     * @param classKeys the class keys
     * @param bagQueryConfig configured bag queries used by BagQueryRunner
     * @param oss summary information for the ObjectStore
     */
    public TestInterMineAPI(ObjectStore objectStore, ProfileManager pm,
            Map<String, List<FieldDescriptor>> classKeys,
            BagQueryConfig bagQueryConfig, ObjectStoreSummary oss) {
        super(objectStore, null, classKeys, bagQueryConfig, oss, null, null);
        this.objectStore = objectStore;
        this.model = objectStore.getModel();
        this.classKeys = classKeys;
        this.bagQueryConfig = bagQueryConfig;
        this.oss = oss;
        this.profileManager = pm;
        this.bagManager = new BagManager(pm.getSuperuserProfile(), model);
        this.templateManager = new TemplateManager(pm.getSuperuserProfile(), model);
        this.templateSummariser = new TemplateSummariser(objectStore,
                pm.getProfileObjectStoreWriter(), oss);
        this.bagQueryRunner =
            new BagQueryRunner(objectStore, classKeys, bagQueryConfig, templateManager);
    }
}
