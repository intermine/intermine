package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    protected Item dataSource, dataSet;
    protected Map<String, String> keggOrganismToTaxonId = new HashMap<String, String>();
    protected HashMap pathwayMap = new HashMap();
    private Map<String, Item> geneItems = new HashMap<String, Item>();
    protected IdResolverFactory resolverFactory;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public KeggPathwayConverter(ItemWriter writer, Model model) {
        super(writer, model, "GenomeNet", "KEGG PATHWAY");

        // Drosophila melanogaster
        keggOrganismToTaxonId.put("dme", "7227");
        // Homo sapiens
        keggOrganismToTaxonId.put("hsa", "9609");
       // Plasmodium falciparum
        keggOrganismToTaxonId.put("pfa", "36329");
        // Drosophila pseudoobscura
//        keggOrganismToTaxonId.put("dpo", "7237");
        // Anopheles gambiae
//        keggOrganismToTaxonId.put("aga", "7165");
        // Apis mellifera
//        keggOrganismToTaxonId.put("dame", "7460");

        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory("gene");
    }


    /**
     * Read each line from flat file, create genes and synonyms.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        // there a two files
        // data is in format
        // CG | list of space separated map Id's
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
                Item pathway = getAndStoreItemOnce("Pathway", "identifier", mapIdentifier);
                pathway.setAttribute("name", mapName);
                store(pathway);
            } else if (matcher.find()) {
                LOG.error("MATCHED");
                String keggOrgName = matcher.group(1);
                String taxonId = keggOrganismToTaxonId.get(keggOrgName);

                if (taxonId != null && taxonId.length() != 0) {
                    String geneName = line[0];

                    // There are some strange ids for D. melanogaster, the rest start with Dmel_,
                    // ignore any D. melanogaster ids without Dmel_ and strip this off the rest
                    if (taxonId.equals("7227") && !geneName.startsWith("Dmel_")) {
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
                        referenceList.addRefId(getAndStoreItemOnce("Pathway", "identifier",
                                                                   mapArray[i]).getIdentifier());
                    }
                    getGene(geneName, taxonId, referenceList);
                }
            }
        }
    }

    private Item getGene(String geneCG, String taxonId, ReferenceList referenceList)
        throws ObjectStoreException {
        String identifier = null;
        IdResolver resolver = resolverFactory.getIdResolver(false);
        if (taxonId.equals("7227") && resolver != null) {
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
            Item organism = getAndStoreItemOnce("Organism", "taxonId", taxonId);
            gene = createItem("Gene");
            if (taxonId.equals("7227") && resolver != null) {
                gene.setAttribute("primaryIdentifier", identifier);
            } else if (taxonId.equals("9606")) {
                gene.setAttribute("ncbiGeneNumber", geneCG);
            } else {
                gene.setAttribute("primaryIdentifier", identifier);
            }
            gene.setReference("organism", organism);
            gene.addCollection(referenceList);
            geneItems.put(identifier, gene);
            store(gene);
        }
        return gene;
    }

    /**
     * Pick up the data location from the ant, the translator needs to open some more files.
     * @param srcdatadir location of the source data
     */
//    public void setSrcDataDir(String srcdatadir) {
//        this.dataLocation = srcdatadir;
//    }
}
