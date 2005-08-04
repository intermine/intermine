package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.io.File;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.FileWriter;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;

import org.intermine.util.TypeUtil;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ReferenceList;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.flymine.io.gff3.GFF3Parser;
import org.flymine.io.gff3.GFF3Record;
import org.flymine.dataconversion.GFF3Converter;

public class ChadoGFF3RecordHandlerTest extends TestCase
{

    Model tgtModel;
    ChadoGFF3RecordHandler handler;
    MockItemWriter writer = new MockItemWriter(new LinkedHashMap());
    String seqClsName = "Chromosome";
    String orgAbbrev = "DM";
    String infoSourceTitle = "FlyBase";
    GFF3Converter converter;
    String tgtNs;
    ItemFactory itemFactory;

    public void setUp() throws Exception {
        tgtModel = Model.getInstanceByName("genomic");
        handler = new ChadoGFF3RecordHandler(tgtModel);
        converter = new GFF3Converter(writer, seqClsName, orgAbbrev, infoSourceTitle, tgtModel,
                                      handler);
        tgtNs = tgtModel.getNameSpace().toString();
        itemFactory = handler.getItemFactory();
    }

    public void testParseFlyBaseId() throws Exception {
        List dbxrefs = new ArrayList(Arrays.asList(new String[] {"FlyBase:FBgn1234", "FlyBase:FBtr1234"}));
        assertEquals("FBgn1234", handler.parseFlyBaseId(dbxrefs, "FBgn").get(0));
        assertEquals("FBtr1234", handler.parseFlyBaseId(dbxrefs, "FBtr").get(0));
    }

    public void testHandleGene() throws Exception {
        String gff = "4\t.\tgene\t230506\t233418\t.\t+\t.\tID=CG1234;Name=Crk;Dbxref=FlyBase:FBan0001587,FlyBase:FBgn0024811;synonym=Crk;synonym_2nd=CRK,D-CRK,Crk,CG5678";
        BufferedReader srcReader = new BufferedReader(new StringReader(gff));

        Iterator iter = GFF3Parser.parse(srcReader);
        GFF3Record record = (GFF3Record) iter.next();

        Item feature = itemFactory.makeItem(null, tgtNs + "Gene", "");
        feature.setAttribute("identifier", "CG1234");

        handler.setFeature(feature);
        handler.process(record);

        Item expectedGene = itemFactory.makeItem(feature.getIdentifier(), tgtNs + "Gene", "");
        expectedGene.setAttribute("organismDbId", "FBgn0024811");
        expectedGene.setAttribute("identifier", "CG1234");
        expectedGene.setAttribute("symbol", "CG1234");

        assertEquals(6, handler.getItems().size());

        assertEquals(1, handler.getFinalItems().size());

        Item actualGene = null;
        iter = handler.getFinalItems().iterator();
        while (iter.hasNext()) {
            Item item = (Item) iter.next();
            if (item.getClassName().equals(tgtNs + "Gene")) {
                actualGene = item;
                expectedGene.setIdentifier(actualGene.getIdentifier());
            }
        }
        assertEquals(expectedGene, actualGene);
    }

    // test that Gene->Pseudogene->Exon get changed to Pseudogene->Transcript->Exon
    public void testHandlePseudoGene() throws Exception {
        String gff =
            "4\t.\tgene\t26994\t32391\t.\t-\t.\tID=CR32011;Dbxref=FlyBase:FBan0032011,FlyBase:FBgn0052011;cyto_range=102A1-102A1;gbunit=AE003845;synonym=1st_CR32011;synonym_2nd=2nd_CG32011\n"
            + "4\t.\tpseudogene\t26994\t32391\t.\t-\t.\tID=CR32011-RA;Dbxref=FlyBase:FBtr0089182,FlyBase:FBgn0052011;Parent=CR32011;dbxref_2nd=Gadfly:CR32011-RA;synonym=1st_CR32011-RA\n"
            + "4\t.\texon\t27167\t27349\t.\t-\t.\tID=CR32011:7;Parent=CR32011-RA\n";

        BufferedReader srcReader = new BufferedReader(new StringReader(gff));

        Iterator iter = GFF3Parser.parse(srcReader);

        List featureIdentifiers = new ArrayList();
        
        List allItems = new ArrayList();
        
        while (iter.hasNext()) {

            GFF3Record record = (GFF3Record) iter.next();

            String term = record.getType();
            String className = TypeUtil.javaiseClassName(term);

            Item feature = itemFactory.makeItem(null, tgtNs + className, "");
            if (term.equals("gene")) {
                feature.setAttribute("identifier", "CR32011");
            } else {
                if (term.equals("pseudogene")) {
                    feature.setAttribute("identifier", "CR32011-RA");
                } else {
                    feature.setAttribute("identifier", "CR32011:7");
                }
            }
            
            handler.setFeature(feature);
            handler.process(record);
            
            featureIdentifiers.add(feature.getIdentifier());
            
            allItems.addAll(handler.getItems());
            handler.clear();
        }

        Item expectedGene =
            itemFactory.makeItem((String) featureIdentifiers.get(0), tgtNs + "Pseudogene", "");
        expectedGene.setAttribute("organismDbId", "FBgn0052011");
        expectedGene.setAttribute("identifier", "CR32011");
        expectedGene.setAttribute("symbol", "CR32011");

        Item expectedTranscript = itemFactory.makeItem((String) featureIdentifiers.get(1),
                                                       tgtNs + "Transcript", "");
        expectedTranscript.setAttribute("identifier", "CR32011-RA");

        Item expectedExon = itemFactory.makeItem((String) featureIdentifiers.get(2), 
                                                 tgtNs + "Exon", "");
        expectedExon.setAttribute("identifier", "CR32011:7");

        allItems.addAll(handler.getFinalItems());
        
        assertEquals(8, allItems.size());

        Item actualGene = null;
        Item actualTranscript = null;
        Item actualExon = null;

        iter = allItems.iterator();

        while (iter.hasNext()) {
            Item item = (Item) iter.next();
            if (item.getClassName().equals(tgtNs + "Pseudogene")) {
                actualGene = item;
            }
            if (item.getClassName().equals(tgtNs + "Transcript")) {
                actualTranscript = item;
            }
            if (item.getClassName().equals(tgtNs + "Exon")) {
                actualExon = item;
            }
        }
        
        assertEquals(expectedGene, actualGene);
        assertEquals(expectedTranscript, actualTranscript);
        assertEquals(expectedExon, actualExon);
    }

    // test that Gene->Pseudogene->Exon get changed to Pseudogene->Transcript->Exon
    public void testHandleDuplicateSymbol() throws Exception {
        String gff =
            "4\t.\tgene\t248174\t250682\t.\t+\t.\tID=CG1629;Name=yellow-h;Dbxref=FlyBase:FBan0001629,FlyBase:FBgn0039896;cyto_range=102B1-102B2;gbunit=AE003844;synonym=yellow-h\n"
            + "4\t.\tgene\t248174\t250682\t.\t+\t.\tID=CG1629-test;Name=yellow-h;Dbxref=FlyBase:FBan0001629,FlyBase:FBgn0039896;cyto_range=102B1-102B2;gbunit=AE003844;synonym=yellow-h\n";

        BufferedReader srcReader = new BufferedReader(new StringReader(gff));

        converter.parse(srcReader);

        Item expectedGene1 = null;
        Item expectedGene2 = null;
            
        Item actualGene1 = null;
        Item actualGene2 = null;


        Iterator iter = writer.getItems().iterator();

        while (iter.hasNext()) {
            Item item = (Item) iter.next();
            if (item.getClassName().equals(tgtNs + "Gene")) {
                if (item.getAttribute("identifier").getValue().equals("CG1629")) {
                    if (actualGene1 == null) {
                        actualGene1 = item;
                        expectedGene1 =
                            itemFactory.makeItem(item.getIdentifier(), tgtNs + "Gene", "");
                        expectedGene1.setAttribute("organismDbId", "FBgn0039896-duplicate-organismDbId-1");
                        expectedGene1.setAttribute("identifier", "CG1629");
                        expectedGene1.setAttribute("symbol", "yellow-h-duplicate-symbol-1");
                        expectedGene1.setReference("organism", handler.getOrganism());
                        List evidence = new ArrayList();
                        evidence.add(handler.getInfoSource().getIdentifier());
                        expectedGene1.setCollection("evidence", evidence);
                    } else {
                        fail("found a gene twice: CG1629");
                    }
                } else {
                    if (item.getAttribute("identifier").getValue().equals("CG1629-test")) {
                        if (actualGene2 == null) {
                            actualGene2 = item;
                            expectedGene2 =
                                itemFactory.makeItem(item.getIdentifier(), tgtNs + "Gene", "");
                            expectedGene2.setAttribute("organismDbId", "FBgn0039896-duplicate-organismDbId-2");
                            expectedGene2.setAttribute("identifier", "CG1629-test");
                            expectedGene2.setAttribute("symbol", "yellow-h-duplicate-symbol-2");
                            expectedGene2.setReference("organism", handler.getOrganism());
                            List evidence = new ArrayList();
                            evidence.add(handler.getInfoSource().getIdentifier());
                            expectedGene2.setCollection("evidence", evidence);
                        } else {
                            fail("found a gene twice: CG1629-test");
                        }
                    } else {
                        fail("found an unknown gene: " + item.getAttribute("identifier").getValue());
                    }
                }
            }
        }
        
        assertNotNull(expectedGene1);
        assertNotNull(expectedGene2);
        assertEquals(expectedGene1, actualGene1);
        assertEquals(expectedGene2, actualGene2);
    }

    public void testHandleGeneNoDbxref() throws Exception {
        String gff = "4\t.\tgene\t230506\t233418\t.\t+\t.\tID=CG1234;Dbxref=FlyBase:FBan0001587;dbxref_2nd=FlyBase:FBgn0024811";
        BufferedReader srcReader = new BufferedReader(new StringReader(gff));

        Iterator iter = GFF3Parser.parse(srcReader);
        GFF3Record record = (GFF3Record) iter.next();

        Item feature = itemFactory.makeItem(null, tgtNs + "Gene", "");
        feature.setAttribute("identifier", "CG1234");

        handler.setFeature(feature);
        handler.process(record);

        Item expectedGene = itemFactory.makeItem(feature.getIdentifier(), tgtNs + "Gene", "");
     
        expectedGene.setAttribute("identifier", "CG1234");
        expectedGene.setAttribute("symbol", "CG1234");

        assertEquals(1, handler.getItems().size());
        assertEquals(1, handler.getFinalItems().size());

        Item actualGene = (Item) handler.getFinalItems().iterator().next();
        expectedGene.setIdentifier(actualGene.getIdentifier());

        assertEquals(expectedGene, actualGene);
    }

    public void testHandleGeneNoFbgn() throws Exception {
        String gff = "4\t.\tgene\t230506\t233418\t.\t+\t.\tID=CG1234;Dbxref=FlyBase:FBan0001587";
        BufferedReader srcReader = new BufferedReader(new StringReader(gff));

        Iterator iter = GFF3Parser.parse(srcReader);
        GFF3Record record = (GFF3Record) iter.next();

        Item feature = itemFactory.makeItem(null, tgtNs + "Gene", "");
        feature.setAttribute("identifier", "CG1234");

        handler.setFeature(feature);
        handler.process(record);

        assertEquals(1, handler.getItems().size());
        assertEquals(1, handler.getFinalItems().size());

        Item expectedGene = itemFactory.makeItem(feature.getIdentifier(), tgtNs + "Gene", "");

        expectedGene.setAttribute("identifier", "CG1234");
        expectedGene.setAttribute("symbol", "CG1234");

        Item actualGene = (Item) handler.getFinalItems().iterator().next();

        expectedGene.setIdentifier(actualGene.getIdentifier());

        assertEquals(expectedGene, actualGene);
    }


    public void testHandleCDS() throws Exception {
        String gff = "2L\t.\ttCDS\t1938089\t1938159\t.\t-\t.\tID=CG11023-PA;Dbxref=FlyBase:FBpp0088316,GB_protein:AAO41164.1,FlyBase:FBgn0031208";

        BufferedReader srcReader = new BufferedReader(new StringReader(gff));
        Iterator iter = GFF3Parser.parse(srcReader);
        GFF3Record record = (GFF3Record) iter.next();

        Item feature = itemFactory.makeItem(null, tgtNs + "CDS", "");
        feature.setAttribute("identifier", "CG11023-PA");
        handler.setFeature(feature);

        Item infoSource = itemFactory.makeItem(null, tgtNs + "InfoSource", "");
        infoSource.setAttribute("title", "FlyBase");
        handler.setInfoSource(infoSource);

        handler.process(record);

        Item expectedTrans = itemFactory.makeItem(null, tgtNs + "Translation", "");
        expectedTrans.setAttribute("identifier", "CG11023-PA");
        expectedTrans.setAttribute("organismDbId", "FBpp0088316");
        expectedTrans.setReference("organism", handler.getOrganism().getIdentifier());
        expectedTrans.addCollection(new ReferenceList("evidence",
                                                    Arrays.asList(new Object[] {handler.getSourceIdentifier("FlyBase")})));
        assertEquals(4, handler.getItems().size());

        Item actualTrans = null;
        iter = handler.getItems().iterator();
        while (iter.hasNext()) {
            Item item = (Item) iter.next();
            if (item.getClassName().equals(tgtNs + "Translation")) {
                actualTrans = item;
                expectedTrans.setIdentifier(actualTrans.getIdentifier());
            }
        }
        assertEquals(expectedTrans, actualTrans);

        Item expectedCDS = itemFactory.makeItem(null, tgtNs + "CDS", "");
        expectedCDS.setAttribute("identifier", "CG11023-PA_CDS");
        expectedCDS.addCollection(new ReferenceList("polypeptides",
                                                    new ArrayList(Collections.singleton(actualTrans.getIdentifier()))));

        Item actualCDS = null;
        iter = handler.getItems().iterator();
        while (iter.hasNext()) {
            Item item = (Item) iter.next();
            if (item.getClassName().equals(tgtNs + "CDS")) {
                actualCDS = item;
                expectedCDS.setIdentifier(actualCDS.getIdentifier());
            }
        }
        assertEquals(expectedCDS, actualCDS);
    }
}
