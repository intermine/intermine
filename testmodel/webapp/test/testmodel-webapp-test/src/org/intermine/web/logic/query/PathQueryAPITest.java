package org.intermine.web.logic.query;

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

import org.intermine.api.query.PathQueryAPI;

/**
 * Tests the PathQueryAPI class.
 *
 * @author Matthew Wakeling
 */
public class PathQueryAPITest extends TestCase
{
    public void test1() throws Exception {
        PathQueryAPI.getPathQueryExecutor();
    }
}
