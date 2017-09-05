package org.intermine.bio.util;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.util.DynamicUtil;

/**
 * Tests for the BioUtil class.
 *
 * @author Julie
 */
public class BioUtilTest extends TestCase
{
    public BioUtilTest(String arg) {
        super(arg);
    }

    protected ObjectStoreWriter osw;


    private Gene storedGene1 = null;
    private Gene storedGene2 = null;
    private Organism storedOrganism1 = null;
    private Organism storedOrganism2 = null;
    List<Integer> bagContents = null;

    private void createData() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        Set toStore = new HashSet();

        storedOrganism1 = (Organism) DynamicUtil.createObject(Collections.singleton(Organism.class));
        storedOrganism1.setShortName("Homo sapiens");
        storedOrganism1.setTaxonId(9606);
        storedOrganism1.setId(new Integer(2001));
        toStore.add(storedOrganism1);

        storedGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        storedGene1.setPrimaryIdentifier("gene1");
        storedGene1.setOrganism(storedOrganism1);
        storedGene1.setId(new Integer(3001));
        toStore.add(storedGene1);

        storedOrganism2 = (Organism) DynamicUtil.createObject(Collections.singleton(Organism.class));
        storedOrganism2.setShortName("Drosophila melanogaster");
        storedOrganism2.setTaxonId(7227);
        storedOrganism2.setId(new Integer(2002));
        toStore.add(storedOrganism2);

        storedGene2 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        storedGene2.setPrimaryIdentifier("gene2");
        storedGene2.setOrganism(storedOrganism2);
        storedGene2.setId(new Integer(3002));
        toStore.add(storedGene2);

        Iterator iter = toStore.iterator();
        while (iter.hasNext()) {
            InterMineObject o = (InterMineObject) iter.next();
            osw.store(o);
        }
        bagContents = Arrays.asList(storedGene1.getId(), storedGene2.getId());
    }

    public void testGetOrganisms() throws Exception {
        createData();
        boolean lowercase = false;
        String organismFieldName = "shortName";
        Collection<String> actualOrganismNames = BioUtil.getOrganisms(osw.getObjectStore(), "Gene", bagContents, lowercase, organismFieldName);
        HashSet<String> expectedOrganismNames = new HashSet(Arrays.asList(new String[] {storedOrganism1.getShortName(),
                storedOrganism2.getShortName()}));
        assertTrue(actualOrganismNames.size() == 2);
        assertTrue(actualOrganismNames.containsAll(expectedOrganismNames));

    }

    public void testReplaceStrain() {
        Integer taxonId = 9606;
        Integer expectedTaxon = 9606;

        Integer actualTaxon = BioUtil.replaceStrain(taxonId);
        assertEquals(expectedTaxon, actualTaxon);

        Integer strainId = 46245;
        taxonId = 7237;
        actualTaxon = BioUtil.replaceStrain(46245);
        assertEquals(taxonId, actualTaxon);
    }
}
