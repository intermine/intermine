package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.intermine.api.InterMineAPITestCase;
import org.intermine.api.bag.BagQueryHandler;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.StoreDataTestCase;
import org.intermine.util.SAXParser;
import org.xml.sax.InputSource;

/**
 * Tests for the TagHandler class.
 */

public class TagHandlerTest extends InterMineAPITestCase
{

    private ProfileManager pm;
    private Map<String, List<FieldDescriptor>>  classKeys;
    private String username = "testUser";

    public TagHandlerTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        classKeys = im.getClassKeys();
        pm = im.getProfileManager();
    }

    public void testParse() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        InputStream is = getClass().getClassLoader().getResourceAsStream("TagHandlerTest.xml");
        if (is == null) {
            throw new IllegalArgumentException("is was null");
        }
        TagHandler handler = new TagHandler(pm, username);
        SAXParser.parse(new InputSource(is), handler);
        int count =  handler.getCount();
        assertEquals(20, count);

    }
}
