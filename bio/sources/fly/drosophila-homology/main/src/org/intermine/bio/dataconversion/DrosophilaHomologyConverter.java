package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
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
            // String geneOrganismRefId = getOrganism("7227");
            String homologue = line[5];
            //String homoOrganismRefId = parseSymbol(line[6]);
            // NULL if not a fly of interest

            String gene1 = getGene(geneIdentifier);
            String gene2 = getGene(homologue);

            if (gene1 != null && gene2 != null) {
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

    private String getGene(String identifier)
        throws ObjectStoreException {
        String geneRefId = genes.get(identifier);
        if (geneRefId != null) {
            return geneRefId;
        }
        Item item = createItem("Gene");
        item.setAttribute("primaryIdentifier", identifier);
        geneRefId = item.getIdentifier();
        genes.put(identifier, geneRefId);
        store(item);
        return geneRefId;
    }
}
