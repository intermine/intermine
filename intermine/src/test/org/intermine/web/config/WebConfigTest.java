package org.flymine.web.config;

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

public class WebConfigTest extends TestCase
{

    public WebConfigTest(String arg) {
        super(arg);
    }

    public void testParse() throws Exception{
        WebConfig wc1 = WebConfig.parse("test/WebConfigTest.xml");

        Displayer disp1 = new Displayer();
        disp1.setSrc("page1.jsp");
        Displayer disp2 = new Displayer();
        disp2.setSrc("tile1.tile");
        Displayer disp3 = new Displayer();
        disp3.setSrc("/model/page2.jsp");
        Type type1 = new Type();
        type1.addShortDisplayer(disp1);
        type1.addShortDisplayer(disp2);
        type1.addLongDisplayer(disp3);

        Displayer disp4 = new Displayer();
        disp4.setSrc("page3.jsp");
        Displayer disp5 = new Displayer();
        disp5.setSrc("/model/page4.jsp");
        Displayer disp6 = new Displayer();
        disp6.setSrc("tile2.tile");
        Type type2 = new Type();
        type2.addShortDisplayer(disp4);
        type2.addLongDisplayer(disp5);
        type2.addLongDisplayer(disp6);

        WebConfig wc2 = new WebConfig();
        wc2.addType(type1);
        wc2.addType(type2);

        assertEquals(wc2, wc1);


    }

}
