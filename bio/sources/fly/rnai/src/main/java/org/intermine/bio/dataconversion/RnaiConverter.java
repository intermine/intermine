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

import java.io.BufferedReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;

/**
 * @author Julie Sullivan
 */
public class RnaiConverter extends BioFileConverter
{

    private static final String DATASET_TITLE = "GenomeRNAi data set";
    private static final String DATA_SOURCE_NAME = "German Cancer Research Center (DKFZ)";
    private static final Logger LOG = Logger.getLogger(RnaiConverter.class);

    private Map<String, String> genes = new HashMap<String, String>();
    private Map<String, String> publications = new HashMap<String, String>();
    private Map<String, String> screens = new HashMap<String, String>();
    private static final String TAXON_FLY = "7227";
    private static final String NCBI = "NCBI Entrez Gene identifiers";
    private Item screen;
    // RNAi people use "np" to signal that there is no data. There is actually a gene with Np
    // as it's symbol, so we have to test for this. All identifiers should start with FBgn
    private static final String NO_DATA = "np";

    protected IdResolver rslv;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public RnaiConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws Exception {
        if (rslv == null) {
            rslv = IdResolverService.getFlyIdResolver();
        }
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.startsWith("#")) {
                processScreen(line);
            } else {
                String[] cols = line.split("\t");
                if (cols.length < 7) {
                    continue;
                }
                processResult(cols);
            }
        }
    }

    private void processScreen(String line) throws ObjectStoreException {
        String[] bits = line.split("=");
        if (bits.length != 2) {
            return;
        }

        String key = bits[0];
        key = key.replace("#", "");
        String value = bits[1];
        if ("Stable ID".equals(key)) {
            screen = createItem("RNAiScreen");
            screen.setAttribute("identifier", value);
        } else if ("Screen Title".equals(key)) {
            screen.setAttribute("name", value);
        } else if ("Pubmed ID".equals(key)) {
            String refId = getPublication(value);
            screen.setReference("publication", refId);
        } else if ("Biosource".equals(key)) {
            screen.setAttribute("bioSourceType", value);
        } else if ("Biomodel".equals(key)) {
            screen.setAttribute("bioSourceName", value);
        } else if ("Assay".equals(key)) {
            screen.setAttribute("assay", value);
        } else if ("Method".equals(key)) {
            screen.setAttribute("method", value);
        } else if ("Reagent Type".equals(key)) {
            screen.setAttribute("reagentType", value);
        } else if ("Score Type".equals(key)) {
            screen.setAttribute("scoreType", value);
        } else if ("Cutoff".equals(key)) {
            screen.setAttribute("scoreCutoff", value);
        } else if (key.startsWith("Library")) {
            Attribute attr = screen.getAttribute("library");
            String library = null;
            if (attr != null) {
                library = attr.getValue();
            }
            if (library == null) {
                screen.setAttribute("library", key + ":  " + value);
            } else {
                screen.setAttribute("library", library + "; " + key + ":  " + value);
            }
        }
    }

    private void storeScreen(String identifier) throws ObjectStoreException {
        if (screens.get(identifier) == null) {
            store(screen);
            screens.put(identifier, screen.getIdentifier());
        }
    }

    private void processResult(String[] line) throws ObjectStoreException {
        String screenId = line[0];
        String geneRefId = null;
        String fbgn = line[2];
        String ncbi = line[1];

        if (fbgn.contains(",") || ncbi.contains(",")) {
            return;
        }

        if (StringUtils.isEmpty(fbgn) || NO_DATA.equals(fbgn)) {
            // some only have entrez IDs and no FBgns.  try both
            geneRefId = getGene(ncbi);
        } else {
            geneRefId = getGene(fbgn);
        }
        String reagentId = line[4];
        String score = line[5];
        String phenotype = line[6];
        String conditions = null;
        if (line.length > 7) {
            conditions = line[7];
        }
        storeScreen(screenId);

        Item result = createItem("RNAiResult");
        if (StringUtils.isNotEmpty(reagentId)) {
            result.setAttribute("reagentId", reagentId);
        }
        if (StringUtils.isNotEmpty(phenotype)) {
            result.setAttribute("phenotype", phenotype);
        }
        if (StringUtils.isNotEmpty(conditions)) {
            result.setAttribute("conditions", conditions);
        }
        if (StringUtils.isNotEmpty(score)) {
            result.setAttribute("score", score);
        }
        if (geneRefId != null) {
            result.setReference("gene", geneRefId);
            if (StringUtils.isNotEmpty(ncbi)) {
                createCrossReference(geneRefId, ncbi, NCBI, true);
            }
        }
        result.setReference("rnaiScreen", screen);
        store(result);
    }

    private String getPublication(String pubmedId) throws ObjectStoreException {
        String refId = publications.get(pubmedId);
        if (refId == null) {
            Item publication = createItem("Publication");
            publication.setAttribute("pubMedId", pubmedId);
            refId = publication.getIdentifier();
            publications.put(pubmedId, refId);
            store(publication);
        }
        return refId;
    }

    private String getGene(String identifier) throws ObjectStoreException {
        if (identifier == null) {
            throw new RuntimeException("geneSymbol can't be null");
        }

        if (rslv == null || !rslv.hasTaxon(TAXON_FLY)) {
            return null;
        }

        int resCount = rslv.countResolutions(TAXON_FLY, identifier);
        if (resCount != 1) {
            LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                    + identifier + " count: " + resCount + " FBgn: "
                    + rslv.resolveId(TAXON_FLY, identifier));
            return null;
        }
        String primaryIdentifier = rslv.resolveId(TAXON_FLY, identifier).iterator().next();
        String refId = genes.get(primaryIdentifier);
        if (refId == null) {
            Item item = createItem("Gene");
            item.setAttribute("primaryIdentifier", primaryIdentifier);
            item.setReference("organism", getOrganism(TAXON_FLY));
            refId = item.getIdentifier();
            store(item);
            genes.put(primaryIdentifier, refId);
        }
        return refId;
    }
}

