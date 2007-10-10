package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.TextFileUtil;
import org.intermine.xml.full.Item;


/**
 * DataConverter to parse an RNAi data file into Items
 * @author Andrew Varley
 * @author Kim Rutherford
 */
public class RNAiConverter extends FileConverter
{
    private Map geneMap = new HashMap(), screenMap = new HashMap(),
        pubMap = new HashMap(), phenotypeMap = new HashMap();
    private Item dataSource, dataSet, org, ontology;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws ObjectStoreException of problem reading/writing data
     */
    public RNAiConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model);

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "WormBase");
        store(dataSource);

        dataSet = createItem("DataSet");
        dataSet.setAttribute("title", "WormBase RNAi Phenotype");
        dataSet.setReference("dataSource", dataSource.getIdentifier());
        store(dataSet);

        org = createItem("Organism");
        org.setAttribute("taxonId", "6239");
        store(org);

        ontology = createItem("Ontology");
        ontology.setAttribute("title", "WormBase phenotype codes");
        store(ontology);
    }

    /**
     * @see FileConverter#process(Reader)
     */
    public void process(Reader reader) throws Exception {
        Iterator lineIter = TextFileUtil.parseTabDelimitedReader(reader);
        boolean readingData = false;
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

                // may be sixth column with a comment
                String comment = null;
                if (line.length == 6) {
                    comment = line[5].trim();
                }
                Item phenotype = createPhenotype(line[3], line[4], comment);
                phenotype.setReference("subject", gene.getIdentifier());
                phenotype.setReference("gene", gene.getIdentifier());

                Item pub = createPub(line[2]);
                phenotype.setCollection("evidence", new ArrayList(
                    Collections.singleton(pub.getIdentifier())));
                Item screen = createScreen(pub);
                phenotype.setReference("analysis", screen.getIdentifier());

                store(phenotype);
            }
        }
    }

    private Item createGene(String organismDbId) throws ObjectStoreException {
        Item gene = (Item) geneMap.get(organismDbId);
        if (gene == null) {
            gene = createItem("Gene");
            gene.setReference("organism", org.getIdentifier());
            gene.setAttribute("organismDbId", organismDbId);
            geneMap.put(organismDbId, gene);

            Item synonym = createItem("Synonym");
            synonym.setAttribute("value", organismDbId);
            synonym.setAttribute("type", "identifier");
            synonym.setReference("subject", gene.getIdentifier());
            synonym.setReference("source", dataSource.getIdentifier());

            store(gene);
            store(synonym);
        }
        return gene;
    }

    private Item createPhenotype(String code, String desc, String comment)
    throws ObjectStoreException {
        Item rnaiPhenotype = createItem("RNAiPhenotype");
        rnaiPhenotype.setAttribute("code", code);
        rnaiPhenotype.setAttribute("name", desc);
        if (comment != null && !comment.equals("")) {
            rnaiPhenotype.setAttribute("comment", comment);
        }

        Item phenotype = (Item) phenotypeMap.get(code);
        if (phenotype == null) {
            phenotype = createItem("Phenotype");
            phenotype.setAttribute("identifier", code);
            phenotype.setAttribute("name", desc);
            phenotype.setReference("ontology", ontology.getIdentifier());
            phenotypeMap.put(code, phenotype);

            store(phenotype);
        }
        rnaiPhenotype.setReference("property", phenotype.getIdentifier());
        return rnaiPhenotype;
    }


    private Item createPub(String pubMedId)
        throws ObjectStoreException {
        Item pub = (Item) pubMap.get(pubMedId);
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
        Item screen = (Item) screenMap.get(pubId);
        if (screen == null) {
            screen = createItem("RNAiScreen");
            screen.setReference("publication", pubId);
            screen.setReference("organism", org.getIdentifier());
            screenMap.put(pubId, screen);
            store(screen);
        }
        return screen;
    }
}

