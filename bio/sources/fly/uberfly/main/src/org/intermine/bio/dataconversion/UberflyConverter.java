package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * Converter to parse modENCODE expression data.
 *
 * @author Julie Sullivan
 */
public class UberflyConverter extends BioDirectoryConverter
{
    //private static final Logger LOG = Logger.getLogger(UberflyConverter.class);
    private static final String DATASET_TITLE = "Uberfly expression data";
    private static final String DATA_SOURCE_NAME = "Uberfly";
    private Item organism, flyDevelopmentOntology, flyAnatomyOntology;
    private static final String TAXON_FLY = "7227";
    private Map<String, String> genes = new HashMap<String, String>();
    private Map<String, Item> libraries = new HashMap<String, Item>();
    private Map<String, String> stages = new HashMap<String, String>();
    private Map<String, String> tissues = new HashMap<String, String>();
    protected IdResolver rslv;
    private File metadataFile;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public UberflyConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);

        flyDevelopmentOntology = createItem("Ontology");
        flyDevelopmentOntology.setAttribute("name", "Fly Development");

        flyAnatomyOntology = createItem("Ontology");
        flyAnatomyOntology.setAttribute("name", "Fly Anatomy");

        organism = createItem("Organism");
        organism.setAttribute("taxonId", TAXON_FLY);

        try {
            store(flyDevelopmentOntology);
            store(flyAnatomyOntology);
            store(organism);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }

        if (rslv == null) {
            rslv = IdResolverService.getFlyIdResolver();
        }
    }

    /**
     * @param metadataFile data file with all the experiment info
     */
    public void setUberflyMetadataFile(File metadataFile) {
        this.metadataFile = metadataFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(File dataDir) throws Exception {
        processMetadataFile(new FileReader(metadataFile));
        for (File file : dataDir.listFiles()) {
            processGeneFile(new FileReader(file));
        }
    }

    @Override
    public void close() {
        for (Item item : libraries.values()) {
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("Failed to store library", e);
            }
        }
    }

    private void processGeneFile(Reader reader) throws ObjectStoreException {
        Iterator<?> tsvIter;
        try {
            tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
        } catch (Exception e) {
            throw new BuildException("cannot parse gene file ", e);
        }

        // skip header
        tsvIter.next();

        while (tsvIter.hasNext()) {
            String[] line = (String[]) tsvIter.next();

            if (line.length < 3) {
                throw new RuntimeException("Expected 3 columns but was " + line.length);
            }

            String fbgn = line[0];
            String count = line[1];
            String library = line[2];

            Item result = createItem("UberFlyRNASeqResult");
            result.setAttribute("count", count);
            result.setReference("library", getLibrary(library));
            String gene = getGene(fbgn);
            if (StringUtils.isNotEmpty(gene)) {
                result.setReference("gene", gene);
                store(result);
            }
        }
    }

    private void processMetadataFile(Reader reader) throws ObjectStoreException {
        Iterator<?> tsvIter;
        try {
            tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
        } catch (Exception e) {
            throw new BuildException("cannot parse metadata file", e);
        }

        // skip header
        tsvIter.next();

        while (tsvIter.hasNext()) {
            String[] line = (String[]) tsvIter.next();

            String libraryIdentifier = line[0].trim();
            String sample = line[1].trim();
            String age = line[2].trim();
            String stage = line[3].trim();
            String name = line[4].trim();
            String sex = line[5].trim();
            String tissue = line[6].trim();

            Item library = getLibrary(libraryIdentifier);

            library.setAttributeIfNotNull("sample", sample);
            library.setAttributeIfNotNull("age", age);
            if (StringUtils.isNotEmpty(stage)) {
                library.setReference("stage", getStage(stage));
            }
            library.setAttributeIfNotNull("name", name);
            library.setAttributeIfNotNull("sex", sex);
            if (StringUtils.isNotEmpty(tissue)) {
                library.setReference("tissue", getTissue(tissue));
            }

            library.setAttributeIfNotNull("adapterBarcode", line[7]);
            // skip age
            library.setAttributeIfNotNull("agePostEclosion", line[9]);
            library.setAttributeIfNotNull("agent", line[10]);
            library.setAttributeIfNotNull("antibody", line[11]);
            library.setAttributeIfNotNull("backgroundStrain", line[12]);
            library.setAttributeIfNotNull("barcode", line[13]);
            library.setAttributeIfNotNull("barcodeKit", line[14]);

            if (StringUtils.isNotEmpty(line[15]) || StringUtils.isNotEmpty(line[16])
                || StringUtils.isNotEmpty(line[17])) {
                library.setAttributeIfNotNull("biologicalReplicate", line[15] + " " + line[16] + " "
                        + line[17]);
            }

            library.setAttributeIfNotNull("biomarker", line[18]);
            library.setAttributeIfNotNull("biomaterialProvider", line[19]);
            library.setAttributeIfNotNull("bioSampleModel", line[20]);
            library.setAttributeIfNotNull("birthDate", line[21]);
            library.setAttributeIfNotNull("birthLocation", line[22]);
            library.setAttributeIfNotNull("bloomingtonStockId", line[23]);
            library.setAttributeIfNotNull("breed", line[24]);
            library.setAttributeIfNotNull("breedingHistory", line[25]);
            library.setAttributeIfNotNull("breedingMethod", line[26]);

            if (StringUtils.isNotEmpty(line[27]) || StringUtils.isNotEmpty(line[28])
                    || StringUtils.isNotEmpty(line[29])) {
                Item item = createItem("UberFlyCellLine");
                item.setAttributeIfNotNull("line", line[27]);
                item.setAttributeIfNotNull("subtype", line[28]);
                item.setAttributeIfNotNull("type", line[29]);
                store(item);
                library.setReference("cellLine", item);
            }

            // ignore [30] checksum
            library.setAttributeIfNotNull("chipOrIpAntibody", line[31]);
            // ignore [32] collectedby
            library.setAttributeIfNotNull("collectionDate", line[33]);
            library.setAttributeIfNotNull("compound", line[34]);
            library.setAttributeIfNotNull("condition", line[35]);
            library.setAttributeIfNotNull("crosses", line[36]);
            library.setAttributeIfNotNull("cultivar", line[37]);
            library.setAttributeIfNotNull("cultureCollection", line[38]);
            library.setAttributeIfNotNull("daysAt29c", line[39]);
            library.setAttributeIfNotNull("daysPostInfection", line[40]);

            library.setAttributeIfNotNull("deathDate", line[41]);
            // ignore [42-48] dev stages
            library.setAttributeIfNotNull("dgrpLine", line[49]);
            library.setAttributeIfNotNull("diet", line[50]);
            library.setAttributeIfNotNull("disease", line[51]);
            library.setAttributeIfNotNull("diseaseStage", line[52]);
            library.setAttributeIfNotNull("dissection", line[53]);
            library.setAttributeIfNotNull("drosdelDeficiency", line[54]);
            library.setAttributeIfNotNull("drosdelId", line[55]);
            library.setAttributeIfNotNull("ecotype", line[56]);

            library.setAttributeIfNotNull("embryonicStage", line[57]);
            library.setAttributeIfNotNull("enaFirstPublic", line[58]);
            library.setAttributeIfNotNull("enaLastUpdate", line[59]);
            library.setAttributeIfNotNull("erccInformation", line[60]);
            library.setAttributeIfNotNull("erccPool", line[61]);
            library.setAttributeIfNotNull("evolutionaryRegime", line[62]);
            library.setAttributeIfNotNull("experiment", line[63]);
            library.setAttributeIfNotNull("experimentPopulation", line[64]);
            library.setAttributeIfNotNull("expression", line[65]);
            library.setAttributeIfNotNull("extractionProtocol", line[66]);

            library.setAttributeIfNotNull("fixation", line[67]);
            library.setAttributeIfNotNull("flag", line[68]);
            library.setAttributeIfNotNull("flowCell", line[69]);
            library.setAttributeIfNotNull("flyLine", line[70]);
            // ignore flybase id 71
            library.setAttributeIfNotNull("fraction", line[72]);
            // ignore [73] gender

            storeStrain(library, line);

            library.setAttributeIfNotNull("geoLocName", line[78]);
            library.setAttributeIfNotNull("germLineKnockDown", line[79]);
            library.setAttributeIfNotNull("germLineKnockDownAndOtherTransgenes", line[80]);

            if (StringUtils.isNotEmpty(line[81]) || StringUtils.isNotEmpty(line[82])) {
                library.setAttributeIfNotNull("growthConditions", line[81] + " " + line[82]);
            }

            library.setAttributeIfNotNull("growthProtocol", line[83]);
            library.setAttributeIfNotNull("healthState", line[84]);

            library.setAttributeIfNotNull("iclipBarcode", line[85]);
            library.setAttributeIfNotNull("illuminaBarcode", line[86]);
            library.setAttributeIfNotNull("index", line[87]);
            library.setAttributeIfNotNull("infection", line[88]);
            library.setAttributeIfNotNull("intialTimePoint", line[89]);

            library.setAttributeIfNotNull("isolate", line[90]);
            library.setAttributeIfNotNull("isolationSource", line[91]);
            library.setAttributeIfNotNull("label", line[92]);
            library.setAttributeIfNotNull("lane", line[93]);
            library.setAttributeIfNotNull("latLon", line[94]);

            // author library

            library.setAttributeIfNotNull("lineSource", line[99]);
            library.setAttributeIfNotNull("marker", line[100]);
            library.setAttributeIfNotNull("matingStatus", line[101]);
            library.setAttributeIfNotNull("muscleType", line[102]);
            library.setAttributeIfNotNull("notes", line[103]);
            // skip organism and organism part

            library.setAttributeIfNotNull("peReadLengthBp", line[106]);
            library.setAttributeIfNotNull("phenotype", line[107]);
            library.setAttributeIfNotNull("plateAndWellId", line[108]);
            library.setAttributeIfNotNull("quantity", line[109]);

            if (StringUtils.isNotEmpty(line[110]) || StringUtils.isNotEmpty(line[111])) {
                library.setAttributeIfNotNull("replicates", line[110] + " " + line[111]);
            }

            library.setAttributeIfNotNull("resistance", line[112]);
            library.setAttributeIfNotNull("sampleExtractionMethod", line[113]);
            // skip name and title
            library.setAttributeIfNotNull("type", line[116]);
            library.setAttributeIfNotNull("sequencer", line[117]);
            // skip sex
            library.setAttributeIfNotNull("sourceName", line[119]);
            // skip species

            library.setAttributeIfNotNull("specimenVoucher", line[121]);
            library.setAttributeIfNotNull("specimenWithKnownStorageState", line[122]);
            library.setAttributeIfNotNull("starvationStatus", line[123]);
            library.setAttributeIfNotNull("stock", line[124]);
            library.setAttributeIfNotNull("storeCond", line[125]);

            // --- strain is processed above -- 126 to 130

            library.setAttributeIfNotNull("studBookNumber", line[131]);
            library.setAttributeIfNotNull("subregion", line[132]);
            library.setAttributeIfNotNull("tag", line[133]);
            library.setAttributeIfNotNull("targetMolecule", line[134]);
            library.setAttributeIfNotNull("technicalReplicate", line[135]);

            library.setAttributeIfNotNull("technicalReplicatesPooled", line[136]);
            // skip temp
            library.setAttributeIfNotNull("temperature", line[138]);
            library.setAttributeIfNotNull("time", line[139]);
            library.setAttributeIfNotNull("timePoint", line[140]);

            if (StringUtils.isNotEmpty(line[141]) || StringUtils.isNotEmpty(line[142])
                    || StringUtils.isNotEmpty(line[143])) {
                Item item = createItem("UberFlyTissue");
                item.setAttributeIfNotNull("tissue", line[141]);
                item.setAttributeIfNotNull("library", line[142]);
                item.setAttributeIfNotNull("type", line[143]);
                store(item);
                library.setReference("uberFlyTissue", item);
            }

            library.setAttributeIfNotNull("treatment", line[144]);

            library.setAttributeIfNotNull("xChromosomeDose", line[145]);
            library.setAttributeIfNotNull("description", line[146]);
            library.setAttributeIfNotNull("sampleTitle", line[147]);
            library.setAttributeIfNotNull("studyAbstract", line[148]);
        }
    }

    private void storeStrain(Item library, String[] line) throws ObjectStoreException {
        if (StringUtils.isNotEmpty(line[74]) || StringUtils.isNotEmpty(line[75])
                || StringUtils.isNotEmpty(line[76]) || StringUtils.isNotEmpty(line[77])
                || StringUtils.isNotEmpty(line[126]) || StringUtils.isNotEmpty(line[127])
                || StringUtils.isNotEmpty(line[128]) || StringUtils.isNotEmpty(line[129])
                || StringUtils.isNotEmpty(line[130])) {
            Item item = createItem("UberFlyStrain");
            item.setAttributeIfNotNull("geneticBackground", line[74]);
            item.setAttributeIfNotNull("geneticModification", line[75]);
            item.setAttributeIfNotNull("genotype", line[76]);
            item.setAttributeIfNotNull("genotypeVariation", line[77]);

            item.setAttributeIfNotNull("strain", line[126]);
            item.setAttributeIfNotNull("strainGenotype", line[127]);
            item.setAttributeIfNotNull("strainBackground", line[128]);
            item.setAttributeIfNotNull("strainOrigin", line[129]);
            item.setAttributeIfNotNull("strainOrLine", line[130]);
            store(item);

            library.setReference("strain", item);
        }
    }

    private String getTissue(String name) throws ObjectStoreException {
        if (tissues.containsKey(name)) {
            return tissues.get(name);
        }
        Item item = createItem("OntologyTerm");
        item.setAttribute("name", name);
        item.setReference("ontology", flyAnatomyOntology);
        String refId = item.getIdentifier();
        tissues.put(name, refId);
        store(item);
        return refId;
    }


    private String getStage(String name) throws ObjectStoreException {
        if (stages.containsKey(name)) {
            return stages.get(name);
        }
        Item item = createItem("OntologyTerm");
        item.setAttribute("name", name);
        item.setReference("ontology", flyDevelopmentOntology);
        String refId = item.getIdentifier();
        stages.put(name, refId);
        store(item);
        return refId;
    }

    private String getGene(String fbgn) throws ObjectStoreException {
        String identifier = resolveGene(fbgn);
        if (StringUtils.isEmpty(identifier)) {
            return null;
        }
        if (genes.containsKey(identifier)) {
            return genes.get(identifier);
        }
        Item gene = createItem("Gene");
        gene.setAttribute("primaryIdentifier", identifier);
        gene.setReference("organism", organism);
        String refId = gene.getIdentifier();
        genes.put(identifier, refId);
        store(gene);
        return refId;
    }

    private Item getLibrary(String identifier) throws ObjectStoreException {
        if (libraries.containsKey(identifier)) {
            return libraries.get(identifier);
        }
        Item item = createItem("UberFlyLibrary");
        item.setAttribute("identifier", identifier);
        libraries.put(identifier, item);
        return item;
    }

    private String resolveGene(String fbgn) {
        // if resolver not exist, return the original id as primary id
        if (rslv == null || !rslv.hasTaxon(TAXON_FLY)) {
            return fbgn;
        }
        boolean currentGene = rslv.isPrimaryIdentifier(TAXON_FLY, fbgn);
        if (currentGene) {
            return fbgn;
        }
        return null;
    }


}
