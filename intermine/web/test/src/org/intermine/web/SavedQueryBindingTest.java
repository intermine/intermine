package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;

import junit.framework.TestCase;

import org.intermine.metadata.Model;

public class SavedQueryBindingTest extends TestCase
{
    private Model model;
    private Date created = new Date(1124276877010L);
    
    protected void setUp() throws Exception {
        super.setUp();
        model = Model.getInstanceByName("testmodel");
    }

    public void testMarshalSavedQuery() throws Exception {
        PathQuery query = new PathQuery(model);
        SavedQuery sq = new SavedQuery("hello", created, query);
        
        String xml = SavedQueryBinding.marshal(sq);
        
        SavedQuery sq2 = (SavedQuery) SavedQueryBinding.unmarshal(new StringReader(xml), 
                                                                  new HashMap()).values().iterator().next();
        
        assertEquals(sq, sq2);
    }
}
