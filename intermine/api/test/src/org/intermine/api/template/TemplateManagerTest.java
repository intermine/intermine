package org.intermine.api.template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.TagManagerFactory;
import org.intermine.api.search.Scope;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.Tag;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.pathquery.PathQuery;

public class TemplateManagerTest extends TestCase {

    private ProfileManager pm;
    private ObjectStore os;
    private ObjectStoreWriter uosw;
    private Profile superUser, testUser, emptyUser;
    private TemplateManager templateManager;
    private TagManager tagManager;
    private Model model;
    private TemplateQuery global1, private1, user1, overrideGlobal;

    public void setUp() throws Exception {
        super.setUp();
        os = ObjectStoreFactory.getObjectStore("os.unittest");
        model = os.getModel();

        uosw =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");

        //clearUserProfile();

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

    private void clearUserProfile() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(UserProfile.class);
        q.addToSelect(qc);
        q.addFrom(qc);

        SingletonResults res = uosw.getObjectStore().executeSingleton(q);
        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            uosw.delete(o);
        }
    }

    public void tearDown() throws Exception {
        Profile[] users = new Profile[] {superUser, testUser, emptyUser};
        for (Profile user : users) {
            Set<String> templateNames = new HashSet<String>(user.getSavedTemplates().keySet());
;            for (String templateName : templateNames) {
                user.deleteTemplate(templateName, null);
            }
            for (Tag tag : tagManager.getUserTags(user.getName())) {
                tagManager.deleteTag(tag);
            }
            uosw.delete(pm.getUserProfile(user.getName()));
        }
        uosw.close();
    }

    private void setUpTemplatesAndTags() {
        PathQuery q = new PathQuery(model);
        global1 = new TemplateQuery("global1", "", "", q);
        superUser.saveTemplate(global1.getName(), global1);
        tagManager.addTag(TagNames.IM_PUBLIC, global1.getName(), TagTypes.TEMPLATE, "superUser");

        private1 = new TemplateQuery("private1", "", "", q);
        superUser.saveTemplate(private1.getName(), private1);

        user1 = new TemplateQuery("user1", "", "", q);
        testUser.saveTemplate(user1.getName(), user1);

        overrideGlobal = new TemplateQuery("global1", "", "", new PathQuery(model));
    }

    public void testGetGlobalTemplate() throws Exception {
        assertEquals(global1, templateManager.getGlobalTemplate("global1"));
        assertNull(templateManager.getGlobalTemplate("private1"));
    }

    public void testGetUserTemplate() throws Exception {
        assertEquals(global1, templateManager.getUserTemplate(superUser, "global1"));
        assertEquals(private1, templateManager.getUserTemplate(superUser, "private1"));
        assertNull(templateManager.getUserTemplate(superUser, "user1"));

        assertEquals(user1, templateManager.getUserTemplate(testUser, "user1"));
        assertNull(templateManager.getUserTemplate(testUser, "global1"));
    }

    public void testGetGlobalTemplates() throws Exception {
        Map<String, TemplateQuery> expected = new HashMap<String, TemplateQuery>();
        expected.put(global1.getName(), global1);
        assertEquals(expected, templateManager.getGlobalTemplates());
    }

    public void testGetUserOrGlobalTemplate() throws Exception {
        // superuser should have same behaviour as get user template
        assertEquals(global1, templateManager.getUserTemplate(superUser, "global1"));
        assertEquals(private1, templateManager.getUserTemplate(superUser, "private1"));
        assertNull(templateManager.getUserTemplate(superUser, "user1"));

        // normal user: user template of same name should override global
        assertEquals(user1, templateManager.getUserTemplate(testUser, "user1"));
        assertNull(templateManager.getUserTemplate(testUser, "global1"));
        testUser.saveTemplate(overrideGlobal.getName(), overrideGlobal);
        assertEquals(overrideGlobal, templateManager.getUserOrGlobalTemplate(testUser, "global1"));
        assertNotSame(global1, templateManager.getUserOrGlobalTemplate(testUser, "global1"));
    }

    public void testGetUserAndGlobalTemplates() throws Exception {
        Map<String, TemplateQuery> expected = new HashMap<String, TemplateQuery>();

        // super user
        expected.put(global1.getName(), global1);
        expected.put(private1.getName(), private1);
        assertEquals(expected, templateManager.getUserAndGlobalTemplates(superUser));

        // test user
        expected = new HashMap<String, TemplateQuery>();
        expected.put(user1.getName(), user1);
        expected.put(global1.getName(), global1);
        assertEquals(expected, templateManager.getUserAndGlobalTemplates(testUser));

        // test user naming collision
        testUser.saveTemplate(overrideGlobal.getName(), overrideGlobal);
        expected.put(overrideGlobal.getName(), overrideGlobal);
        assertEquals(expected, templateManager.getUserAndGlobalTemplates(testUser));

        // empty user
        expected = new HashMap<String, TemplateQuery>();
        expected.put(global1.getName(), global1);
        assertEquals(expected, templateManager.getUserAndGlobalTemplates(emptyUser));
    }

    public void testGetTemplate() throws Exception {
        assertNull(templateManager.getTemplate(testUser, "global1", Scope.USER));
        assertEquals(user1, templateManager.getTemplate(testUser, "user1", Scope.USER));
        assertEquals(user1, templateManager.getTemplate(testUser, "user1", Scope.ALL));
        assertNull(templateManager.getTemplate(testUser, "user1", Scope.GLOBAL));
        assertNull(templateManager.getTemplate(testUser, "nothing", Scope.USER));

        // now cause a name collision
        testUser.saveTemplate(overrideGlobal.getName(), overrideGlobal);
        assertEquals(global1, templateManager.getTemplate(testUser, "global1", Scope.GLOBAL));
        assertEquals(overrideGlobal, templateManager.getTemplate(testUser, "global1", Scope.ALL));
        assertEquals(overrideGlobal, templateManager.getTemplate(testUser, "global1", Scope.USER));

        try {
            templateManager.getTemplate(testUser, "nothing", "not a scope");
            fail("Expceted a RuntimeException for an invalid scope");
        } catch (RuntimeException e) {
            // expected
        }
    }


}
