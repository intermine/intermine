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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.dataconversion.BioFileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Item;

/**
 * @author Fengyuan Hu
 */
public class HomologeneConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(HomologeneConverter.class);

    private static final String DATASET_TITLE = "HomoloGene data set";
    private static final String DATA_SOURCE_NAME = "HomoloGene";

    private static final String PROP_FILE = "homologene_config.properties";
    private static final String DEFAULT_IDENTIFIER_FIELD = "symbol";
    private Set<String> taxonIds = new HashSet<String>();

    private static final String ORTHOLOGUE = "orthologue";
    private static final String PARALOGUE = "paralogue";

    private static final String EVIDENCE_CODE_ABBR = "AA";
    private static final String EVIDENCE_CODE_NAME = "Amino acid sequence comparison";

    private Properties props = new Properties();
    private Map<String, String> config = new HashMap<String, String>();
    private static String evidenceRefId = null;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public HomologeneConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
        readConfig();
    }

    /**
     * Sets the list of taxonIds that should be processed.  All genes will be loaded.
     *
     * @param taxonIds a space-separated list of taxonIds
     */
    public void setHomologeneOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtil.split(taxonIds, " ")));
        LOG.info("Setting list of organisms to " + taxonIds);
    }

    /**
     * Sets the list of taxonIds of homologues that should be processed.  These homologues will only
     * be processed if they are homologues for the organisms of interest.
     *
     * @param homologues a space-separated list of taxonIds
     */
//    public void setHomologeneHomologues(String homologues) {
//        this.homologues = new HashSet<String>(Arrays.asList(StringUtil.split(homologues, " ")));
//        LOG.info("Setting list of homologues to " + homologues);
//    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        /*
            homologene.data is a tab delimited file containing the following
            columns:

            1) HID (HomoloGene group id) - uid, http://www.ncbi.nlm.nih.gov/homologene?term=3[uid]
            2) Taxonomy ID
            3) Gene ID - NBCI Id
            4) Gene Symbol
            5) Protein gi
            6) Protein accession
        */

        String currentGroup = null;
        String previousGroup = null;

        // flat structure of homologue info
        List<List<String>> homologueList = new ArrayList<List<String>>();

        if (taxonIds.isEmpty()) {
            LOG.warn("homologene.organisms property not set in project XML file");
        }
//        if (homologues.isEmpty()) {
//            LOG.warn("homologene.homologues property not set in project XML file");
//        }

        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] bits = lineIter.next();
            if (bits.length < 6) {
                continue;
            }

            String groupId = bits[0];
            currentGroup = groupId;

            // at a different groupId, process previous homologue group
            if (previousGroup != null && !currentGroup.equals(previousGroup)) {
                if (homologueList.size() >= 2) {
                    processHomologues(homologueList, previousGroup);
                }
                homologueList = new ArrayList<List<String>>(); // reset the list
            }

            String taxonId = bits[1];
            if (!isValid(taxonId)) {
                // not an organism of interest, skip
                previousGroup = groupId;
                continue;
            }

            String ncbiId = bits[2];
            String symbol = bits[3];
            String gene = getGene(ncbiId, symbol, taxonId);

            List<String> recordList = new ArrayList<String>();
            recordList.add(taxonId);
            recordList.add(gene);
            homologueList.add(recordList);

            previousGroup = groupId;
        }
    }

    private void readConfig() {
        try {
            props.load(getClass().getClassLoader().getResourceAsStream(
                    PROP_FILE));
        } catch (IOException e) {
            throw new RuntimeException("Problem loading properties '"
                    + PROP_FILE + "'", e);
        }

        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey(); // e.g. 10090.identifier
            String value = ((String) entry.getValue()).trim(); // e.g. symbol

            String[] attributes = key.split("\\.");
            if (attributes.length == 0) {
                throw new RuntimeException("Problem loading properties '"
                        + PROP_FILE + "' on line " + key);
            }
            String taxonId = attributes[0];
            config.put(taxonId, value);
        }
    }

    private void processHomologues(List<List<String>> homologueList, String groupId)
            throws ObjectStoreException {
        int m = 2;
        Vector<List<String>> data = new Vector<List<String>>(homologueList);
        @SuppressWarnings("unchecked")
        Vector<Vector<List<String>>> combns = getAllCombinations(data, m);

        for (int i=0; i<combns.size(); i++) {
            List<String> record1 = combns.elementAt(i).elementAt(0);
            List<String> record2 = combns.elementAt(i).elementAt(1);

            String taxonId1 = record1.get(0);
            String gene1 = record1.get(1);

            String taxonId2 = record2.get(0);
            String gene2 = record2.get(1);

            Item homologue = createItem("Homologue");
            homologue.setReference("gene", gene1);
            homologue.setReference("homologue", gene2);
            homologue.addToCollection("evidence", getEvidence());
            homologue.setAttribute("type", taxonId1.equals(taxonId2)? PARALOGUE : ORTHOLOGUE);
            homologue.addToCollection(
                    "crossReferences",
                    createCrossReference(homologue.getIdentifier(), groupId,
                            DATA_SOURCE_NAME, true));
            store(homologue);
        }
    }

    // genes (in taxonIDs) are always processed
    // homologues are only processed if they are of an organism of interest
    private boolean isValid(String taxonId) {
        if (taxonIds.isEmpty()) {
            // no config so process everything
            return true;
        }
        if (taxonIds.contains(taxonId)) {
            // both are organisms of interest
            return true;
        }
//        if (homologues.isEmpty()) {
//            // only interested in homologues of interest, so at least one of
//            // this pair isn't valid
//            return false;
//        }
        // one gene is from an organism of interest
        // one homologue is from an organism we want
        if (taxonIds.contains(taxonId)) {
            return true;
        }
//        if (homologues.contains(taxonId)) {
//            return true;
//        }
        return false;
    }

    private String getGene(String ncbiId, String symbol, String taxonId)
            throws ObjectStoreException {
        String identifierType = config.get(taxonId);
        if (StringUtils.isEmpty(identifierType)) {
            identifierType = DEFAULT_IDENTIFIER_FIELD;
        }

        Item item = createItem("Gene");
        item.setAttribute(identifierType, symbol);
        item.setReference("organism", getOrganism(taxonId));
        store(item);

        return item.getIdentifier();
    }

    private String getEvidence() throws ObjectStoreException {
        if (evidenceRefId == null) {
            Item item = createItem("OrthologueEvidenceCode");
            item.setAttribute("abbreviation", EVIDENCE_CODE_ABBR);
            item.setAttribute("name", EVIDENCE_CODE_NAME);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
            String refId = item.getIdentifier();

            item = createItem("OrthologueEvidence");
            item.setReference("evidenceCode", refId);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }

            evidenceRefId = item.getIdentifier();
        }
        return evidenceRefId;
    }

    @SuppressWarnings({ "rawtypes" })
    private static Vector getAllCombinations(Vector data, int length)
    {
        Vector allCombinations = new Vector();
        Vector initialCombination = new Vector();
        combination(allCombinations, data, initialCombination, length);
        return allCombinations;
    }

    /**
     * combination algorithm, return all combinations of n from m
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void combination(Vector allCombinations, Vector data,
        Vector initialCombination, int length)
    {
        if(length == 1)
        {
            for(int i=0; i<data.size(); i++)
            {
                Vector newCombination = new Vector(initialCombination);
                newCombination.add(data.elementAt(i));
                allCombinations.add(newCombination);
            }
        }

        if(length > 1)
        {
            for(int i=0; i<data.size(); i++)
            {
                Vector newCombination = new Vector(initialCombination);
                newCombination.add(data.elementAt(i));

                Vector newData = new Vector(data);
                for(int j=0; j<=i; j++)
                    newData.remove(data.elementAt(j));

                combination(allCombinations, newData, newCombination, length - 1);
            }
        }
    }
}
