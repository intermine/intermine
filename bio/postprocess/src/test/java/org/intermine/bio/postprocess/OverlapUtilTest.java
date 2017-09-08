package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.model.bio.Author;
import org.intermine.model.bio.Exon;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.MeshTerm;

/**
 * Tests for the OverlapUtil class.
 */
public class OverlapUtilTest extends TestCase {

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testIgnoreCombination() throws Exception {

        Class<?> ignoredClass = Author.class;
        Class<?> anotherIgnoredClass = MeshTerm.class;

        Class<?> class1 = Gene.class;
        Class<?> class2 = Exon.class;

        HashSet<Class<?>> ignored = new HashSet(Arrays.asList(new Class[] {ignoredClass, anotherIgnoredClass}));

        HashMap<Class<?>, Set<Class<?>>> classesToIgnore = new HashMap();
        classesToIgnore.put(class1, ignored);

        assertTrue(OverlapUtil.ignoreCombination(classesToIgnore, class1, ignoredClass));
        assertFalse(OverlapUtil.ignoreCombination(classesToIgnore, class1, class2));
    }

}
