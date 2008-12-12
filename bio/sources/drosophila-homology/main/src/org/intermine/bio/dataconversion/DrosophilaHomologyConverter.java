package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.bio.util.OrganismData;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * Parse Drosophila 12 genome homology file and create pairwise Homologue objects.
 *
 * @author Richard Smith
 */
public class DrosophilaHomologyConverter extends BioFileConverter
{
    private Item pub;
    private Map<String, String> genes = new HashMap();
    private Map<String, String> fbgns = new HashMap();
    private Map<String, String> organisms = new HashMap();
    private OrganismRepository or = null;
    protected IdResolverFactory resolverFactory;
    private IdResolver resolver = null;

    protected static final Logger LOG = Logger.getLogger(DrosophilaHomologyConverter.class);

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     */
    public DrosophilaHomologyConverter(ItemWriter writer, Model model)
        throws ObjectStoreException, MetaDataException {
        super(writer, model, "FlyBase", "Drosophila 12 Genomes Consortium homology");

        pub = createItem("Publication");
        pub.setAttribute("pubMedId", "17994087");
        store(pub);

        or = OrganismRepository.getOrganismRepository();

        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory("gene");
    }


    /**
     * Read each line from flat file, create genes and synonyms.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        // read the header line (a comment) and extract 12 species abbreviations
        String[] species = new String[12];
        BufferedReader br = new BufferedReader(reader);
        String[] header = br.readLine().split("\t");
        System.arraycopy(header, 2, species, 0, 12);

        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(br);

        while (lineIter.hasNext()) {
            // key is a 12 element string: 1 = ortholog, n = potential paralogue, 0 = not in cluster
            // the values correspond to gene ids for each species in columns 2-14.

            // for each row: read gene ids into array, read roles into array
            // for all species - i
            //    for all species - j
            //        if (i == j)
            //            continue
            //        if (key[i] == 0 or key[j] == 0)
            //            no nothing
            //        else if (key[i] == 1 && key[j] == 1)
            //            create an orthologue
            //        else if (key[i] == n or key[j] == n)
            //            create a paralogue

            String[] line = (String[]) lineIter.next();

            String clusterNum = line[0];
            String keyStr = line[1];

            char[] key = keyStr.toCharArray();

            for (int i = 0; i < species.length; i++) {
                for (int j = 0; j < species.length; j++) {
                    // same species, do nothing
                    if (i == j) {
                        continue;
                    }
                    String type = null;
                    if ((key[i] == '0') || (key[j] == '0')) {
                        // one of the species not in cluster, do nothing
                        continue;
                    } else if ((key[i] == 'n') || (key[j] == 'n')) {
                        // one of the species is a paralogue
                        type = "paralogue";
                    } else if ((key[i] == '1') || (key[j] == '1')) {
                        // create an orthologue
                        type = "orthologue";
                    } else {
                        throw new IllegalArgumentException("Unexpected key configuration: " + keyStr
                                                           + " for cluster: " + clusterNum);
                    }
                    // each element can have multiple comma separated gene identfiers
                    List<String> genes = Arrays.asList(line[i + 2].split(","));
                    List<String> homGenes = Arrays.asList(line[j + 2].split(","));
                    for (String gene : genes) {
                        for (String homGene : homGenes) {
                            createHomologue(getGene(gene, species[i]),
                                            getGene(homGene, species[j]),
                                            type, clusterNum);
                        }
                    }
                }
            }
        }
    }


    // create and store a Homologue with identifiers of Gene items
    private void createHomologue(String gene, String homGene, String type, String cluster)
    throws ObjectStoreException {

        // if no genes created then ids could not be resolved, don't create a homologue
        if (gene == null || homGene == null) {
            return;
        }

        Item homologue = createItem("Homologue");
        homologue.setAttribute("type", type);
        homologue.setAttribute("clusterName", "Drosophila homology:" + cluster);
        homologue.setReference("gene", gene);
        homologue.setReference("homologue", homGene);
        homologue.addToCollection("publications", pub);
        store(homologue);
    }

    // fetch a gene or create and store
    private String getGene(String symbol, String org) throws ObjectStoreException {
        String geneId = genes.get(symbol + org);
        if (geneId == null) {
            // attempt to resolve to a current FlyBase gene
            OrganismData oData = or.getOrganismDataByAbbreviation(org);
            String taxonId = "" + oData.getTaxonId();
            if (resolver == null) {
                resolver = resolverFactory.getIdResolver();
            }
            int resCount = resolver.countResolutions(taxonId, symbol);
            if (resCount != 1) {
                LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                         + symbol + " count: " + resCount + " FBgn: "
                         + resolver.resolveId(taxonId, symbol));
                return null;
            }
            String primaryIdentifier = resolver.resolveId(taxonId, symbol).iterator().next();
            geneId = fbgns.get(primaryIdentifier);
            if (geneId == null) {
                Item gene = createItem("Gene");
                gene.setAttribute("primaryIdentifier", primaryIdentifier);
                gene.setReference("organism", getOrganism(org));
                store(gene);
                geneId = gene.getIdentifier();
                fbgns.put(primaryIdentifier, geneId);
            }
            genes.put(symbol + org, geneId);
        }
        return geneId;
    }

    // create and store an organism
    private String getOrganism(String abbrev) throws ObjectStoreException {
        String orgId = organisms.get(abbrev);
        if (orgId == null) {
            Item organism = createItem("Organism");
            OrganismData oData = or.getOrganismDataByAbbreviation(abbrev);
            if (oData == null) {
                throw new IllegalArgumentException("No organism data found for abbreviation: "
                                                   + abbrev);
            }
            String taxonId = "" + oData.getTaxonId();
            organism.setAttribute("taxonId", taxonId);
            store(organism);
            orgId = organism.getIdentifier();
            organisms.put(abbrev, orgId);
        }
        return orgId;
    }
}
