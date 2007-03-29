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

import java.util.Date;

import org.intermine.metadata.Model;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.query.SavedQuery;

import junit.framework.TestCase;

public class SavedQueryTest extends TestCase
{
    private Model model;
    private Date created;
    
    protected void setUp() throws Exception {
        super.setUp();
        model = Model.getInstanceByName("testmodel");
        created = new Date();
    }

    public void testConstructSavedQuery() {
        PathQuery query = new PathQuery(model);
        SavedQuery sq = new SavedQuery("hello", created, query);
        assertEquals(sq.getName(), "hello");
        assertEquals(sq.getPathQuery(), query);
        assertEquals(sq.getDateCreated(), created);
        
        assertEquals(new SavedQuery("hello", new Date(created.getTime()), query), sq);
    }
}
