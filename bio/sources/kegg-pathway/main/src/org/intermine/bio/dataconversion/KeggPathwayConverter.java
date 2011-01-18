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
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;


/**
 * DataConverter to load Kegg Pathways and link them to Genes
 *
 * @author Xavier Watkins
 */
public class KeggPathwayConverter extends BioFileConverter
{
    protected static final Logger LOG = Logger.getLogger(KeggPathwayConverter.class);
    private static final String PROP_FILE = "kegg_config.properties";
    private Map<String, Item> geneItems = new HashMap<String, Item>();
    protected IdResolverFactory resolverFactory;
    private Map<String, String[]> config = new HashMap<String, String[]>();
    private Set<String> taxonIds = new HashSet<String>();

    protected Map<String, String> pathwayIdentifiers = new HashMap<String, String>();
    protected Map<String, Item> pathwaysNotStored = new HashMap<String, Item>();
    protected Map<String, String> organismIdentifiers = new HashMap<String, String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public KeggPathwayConverter(ItemWriter writer, Model model) {
        super(writer, model, "GenomeNet", "KEGG pathways data set");
        readConfig();
        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory("gene");
    }

    /**
     * Sets the list of taxonIds that should be imported
     *
     * @param taxonIds a space-separated list of taxonIds
     */
    public void setKeggOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtils.split(taxonIds, " ")));
        LOG.info("Setting list of organisms to " + this.taxonIds);
    }

    private void readConfig() {
        Properties props = new Properties();
        try {
            props.load(getClass().getClassLoader().getResourceAsStream(PROP_FILE));
        } catch (IOException e) {
            throw new RuntimeException("Problem loading properties '" + PROP_FILE + "'", e);
        }

        for (Map.Entry<Object, Object> entry: props.entrySet()) {

            String key = (String) entry.getKey();
            String value = ((String) entry.getValue()).trim();

            String[] attributes = key.split("\\.");
            if (attributes.length == 0) {
                throw new RuntimeException("Problem loading properties '" + PROP_FILE + "' on line "
                                           + key);
            }
            String organism = attributes[0];

            if (config.get(organism) == null) {
                String[] configs = new String[2];
                configs[1] = "primaryIdentifier";
                config.put(organism, configs);
            }
            if (attributes[1].equals("taxonId")) {
                config.get(organism)[0] = value;
            } else if (attributes[1].equals("identifier")) {
                config.get(organism)[1] = value;
            } else {
                String msg = "Problem processing properties '" + PROP_FILE + "' on line " + key
                    + ".  This line has not been processed.";
                LOG.error(msg);
            }
        }
    }

    /**
     * Read each line from flat file, create genes and synonyms.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        // there are two files
        // data is in format
        // CG | list of space separated map Ids
        // and
        // Map Id | name

        File currentFile = getCurrentFile();

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            Pattern filePattern = Pattern.compile("^(\\S+)_gene_map.*");
            Matcher matcher = filePattern.matcher(currentFile.getName());
            if (line.length <= 1 || line[0].startsWith("#")) {
                continue;
            }
            if (currentFile.getName().startsWith("map_title")) {
                String mapIdentifier = line[0];
                String mapName = line[1];
                Item pathway = pathwaysNotStored.remove(mapIdentifier);
                if (pathway == null) {
                    pathway = createItem("Pathway");
                    pathway.setAttribute("identifier", mapIdentifier);
                }
                pathway.setAttribute("name", mapName);
                pathwayIdentifiers.put(mapIdentifier, pathway.getIdentifier());
                store(pathway);
            } else if (matcher.find()) {
                String organism = matcher.group(1);
                String taxonId = config.get(organism)[0];
                // only process organisms set in project.xml
                if (!taxonIds.isEmpty() && !taxonIds.contains(taxonId)) {
                    continue;
                }
                if (taxonId != null && taxonId.length() != 0) {
                    String geneName = line[0];

                    // There are some strange ids for D. melanogaster, the rest start with Dmel_,
                    // ignore any D. melanogaster ids without Dmel_ and strip this off the rest
                    if ("7227".equals(taxonId) && !geneName.startsWith("Dmel_")) {
                        continue;
                    }

                    // We don't want Dmel_ prefix on D. melanogaster genes
                    if (geneName.startsWith("Dmel_")) {
                        geneName = geneName.substring(5);
                    }

                    String mapIdentifiers = line[1];
                    ReferenceList referenceList = new ReferenceList("pathways");
                    String [] mapArray = mapIdentifiers.split(" ");
                    for (int i = 0; i < mapArray.length; i++) {
                        String pathwayId = pathwayIdentifiers.get(mapArray[i]);
                        if (pathwayId == null) {
                            Item pathway = createItem("Pathway");
                            pathway.setAttribute("identifier", mapArray[i]);
                            pathwaysNotStored.put(mapArray[i], pathway);
                            pathwayId = pathway.getIdentifier();
                            pathwayIdentifiers.put(mapArray[i], pathwayId);
                        }
                        referenceList.addRefId(pathwayId);
                    }
                    getGene(geneName, organism, referenceList);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws ObjectStoreException {
        for (Item pathway : pathwaysNotStored.values()) {
            store(pathway);
        }
        pathwaysNotStored.clear();
    }

    private Map<String, String> geneCollectionCheck = new HashMap<String, String>();

    private Item getGene(String geneCG, String organism, ReferenceList referenceList)
        throws ObjectStoreException {
        String identifier = null;
        IdResolver resolver = resolverFactory.getIdResolver(false);
        String taxonId = config.get(organism)[0];
        if ("7227".equals(taxonId) && resolver != null) {
            int resCount = resolver.countResolutions(taxonId, geneCG);
            if (resCount != 1) {
                LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                         + geneCG + " count: " + resCount + " FBgn: "
                         + resolver.resolveId(taxonId, geneCG));
                return null;
            }
            identifier = resolver.resolveId(taxonId, geneCG).iterator().next();
        } else {
            identifier = geneCG;
        }

        Item gene = geneItems.get(identifier);
        if (gene == null) {
            gene = createItem("Gene");
            gene.setAttribute(config.get(organism)[1], identifier);
            String organismId = organismIdentifiers.get(taxonId);
            if (organismId == null) {
                Item organismItem = createItem("Organism");
                organismItem.setAttribute("taxonId", taxonId);
                organismId = organismItem.getIdentifier();
                store(organismItem);
                organismIdentifiers.put(taxonId, organismId);
            }
            gene.setReference("organism", organismId);
            gene.addCollection(referenceList);
            geneItems.put(identifier, gene);
            store(gene);
            geneCollectionCheck.put(identifier, referenceList.getRefIds().toString());
        } else {
            if (!referenceList.getRefIds().toString().equals(geneCollectionCheck.get(identifier))) {
                throw new IllegalArgumentException("Not storing Gene for a second time: " + geneCG
                        + ", " + identifier + ", but collections differ: "
                        + referenceList.getRefIds() + " versus "
                        + geneCollectionCheck.get(identifier));
            }
        }
        return gene;
    }
}
