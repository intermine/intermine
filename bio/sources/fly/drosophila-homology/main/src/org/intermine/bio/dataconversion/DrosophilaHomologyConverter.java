package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2013 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Item;


/**
 * Parse Drosophila 12 genome homology file and create pairwise Homologue objects.
 *
 * @author Julie Sullivan
 */
public class DrosophilaHomologyConverter extends BioFileConverter
{
    private Item pub, evidence;
    private Map<String, String> genes = new HashMap<String, String>();
    protected static final Logger LOG = Logger.getLogger(DrosophilaHomologyConverter.class);
    private static final String EVIDENCE_CODE_ABBR = "AA";
    private static final String EVIDENCE_CODE_NAME = "Amino acid sequence comparison";
    private Set<String> taxonIds = new HashSet<String>();
    private static final Map<String, String> DROSOPHILAS = new HashMap<String, String>();
    
    static  {
        DROSOPHILAS.put("Dana", "7217");
        DROSOPHILAS.put("Dere", "7220");
        DROSOPHILAS.put("Dgri", "7222");
        DROSOPHILAS.put("Dmoj", "7230");
        DROSOPHILAS.put("Dper", "7234");
        DROSOPHILAS.put("Dpse", "7237");
        DROSOPHILAS.put("Dsec", "7238");
        DROSOPHILAS.put("Dsim", "7240");
        DROSOPHILAS.put("Dvir", "7244");
        DROSOPHILAS.put("Dwil", "7260");
        DROSOPHILAS.put("Dyak", "7245");        
    }
    
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
        Item evidenceCode = createItem("OrthologueEvidenceCode");
        evidenceCode.setAttribute("abbreviation", EVIDENCE_CODE_ABBR);
        evidenceCode.setAttribute("name", EVIDENCE_CODE_NAME);
        store(evidenceCode);
        evidence = createItem("OrthologueEvidence");
        evidence.setReference("evidenceCode", evidenceCode);
        evidence.addToCollection("publications", pub);
        store(evidence);
    }

    /**
     * Set the organisms to include by a space separated list of taxon ids.
     * @param taxonIds the organisms to include
     */
    public void setOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtil.split(taxonIds, " ")));
    }
    
    /**
     * Read each line from flat file, create genes and synonyms.
     *
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws Exception {
        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            if (line.length < 6) {
                continue;
            }
            String geneIdentifier = line[0];
            String geneOrganismRefId = getOrganism("7227");
            String homologue = line[5];
            String homoOrganismRefId = parseSymbol(line[6]);
            // NULL if not a fly of interest 
            if (homoOrganismRefId != null) {
                String gene1 = getGene(geneIdentifier, geneOrganismRefId);
                String gene2 = getGene(homologue, homoOrganismRefId);
                createHomologue(gene1, gene2);
                createHomologue(gene2, gene1);
            }
        }
    }

    private void createHomologue(String gene, String homGene)
        throws ObjectStoreException {
        // if no genes created then ids could not be resolved, don't create a homologue
        if (gene == null || homGene == null) {
            return;
        }
        Item homologue = createItem("Homologue");
        homologue.setAttribute("type", "orthologue");
        homologue.setReference("gene", gene);
        homologue.setReference("homologue", homGene);
        homologue.addToCollection("evidence", evidence);
        store(homologue);
    }

    private String getGene(String identifier, String organismRefId)
        throws ObjectStoreException {
        String geneRefId = genes.get(identifier);
        if (geneRefId != null) {
            return geneRefId;
        }
        Item item = createItem("Gene");
        item.setAttribute("primaryIdentifier", identifier);
        item.setReference("organism", organismRefId);
        geneRefId = item.getIdentifier();
        genes.put(identifier, geneRefId);
        store(item);
        return geneRefId;
    }

    private String parseSymbol(String symbol)  {
        String species = symbol.substring(0, 4);
        String taxonId = DROSOPHILAS.get(species); 
        if (taxonId != null & (taxonIds.contains(taxonId) || taxonIds.isEmpty())) {
            return getOrganism(taxonId);
        }
        return null;
    }
}
