package org.intermine.web.logic.help;

import java.util.Properties;

import junit.framework.TestCase;

import org.intermine.web.logic.results.WebState;

public class HintManagerTest extends TestCase
{
    Properties hintProps;
    WebState webState;

    public HintManagerTest() throws Exception {
        hintProps = new Properties();
        hintProps.load(getClass().getClassLoader().getResourceAsStream("HintManagerTest.properties"));
    }

    @Override
    public void setUp() {
        webState = new WebState();
    }

    public void testConstruct() throws Exception {
        HintManager.getInstance(hintProps);
    }

    public void testGetHintForPageNoHint() {
        HintManager hintManager = HintManager.getInstance(hintProps);
        assertNull(hintManager.getHintForPage("dummy", webState));
    }

    public void testGetHintForPage() {
        HintManager hintManager = HintManager.getInstance(hintProps);
        String hint = hintManager.getHintForPage("templates", webState);
        assertTrue(hint.startsWith("Templates hint"));
    }

    // there are two hints for begin, they should rotate
    public void testGetHintForPageRotation() {
        HintManager hintManager = HintManager.getInstance(hintProps);
        String begin1 = hintManager.getHintForPage("begin", webState);
        String begin2 = hintManager.getHintForPage("begin", webState);
        assertFalse(begin1.equals(begin2));
    }
    
    // one hint should only be returned a maximum number of times
    public void testGetHintForPageMaxDisplay() {
        HintManager hintManager = HintManager.getInstance(hintProps);
        for (int i = 0; i < HintManager.MAX_HINT_DISPLAY; i++) {
            assertNotNull(hintManager.getHintForPage("templates", webState));
        }
        assertNull(hintManager.getHintForPage("templates", webState));
    }
}
