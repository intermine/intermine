package org.intermine.webservice.client.util;

import java.util.List;

import junit.framework.TestCase;

import org.intermine.webservice.client.services.ModelService;
import org.intermine.webservice.client.services.QueryService;
import org.intermine.webservice.client.services.TemplateService;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * @author Jakub Kulaviak
 **/
public class TestUtil extends TestCase 
{

    public static QueryService getQueryService() {
        return new QueryService(getRootUrl(), "TestUtil");
    }
    
    public static ModelService getModelService() {
        return new ModelService(getRootUrl(), "TestUtil");
    }
    
    public static TemplateService getTemplateService() {
        return new TemplateService(getRootUrl(), "TestUtil");
    }    
            
    public static String getRootUrl() {
        return "http://localhost:8080/intermine-test/service";
    }
    
    public static void checkRow(List<String> actual, String ... expected) {
        assertEquals(expected.length, actual.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual.get(i));
        }
    }
}
