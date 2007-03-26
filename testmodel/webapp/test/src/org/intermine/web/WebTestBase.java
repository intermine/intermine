package org.intermine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;

import net.sourceforge.jwebunit.WebTestCase;

/**
 *
 * @author tom riley
 */
public class WebTestBase extends WebTestCase
{
    private String baseUrl;

    public WebTestBase(String name) {
        super(name);

    }

    public void setUp() throws Exception {
        super.setUp();
        Properties props = new Properties();
        try {
            props.load(getClass().getClassLoader().getResourceAsStream("build.properties"));
            baseUrl = props.getProperty("webapp.deploy.url") + "/" + props.getProperty("webapp.path");
            System.out.println("baseUrl is " + baseUrl);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
        getTestContext().setBaseUrl(baseUrl);
    }
}
