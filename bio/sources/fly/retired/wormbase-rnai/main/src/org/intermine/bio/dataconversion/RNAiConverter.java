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
import java.io.File;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * DataConverter to parse an RNAi data file into Items
 * @author Andrew Varley
 * @author Kim Rutherford
 */
public class RNAiConverter extends BioFileConverter
{
    private Map<String, Item> geneMap = new HashMap<String, Item>(),
    screenMap = new HashMap<String, Item>(),
    pubMap = new HashMap<String, Item>(),
    phenotypeMap = new HashMap<String, Item>();
    private Item ontology;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws ObjectStoreException of problem reading/writing data
     */
    public RNAiConverter(ItemWriter writer, Model model)
        throws ObjectStoreException {
        super(writer, model, "WormBase", "WormBase RNAi Phenotypes");

        ontology = createItem("Ontology");
        ontology.setAttribute("name", "WormBase phenotype codes");
        store(ontology);
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        boolean readingData = false;
        File currentFile = getCurrentFile();
        if (currentFile.getName().contains("-final")) {
            while (lineIter.hasNext()) {
                String[] line = (String[]) lineIter.next();

                // throw out any headers
                if (!readingData) {
                    if (line[0].startsWith("WBGene")) {
                        readingData = true;
                    } else {
                        continue;
                    }
                }

                if (readingData) {
                    Item gene = createGene(line[0]);

                    String comment = null;
                    if (!StringUtils.isEmpty(line[10].trim())) {
                        comment = line[10].trim();
                    }
                    String isObserved = null;
                    if (line[5] != null && ("1").equals(line[5])) {
                        isObserved = "true";
                    } else if (line[6] != null && ("1").equals(line[6])) {
                        isObserved = "false";
                    }
                    Item phenotype = createPhenotype(line[2], line[4], line[3], comment, isObserved,
                        line[7], line[8]);
                    phenotype.setReference("gene", gene.getIdentifier());

                    Item pub = createPub(line[11]);
                    phenotype.addToCollection("publications", pub.getIdentifier());
                    Item screen = createScreen(pub);
                    phenotype.setReference("screen", screen.getIdentifier());
                    store(phenotype);
                }
            }
        }
    }

    private Item createGene(String primaryIdentifier)
        throws ObjectStoreException {
        Item gene = geneMap.get(primaryIdentifier);
        if (gene == null) {
            gene = createItem("Gene");
            gene.setReference("organism", getOrganism("6239"));
            gene.setAttribute("primaryIdentifier", primaryIdentifier);
            geneMap.put(primaryIdentifier, gene);
            store(gene);
        }
        return gene;
    }

    private Item createPhenotype(String identifier, String code, String desc, String comment,
                                 String isObserved, String penetranceFrom, String penetranceTo)
        throws ObjectStoreException {
        Item rnaiPhenotype = createItem("RNAiPhenotype");
        if (!StringUtils.isEmpty(code)) {
            rnaiPhenotype.setAttribute("code", code);
        }
        rnaiPhenotype.setAttribute("name", desc);
        rnaiPhenotype.setAttribute("observed", isObserved);
        if (!StringUtils.isEmpty(penetranceFrom)) {
            rnaiPhenotype.setAttribute("penetranceFrom", penetranceFrom);
        }
        if (!StringUtils.isEmpty(penetranceTo)) {
            rnaiPhenotype.setAttribute("penetranceTo", penetranceTo);
        }
        if (comment != null && !"".equals(comment)) {
            rnaiPhenotype.setAttribute("comment", comment);
        }

        Item phenotype = phenotypeMap.get(identifier);
        if (phenotype == null) {
            phenotype = createItem("Phenotype");
            phenotype.setAttribute("identifier", identifier);
            if (!StringUtils.isEmpty(code)) {
                phenotype.setAttribute("code", code);
            }
            phenotype.setAttribute("name", desc);
            phenotype.setReference("ontology", ontology.getIdentifier());
            phenotypeMap.put(identifier, phenotype);

            store(phenotype);
        }
        rnaiPhenotype.setReference("phenotype", phenotype.getIdentifier());
        return rnaiPhenotype;
    }


    private Item createPub(String pubMedId)
        throws ObjectStoreException {
        Item pub = pubMap.get(pubMedId);
        if (pub == null) {
            pub = createItem("Publication");
            pub.setAttribute("pubMedId", pubMedId);
            pubMap.put(pubMedId, pub);
            store(pub);
        }
        return pub;
    }

    private Item createScreen(Item pub)
        throws ObjectStoreException {
        String pubId = pub.getIdentifier();
        Item screen = screenMap.get(pubId);
        if (screen == null) {
            screen = createItem("RNAiScreen");
            screen.setReference("publication", pubId);
            screen.setReference("organism", getOrganism("6239"));
            screenMap.put(pubId, screen);
            store(screen);
        }
        return screen;
    }
}

