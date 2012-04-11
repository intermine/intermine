package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.sql.Database;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

/**
 * Read Ensembl SNP data directly from MySQL variation database.
 * @author Richard Smith
 */
public class EnsemblSnpDbConverter extends BioDBConverter
{
    private static final String DATASET_TITLE = "Ensembl SNP data";
    private static final String DATA_SOURCE_NAME = "Ensembl";
    private final Map<String, Set<String>> pendingSnpConsequences =
        new HashMap<String, Set<String>>();
    private final Map<String, Integer> storedSnpIds = new HashMap<String, Integer>();
    private final Map<String, String> storedSnpItemIdentifiers = new HashMap<String, String>();
    private Set<String> snpSourceIds = null;

    // store a mapping from variation_id in ensembl database to stored SNP id in objectstore
    //private IntToIntMap variationIdToObjectId = new IntToIntMap();
    private Map<Integer, String> variationIdToItemIdentifier = new HashMap<Integer, String>();

    // default to human or take value set by parser
    Integer taxonId = null;
    private static final int PLANT = 3702;

    // There may be SNPs from multiple sources in the database, optionally restrict them
    Set<String> snpSources = new HashSet<String>();

    // Edit to restrict to loading fewer chromosomes
    private static final int MIN_CHROMOSOME = 1;

    private Map<String, String> sources = new HashMap<String, String>();
    private Map<String, String> states = new HashMap<String, String>();
    private Map<String, String> transcripts = new HashMap<String, String>();
    private Map<String, String> consequenceTypes = new HashMap<String, String>();

    private Map<Integer, Integer> strainIds = new HashMap<Integer, Integer>();
    private Map<Integer, String> popIdentifiers = new HashMap<Integer, String>();

    private static final Logger LOG = Logger.getLogger(EnsemblSnpDbConverter.class);
    /**
     * Construct a new EnsemblSnpDbConverter.
     * @param database the database to read from
     * @param model the Model used by the object store we will write to with the ItemWriter
     * @param writer an ItemWriter used to handle Items created
     */
    public EnsemblSnpDbConverter(Database database, Model model, ItemWriter writer) {
        super(database, model, writer, DATA_SOURCE_NAME, DATASET_TITLE);
    }


    /**
     * Set the organism to load
     * @param taxonId the organism to load
     */
    public void setOrganism(Integer taxonId) {
        this.taxonId = taxonId;
    }

    /**
     * Optionally restrict the sources of SNPs to load by entries in source table, e.g. to dbSNP.
     * @param sourceStr a space-separated list of sources
     */
    public void setSources(String sourceStr) {
        for (String source : sourceStr.split(" ")) {
            snpSources.add(source.trim());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void process() throws Exception {
        // a database has been initialised from properties starting with db.ensembl-snp-db
        if (this.taxonId == null) {
            throw new IllegalArgumentException("Must supply a taxon id for this variation database"
                    + " set the 'organism' property in project.xml");
        }

        Connection connection = getDatabase().getConnection();

        List<String> chrNames = new ArrayList<String>();
        for (int i = MIN_CHROMOSOME; i <= 22; i++) {
            chrNames.add("" + i);
        }
        chrNames.add("X");
        chrNames.add("Y");
        chrNames.add("MT");
        chrNames.add("Mt");
        chrNames.add("Pt");

        for (String chrName : chrNames) {
            LOG.info("Starting to process chromosome " + chrName);
            ResultSet res = queryVariation(connection, chrName);
            process(res, chrName);
            createSynonyms(connection, chrName);
        }
        storeFinalSnps();

        if (PLANT == this.taxonId.intValue()) {
            processGenotypes(connection);
            processPopulations(connection);
            processStrainPopulationReferences(connection);
        }
        connection.close();
    }

    private void storeFinalSnps() throws Exception {
        LOG.info("storeFinalSnps() pendingConsequences.size(): " + pendingSnpConsequences.size());
        LOG.info("storeFinalSnps() storedSnpIds.size(): " + storedSnpIds.size());
        for (String rsNumber : pendingSnpConsequences.keySet()) {
            Integer storedSnpId = storedSnpIds.get(rsNumber);
            Set<String> consequenceIdentifiers = pendingSnpConsequences.get(rsNumber);
            ReferenceList col =
                new ReferenceList("consequences", new ArrayList<String>(consequenceIdentifiers));
            store(col, storedSnpId);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void process(ResultSet res, String chrName) throws Exception {

        int counter = 0;
        int snpCounter = 0;
        int consequenceCounter = 0;
        Item currentSnp = null;
        Set<String> seenLocsForSnp = new HashSet<String>();
        String previousRsNumber = null;
        Boolean previousUniqueLocation = true;
        String previousTranscriptStableId = null;
        Set<String> consequenceIdentifiers = new HashSet<String>();
        boolean storeSnp = false;
        String currentSnpIdentifier = null;
        Integer currentVariationId = null;
        Map<String, Integer> nonTranscriptConsequences = new HashMap<String, Integer>();

        // This code is complicated because not all SNPs map to a unique location and often have
        // locations on multiple chromosomes - we're processing one chromosome at a time for faster
        // queries to mySQL.
        while (res.next()) {
            counter++;
            String rsNumber = res.getString("variation_name");
            boolean newSnp = rsNumber.equals(previousRsNumber) ? false : true;

            if (newSnp) {
                // starting a new SNP, store the one just finished - previousRsNumber

                Integer storedSnpId = storedSnpIds.get(previousRsNumber);

                // if we didn't get back a storedSnpId this was the first time we found this SNP,
                // so store it now
                if (storeSnp && storedSnpId == null) {
                    storedSnpId = store(currentSnp);
                    variationIdToItemIdentifier.put(currentVariationId, currentSnp.getIdentifier());
                    snpCounter++;
                }

                if (previousUniqueLocation) {
                    // the SNP we just stored has only one location so we won't see it again
                    storeSnpCollections(storedSnpId, consequenceIdentifiers);
                } else {
                    // we'll see this SNP multiple times so hang onto data
                    Set<String> snpConsequences = pendingSnpConsequences.get(previousRsNumber);
                    if (snpConsequences == null) {
                        snpConsequences = new HashSet<String>();
                        pendingSnpConsequences.put(previousRsNumber, snpConsequences);
                    }
                    snpConsequences.addAll(consequenceIdentifiers);
                    if (!storedSnpIds.containsKey(previousRsNumber)) {
                        storedSnpIds.put(previousRsNumber, storedSnpId);
                        storedSnpItemIdentifiers.put(previousRsNumber, currentSnp.getIdentifier());
                    }
                }

                // START NEW SNP
                previousRsNumber = rsNumber;
                seenLocsForSnp = new HashSet<String>();
                consequenceIdentifiers = new HashSet<String>();
                storeSnp = true;

                // map weight is the number of chromosome locations for the SNP, in practice there
                // are sometimes fewer locations than the map_weight indicates
                int mapWeight = res.getInt("map_weight");
                boolean uniqueLocation = (mapWeight == 1) ? true : false;
                previousUniqueLocation = uniqueLocation;

                // if not a unique location and we've seen the SNP before, don't store
                if (!uniqueLocation && pendingSnpConsequences.containsKey(rsNumber)) {
                    storeSnp = false;
                    currentSnpIdentifier = storedSnpItemIdentifiers.get(rsNumber);
                }

                if (storeSnp) {
                    currentSnp = createItem("SNP");
                    currentSnp.setAttribute("primaryIdentifier", rsNumber);
                    currentSnp.setReference("organism", getOrganismItem(taxonId));
                    currentSnp.setAttribute("uniqueLocation", "" + uniqueLocation);

                    currentSnpIdentifier = currentSnp.getIdentifier();
                    currentVariationId = res.getInt("variation_id");

                    String alleles = res.getString("allele_string");
                    if (!StringUtils.isBlank(alleles)) {
                        currentSnp.setAttribute("alleles", alleles);
                    }

                    String type = determineType(alleles);
                    if (type != null) {
                        currentSnp.setAttribute("type", type);
                    }

                    // CHROMOSOME AND LOCATION
                    // if SNP is mapped to multiple locations don't set chromosome and
                    // chromosomeLocation references
                    int start = res.getInt("seq_region_start");
                    int end = res.getInt("seq_region_end");
                    int chrStrand = res.getInt("seq_region_strand");

                    int chrStart = Math.min(start, end);
                    int chrEnd = Math.max(start, end);

                    Item loc = createItem("Location");
                    loc.setAttribute("start", "" + chrStart);
                    loc.setAttribute("end", "" + chrEnd);
                    loc.setAttribute("strand", "" + chrStrand);
                    loc.setReference("locatedOn", getChromosome(chrName, taxonId));
                    loc.setReference("feature", currentSnpIdentifier);
                    store(loc);

                    // if mapWeight is 1 there is only one chromosome location, so set shortcuts
                    if (uniqueLocation) {
                        currentSnp.setReference("chromosome", getChromosome(chrName, taxonId));
                        currentSnp.setReference("chromosomeLocation", loc);
                    }
                    seenLocsForSnp.add(chrName + ":" + chrStart);

                    // SOURCE
                    String source = res.getString("s.name");
                    currentSnp.setReference("source", getSourceIdentifier(source));

                    // VALIDATION STATES
                    String validationStatus = res.getString("validation_status");
                    List<String> validationStates = getValidationStateCollection(validationStatus);
                    if (!validationStates.isEmpty()) {
                        currentSnp.setCollection("validations", validationStates);
                    }
                }
            }

            int mapWeight = res.getInt("map_weight");
            boolean uniqueLocation = (mapWeight == 1) ? true : false;

            // we're on the same SNP but maybe a new location

            int start = res.getInt("seq_region_start");
            int end = res.getInt("seq_region_end");
            int strand = res.getInt("seq_region_strand");

            int chrStart = Math.min(start, end);
            int chrEnd = Math.max(start, end);

            if (currentSnp == null) {
                LOG.error("currentSNP is null.  vf.variation_feature_id: "
                        + res.getString("variation_feature_id") + " rsNumber: " + rsNumber
                        + " previousRsNumber: " + previousRsNumber + " storeSnp: " + storeSnp);
            }

            String chrLocStr = chrName + ":" + chrStart;
            if (!seenLocsForSnp.contains(chrLocStr)) {
                seenLocsForSnp.add(chrLocStr);

                // if this location is on a chromosome we want, store it
                Item loc = createItem("Location");
                loc.setAttribute("start", "" + chrStart);
                loc.setAttribute("end", "" + chrEnd);
                loc.setAttribute("strand", "" + strand);
                loc.setReference("feature", currentSnpIdentifier);
                loc.setReference("locatedOn", getChromosome(chrName, taxonId));
                store(loc);
            }

            // CONSEQUENCE TYPES
            // for SNPs without a uniqueLocation there will be different consequences at each one.
            // some consequences will need to stored at the end
            String transcriptStableId = res.getString("feature_stable_id");

            if (!StringUtils.isBlank(transcriptStableId)) {
            // In Ensembl 66, there are records with same transcript different allel_string
            // | variation_feature_id | feature_stable_id | allele_string | consequence_types     |
            // |             53025155 | ENST00000465814   | A/T           | nc_transcript_variant |
            // |             53025155 | ENST00000465814   | A/C           | nc_transcript_variant |
            // |             53025155 | ENST00000465814   | A/G           | nc_transcript_variant |
                boolean newConsequenceType =
                    transcriptStableId.equals(previousTranscriptStableId) ? false : true;
                if (newConsequenceType) {
                    previousTranscriptStableId = transcriptStableId;
                    String type = res.getString("tv.consequence_types");
                    // Seen one example so far where consequence type is an empty string
                    if (StringUtils.isBlank(type)) {
                        type = "UNKNOWN";
                    }

                    Item consequenceItem = createItem("Consequence");
                    consequenceItem.setAttribute("description", type);
                    for (String individualType : type.split(",")) {
                        consequenceItem.addToCollection("types",
                                    getConsequenceType(individualType.trim()));
                    }
                    setAttIfValue(consequenceItem, "peptideAlleles",
                            res.getString("pep_allele_string"));
                    setAttIfValue(consequenceItem, "siftPrediction",
                            res.getString("sift_prediction"));
                    setAttIfValue(consequenceItem, "siftScore", res.getString("sift_score"));
                    setAttIfValue(consequenceItem, "polyphenPrediction",
                            res.getString("polyphen_prediction"));
                    setAttIfValue(consequenceItem, "polyphenScore",
                            res.getString("polyphen_score"));

                    consequenceItem.setReference("transcript",
                            getTranscriptIdentifier(transcriptStableId));

                    consequenceIdentifiers.add(consequenceItem.getIdentifier());
                    store(consequenceItem);
                    consequenceCounter++;
                }
            } else {
                String variationConsequences = res.getString("vf.consequence_type");
                Integer consequenceCount = nonTranscriptConsequences.get(variationConsequences);

                if (consequenceCount == null) {
                    consequenceCount = new Integer(0);
                }

                nonTranscriptConsequences.put(variationConsequences,
                        new Integer(consequenceCount + 1));
            }

            if (counter % 100000 == 0) {
                LOG.info("Read " + counter + " rows total, stored " + snpCounter + " SNPs. for chr "
                        + chrName);
            }
        }

        if (currentSnp != null && storeSnp) {
            Integer storedSnpId = store(currentSnp);
            variationIdToItemIdentifier.put(currentVariationId, currentSnp.getIdentifier());
            if (!storedSnpIds.containsKey(storedSnpId)) {
                storeSnpCollections(storedSnpId, consequenceIdentifiers);
            }
        }
        LOG.info("Finished " + counter + " rows total, stored " + snpCounter + " SNPs for chr "
                + chrName);
        LOG.info("variationIdToItemIdentifier.size() = " + variationIdToItemIdentifier.size());
        LOG.info("Consequence count: " + consequenceCounter);
        LOG.info("Consequence types (consequence type to count) without transcript on Chromosome "
                + chrName + " : " + nonTranscriptConsequences);
    }

    private void setAttIfValue(Item item, String attName, String attValue) {
        if (!StringUtils.isBlank(attValue)) {
            item.setAttribute(attName, attValue);
        }
    }

    private String getConsequenceType(String type) throws ObjectStoreException {
        if (!consequenceTypes.containsKey(type)) {
            Item consequenceType = createItem("ConsequenceType");
            consequenceType.setAttribute("type", type);
            store(consequenceType);
            consequenceTypes.put(type, consequenceType.getIdentifier());
        }
        return consequenceTypes.get(type);
    }

    // This has to be called after process() called for the chromosome because it needs
    // variationIdToItemIdentifier to be populated.
    private void createSynonyms(Connection connection, String chrName)
        throws SQLException, ObjectStoreException {
        ResultSet res = querySynonyms(connection, chrName);

        int synonymCounter = 0;
        while (res.next()) {
            Integer variationId = res.getInt("variation_id");
            String synonym = res.getString("name");

            if (!StringUtils.isBlank(synonym)) {
                synonymCounter++;
                createSynonym(variationIdToItemIdentifier.get(variationId), synonym, true);
            }
        }
        LOG.info("Created " + synonymCounter + " synonyms for chr " + chrName);
    }

    private void processGenotypes(Connection connection) throws Exception {
        // query for strains
        ResultSet res = queryStrains(connection);
        int strainCounter = 0;
        while (res.next()) {
            Integer strainId = res.getInt("sample_id");
            String strainName = res.getString("name");

            Item strain = createItem("Strain");
            strain.setAttribute("name", strainName);
            Integer storedStrainId = store(strain);
            strainIds.put(strainId, storedStrainId);
            LOG.warn("Read strain: " + strainId);
            System.out.println("Read strain: " + strainId);

            // for each strain query and store genotypes
            processGenotypesForStrain(connection, strainId, strain.getIdentifier());

            //strainCounter++;
            //if (strainCounter >= 100) {
            //    break;
            //}
        }
    }

    private void processGenotypesForStrain(Connection connection, Integer strainId,
            String strainIdentifier) throws Exception {
        // One table contains SNPs and once contains bigger indels, etc.
        ResultSet res = queryGenotypesForStrainSingleBp(connection, strainId);
        createGeneotypesForStrain(res, strainId, strainIdentifier);

        res = queryGenotypesForStrainMultipleBp(connection, strainId);
        createGeneotypesForStrain(res, strainId, strainIdentifier);
    }

    private void createGeneotypesForStrain(ResultSet res, Integer strainId, String strainIdentifier)
        throws Exception {
        int snpReferenceCount = 0;
        int ignoredCount = 0;
        while (res.next()) {
            Integer variationId = res.getInt("variation_id");
            String allele1 = res.getString("allele_1");
            String allele2 = res.getString("allele_2");

            String snpItemIdentifier = variationIdToItemIdentifier.get(variationId);

            Item genotype = createItem("StrainGenotype");
            genotype.setAttribute("allele1", allele1);
            genotype.setAttribute("allele2", allele2);
            if (snpItemIdentifier != null) {
                genotype.setReference("snp", snpItemIdentifier);
                snpReferenceCount++;

            }
            else {
                ignoredCount++;
            }
            genotype.setReference("strain", strainIdentifier);

            store(genotype);
        }
        String message = "For strain " + strainId + " snp ref: " + snpReferenceCount + ", no ref: "
            + ignoredCount;
        LOG.info(message);
        System.out.println(message);
    }


    private void processPopulations(Connection connection) throws Exception {

        ResultSet res = queryPopulations(connection);
        while (res.next()) {
            Integer popId = res.getInt("sample_id");
            String popName = res.getString("name");
            String popDesc = res.getString("description");

            Item pop = createItem("Population");
            pop.setAttribute("name", popName);
            pop.setAttribute("description", popDesc);
            store(pop);
            popIdentifiers.put(popId, pop.getIdentifier());
            LOG.warn("Processing population: " + popId);
            System.out.println("Processing population: " + popId);

            // for each population query and store genotypes
            processAllelesForPopulation(connection, popId, pop.getIdentifier());
        }
    }

    private void processAllelesForPopulation(Connection connection, Integer popId,
            String popIdentifier) throws SQLException, ObjectStoreException {
        ResultSet res = queryAllelesForPopulation(connection, popId);

        int snpReferenceCount = 0;
        int ignoredCount = 0;
        int counter = 0;
        while (res.next()) {
            Integer variationId = res.getInt("variation_id");
            String allele = res.getString("allele");
            String frequency = res.getString("frequency");

            String snpItemIdentifier = variationIdToItemIdentifier.get(variationId);

            Item genotype = createItem("PopulationGenotype");
            genotype.setAttribute("allele", allele);
            genotype.setAttribute("frequency", frequency);
            if (snpItemIdentifier != null) {
                genotype.setReference("snp", snpItemIdentifier);
                snpReferenceCount++;
            }
            else {
                ignoredCount++;
            }
            genotype.setReference("population", popIdentifier);

            store(genotype);

            counter++;
            if (counter % 100000 == 0) {
                String message = "Read " + counter + " alleles.";
                LOG.info(message);
                System.out.println(message);
            }

        }
        String message = "For population " + popId + " snp ref: " + snpReferenceCount + ", no ref: "
            + ignoredCount;
        LOG.info(message);
        System.out.println(message);
    }

    private void processStrainPopulationReferences(Connection connection)
        throws SQLException, ObjectStoreException {
        ResultSet res = queryStrainPopulationReferences(connection);

        Map<Integer, List<String>> strainToPopulation = new HashMap<Integer, List<String>>();
        while (res.next()) {
            Integer strainId = res.getInt("individual_sample_id");
            Integer popId = res.getInt("population_sample_id");

            List<String> strainPopIdentifiers = strainToPopulation.get(strainId);
            if (strainPopIdentifiers == null) {
                strainPopIdentifiers = new ArrayList<String>();
                strainToPopulation.put(strainId, strainPopIdentifiers);
            }
            strainPopIdentifiers.add(popIdentifiers.get(popId));
        }
        for (Integer strainId : strainToPopulation.keySet()) {
            ReferenceList populations = new ReferenceList("populations",
                    strainToPopulation.get(strainId));
            if (strainIds.containsKey(strainId)) {
                store(populations, strainIds.get(strainId));
            }
        }
    }

    private ResultSet queryStrainPopulationReferences(Connection connection)
        throws SQLException {
        String query = "SELECT individual_sample_id, population_sample_id"
            + " FROM individual_population";
        LOG.warn(query);
        System.out.println(query);

        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    private ResultSet queryGenotypesForStrainSingleBp(Connection connection, Integer strainId)
        throws SQLException {
        String query = "SELECT variation_id, allele_1, allele_2"
            + " FROM tmp_individual_genotype_single_bp"
            + " WHERE sample_id = " + strainId;
        LOG.warn(query);
        System.out.println(query);

        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    private ResultSet queryGenotypesForStrainMultipleBp(Connection connection, Integer strainId)
        throws SQLException {
        String query = "SELECT variation_id, allele_1, allele_2"
            + " FROM individual_genotype_multiple_bp"
            + " WHERE sample_id = " + strainId;
        LOG.warn(query);
        System.out.println(query);

        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    private ResultSet queryAllelesForPopulation(Connection connection, Integer popId)
    throws SQLException {
        String query = "SELECT a.variation_id, a.sample_id, ac.allele, a.frequency"
            + " FROM allele a, allele_code ac"
            + " WHERE a.sample_id = " + popId
            + " AND a.allele_code_id = ac.allele_code_id";
        LOG.warn(query);
        System.out.println(query);

        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    private void storeSnpCollections(Integer storedSnpId, Set<String> consequenceIdentifiers)
        throws ObjectStoreException {
        if (!consequenceIdentifiers.isEmpty()) {
            ReferenceList col =
                new ReferenceList("consequences", new ArrayList<String>(consequenceIdentifiers));
            store(col, storedSnpId);
        }
    }

    /**
     * Given an allele string read from the database determine the type of variation, e.g. snp,
     * in-del, etc.  This is a re-implementation of code from the Ensembl perl API, see:
     * http://www.ensembl.org/info/docs/Doxygen/variation-api/
     *    classBio_1_1EnsEMBL_1_1Variation_1_1Utils_1_1Sequence.html
     * @param alleleStr the alleles to determine the type for
     * @return a variation class or null if none can be determined
     */
    protected String determineType(String alleleStr) {
        String type = null;

        final String VALID_BASES = "ATUGCYRSWKMBDHVN";

        alleleStr = alleleStr.toUpperCase();
        if (!StringUtils.isBlank(alleleStr)) {
            // snp if e.g. A/C or A|C
            if (alleleStr.matches("^[" + VALID_BASES + "]([\\/\\|\\\\][" + VALID_BASES + "])+$")) {
                type = "snp";
            } else if ("CNV".equals(alleleStr)) {
                type = alleleStr.toLowerCase();
            } else if ("CNV_PROBE".equals(alleleStr)) {
                type = "cnv probe";
            } else if ("HGMD_MUTATION".equals(alleleStr)) {
                type = alleleStr.toLowerCase();
            } else {
                String[] alleles = alleleStr.split("[\\|\\/\\\\]");

                if (alleles.length == 1) {
                    type = "het";
                } else if (alleles.length == 2) {
                    if ((StringUtils.containsOnly(alleles[0],
                            VALID_BASES) && "-".equals(alleles[1]))
                            || (StringUtils.containsOnly(alleles[1], VALID_BASES)
                                    && "-".equals(alleles[0]))) {
                        type = "in-del";
                    } else if (containsOneOf(alleles[0], "LARGE", "INS", "DEL")
                            || containsOneOf(alleles[1], "LARGE", "INS", "DEL")) {
                        type = "named";
                    } else if ((StringUtils.containsOnly(alleles[0], VALID_BASES)
                            && alleles[0].length() > 1)
                            || (StringUtils.containsOnly(alleles[1], VALID_BASES)
                                    && alleles[1].length() > 1)) {
                        // AA/GC 2 alleles
                        type = "substitution";
                    }
                } else if (alleles.length > 2) {
                    if (containsDigit(alleles[0])) {
                        type = "microsat";
                    } else if (anyContainChar(alleles, "-")) {
                        type = "mixed";
                    }
                }
                if (type == null) {
                    LOG.warn("Failed to work out allele type for: " + alleleStr);
                }
            }
        }

        return type;
    }

    private String getSourceIdentifier(String name) throws ObjectStoreException {
        String sourceIdentifier = sources.get(name);
        if (sourceIdentifier == null) {
            Item source = createItem("Source");
            source.setAttribute("name", name);
            store(source);
            sourceIdentifier = source.getIdentifier();
            sources.put(name, sourceIdentifier);
        }
        return sourceIdentifier;
    }

    private String getTranscriptIdentifier(String transcriptStableId) throws ObjectStoreException {
        String transcriptIdentifier = transcripts.get(transcriptStableId);
        if (transcriptIdentifier == null) {
            Item transcript = createItem("Transcript");
            transcript.setAttribute("primaryIdentifier", transcriptStableId);
            store(transcript);
            transcriptIdentifier = transcript.getIdentifier();
            transcripts.put(transcriptStableId, transcriptIdentifier);
        }
        return transcriptIdentifier;
    }

    private List<String> getValidationStateCollection(String input) throws ObjectStoreException {
        List<String> stateIdentifiers = new ArrayList<String>();
        if (!StringUtils.isBlank(input)) {
            for (String state : input.split(",")) {
                stateIdentifiers.add(getStateIdentifier(state));
            }
        }
        return stateIdentifiers;
    }

    private String getStateIdentifier(String name) throws ObjectStoreException {
        String stateIdentifier = states.get(name);
        if (stateIdentifier == null) {
            Item state = createItem("ValidationState");
            state.setAttribute("name", name);
            store(state);
            stateIdentifier = state.getIdentifier();
            states.put(name, stateIdentifier);
        }
        return stateIdentifier;
    }

    private ResultSet queryVariation(Connection connection, String chrName)
        throws SQLException {

        String query = "SELECT vf.variation_feature_id, vf.variation_name, vf.variation_id,"
            + " vf.allele_string, sr.name,"
            + " vf.map_weight, vf.seq_region_start, vf.seq_region_end, vf.seq_region_strand, "
            + " s.name,"
            + " vf.validation_status,"
            + " vf.consequence_type,"
            + " tv.cdna_start,tv.consequence_types,tv.pep_allele_string,tv.feature_stable_id,"
            + " tv.sift_prediction, tv.sift_score, tv.polyphen_prediction, tv.polyphen_score"
            //+ " tv.sift_prediction, tv.polyphen_prediction"
            + " FROM seq_region sr, source s, variation_feature vf "
            + " LEFT JOIN (transcript_variation tv)"
            + " ON (vf.variation_feature_id = tv.variation_feature_id"
            + "    AND tv.consequence_types NOT IN ('5KB_downstream_variant',"
            + "    '5KB_upstream_variant','500B_downstream_variant','2KB_upstream_variant'))"
            + " WHERE vf.seq_region_id = sr.seq_region_id"
            + " AND vf.source_id = s.source_id"
            + " AND sr.name = '" + chrName + "'"
            + " ORDER BY vf.variation_id";

        LOG.warn(query);
        System.out.println(query);

        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    private ResultSet querySynonyms(Connection connection, String chrName)
        throws SQLException {
        String query = "SELECT vs.variation_id, vs.name"
            + " FROM variation_synonym vs, variation_feature vf, seq_region sr"
            + " WHERE vs.variation_id = vf.variation_id"
            + " AND vf.seq_region_id = sr.seq_region_id"
            + " AND sr.name = '" + chrName + "'"
            + " AND vs.source_id IN (" + StringUtil.join(getSnpSourceIds(connection), ",") + ")"
            + " ORDER BY vs.variation_id";

        LOG.warn(query);
        System.out.println(query);

        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    private Set<String> getSnpSourceIds(Connection connection) throws SQLException {
        if (snpSourceIds == null) {
            snpSourceIds = new HashSet<String>();
            String sql = "SELECT source_id FROM source";
            if (snpSources != null && !snpSources.isEmpty()) {
                sql += " WHERE name IN (" + makeInList(snpSources) + ")";
            }
            Statement stmt = connection.createStatement();
            ResultSet res = stmt.executeQuery(sql);
            while (res.next()) {
                snpSourceIds.add(res.getString("source_id"));
            }
            if (snpSourceIds.isEmpty()) {
                throw new RuntimeException("Failed to retrieve source_ids for dbSNP source");
            }
        }
        return snpSourceIds;
    }

    private String makeInList(Collection<String> strings) {
        Set<String> quoted = new HashSet<String>();
        for (String s : strings) {
            quoted.add("\"" + s + "\"");
        }
        return StringUtil.join(quoted, ",");
    }

    private ResultSet queryStrains(Connection connection)
        throws SQLException {

        String query = "SELECT s.sample_id, s.name"
            + " FROM sample s, individual i"
            + " WHERE i.sample_id = s.sample_id";
        LOG.warn(query);
        System.out.println(query);

        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    private ResultSet queryPopulations(Connection connection)
    throws SQLException {

        String query = "SELECT s.sample_id, s.name, s.description"
            + " FROM sample s, population p"
            + " WHERE p.sample_id = s.sample_id";
        LOG.warn(query);
        System.out.println(query);

        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public String getDataSetTitle(int taxonId) {
        return DATASET_TITLE;
    }

    private boolean containsOneOf(String target, String... substrings) {
        for (String substring : substrings) {
            if (target.contains(substring)) {
                return true;
            }
        }
        return false;
    }

    private boolean anyContainChar(String[] targets, String substring) {
        for (String target : targets) {
            if (target.contains(substring)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsDigit(String target) {
        for (int i = 0; i < target.length(); i++) {
            if (Character.isDigit(target.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
