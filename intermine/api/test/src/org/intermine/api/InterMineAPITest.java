package org.intermine.api;


import org.intermine.api.profile.ProfileManager;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.query.WebResultsExecutor;

public class InterMineAPITest extends InterMineAPITestCase {

	public InterMineAPITest(String arg) {
        super(arg);
    }

	@Override
    public void setUp() throws Exception {
        super.setUp();
    }
	
    public void testGetObjectStore() {
        assertEquals(os, im.getObjectStore());
    }

    public void testGetModel() {
        assertEquals(os.getModel(), im.getModel());
    }
    
    public void testGetProfileManager() {
    	ProfileManager pm = im.getProfileManager();
    	assertEquals(os, pm.getProductionObjectStore());
        assertEquals(uosw, pm.getProfileObjectStoreWriter());
        assertEquals("superUser", pm.getSuperuser());
    }

    public void testGetTemplateManager() {
    	assertNotNull(im.getTemplateManager());
    }

    public void testGetBagManager() {
        assertNotNull(im.getBagManager());
    }
    
    public void getTemplateSummariser() {
    	assertNotNull(im.getTemplateSummariser());
    }
    
    public void testGetWebResultsExecutor() {
    	WebResultsExecutor wre = im.getWebResultsExecutor(testUser);
    	assertEquals(wre, im.getWebResultsExecutor(testUser));
    }

    public void testGetPathQueryExecutor() {
    	PathQueryExecutor pqe = im.getPathQueryExecutor(testUser);
    	assertEquals(pqe, im.getPathQueryExecutor(testUser));
    }

    @Override
    public void tearDown()throws Exception {
        super.tearDown();
    } 
}
