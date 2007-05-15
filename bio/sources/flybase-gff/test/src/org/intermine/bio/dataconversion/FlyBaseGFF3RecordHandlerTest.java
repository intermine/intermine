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

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.intermine.bio.io.gff3.GFF3Parser;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.util.TypeUtil;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ReferenceList;


public class FlyBaseGFF3RecordHandlerTest extends ItemsTestCase
{

    Model tgtModel;
    FlyBaseGFF3RecordHandler handler;
    MockItemWriter tgtIw;
    String seqClsName = "Chromosome";
    String taxonId = "7227";
    String dataSourceName = "FlyBase";
    String dateSetTitle = "FlyBase Drosophila melanogaster data set";
    GFF3Converter converter;
    String tgtNs;
    ItemFactory itemFactory;

    
    
    public FlyBaseGFF3RecordHandlerTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        tgtModel = Model.getInstanceByName("genomic");
        handler = new FlyBaseGFF3RecordHandler(tgtModel);
        tgtIw = new MockItemWriter(new LinkedHashMap());
        converter = new GFF3Converter(tgtIw, seqClsName, taxonId, dataSourceName, dateSetTitle,
                                      "FlyBase", tgtModel, handler, new FlyBaseGFF3SeqHandler());
        tgtNs = tgtModel.getNameSpace().toString();
        itemFactory = handler.getItemFactory();
    }

    public void testParseFlyBaseId() throws Exception {
        List dbxrefs = new ArrayList(Arrays.asList(new String[] {"FlyBase:FBgn1234", "FlyBase:FBtr1234"}));
        assertEquals("FBgn1234", handler.parseFlyBaseId(dbxrefs, "FBgn").get(0));
        assertEquals("FBtr1234", handler.parseFlyBaseId(dbxrefs, "FBtr").get(0));
    }

    public void testHandleGene() throws Exception {
        String gff = "4\tFlyBase\tgene\t24068\t25621\t.\t+\t.\tID=FBgn0040037;Name=CG17923;Alias=FBan0017923,CG17923;cyto_range=101F1-101F1;gbunit=AE003845;\n";
        BufferedReader srcReader = new BufferedReader(new StringReader(gff));

        Iterator iter = GFF3Parser.parse(srcReader);
        GFF3Record record = (GFF3Record) iter.next();

        Item feature = itemFactory.makeItem(null, tgtNs + "Gene", "");
        feature.setAttribute("identifier", "FBgn0040037");
        //feature.setAttribute("organismDbId", "FBgn0040037");

        handler.setFeature(feature);
        handler.process(record);

        Item expectedGene = itemFactory.makeItem(feature.getIdentifier(), tgtNs + "Gene", "");
        expectedGene.setAttribute("organismDbId", "FBgn0040037");
        expectedGene.setAttribute("curated", "true");

        assertEquals(0, handler.getItems().size());

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
            "4\tFlyBase\tgene\t26994\t32391\t.\t-\t.\tID=FBgn0052011;Name=CR32011;Alias=FBan0032011,CR32011;cyto_range=101F1-102A1;gbunit=AE003845;putative_ortholog_of=FBgn0076625\n"
            + "4\tFlyBase\tpseudogene\t26994\t32391\t.\t-\t.\tID=FBtr0089182;Name=CR32011-RA;Parent=FBgn0052011;Alias=CR32011-RA\n"
            + "4\tFlyBase\texon\t27167\t27349\t.\t-\t.\tID=CR32011%3A7;Name=CR32011%3A7;Parent=FBtr0089182\n";

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
                feature.setAttribute("identifier", "FBgn0052011");
            } else {
                if (term.equals("pseudogene")) {
                    feature.setAttribute("identifier", "FBtr0089182");
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
        expectedGene.setAttribute("curated", "true");

        Item expectedTranscript = itemFactory.makeItem((String) featureIdentifiers.get(1),
                                                       tgtNs + "Transcript", "");
        expectedTranscript.setAttribute("organismDbId", "FBtr0089182");
        expectedTranscript.setAttribute("curated", "true");

        Item expectedExon = itemFactory.makeItem((String) featureIdentifiers.get(2),
                                                 tgtNs + "Exon", "");
        expectedExon.setAttribute("identifier", "CR32011:7");
        expectedExon.setAttribute("curated", "true");

        allItems.addAll(handler.getFinalItems());

        assertEquals(3, allItems.size());

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

    public void testHandleCDS() throws Exception {
        // expect two transcripts each with one CDS
        // it is possible in gff spec for one transcript to have more than one CDS, hopefully
        // this would mean two separate CDS identifiers - to be confirmed
        String ENDL = System.getProperty("line.separator");
        String gff = "4\tFlyBase\tCDS\t100\t200\t.\t+\t.\tID=CDS_CG17469:2_742;Name=Mitf-cds;Parent=FBtr0100347;" + ENDL
            + "4\tFlyBase\tCDS\t300\t400\t.\t+\t.\tID=CDS_CG17469:3_742;Name=Mitf-cds;Parent=FBtr0100347;" + ENDL
            + "4\tFlyBase\tCDS\t500\t600\t.\t-\t.\tID=CDS_CG17469:2_744;Name=Mitf-cds;Parent=FBtr0100348;" + ENDL
            + "4\tFlyBase\tCDS\t600\t700\t.\t+\t.\tID=CDS_CG17469:3_744;Name=Mitf-cds;Parent=FBtr0100348;" + ENDL
            + "4\tFlyBase\tCDS\t800\t900\t.\t+\t.\tID=CDS_CG17469:3_745;Name=Mitf-cds;Parent=FBtr0100348;" + ENDL
            + "4\tFlyBase\tmRNA\t1223426\t1223545\t.\t+\t.\tID=FBtr0100347;Name=tran-1;" + ENDL
            + "4\tFlyBase\tmRNA\t1223426\t1223545\t.\t+\t.\tID=FBtr0100348;Name=tran-2;" + ENDL;


        BufferedReader srcReader = new BufferedReader(new StringReader(gff));
        converter.parse(srcReader);

        Iterator iter = handler.createCDSs().iterator();
        while (iter.hasNext()) {
            iter.next();
        }
        converter.store();
        converter.close();
        // uncomment to write out a new target items file
        //writeItemsFile(tgtIw.getItems(), "flybase_cds_tgt.xml");
        Set expected = readItemSet("FlyBaseGFF3RecordHandlerTest_cds_tgt.xml"); 
        assertEquals(expected, tgtIw.getItems());
    }

    public void testHandleSequenceVariant() throws Exception {
        String gff = "2L\t.\tsequence_variant\t13563\t22471\t.\t-\t.\tID=l(2)gl[275];Parent=CG2671";

        BufferedReader srcReader = new BufferedReader(new StringReader(gff));
        Iterator iter = GFF3Parser.parse(srcReader);
        GFF3Record record = (GFF3Record) iter.next();

        Item feature = itemFactory.makeItem(null, tgtNs + "SequenceVariant", "");
        feature.setAttribute("identifier", "l(2)gl[275]");
        handler.setFeature(feature);

        Item gene = itemFactory.makeItem(null, tgtNs + "Gene", "");


        Item simpleRelation = itemFactory.makeItem(null, tgtModel.getNameSpace() + "SimpleRelation", "");
        simpleRelation.setReference("object", gene.getIdentifier());
        simpleRelation.setReference("subject", feature.getIdentifier());
        handler.addParentRelation(simpleRelation);


        Item dataSet = itemFactory.makeItem(null, tgtNs + "DataSet", "");
        dataSet.setAttribute("title", "FlyBase Drosophila melanogaster data set");
        handler.setDataSet(dataSet);

        handler.process(record);

        handler.setReferences(handler.references); 

        Item expectedGene = itemFactory.makeItem(null, tgtNs + "Gene", "");
        expectedGene.setAttribute("identifier", "CG2671");
        expectedGene.setReference("organism", handler.getOrganism().getIdentifier());
        expectedGene.setCollection("evidence",
                                   Arrays.asList(new Object[] { dataSet.getIdentifier()}));
        assertEquals(2, handler.getItems().size());

        Item expectedSequenceVariant = itemFactory.makeItem(null, tgtNs + "SequenceVariant", "");
        expectedSequenceVariant.setAttribute("identifier", "l(2)gl[275]");
        expectedSequenceVariant.addCollection(new ReferenceList("genes",
                            new ArrayList(Collections.singleton(gene.getIdentifier()))));
        expectedSequenceVariant.setAttribute("curated", "true");

        Item actualSequenceVariant = null;
        iter = handler.getItems().iterator();
        while (iter.hasNext()) {
            Item item = (Item) iter.next();
            if (item.getClassName().equals(tgtNs + "SequenceVariant")) {
                actualSequenceVariant = item;
                expectedSequenceVariant.setIdentifier(actualSequenceVariant.getIdentifier());
            }
        }
        assertEquals(expectedSequenceVariant, actualSequenceVariant);
    }
    
    public void testHandleSynonyms() throws Exception{
//        String ENDL = System.getProperty("line.separator");
//        String gff = "4\tFlyBase\tCDS\t100\t200\t.\t+\t.\tID=CDS_CG17469:2_742;Name=Mitf-cds;Parent=FBtr0100347;" + ENDL
//            + "4\tFlyBase\tCDS\t300\t400\t.\t+\t.\tID=CDS_CG17469:3_742;Name=Mitf-cds;Parent=FBtr0100347;" + ENDL
//            + "4\tFlyBase\tmRNA\t1223426\t1223545\t.\t+\t.\tID=FBtr0100348;Name=tran-2;" + ENDL;
        String gff = "4\tFlyBase\tmRNA\t3871513\t3872386\t.\t-\t.\tID=FBtr0077514;Name=symbol-RA;Parent=FBgn0051957;Dbxref=FlyBase_Annotation_IDs:CG31957-RA;";
        BufferedReader srcReader = new BufferedReader(new StringReader(gff));
        converter.parse(srcReader);
        converter.store();
        converter.close();
        System.out.println(FullRenderer.render(tgtIw.getItems()));
    }
    
    public void testUnknownChromosomes() throws Exception{
      String gff = "Unknown_4\tFlyBase\tmRNA\t3871513\t3872386\t.\t-\t.\tID=FBtr0077514;Name=symbol-RA;Parent=FBgn0051957;Dbxref=FlyBase_Annotation_IDs:CG31957-RA;";
      BufferedReader srcReader = new BufferedReader(new StringReader(gff));
      converter.parse(srcReader);
      converter.store();
      converter.close();
      // uncomment to write out a new target items file
      writeItemsFile(tgtIw.getItems(), "flybase_cds_tgt.xml");
      Set expected = readItemSet("FlyBaseGFF3RecordHandlerTest_unknown_chr_tgt.xml"); 
      assertEquals(expected, tgtIw.getItems());
  }
}
