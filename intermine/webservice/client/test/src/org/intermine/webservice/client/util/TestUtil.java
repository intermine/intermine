package org.intermine.webservice.client.util;

import java.util.List;

import junit.framework.TestCase;

import org.intermine.webservice.client.services.DummyModelService;
import org.intermine.webservice.client.services.DummyQueryService;
import org.intermine.webservice.client.services.DummyTemplateService;

/*
 * Copyright (C) 2002-2012 FlyMine
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

    public static DummyQueryService getQueryService() {
        return new DummyQueryService(getRootUrl(), "TestUtil");
    }

    public static DummyModelService getModelService() {
        return new DummyModelService(getRootUrl(), "TestUtil");
    }

    public static DummyTemplateService getTemplateService() {
        return new DummyTemplateService(getRootUrl(), "TestUtil");
    }

    public static String getRootUrl() {
        return "http://localhost:8080/intermine-test/service";
    }

    public static void checkRow(List<String> actual, Object ... expected) {
        assertEquals(expected.length, actual.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual.get(i));
        }
    }
}
