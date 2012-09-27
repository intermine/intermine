package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import org.intermine.bio.util.OrganismData;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Item;

/**
 * Orthodb data Converter
 *
 * @author Fengyuan Hu
 */
public class OrthodbConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(OrthodbConverter.class);

    private static final String DATASET_TITLE = "OrthoDB data set";
    private static final String DATA_SOURCE_NAME = "OrthoDB";

    private static final String PROP_FILE = "orthodb_config.properties";
    private static final String DEFAULT_IDENTIFIER_FIELD = "primaryIdentifier";

    private Set<String> taxonIds = new HashSet<String>();
    private Set<String> homologues = new HashSet<String>();

    private static final String ORTHOLOGUE = "orthologue";
    private static final String PARALOGUE = "paralogue";

    private static final String EVIDENCE_CODE_ABBR = "AA";
    private static final String EVIDENCE_CODE_NAME = "Amino acid sequence comparison";

    private Properties props = new Properties();
    private Map<String, String> config = new HashMap<String, String>();
    private static String evidenceRefId = null;
    private OrganismRepository or;

    private Map<String, String> identifiersToGenes = new HashMap<String, String>();

    private IdResolver rslv;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public OrthodbConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
        readConfig();
        or = OrganismRepository.getOrganismRepository();
    }

    /**
     * Sets the list of taxonIds that should be processed.  All genes will be loaded.
     *
     * @param taxonIds a space-separated list of taxonIds
     */
    public void setOrthodbOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtil.split(taxonIds, " ")));
        LOG.info("Setting list of organisms to " + taxonIds);
    }

    /**
     * Sets the list of taxonIds of homologues that should be processed.  These homologues will only
     * be processed if they are homologues for the organisms of interest.
     *
     * @param homologues a space-separated list of taxonIds
     */
    public void setOrthodbHomologues(String homologues) {
        this.homologues = new HashSet<String>(Arrays.asList(StringUtil.split(homologues, " ")));
        LOG.info("Setting list of homologues to " + homologues);
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        /*
            OrthoDB5_ALL_tabtext is a tab delimited file containing the following
            columns:

            1) Level
            2) OG_ID - OrthoDB group id
            3) Protein_ID
            4) Gene_ID, e.g. FBgn0162343(fly), ENSMUSG00000027919(mouse)
            5) Organism - full name
            6) UniProt_ACC
            7) UniProt_Description
        */

        String currentGroup = null;
        String previousGroup = null;

        // flat structure of homologue info
        List<List<String>> homologueList = new ArrayList<List<String>>();

        if (taxonIds.isEmpty()) {
            LOG.warn("orthodb.organisms property not set in project XML file");
        }
        if (homologues.isEmpty()) {
            LOG.warn("orthodb.homologues property not set in project XML file");
        }

        Set<String> allTaxonIds = new HashSet<String>() {
            private static final long serialVersionUID = 1L;
            {
                addAll(taxonIds);
                addAll(homologues);
            }
        };
        rslv = IdResolverService.getIdResolverByOrganism(allTaxonIds);

        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] bits = lineIter.next();
            if (bits.length < 7) {
                continue;
            }

            // Level is an integer, ignore the title line
            if (!bits[0].matches("^\\d*$")) {
                continue;
            }

            String groupId = bits[1];
            currentGroup = groupId;

            // at a different groupId, process previous homologue group
            if (previousGroup != null && !currentGroup.equals(previousGroup)) {
                if (homologueList.size() >= 2) {
                    processHomologues(homologueList, previousGroup);
                }
                homologueList = new ArrayList<List<String>>(); // reset the list
            }

            String taxonId = getTaxon(bits[4]);
            if (!isValid(taxonId) || taxonId == null) {
                // not an organism of interest, skip
                previousGroup = groupId;
                continue;
            }

            String geneId = bits[3];
            String gene = getGene(geneId, taxonId);

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

            if (gene1 == null || gene2 == null) {
                continue;
            }

            // Create both way relations
            createHomologue(gene1, taxonId1, gene2, taxonId2, groupId);
            createHomologue(gene2, taxonId2, gene1, taxonId1, groupId);
        }
    }

    private void createHomologue(String gene1, String taxonId1, String gene2,
            String taxonId2, String groupId) throws ObjectStoreException {
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
        if (homologues.isEmpty()) {
            // only interested in homologues of interest, so at least one of
            // this pair isn't valid
            return false;
        }
        // one gene is from an organism of interest
        // one homologue is from an organism we want
        if (taxonIds.contains(taxonId)) {
            return true;
        }
        if (homologues.contains(taxonId)) {
            return true;
        }
        return false;
    }

    private String getGene(String geneId, String taxonId)
            throws ObjectStoreException {
        String identifierType = config.get(taxonId);
        if (StringUtils.isEmpty(identifierType)) {
            identifierType = DEFAULT_IDENTIFIER_FIELD;
        }

        geneId = resolveGene(taxonId, geneId);
        if (geneId == null) {
            return null;
        }

        String refId = identifiersToGenes.get(geneId);
        if (refId == null) {
            Item item = createItem("Gene");
            item.setAttribute(identifierType, geneId);

            {
            /**
             * !!! Ugly Code Ahead
             * OrthoDB use secondaryIdentifier for worm gene, in wormbase-identifiers, gene
             * WBGene00006756 (ZC416.8, unc-17) and WBGene00000481 (ZC416.8, cha-1) have the same
             * secondaryIdentifier ZC416.8, but OrthoDB points to cha-1 in term of the protein id
             * ZC416.8b. To fix the issue, set symbol as another key to filter the duplication.
             * Same for Y105E8A.7 and B0564.1
             *
             * For a better fix, load uniprot data, set key to secondaryIdentifier, protein and
             * organism. But MasterMine tries to not load protein data.
             */

                if ("ZC416.8".equals(geneId)) {
                    item.removeAttribute(identifierType);
                    item.setAttribute("symbol", "cha-1");
                }

                if ("Y105E8A.7".equals(geneId)) {
                    item.removeAttribute(identifierType);
                    item.setAttribute("symbol", "lev-10");
                }

                if ("B0564.1".equals(geneId)) {
                    item.removeAttribute(identifierType);
                    item.setAttribute("symbol", "exos-4.1");
                }
            }

            item.setReference("organism", getOrganism(taxonId));
            refId = item.getIdentifier();
            identifiersToGenes.put(geneId, refId);
            store(item);
        }
        return refId;
    }

    private String getTaxon(String name) {
        OrganismData od = or.getOrganismDataByFullName(name);
        if (od == null) {
            // Not throw BuildException
            // TODO add more taxons to organism_config.properties?
            LOG.warn("No data for `" + name + "`.  Please add to repository.");
            return null;
//            throw new BuildException("No data for `" + name + "`.  Please add to repository.");
        }

        int taxonId = od.getTaxonId();
        String taxonIdString = String.valueOf(taxonId);
        return taxonIdString;
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

    private String resolveGene(String taxonId, String identifier) {

        if (rslv == null) {
            // no id resolver available, so return the original identifier
            return identifier;
        }
        int resCount = rslv.countResolutions(taxonId, identifier);
        if (resCount != 1) {
            LOG.info("RESOLVER: failed to resolve fly gene to one identifier, ignoring gene: "
                     + identifier + " count: " + resCount + " FBgn: "
                     + rslv.resolveId(taxonId, identifier));
            return null;
        }
        return rslv.resolveId(taxonId, identifier).iterator().next();
    }
}
