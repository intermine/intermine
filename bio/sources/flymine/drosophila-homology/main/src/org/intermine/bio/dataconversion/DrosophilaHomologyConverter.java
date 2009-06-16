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
    private Item pub;
    private Map<String, String> genes = new HashMap();
    protected IdResolverFactory resolverFactory;
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
//        or = OrganismRepository.getOrganismRepository();
    }


    /**
     * Read each line from flat file, create genes and synonyms.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            if (line.length < 6) {
                continue;
            }
            String geneIdentifier = line[0];
            String homologue = line[5];
            createHomologue(getGene(geneIdentifier), getGene(homologue));
        }
    }


    // create and store a Homologue with identifiers of Gene items
    private void createHomologue(String gene, String homGene)
    throws ObjectStoreException {
        // if no genes created then ids could not be resolved, don't create a homologue
        if (gene == null || homGene == null) {
            return;
        }
        Item homologue = createItem("Homologue");
        // TODO this is a just a guess.  I have no idea if it's correct or not.
        homologue.setAttribute("type", "orthologue");
        homologue.setReference("gene", gene);
        homologue.setReference("homologue", homGene);
        homologue.addToCollection("publications", pub);
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
        getSynonym(geneRefId, "identifier", identifier);
        store(item);
        return geneRefId;
    }

    private void getSynonym(String subjectId, String type, String value)
    throws ObjectStoreException {
        Item syn = createItem("Synonym");
        syn.setReference("subject", subjectId);
        syn.setAttribute("type", type);
        syn.setAttribute("value", value);
        try {
            store(syn);
        } catch (ObjectStoreException e) {
            throw new ObjectStoreException(e);
        }
    }
}
