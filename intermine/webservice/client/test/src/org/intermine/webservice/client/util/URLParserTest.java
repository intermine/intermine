package org.intermine.webservice.client.util;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

/*
 * Copyright (C) 2002-2016 FlyMine
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
public class URLParserTest extends TestCase
{

    public void testParseServiceUrl() throws MalformedURLException {
        String url = "http://www.flymine.org/query/service/template/results?name=AllGene_Chromosome&op1=eq&value1=Drosophila+melanogaster&size=10&format=tab";
        String serviceUrl = URLParser.parseServiceUrl(url);
        assertEquals("http://www.flymine.org/query/service/template/results", serviceUrl);
    }

    public void testParseParameterMap() throws MalformedURLException {
        String url = "http://www.flymine.org/query/service/template/results?name=AllGene_Chromosome&op1=eq&value1=Drosophila+melanogaster&size=10&format=tab";
        Map<String, List<String>> map = URLParser.parseParameterMap(url);
        assertEquals(5, map.keySet().size());
        assertEquals("AllGene_Chromosome", map.get("name").get(0));
        assertEquals("eq", map.get("op1").get(0));
        assertEquals("Drosophila+melanogaster", map.get("value1").get(0));
        assertEquals("10", map.get("size").get(0));
        assertEquals("tab", map.get("format").get(0));
    }

}
