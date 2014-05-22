package org.intermine.webservice.client.live;

import static org.junit.Assert.*;

import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.util.TestUtil;
import org.junit.Test;

public class LiveServiceTest {

    private static final ServiceFactory servicef = new ServiceFactory(TestUtil.getRootUrl());

    @Test
    public void testVersion() {
        int apiVersion = servicef.getQueryService().getAPIVersion();
        assertTrue(apiVersion + " is > 5", apiVersion > 5);
    }

    @Test
    public void testRelease() {
        String release = servicef.getQueryService().getRelease();
        assertTrue(release + " contains 'test'", release.contains("test"));
    }

}
