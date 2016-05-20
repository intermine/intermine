package org.intermine.bio.ontology;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

public class OboTermTest extends TestCase
{
    public OboTermTest(String arg) {
        super(arg);
    }

    public void testEquals() {
        OboTerm d1 = new OboTerm("name", "id");
        OboTerm d2 = new OboTerm("name", "id");

        assertTrue(equalOboTerms(d1, d2));
        assertTrue(equalOboTerms(d2, d1));

        d1.addSynonym(new OboTermSynonym("s1", "exact"));
        d1.addSynonym(new OboTermSynonym("s2", "exact"));

        assertFalse(equalOboTerms(d1, d2));
        assertFalse(equalOboTerms(d2, d1));

        d2.addSynonym(new OboTermSynonym("s1", "exact"));
        d2.addSynonym(new OboTermSynonym("s2", "exact"));

        assertTrue(equalOboTerms(d1, d2));
        assertTrue(equalOboTerms(d2, d1));

    }

    public static boolean equalOboTerms(OboTerm t1, OboTerm t2) {
        return t1.getId().equals(t2.getId()) && t1.getName().equals(t2.getName()) &&
            t1.getSynonyms().equals(t2.getSynonyms());
    }
}
