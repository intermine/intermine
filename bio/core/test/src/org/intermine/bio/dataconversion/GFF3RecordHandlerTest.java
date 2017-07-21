package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
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

    public void setUp() throws Exception {
        tgtModel = Model.getInstanceByName("genomic");
        itemFactory = new ItemFactory(tgtModel);
    }

    public void testSetReference() throws Exception {
        GFF3RecordHandler handler = new GFF3RecordHandler(tgtModel);

        Item threePrimeUTR = itemFactory.makeItem(null, "ThreePrimeUTR", "");
        handler.setFeature(threePrimeUTR);

        Item gene = itemFactory.makeItem(null, "Gene", "");

//        Item relation = itemFactory.makeItem(null, "SimpleRelation", "");
//        relation.setReference("subject", threePrimeUTR.getIdentifier());
//        relation.setReference("object", gene.getIdentifier());
        handler.addParent(gene.getIdentifier());

        Item expected = itemFactory.makeItem(threePrimeUTR.getIdentifier(), "ThreePrimeUTR", "");
//        expected.setReference("gene", gene.getIdentifier());

        Map refs = new HashMap();
        refs.put("ThreePrimeUTR", "gene");

//        handler.setReferences(refs);
        assertEquals(expected, handler.getFeature());
    }

    public void testSetCollection() throws Exception {
        GFF3RecordHandler handler = new GFF3RecordHandler(tgtModel);

        Item exon = itemFactory.makeItem(null, "Exon", "");
        handler.setFeature(exon);

        Item transcript1 = itemFactory.makeItem(null, "Transcript", "");
        Item transcript2 = itemFactory.makeItem(null, "Transcript", "");

//        Item relation1 = itemFactory.makeItem(null, "SimpleRelation", "");
//        relation1.setReference("subject", exon.getIdentifier());
//        relation1.setReference("object", transcript1.getIdentifier());
        handler.addParent(transcript1.getIdentifier());
//        Item relation2 = itemFactory.makeItem(null, "SimpleRelation", "");
//        relation2.setReference("subject", exon.getIdentifier());
//        relation2.setReference("object", transcript2.getIdentifier());
        handler.addParent(transcript2.getIdentifier());

        Item expected = itemFactory.makeItem(exon.getIdentifier(), "Exon", "");
        ReferenceList transcripts = new ReferenceList("transcripts", Arrays.asList(new String[] {transcript1.getIdentifier(),
                                                                                                 transcript2.getIdentifier()}));
//        expected.addCollection(transcripts);

        Map refs = new HashMap();
        refs.put("Exon", "transcripts");

//        handler.setReferences(refs);
        assertEquals(expected, handler.getFeature());
    }
}
