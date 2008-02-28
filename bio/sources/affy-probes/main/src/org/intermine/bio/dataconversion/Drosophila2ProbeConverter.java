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

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.TextFileUtil;
import org.intermine.xml.full.Item;

/**
 * 
 * @author Julie Sullivan
 */
public class Drosophila2ProbeConverter extends FileConverter
{
    protected static final Logger LOG = Logger.getLogger(Drosophila2ProbeConverter.class);

    protected Item dataSource, dataSet, org;
    protected Map bioMap = new HashMap(), chrMap = new HashMap();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     */
    public Drosophila2ProbeConverter(ItemWriter writer, Model model)
        throws ObjectStoreException, MetaDataException {
        super(writer, model);

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "Affymetrix");
        store(dataSource);

        dataSet = createItem("DataSet");
        dataSet.setReference("dataSource", dataSource.getIdentifier());
        
        org = createItem("Organism");
        org.setAttribute("taxonId", "7227");
        store(org);
    }


    /**
     * Read each line from flat file.
     *
     * @see DataConverter#process
     */
    public void process(Reader reader) throws Exception {
        
/*
        0 "Probe Set ID"
        1 "GeneChip Array"
        2 "Species Scientific Name"
        3 "Annotation Date"
        4 "Sequence Type"
        5 "Sequence Source"
        6 "Transcript ID(Array Design)"
        7 "Target Description"
        8 "Representative Public ID"
        9 "Archival UniGene Cluster"
        10 "UniGene ID"
        11 "Genome Version"
        12 "Alignment
        13 "Gene Title"
        14 "Gene Symbol"
        15 "Chromosomal Location"
        16 "Unigene Cluster Type"
        17 "Ensembl"
        18 "Entrez Gene"
        19 "SwissProt"
        20 "EC"
        21 "OMIM"
        22 "RefSeq Protein ID"
        23 "RefSeq Transcript ID"
        24 "FlyBase"
        25 "AGI"
        26 "WormBase",
        27 "MGI Name"
        28 "RGD Name"
        29 "SGD accession number"
        30 "Gene Ontology Bilogical Process"
        31 "Gene Ontology Cellular Component"
        32 "Gene Ontology Molecular Function"
        33 "Pathway"
        34 "InterPro",
        35 "Trans Membrane"
        36 "QTL"
        37 "Annotation Description"
        38 "Annotation Transcript Cluster"
        39 "Transcript Assignments"
        40 "Annotation Notes"
        
        
        0 "1616608_a_at",
        1 "Drosophila_2 Array",
        2 "Drosophila melanogaster",
        3 "Jul 11, 2007"
        4 "Consensus sequence",
        5 "Flybase",
        6 "CG9042-RB",
        7 "CG9042-RB /FEA=BDGP /GEN=Gpdh /DB_XREF=CG9042 FBgn0001128 /SEG=chr2L:+5935896,5940528 /MAP=26A3-26A3 /LEN=1934 /DEF=(CG9042 gene symbol:Gpdh FBgn0001128 (GO:0005737 cytoplasm) (GO:0006127 glycerophosphate shuttle) (GO:0004367 glycerol-3-phosphate dehydrogenase (NAD+)))",
        8 "CG9042-RB",
        9 "---",
        10 "---",
        11 "April 2004 (BDGP v4.0)",
        12 "arm_2L:5943681-5948313 (+) // 100.0 //",
        13 "5' gene",
        14 "Gpdh",
        15 "chr2-17.8",
        16 "full length",
        17 "CG9042",
        18 "33824",
        19 "P13706",
        20 "EC:1.1.1.8",
        21 "---",
        22 "NP_476565.1 /// NP_476566.1 /// NP_476567.1",
        23 "NM_057217 /// NM_057218 /// NM_057219",
        24 "FBgn0001128",
        25 "---",
        26 "---",
        27 "---",
        28 "---",
        29 "---",
        30 "0005975 // carbohydrate metabolic process // inferred from electronic annotation /// 0006072 // glycerol-3-phosphate metabolic process // inferred from sequence or structural similarity /// 0006072 // glycerol-3-phosphate metabolic process // inferred from electronic annotation /// 0006127 // glycerophosphate shuttle // non-traceable author statement /// 0006629 // lipid metabolic process // inferred from electronic annotation /// 0006641 // triacylglycerol metabolic process // inferred from mutant phenotype /// 0007629 // flight behavior // inferred from mutant phenotype /// 0046168 // glycerol-3-phosphate catabolic process // inferred from electronic annotation",
        31 "0005737 // cytoplasm // non-traceable author statement /// 0005737 // cytoplasm // inferred from electronic annotation /// 0009331 // glycerol-3-phosphate dehydrogenase complex // inferred from electronic annotation",
        32 "0004367 // glycerol-3-phosphate dehydrogenase (NAD+) activity // inferred from sequence or structural similarity /// 0004367 // glycerol-3-phosphate dehydrogenase (NAD+) activity // non-traceable author statement /// 0004367 // glycerol-3-phosphate dehydrogenase (NAD+) activity // inferred from electronic annotation /// 0016491 // oxidoreductase activity // inferred from electronic annotation /// 0016614 // oxidoreductase activity, acting on CH-OH group of donors // inferred from electronic annotation /// 0016616 // oxidoreductase activity, acting on the CH-OH group of donors, NAD or NADP as acceptor // inferred from electronic annotation /// 0050662 // coenzyme binding // inferred from electronic annotation /// 0051287 // NAD binding // inferred from electronic annotation",
        33 "---",
        34 "---",
        35 "---",
        36 "---",
        37 "This probe set was annotated using the Matching Probes based pipeline to a Entrez Gene identifier using 3 transcripts. // false // Matching Probes // A",
        38 "NM_057217(14),NM_057218(14),NM_057219(14)",
        39 "CG9042-RA // cdna:known chromosome:BDGP4.3:2L:5943682:5947625:1 gene:CG9042 // ensembl_transcript // 14 // --- /// CG9042-RB // cdna:known chromosome:BDGP4.3:2L:5943682:5948313:1 gene:CG9042 // ensembl_transcript // 14 // --- /// CG9042-RC // cdna:known chromosome:BDGP4.3:2L:5943682:5949092:1 gene:CG9042 // ensembl_transcript // 14 // --- /// FBtr0079146 // type=mRNA; loc=2L:5943682..5948313; ID=FBtr0079146; name=Gpdh-RB; dbxref=FlyBase:FBtr0079146,FlyBase_Annotation_IDs:CG9042-RB,FlyBase:FBtr0000279,FlyBase:FBtr0000280; MD5=edd4e93097d191262c895d5686825fc6; length=1934; parent=FBgn0001128; release=r5.1; species=Dmel; // flybase // 14 // --- /// FBtr0079147 // type=mRNA; loc=2L:5943682..5949092; ID=FBtr0079147; name=Gpdh-RC; dbxref=FlyBase:FBtr0079147,FlyBase_Annotation_IDs:CG9042-RC,FlyBase:FBtr0003832; MD5=dff71be420f9c3184f570501d24cebf8; length=2163; parent=FBgn0001128; release=r5.1; species=Dmel; // flybase // 14 // --- /// FBtr0079148 // type=mRNA; loc=2L:5943682..5947625; ID=FBtr0079148; name=Gpdh-RA; dbxref=FlyBase:FBtr0079148,FlyBase_Annotation_IDs:CG9042-RA,FlyBase:FBtr0000278,FlyBase:FBtr0000279; MD5=382679b4ce3a1592d3d86c36cae801fc; length=1770; parent=FBgn0001128; release=r5.1; species=Dmel; // flybase // 14 // --- /// NM_057217 // Drosophila melanogaster Glycerol 3 phosphate dehydrogenase CG9042-RB, transcript variant B (Gpdh), mRNA. // refseq // 14 // --- /// NM_057218 // Drosophila melanogaster Glycerol 3 phosphate dehydrogenase CG9042-RC, transcript variant C (Gpdh), mRNA. // refseq // 14 // --- /// NM_057219 // Drosophila melanogaster Glycerol 3 phosphate dehydrogenase CG9042-RA, transcript variant A (Gpdh), mRNA. // refseq // 14 // --- /// SNAP00000029920 // cdna:snap chromosome:BDGP4.3:2L:5944110:5948419:1 // ensembl_prediction // 14 // ---",
        40 "GENSCAN00000003302 // ensembl_prediction // 4 // Cross Hyb Matching Probes"
        
        "1622892_s_at","Drosophila_2 Array","Drosophila melanogaster","Jul 11, 2007","Consensus sequence","Flybase","CG7163-RA","CG7163-RA /FEA=BDGP /GEN=CG7163 /DB_XREF=CG7163 FBgn0035889 /SEG=chr3L:-8361016,8363982 /MAP=66C11-66C11 /LEN=2211 /DEF=(CG7163 gene symbol:CG7163 FBgn0035889 )","CG7163-RA","---","---","April 2004 (BDGP v4.0)","arm_3L:8395406-8398372 (-) // 100.0 //","CG33057 /// monkey king protein","CG33057 /// mkg-p","---","full length","CG33057 /// CG7163","318833 /// 38955","Q6Q376 /// Q86BH3 /// Q95U58 /// Q9VSJ4","---","---","NP_648219.1 /// NP_729375.1 /// NP_788477.1","NM_139962 /// NM_168272 /// NM_176299","FBgn0035889 /// FBgn0053057","---","---","---","---","---","0006388 // tRNA splicing // inferred from electronic annotation /// 0006388 // tRNA splicing // inferred from sequence or structural similarity","0005622 // intracellular // inferred from electronic annotation","0003676 // nucleic acid binding // inferred from electronic annotation /// 0008270 // zinc ion binding // inferred from electronic annotation /// 0008665 // 2'-phosphotransferase activity // inferred from sequence or structural similarity /// 0016779 // nucleotidyltransferase activity // inferred from electronic annotation","---","IPR002934 // DNA polymerase, beta-like region // 2.4E-5 /// IPR002934 // DNA polymerase, beta-like region // 2.4E-5","---","---","This probe set was annotated using the Matching Probes based pipeline to a Entrez Gene identifier using 3 transcripts. // true // Matching Probes // A","FBtr0076678(14),FBtr0076664(14),FBtr0076665(14),NM_176299(14),NM_168272(14),NM_139962(14),CG7163-RA(14),CG7163-RB(14),CG33057-RA(14),SNAP00000033952(13),GENSCAN00000009527(14)","CG33057-RA // cdna:novel chromosome:BDGP4.3:3L:8376114:8379219:-1 gene:CG33057 // ensembl_transcript // 14 // --- /// CG7163-RA // cdna:known chromosome:BDGP4.3:3L:8376254:8379219:-1 gene:CG7163 // ensembl_transcript // 14 // --- /// CG7163-RB // cdna:known chromosome:BDGP4.3:3L:8376254:8378503:-1 gene:CG7163 // ensembl_transcript // 14 // --- /// FBtr0076664 // type=mRNA; loc=3L:complement(8395407..8398372); ID=FBtr0076664; name=mkg-p-RA; dbxref=FlyBase:FBtr0076664,FlyBase_Annotation_IDs:CG7163-RA; MD5=8317928f0cc532272e9747ef85e7c6ec; length=2211; parent=FBgn0035889; release=r5.1; species=Dmel; // flybase // 14 // --- /// FBtr0076665 // type=mRNA; loc=3L:complement(8395407..8397656); ID=FBtr0076665; name=mkg-p-RB; dbxref=FlyBase:FBtr0076665,FlyBase_Annotation_IDs:CG7163-RB; MD5=3d254b2b1feffa68ff132ffc07f3cecc; length=2198; parent=FBgn0035889; release=r5.1; species=Dmel; // flybase // 14 // --- /// FBtr0076678 // type=mRNA; loc=3L:complement(8395267..8398372); ID=FBtr0076678; name=CG33057-RA; dbxref=FlyBase:FBtr0076678,FlyBase_Annotation_IDs:CG33057-RA; MD5=de326090468e5279020aa33a34053846; length=3106; parent=FBgn0053057; release=r5.1; species=Dmel; // flybase // 14 // --- /// GENSCAN00000009527 // cdna:Genscan chromosome:BDGP4.3:3L:8376424:8379144:-1 // ensembl_prediction // 14 // --- /// NM_139962 // Drosophila melanogaster monkey king protein CG7163-RB, transcript variant B (mkg-p), mRNA. // refseq // 14 // --- /// NM_168272 // Drosophila melanogaster monkey king protein CG7163-RA, transcript variant A (mkg-p), mRNA. // refseq // 14 // --- /// NM_176299 // Drosophila melanogaster CG33057-RA, transcript variant A (CG33057), mRNA. // refseq // 14 // --- /// SNAP00000033952 // cdna:snap chromosome:BDGP4.3:3L:8374472:8378455:-1 // ensembl_prediction // 13 // ---","---"

        
*/
        
        String arrayName = "";
        Iterator lineIter = TextFileUtil.parseCsvDelimitedReader(reader);
        boolean readingData = false;

        while (lineIter.hasNext() ) {
            String[] line = (String[]) lineIter.next();
            
            if (readingData) {

                List<Item> delayedItems = new ArrayList<Item>();
                Item probeSet = createProbeSet(line[0], delayedItems);

                String seqType = line[4];
                
                if (seqType.equalsIgnoreCase("control sequence")) {
                    // TODO add a description and flag
                    // probeSet.setAttribute("description", line[4]);
                    probeSet.setAttribute("isControl", "true");
                } else {

                    // create chromosome location for probe set
                    // "arm_2L:5943681-5948313 (+)" 
                    String alignment = line[12];
                    if (alignment != null && !alignment.equals("---")) {
                        
                        if (alignment.contains(":") && alignment.contains(" ") 
                                        && alignment.contains("-")) {
                        
                            String[] s = alignment.split(":");
                            Item chr = createChromosome(s[0]);
                            s = s[1].split(" ");
                            String strand = s[1];
                            s = s[0].split("-");
                            String start = s[0];
                            String end = s[1];

                            Item loc = createItem("Location");
                            loc.setReference("object", chr.getIdentifier());
                            loc.setReference("subject", probeSet.getIdentifier());
                            loc.setAttribute("strand", strand.contains("+") ? "1" : "-1");
                            loc.setAttribute("start", start);
                            loc.setAttribute("end", end);
                            loc.setCollection("evidence",
                            new ArrayList(Collections.singleton(dataSet.getIdentifier())));

                            delayedItems.add(loc);
                        } else {
                            LOG.error("Can't parse chromosome: " + alignment);
                        }
                    }
                }
                String fbgn = line[24];
                if (!fbgn.equals("---")) {
                    // set reference to transcript
                    Item transcript = createBioEntity("Transcript", line[6]);
                    probeSet.setReference("transcript", transcript.getIdentifier());
                    
                    Item gene = createBioEntity("Gene", fbgn);
                    probeSet.setReference("gene", gene.getIdentifier());
                    
                }
                store(probeSet);
                for (Item item : delayedItems) {
                    store(item);
                }
            } else {
                // still in the header
                dataSet.setAttribute("title", "Affymetrix array: " + line[1]);
                store(dataSet);
                readingData = true;
            }
        }
    }

    private Item createBioEntity(String clsName, String identifier)
        throws ObjectStoreException {
        Item bio = (Item) bioMap.get(identifier);
        if (bio == null) {
            bio = createItem(clsName);
            bio.setReference("organism", org.getIdentifier());
            bio.setAttribute("primaryIdentifier", identifier);
            bioMap.put(identifier, bio);
            store(bio);
        }
        return bio;
    }

    /**
     * @param clsName target class name
     * @param id identifier
     * @param ordId ref id for organism
     * @param datasourceId ref id for datasource item
     * @param datasetId ref id for dataset item
     * @param writer itemWriter write item to objectstore
     * @return item
     * @throws exception if anything goes wrong when writing items to objectstore
     */
    private Item createProbeSet(String probeSetId, List<Item> delayedItems)
        throws ObjectStoreException {
        Item probeSet = createItem("ProbeSet");
        probeSet.setAttribute("primaryIdentifier", probeSetId);
        probeSet.setAttribute("name", probeSetId);
        probeSet.setReference("organism", org.getIdentifier());
        probeSet.setCollection("evidence",
            new ArrayList(Collections.singleton(dataSet.getIdentifier())));

        Item synonym = createItem("Synonym");
        synonym.setAttribute("type", "identifier");
        synonym.setAttribute("value", probeSetId);
        synonym.setReference("source", dataSource.getIdentifier());
        synonym.setReference("subject", probeSet.getIdentifier());
        delayedItems.add(synonym);

        return probeSet;
    }

    private Item createChromosome(String chrId) throws ObjectStoreException {
        Item chr = (Item) chrMap.get(chrId);
        if (chr == null) {
            chr = createItem("Chromosome");
            String primaryIdentifier = null;
            // convert 'arm_2L' -> '2L'
            if (chrId.contains("_")) {
                String[] s = chrId.split("_");
                primaryIdentifier = s[1];
            } else {
                primaryIdentifier = chrId;
            }
            chr.setAttribute("primaryIdentifier", primaryIdentifier);
            chr.setReference("organism", org.getIdentifier());
            chrMap.put(chrId, chr);
            store(chr);
        }
        return chr;
    }
}
