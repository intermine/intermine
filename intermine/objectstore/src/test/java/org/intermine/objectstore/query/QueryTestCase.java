package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.testing.OneTimeTestCase;

public class QueryTestCase extends OneTimeTestCase
{
    public QueryTestCase(String arg1) {
        super(arg1);
    }

    protected void assertEquals(Query q1, Query q2) {
        assertEquals("asserting equal", q1, q2);
    }

    protected void assertEquals(String msg, Query q1, Query q2) {
        QueryAssert.assertEquals(msg, q1, q2);
    }
}
