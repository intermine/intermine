package org.intermine.api.mines;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Properties;
import java.util.Set;

import org.intermine.api.InterMineAPITestCase;

public class FriendlyMineManagerTest extends InterMineAPITestCase
{

    public class MockRequester implements MineRequester {

        private Properties versions;

        MockRequester() {
            this.versions = new Properties();
        }

        @Override
        public BufferedReader requestURL(String urlString, ContentType contentType) {
            for (Object prefix: versions.keySet()) {
                String name = prefix.toString();
                if (urlString.startsWith(name)) {
                    return new BufferedReader(new StringReader(versions.getProperty(name)));
                }
            }
            throw new IllegalArgumentException("Cannot retrieve " + urlString);
        }

        @Override
        public void configure(Properties requesterConfig) {
            versions.putAll(requesterConfig);
        }

    }

    private static final String PROP_FILE = "friendlymines.properties";
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
                    .getResourceAsStream(PROP_FILE));
        } catch (Exception e) {
            e.printStackTrace();
        }
        MineRequester mockRequester = new MockRequester();
        linkManager = new FriendlyMineManager(im, webProperties, mockRequester);
    }

    public void testGetFriendlyMines() throws Exception {
        assertEquals(2, linkManager.getFriendlyMines().size());
    }

    public void testGetLocalMine() throws Exception {
        Mine flymine = linkManager.getLocalMine();
        assertEquals("FlyMine", flymine.getName());
        assertEquals("flymine_logo_link.gif", flymine.getLogo());
        assertEquals("#5C0075", flymine.getBgcolor());
        assertEquals("#FFF", flymine.getFrontcolor());
        Set<String> values = flymine.getDefaultValues();
        assertEquals(1, values.size());
        assertEquals("12345", flymine.getReleaseVersion());
    }

    public void testGetNonexistantMine() throws Exception {
        assertEquals(null, linkManager.getMine("MonkeyMine"));
    }

    public void testGetRemoteMine() throws Exception {
        Mine modMine = linkManager.getMine("modMine");
        
        assertEquals("modMine", modMine.getName());
        assertEquals("modminelogo.png", modMine.getLogo());
        assertEquals("#396A81", modMine.getBgcolor());
        assertEquals("#FFF", modMine.getFrontcolor());
        Set<String> values = modMine.getDefaultValues();
        assertEquals(2, values.size());
        assertEquals("modish", modMine.getReleaseVersion());
    }
}