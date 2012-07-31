package org.intermine.web.util;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

/**
 * @author Jakub Kulaviak
 **/
public class URLUtilTest extends TestCase
{

    public void testEncodeURL() {
        String url = "http://localhost:8080/query/service/template/results?" +
        		"name=AllGene_Chromosome&op1=eq&value1=Drosophila melanogaster&size=10&format=tab";
        String actual = URLUtil.encodeURL(url);
        String expected = "http://localhost:8080/query/service/template/results?" +
            "name=AllGene_Chromosome&op1=eq&value1=Drosophila+melanogaster&size=10&format=tab";
        assertEquals(expected, actual);
    }
    
}
