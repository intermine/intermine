package org.intermine.api.template;

import java.util.HashMap;
import java.util.Map;

import org.intermine.api.InterMineAPITestCase;
import org.intermine.api.profile.BadTemplateException;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.TagManager.TagException;
import org.intermine.api.search.Scope;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;

public class TemplateManagerTest extends InterMineAPITestCase {

    private Profile superUser, emptyUser;
    private ApiTemplate global1, private1, user1, overrideGlobal;
    private TemplateManager templateManager;

    public TemplateManagerTest() throws Exception {
        super(null);
    }

    public void setUp() throws Exception {
        super.setUp();

        templateManager = im.getTemplateManager();

        ProfileManager pm = im.getProfileManager();

        // superUser profile already exists
        superUser = pm.getSuperuserProfile();

        emptyUser = new Profile(pm, "emptyUser", null, "password", new HashMap(), new HashMap(),
                                new HashMap(), "token", true, false);
        pm.createProfile(emptyUser);
        setUpTemplatesAndTags();
    }

    private void setUpTemplatesAndTags() throws TagException, BadTemplateException {
        TagManager tagManager = im.getTagManager();

        PathQuery q = new PathQuery(im.getModel());
        global1 = new ApiTemplate("global1", "", "", q);
        superUser.saveTemplate(global1.getName(), global1);

        tagManager.addTag(TagNames.IM_PUBLIC, global1, superUser);

        private1 = new ApiTemplate("private1", "", "", q);
        superUser.saveTemplate(private1.getName(), private1);

        user1 = new ApiTemplate("user1", "", "", q);
        testUser.saveTemplate(user1.getName(), user1);

        overrideGlobal = new ApiTemplate("global1", "", "", new PathQuery(im.getModel()));
    }

    public void testGetGlobalTemplate() throws Exception {
        assertEquals(global1, templateManager.getGlobalTemplate("global1"));
        assertNull(templateManager.getGlobalTemplate("private1"));
        assertNull(templateManager.getGlobalTemplate("non-existant"));
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
//        // superuser should have same behaviour as get user template
        assertEquals(global1, templateManager.getUserTemplate(superUser, "global1"));
        assertEquals(private1, templateManager.getUserTemplate(superUser, "private1"));
        assertNull(templateManager.getUserTemplate(superUser, "user1"));

//        // normal user: user template of same name should override global
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
