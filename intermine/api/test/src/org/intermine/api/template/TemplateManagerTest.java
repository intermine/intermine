package org.intermine.api.template;

import java.util.HashMap;

import junit.framework.TestCase;

import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.TagManagerFactory;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.metadata.Model;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.pathquery.PathQuery;

public class TemplateManagerTest extends TestCase {
    
    private ProfileManager pm;
    private ObjectStore os;
    private ObjectStoreWriter uosw;
    private Profile superUser, testUser, emptyUser;
    private TemplateManager templateManager;
    private TagManager tagManager;
    private Model model;
    private TemplateQuery global1;
    
    public void setUp() throws Exception {
        super.setUp();
        os = ObjectStoreFactory.getObjectStore("os.unittest");
        model = os.getModel();
        
        uosw =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        pm = new ProfileManager(os, uosw);

        superUser = new Profile(pm, "superUser", null, "password", new HashMap(), new HashMap(), new HashMap());
        pm.createProfile(superUser);
        pm.setSuperuser("superUser");

        testUser = new Profile(pm, "testUser", null, "password", new HashMap(), new HashMap(), new HashMap());
        pm.createProfile(testUser);

        emptyUser = new Profile(pm, "emptyUser", null, "password", new HashMap(), new HashMap(), new HashMap());
        pm.createProfile(emptyUser);

        tagManager = new TagManagerFactory(pm).getTagManager();
        templateManager = new TemplateManager(superUser, os.getModel());

        setUpTemplatesAndTags();
    }

    public void tearDown() throws Exception {
        for (TemplateQuery templateQuery : superUser.getSavedTemplates().values()) {
            superUser.deleteTemplate(templateQuery.getName());
        }
        for (Tag tag : tagManager.getUserTags(superUser.getName())) {
            tagManager.deleteTag(tag);
        }
        uosw.delete(pm.getUserProfile(superUser.getName()));
        uosw.close();
    }
    
    private void setUpTemplatesAndTags() {
        PathQuery q = new PathQuery(model);
        global1 = new TemplateQuery("global1", "", "", q);
        superUser.saveTemplate(global1.getName(), global1);
        tagManager.addTag(TagNames.IM_PUBLIC, global1.getName(), TagTypes.TEMPLATE, "superUser");
        
    }
    
    
    
    public void testGetGlobalTemplates() throws Exception {
        assertEquals(global1, templateManager.getGlobalTemplates());
    }
    
    
}
