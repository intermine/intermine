package org.intermine.ontology;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

public class DagTermTest extends TestCase
{
    public DagTermTest(String arg) {
        super(arg);
    }

    public void testEquals() {
        DagTerm d1 = new DagTerm("name", "id");
        DagTerm d2 = new DagTerm("name", "id");
        DagTerm d3 = new DagTerm("name", "id3");

        assertTrue(equalDagTerms(d1, d2));
        assertTrue(equalDagTerms(d2, d1));

        d1.addSynonym(new DagTermSynonym("s1"));
        d1.addSynonym(new DagTermSynonym("s2"));
        
        assertFalse(equalDagTerms(d1, d2));
        assertFalse(equalDagTerms(d2, d1));
        
        d2.addSynonym(new DagTermSynonym("s1"));
        d2.addSynonym(new DagTermSynonym("s2"));
        
        assertTrue(equalDagTerms(d1, d2));
        assertTrue(equalDagTerms(d2, d1));
        
        d1.addChild(d3);
        
        assertFalse(equalDagTerms(d1, d2));
        assertFalse(equalDagTerms(d2, d1));
        
        d2.addChild(d3);
        
        assertTrue(equalDagTerms(d1, d2));
        assertTrue(equalDagTerms(d2, d1));
        
        d1.addComponent(d3);
        
        assertFalse(equalDagTerms(d1, d2));
        assertFalse(equalDagTerms(d2, d1));
        
        d2.addComponent(d3);
        
        assertTrue(equalDagTerms(d1, d2));
        assertTrue(equalDagTerms(d2, d1));
    }
    
    public static boolean equalDagTerms(DagTerm t1, DagTerm t2) {
        return t1.getId().equals(t2.getId()) && t1.getName().equals(t2.getName()) &&
            t1.getChildren().equals(t2.getChildren()) &&
            t1.getComponents().equals(t2.getComponents()) &&
            t1.getSynonyms().equals(t2.getSynonyms());
    }
}
