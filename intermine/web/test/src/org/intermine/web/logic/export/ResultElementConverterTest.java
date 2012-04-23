package org.intermine.web.logic.export;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.intermine.api.results.ResultElement;

/*
 * Copyright (C) 2002-2007 FlyMine
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
public class ResultElementConverterTest extends TestCase
{

    public void testConvert() {
        Object o1 = new Integer(1);
        Object o2 = new Date();
        Object o3 = "test";
        ResultElement el1 = new ResultElement(o1);
        ResultElement el2 = new ResultElement(o2);
        ResultElement el3 = new ResultElement(o3);
        List<ResultElement> els = new ArrayList<ResultElement>();
        els.add(el1);
        els.add(el2);
        els.add(el3);
        List<Object> objs = new ResultElementConverter().convert(els, null, null);
        assertEquals(o1, objs.get(0));
        assertEquals(o2, objs.get(1));
        assertEquals(o3, objs.get(2));
    }

}
