package org.intermine.web.logic.help;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.intermine.util.PropertiesUtil;
import org.intermine.web.logic.results.WebState;

/**
 * Hints are shown at the top of the pages in the web application, this class reads the hints in
 * from properties and returns appropriate hints for a specified page.
 * @author Richard Smith
 *
 */
public class HintManager
{
    private static HintManager hintManager = null;
    private Map<String, Map<String, String>> hintsMap = null;

    /**
     * The maximum number of times a particular hint should be displayed in a session.
     */
    public static final int MAX_HINT_DISPLAY = 3;

    /**
     * Return an instance of a HintManager.
     * @param webProperties the InterMine web properties
     * @return an instance of the HintManager
     */
    public static synchronized HintManager getInstance(Properties webProperties) {
        if (hintManager == null) {
            hintManager = new HintManager(webProperties);
        }
        return hintManager;
    }

    /**
     * Construct with the InterMine web properties, extract the configured hints and set up in an
     * internal map.
     * @param webProperties the InterMine web properties
     */
    protected HintManager(Properties webProperties) {
        hintsMap = new HashMap<String, Map<String, String>>();

        Properties hintProps = PropertiesUtil.getPropertiesStartingWith("hint", webProperties);
        Iterator<Object> propIter = hintProps.keySet().iterator();
        while (propIter.hasNext()) {
            // key has format:  hint.pagename.hintId
            String key = (String) propIter.next();
            String hint = hintProps.getProperty(key);
            // NOTE the hintId is pagename.hintId so this is a unique string for WebState to count
            String hintId = key.substring(key.indexOf('.') + 1);
            String page = hintId.substring(0, hintId.indexOf('.'));

            addHint(page, hintId, hint);
        }
    }


    private synchronized void addHint(String page, String hintId, String hint) {
        Map<String, String> pageHints = hintsMap.get(page);
        if (pageHints == null) {
            pageHints = new HashMap<String, String>();
            hintsMap.put(page, pageHints);
        }
        pageHints.put(hintId, hint);
    }

    /**
     * Fetch a hint for the given page or return null if there are no hints or all hints have been
     * displayed the maximum number of times.  There may be multiple hints per page, a hint that has
     * been displayed the fewest number of times is selected.  Hint display counts are stored and
     * incremented in the user's WebState object.
     * @param page name of the page to dispay hints for
     * @param webState state from the current session
     * @return a hint or null if there are none to display
     */
    public String getHintForPage(String page, WebState webState) {
        String hint = null;
        Map<String, String> pageHints = hintsMap.get(page);
        if (pageHints != null) {
            TreeMap<Integer, String> availableHints = new TreeMap<Integer, String>();
            for (String hintId : pageHints.keySet()) {
                int hintCount = webState.getHintCount(hintId);
                if (hintCount < MAX_HINT_DISPLAY) {
                    availableHints.put(new Integer(hintCount), hintId);
                }
            }
            if (!availableHints.isEmpty()) {
                String hintId = availableHints.get(availableHints.firstKey());
                webState.incrementHintCount(hintId);
                hint = pageHints.get(hintId);
            }
        }

        return hint;
    }
}
