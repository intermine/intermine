package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.intermine.bio.dataconversion.GFF3RecordHandler;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ReferenceList;

import junit.framework.TestCase;


public class GFF3RecordHandlerTest extends TestCase
{
    private Model tgtModel;
    private ItemFactory itemFactory;
    private String tgtNs;

    public void setUp() throws Exception {
        tgtModel = Model.getInstanceByName("genomic");
        itemFactory = new ItemFactory(tgtModel);
        tgtNs = tgtModel.getNameSpace().toString();
    }

    public void testSetReference() throws Exception {
        GFF3RecordHandler handler = new GFF3RecordHandler(tgtModel);

        Item threePrimeUTR = itemFactory.makeItem(null, tgtNs + "ThreePrimeUTR", "");
        handler.setFeature(threePrimeUTR);

        Item gene = itemFactory.makeItem(null, tgtNs + "Gene", "");

        Item relation = itemFactory.makeItem(null, tgtNs + "SimpleRelation", "");
        relation.setReference("subject", threePrimeUTR.getIdentifier());
        relation.setReference("object", gene.getIdentifier());
        handler.addParentRelation(relation);

        Item expected = itemFactory.makeItem(threePrimeUTR.getIdentifier(), tgtNs + "ThreePrimeUTR", "");
        expected.setReference("gene", gene.getIdentifier());

        Map refs = new HashMap();
        refs.put("ThreePrimeUTR", "gene");

        handler.setReferences(refs);
        assertEquals(expected, handler.getFeature());
    }

    public void testSetCollection() throws Exception {
        GFF3RecordHandler handler = new GFF3RecordHandler(tgtModel);

        Item exon = itemFactory.makeItem(null, tgtNs + "Exon", "");
        handler.setFeature(exon);

        Item transcript1 = itemFactory.makeItem(null, tgtNs + "Transcript", "");
        Item transcript2 = itemFactory.makeItem(null, tgtNs + "Transcript", "");

        Item relation1 = itemFactory.makeItem(null, tgtNs + "SimpleRelation", "");
        relation1.setReference("subject", exon.getIdentifier());
        relation1.setReference("object", transcript1.getIdentifier());
        handler.addParentRelation(relation1);
        Item relation2 = itemFactory.makeItem(null, tgtNs + "SimpleRelation", "");
        relation2.setReference("subject", exon.getIdentifier());
        relation2.setReference("object", transcript2.getIdentifier());
        handler.addParentRelation(relation2);

        Item expected = itemFactory.makeItem(exon.getIdentifier(), tgtNs + "Exon", "");
        ReferenceList transcripts = new ReferenceList("transcripts", Arrays.asList(new Object[] {transcript2.getIdentifier(),
                                                                                                 transcript1.getIdentifier()}));
        expected.addCollection(transcripts);

        Map refs = new HashMap();
        refs.put("Exon", "transcripts");

        handler.setReferences(refs);
        assertEquals(expected, handler.getFeature());
    }


}
