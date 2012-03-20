package org.intermine.webservice.client.live;

import static org.junit.Assert.*;

import java.io.InputStreamReader;
import java.io.Reader;

import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.QueryService;
import org.junit.BeforeClass;
import org.junit.Test;

public class LiveServiceTest {

    private static final String baseUrl = "http://localhost/intermine-test/service";
    private static final ServiceFactory servicef = new ServiceFactory(baseUrl);

    @Test
    public void testVersion() {
        System.out.printf("API version: %d\n", servicef.getQueryService().getAPIVersion());
        assertTrue(servicef.getQueryService().getAPIVersion() > 5);
    }


    @Test
    public void testRelease() {
        System.out.printf("API release: %s\n", servicef.getQueryService().getRelease());
        assertTrue(servicef.getQueryService().getRelease().contains("test"));
    }

}
