package org.intermine.api.mines;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class MineTest extends TestCase {

    final String mineName = "testmodelMine";

    Set ORGANISM_IN_OTHER_MINE = new HashSet(){{
        add("H. sapiens");
    }};


    public MineTest(String arg0) {
        super(arg0);

    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testCreateMine() {
        Mine mine = new Mine(mineName);
        assertEquals(mineName, mine.getName());
    }

    /**
     * local mine - Dmel
     * remote mine - human
     *
     * list contains dmel genes
     */
    public void testGetMapValues() {
        Mine mine = new Mine(mineName);
        mine.setMineMap(getLocalMap());
        Set matchingOrganisms = mine.getMatchingMapValues(ORGANISM_IN_OTHER_MINE, Arrays.asList("D. melanogaster"));
        assertEquals(1, matchingOrganisms.size());
        assertTrue(matchingOrganisms.contains("H. sapiens"));
    }

    /**
     * local mine - human
     * remote mine - fish
     *
     * list contains fish genes
     */
    public void testGetMapKeys() {
        Mine mine = new Mine(mineName);
        mine.setMineMap(getRemoteMap());
        Set matchingOrganisms = mine.getMatchingMapKeys(ORGANISM_IN_OTHER_MINE, Arrays.asList("D. rerio"));
        assertEquals(1, matchingOrganisms.size());
        assertTrue(matchingOrganisms.contains("H. sapiens"));
    }

    // list of organisms and homologues for local mine
    private Map<String, Set<String>> getLocalMap() {
        Map map = new HashMap<String, Set<String>>();
        Set set = new HashSet();
        set.add("C. elegans");
        set.add("H. sapiens");
        map.put("D. melanogaster", set);
        return map;
    }

    // list of organisms and homologues for remote mine
    private Map<String, Set<String>> getRemoteMap() {
        Map map = new HashMap<String, Set<String>>();
        Set set = new HashSet();
        set.add("G. gallus");
        set.add("D. rerio");
        map.put("H. sapiens", set);
        return map;
    }
}