package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

/**
 * @author Thomas Riley
 */
public class ItemPathTest extends TestCase {
    
    
    public void testItemPath() {
        ItemPath path = new ItemPath("(transcript.translation <- object_xref.ensembl).xref.external_db", "intermine#");
        printIPDs(Collections.singleton(path.getItemPrefetchDescriptor()), 0);
    }
    
    public void testItemPath2() {
        ItemPath path = new ItemPath("((gene <- transcript.gene).translation <- object_xref.ensembl).xref.external_db", "intermine#");
        printIPDs(Collections.singleton(path.getItemPrefetchDescriptor()), 0);
    }
    
    public void testItemPath3() {
        ItemPath path = new ItemPath("contig.dna", "intermine#");
        printIPDs(Collections.singleton(path.getItemPrefetchDescriptor()), 0);
    }
    
    public void testItemPath4() {
        ItemPath path = new ItemPath("(translation<-translation_stable_id . translation)", "intermine#");
        printIPDs(Collections.singleton(path.getItemPrefetchDescriptor()), 0);
    }
    
    public void testItemPathWithConstraint() {
        ItemPath path = new ItemPath("Type.collection[fieldX='valueX']", "intermine#");
        printIPDs(Collections.singleton(path.getItemPrefetchDescriptor()), 0);
    }
    
    public void testItemPathWithConstraint2() {
        ItemPath path = new ItemPath("Type.collection[fieldX='valueX'].field", "intermine#");
        assertEquals(1, path.getFieldValueConstrainsts(path.getItemPrefetchDescriptor(), new Object[0]).size());
        printIPDs(Collections.singleton(path.getItemPrefetchDescriptor()), 0);
    }
    
    public void testItemPathWith2Constraints() {
        ItemPath path = new ItemPath("Type.collection[fieldX='valueX' && fieldY='valueY'].field", "intermine#");
        assertEquals(2, path.getFieldValueConstrainsts(path.getItemPrefetchDescriptor(), new Object[0]).size());
        printIPDs(Collections.singleton(path.getItemPrefetchDescriptor()), 0);
    }
    
    public void testItemPathWithSubPathConstraint() {
        ItemPath path = new ItemPath("Type.collection[sub.path='valueX']", "intermine#");
        assertEquals(0, path.getFieldValueConstrainsts(path.getItemPrefetchDescriptor(), new Object[0]).size());
        assertEquals(1, path.getSubItemPathConstraints(path.getItemPrefetchDescriptor()).size());
        
        ItemPrefetchDescriptor subpath = (ItemPrefetchDescriptor) path.getSubItemPathConstraints(path.getItemPrefetchDescriptor()).iterator().next();
        assertEquals(1, path.getFieldValueConstrainsts(subpath, new Object[0]).size());
        
        printIPDs(Collections.singleton(path.getItemPrefetchDescriptor()), 0);
    }
    
    public void testItemPathWithVariable() {
        ItemPath path = new ItemPath("Type.collection[fieldX=$0].field", "intermine#");
        assertEquals(1, path.getFieldValueConstrainsts(path.getItemPrefetchDescriptor(), new Object[]{"value"}).size());
        printIPDs(Collections.singleton(path.getItemPrefetchDescriptor()), 0);
    }
    
    public void testBadPaths() {
        try {
            ItemPath path = new ItemPath("translation<-translation_stable_id.translation)", "intermine#");
            fail("should have got an IllegalArgumentException");
        } catch (IllegalArgumentException err) {}
        
        try {
            new ItemPath("1a@sdf.asdf", "intermine#");
            fail("should have got an IllegalArgumentException");
        } catch (IllegalArgumentException err) {
        }

        try {
            new ItemPath(".asdf", "intermine#");
            fail("should have got an IllegalArgumentException");
        } catch (IllegalArgumentException err) {
        }

        try {
            new ItemPath("", "intermine#");
            fail("should have got an IllegalArgumentException");
        } catch (IllegalArgumentException err) {
        }

    }
    
    private void printIPDs(Set ipds, int indent) {
        Iterator iter = ipds.iterator();
        while (iter.hasNext()) {
            ItemPrefetchDescriptor ipd = (ItemPrefetchDescriptor) iter.next();
            for (int i = 0 ; i < indent ; i++) {
                System.out.print(' ');
            }
            System.out.println(ipd.getDisplayName());
            printIPDs(ipd.getPaths(), indent + 4);
        }
    }
}
