package org.flymine.web.results;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import org.flymine.objectstore.dummy.ObjectStoreDummyImpl;
import org.flymine.objectstore.query.fql.FqlQuery;
import org.flymine.objectstore.query.Results;

public class DisplayableResultsTest extends TestCase
{
    public DisplayableResultsTest(String arg) {
        super(arg);
    }

    private Results results;

    public void setUp() throws Exception {
        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        os.setResultsSize(15);
        results = os.execute(new FqlQuery("select c, d from Company as c, Department as d", "org.flymine.model.testmodel").toQuery());
    }

    public void testConstructor() {
        DisplayableResults dr = new DisplayableResults(results);
        assertEquals(2, dr.getColumns().size());
    }


    public void testEnd() throws Exception {
        DisplayableResults dr = new DisplayableResults(results);
        dr.setEnd(10);
        assertEquals(10, dr.getEnd());
        dr.setEnd(20);
        assertEquals(14, dr.getEnd());
    }

}
