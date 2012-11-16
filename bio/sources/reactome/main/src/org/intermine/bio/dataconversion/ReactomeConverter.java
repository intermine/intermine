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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.PropertiesUtil;
import org.intermine.xml.full.Item;


/**
 *
 *
 * @author Julie Sullivan
 */
public class ReactomeConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(ReactomeConverter.class);
    private static final String PROP_FILE = "reactome_config.properties";
    private Set<String> taxonIds;
    private static final String DATASET_TITLE = "Reactome data set";
    private static final String DATA_SOURCE_NAME = "Reactome";
    private Map<String, Item> pathways = new HashMap<String, Item>();
    private Map<String, String> genes = new HashMap<String, String>();
    private Map<String, String> config = new HashMap<String, String>();
    private static final String DEFAULT_IDENTIFIER = "primaryIdentifier";
    protected IdResolver rslv;
    private static final String FLY = "7227";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public ReactomeConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
        readConfig();
    }

    /**
     * Sets the list of taxonIds that should be imported if using split input files.
     *
     * @param taxonIds a space-separated list of taxonIds
     */
    public void setReactomeOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtils.split(taxonIds, " ")));
    }

    private void readConfig() {
        Properties props = new Properties();
        try {
            props.load(getClass().getClassLoader().getResourceAsStream(PROP_FILE));
        } catch (IOException e) {
            throw new RuntimeException("Problem loading properties '" + PROP_FILE + "'", e);
        }
        Enumeration<?> propNames = props.propertyNames();
        while (propNames.hasMoreElements()) {
            String taxonId = (String) propNames.nextElement();
            taxonId = taxonId.substring(0, taxonId.indexOf("."));

            Properties taxonProps = PropertiesUtil.stripStart(taxonId,
                PropertiesUtil.getPropertiesStartingWith(taxonId, props));
            String identifier = taxonProps.getProperty("identifier");
            if (identifier == null) {
                throw new IllegalArgumentException("Unable to find geneAttribute property for "
                                                   + "taxon: " + taxonId + " in file: "
                                                   + PROP_FILE);
            }
            if (!("symbol".equals(identifier)
                            || "primaryIdentifier".equals(identifier)
                            || "secondaryIdentifier".equals(identifier)
                            || "primaryAccession".equals(identifier)
                            )) {
                throw new IllegalArgumentException("Invalid identifier value for taxon: "
                                                   + taxonId + " was: " + identifier);
            }
            config.put(taxonId, identifier);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        if (taxonIds == null || taxonIds.isEmpty()) {
            throw new IllegalArgumentException("No organism data provided for reactome");
        }

        if (rslv == null) {
            rslv = IdResolverService.getIdResolverByOrganism(FLY);
        }

        String fileName = getCurrentFile().getName();

        if ("pathways.tsv".equals(fileName)) {
            processPathwaysFile(reader);
        } else if (fileName.endsWith(".tsv")) {
            String taxonId = fileName.substring(0, fileName.indexOf("."));
            if (taxonId != null && taxonIds.contains(taxonId)) {
                if (!config.containsKey(taxonId)) {
                    config.put(taxonId, DEFAULT_IDENTIFIER);
                }
                processGeneFile(reader, taxonId);
            }
        }
    }

    private void processPathwaysFile(Reader reader) throws IOException {
        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        if (!lineIter.hasNext()) {
            return;
        }
        // file includes column headings
        lineIter.next();
        while (lineIter.hasNext()) {
            String[] line = lineIter.next();
            if (line.length != 3) {
                throw new RuntimeException("Invalid line, should be identifier\tpathway\tname but "
                        + " is '" + line + "' instead");
            }

//            String stableId = line[0];
            String identifier = line[1];
            String name = line[2];
            Item pathway = getPathway(identifier);
            pathway.setAttribute("name", name);
        }
    }

    private void processGeneFile(Reader reader, String taxonId)
        throws IOException, ObjectStoreException {
        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        if (!lineIter.hasNext()) {
            return;
        }
        // file includes column headings
        lineIter.next();
        while (lineIter.hasNext()) {
            String[] line = lineIter.next();
            if (line.length != 2) {
                throw new RuntimeException("Invalid line, should be pathway\tgene but is '"
                        + line + "' instead");
            }
            String geneIdentifier = line[1];
            String geneRefId = getGene(geneIdentifier, taxonId);
            if (geneRefId == null) {
                continue;
            }

            String pathwayIdentifier = line[0];
            Item pathway = getPathway(pathwayIdentifier);
            pathway.addToCollection("genes", geneRefId);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws ObjectStoreException {
        for (Item item : pathways.values()) {
            store(item);
        }
    }

    private Item getPathway(String pathwayId) {
        Item item = pathways.get(pathwayId);
        if (item == null) {
            item = createItem("Pathway");
            item.setAttribute("identifier", pathwayId);
            pathways.put(pathwayId, item);
        }
        return item;
    }

    private String getGene(String identifier, String taxonId)
        throws ObjectStoreException {
        String newIdentifier = identifier;
        if (FLY.equals(taxonId)) {
            newIdentifier = resolveGene(taxonId, identifier);
            if (newIdentifier == null) {
                return null;
            }
        }
        String refId = genes.get(newIdentifier);
        if (refId == null) {
            String identifierFieldName = config.get(taxonId);
            Item item = createItem("Gene");
            item.setAttribute(identifierFieldName, newIdentifier);
            item.setReference("organism", getOrganism(taxonId));
            store(item);
            refId = item.getIdentifier();
            genes.put(newIdentifier, refId);
        }
        return refId;
    }

    private String resolveGene(String taxonId, String identifier) {
        String id = identifier;
        if (FLY.equals(taxonId) && rslv != null && rslv.hasTaxon(FLY)) {
            int resCount = rslv.countResolutions(taxonId, identifier);
            if (resCount != 1) {
                LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                         + identifier + " count: " + resCount + " FBgn: "
                         + rslv.resolveId(taxonId, identifier));
                return null;
            }
            id = rslv.resolveId(taxonId, identifier).iterator().next();
        }
        return id;
    }
}
