package org.intermine.api.xml;

/*
 * Copyright (C) 2002-2016 FlyMine
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

import org.intermine.api.profile.SavedQuery;
import org.intermine.metadata.Model;
import org.intermine.pathquery.PathQuery;

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
        String xml = SavedQueryBinding.marshal(sq, PathQuery.USERPROFILE_VERSION);
        SavedQuery sq2 = (SavedQuery) SavedQueryBinding.unmarshal(new StringReader(xml),
                new HashMap(), PathQuery.USERPROFILE_VERSION).values().iterator().next();
        String expected = "\n<saved-query name=\"hello\" date-created=\"1124276877010\"><query name=\"hello\" model=\"testmodel\" view=\"\" longDescription=\"\"></query></saved-query>";
        assertEquals(expected, xml);
    }
}
