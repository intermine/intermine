package org.intermine.api.mines;

import java.util.Properties;
import java.util.Set;

import org.intermine.api.InterMineAPITestCase;

public class FriendlyMineManagerTest extends InterMineAPITestCase
{

    Properties webProperties = null;
    FriendlyMineManager linkManager = null;

    public FriendlyMineManagerTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        webProperties = new Properties();
        try {
            webProperties.load(this.getClass().getClassLoader()
                    .getResourceAsStream("global.web.properties"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        linkManager = FriendlyMineManager.getInstance(im, webProperties);
    }


    public void testGetFriendlyMines() throws Exception {
        assertEquals(2, linkManager.getFriendlyMines().size());
    }

    public void testGetLocalMine() throws Exception {
        assertEquals(linkManager.getLocalMine().getName(), "FlyMine");
    }

    public void testGetMine() throws Exception {
        assertEquals(null, linkManager.getMine("MonkeyMine"));
        Mine modMine = linkManager.getMine("modMine");
        assertEquals("modMine", modMine.getName());
        assertEquals("modminelogo.png", modMine.getLogo());
        assertEquals("#396A81", modMine.getBgcolor());
        assertEquals("#FFF", modMine.getFrontcolor());
        Set values = modMine.getDefaultValues();
        assertTrue(values.size() == 2);
        assertEquals(null, modMine.getReleaseVersion());
    }
}