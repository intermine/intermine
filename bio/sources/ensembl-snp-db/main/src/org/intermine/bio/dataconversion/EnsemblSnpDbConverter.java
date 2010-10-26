package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2010 FlyMine
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
import org.intermine.xml.full.Item;

/**
 * Read Ensembl SNP data directly from MySQL variarion database.
 * @author Richard Smith
 */
public class EnsemblSnpDbConverter extends BioDBConverter
{
    private static final String DATASET_TITLE = "Ensembl SNP data";
    private static final String DATA_SOURCE_NAME = "Ensembl";

    private Map<String, String> sources = new HashMap<String, String>();
    private Map<String, String> states = new HashMap<String, String>();
    private Map<String, String> transcripts = new HashMap<String, String>();
    private Map<String, String> noTranscriptConsequences = new HashMap<String, String>();

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
     * {@inheritDoc}
     */
    public void process() throws Exception {
        // a database has been initialised from properties starting with db.ensembl-snp-db

        Connection connection = getDatabase().getConnection();
        Set<String> chrNames = new HashSet<String>();

        for (int i = 1; i <= 22; i++) {
            chrNames.add("" + i);
        }
        chrNames.add("X");
        chrNames.add("Y");

        for (String chrName : chrNames) {
            processChromosome(connection, chrName);
        }
        connection.close();
    }

    /**
     * {@inheritDoc}
     */
    public void processChromosome(Connection connection, String chrName) throws Exception {
        // a database has been initialised from properties starting with db.ensembl-snp-db

        System.out.println("Starting to process chromosome " + chrName);
        LOG.info("Starting to process chromosome " + chrName);

        // TODO move this to a parser arguement
        int taxonId = 9606;

        int counter = 0;
        int snpCounter = 0;
        Item currentSnp = null;
        String currentRsNumber = null;
        Set<String> consequenceIdentifiers = new HashSet<String>();
        ResultSet res = queryVariation(connection, chrName);
        while (res.next()) {
            counter++;
            String rsNumber = res.getString("variation_name");

            if (!rsNumber.equals(currentRsNumber)) {
                if (currentSnp != null) {
                    storeSnp(currentSnp, consequenceIdentifiers);
                }
                currentRsNumber = rsNumber;
                consequenceIdentifiers = new HashSet<String>();
                //String chrName = res.getString("sr.name");
                String alleles = res.getString("allele_string");

                currentSnp = createItem("SNP");
                currentSnp.setAttribute("primaryIdentifier", rsNumber);
                currentSnp.setAttribute("alleles", alleles);
                currentSnp.setReference("organism", getOrganismItem(taxonId));

                Item chr = getChromosome(chrName, taxonId);
                currentSnp.setReference("chromosome", chr);

                // LOCATION
                int chrStart = res.getInt("seq_region_start");
                int chrEnd = res.getInt("seq_region_end");
                String chrStrand = "" + res.getInt("seq_region_strand");
                Item loc = createItem("Location");
                loc.setAttribute("start", "" + Math.min(chrStart, chrEnd));
                loc.setAttribute("end", "" + Math.max(chrStart, chrEnd));
                loc.setAttribute("strand", chrStrand);
                loc.setReference("locatedOn", chr);
                loc.setReference("feature", currentSnp);
                store(loc);

                currentSnp.setReference("chromosomeLocation", loc);

                // SOURCE
                String source = res.getString("s.name");
                currentSnp.setReference("source", getSourceIdentifier(source));

                // VALIDATION STATES
                String validationStatus = res.getString("validation_status");
                List<String> validationStates = getValidationStateCollection(validationStatus);
                if (!validationStates.isEmpty()) {
                    currentSnp.setCollection("validations", validationStates);
                }

                snpCounter++;
                if (snpCounter % 1000 == 0) {
                    LOG.info("Read " + snpCounter + " SNPs, " + counter + " rows total.");
                }
            }
            // CONSEQUENCE TYPES
            String cdnaStart = res.getString("cdna_start");
            if (StringUtils.isBlank(cdnaStart)) {
                String typeStr = res.getString("vf.consequence_type");
                for (String type : typeStr.split(",")) {
                    consequenceIdentifiers.add(getConsequenceIdentifier(type));
                }
            } else {
                String type = res.getString("tv.consequence_type");
                String peptideAlleles = res.getString("peptide_allele_string");
                String transcriptStableId = res.getString("transcript_stable_id");

                Item consequenceItem = createItem("Consequence");
                consequenceItem.setAttribute("type", type);
                if (!StringUtils.isBlank(peptideAlleles)) {
                    consequenceItem.setAttribute("peptideAlleles", peptideAlleles);
                }
                if (!StringUtils.isBlank(transcriptStableId)) {
                    consequenceItem.setReference("transcript",
                            getTranscriptIdentifier(transcriptStableId));
                }
                consequenceIdentifiers.add(consequenceItem.getIdentifier());
                store(consequenceItem);
            }

        }
        if (currentSnp != null) {
            storeSnp(currentSnp, consequenceIdentifiers);
        }

        LOG.info("Finished chromosome " + chrName + " stored " + snpCounter + " SNPs.");
    }

    private void storeSnp(Item snp, Set<String> consequenceIdentifiers)
        throws ObjectStoreException {
        if (!consequenceIdentifiers.isEmpty()) {
            snp.setCollection("consequences", new ArrayList<String>(consequenceIdentifiers));
        }
        store(snp);
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
            Item source = createItem("Transcript");
            source.setAttribute("primaryIdentifier", transcriptStableId);
            store(source);
            transcriptIdentifier = source.getIdentifier();
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

    private String getConsequenceIdentifier(String type) throws ObjectStoreException {
        String consequenceIdentifier = noTranscriptConsequences.get(type);
        if (consequenceIdentifier == null) {
            Item consequence = createItem("Consequence");
            consequence.setAttribute("type", type);
            store(consequence);
            consequenceIdentifier = consequence.getIdentifier();
            noTranscriptConsequences.put(type, consequenceIdentifier);
        }
        return consequenceIdentifier;
    }

    private ResultSet queryVariation(Connection connection, String chrName) throws SQLException {
        String query = "SELECT vf.variation_name, vf.allele_string, "
            + " sr.name, vf.seq_region_start, vf.seq_region_end, vf.seq_region_strand, "
            + " s.name,"
            + " vf.validation_status,"
            + " vf.consequence_type,"
            + " tv.cdna_start, tv.consequence_type,tv.peptide_allele_string,tv.transcript_stable_id"
            + " FROM seq_region sr, source s, variation_feature vf "
            + " LEFT JOIN (transcript_variation tv)"
            + " ON (vf.variation_feature_id = tv.variation_feature_id"
            + " AND tv.peptide_allele_string is not null)"
            + " WHERE vf.seq_region_id = sr.seq_region_id"
            + " AND vf.source_id = s.source_id"
            + " AND sr.name = '" + chrName + "'";

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
}
