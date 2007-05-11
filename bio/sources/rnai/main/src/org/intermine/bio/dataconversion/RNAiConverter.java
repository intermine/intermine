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
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.intermine.util.TextFileUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.ItemFactory;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.FileConverter;

/**
 * DataConverter to parse an RNAi data file into Items
 * @author Andrew Varley
 * @author Kim Rutherford
 */
public class RNAiConverter extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";

    private Map geneMap = new HashMap(), screenMap = new HashMap(),
        pubMap = new HashMap(), phenotypeMap = new HashMap();
    private Map ids = new HashMap();
    private Item dataSource, dataSet, org, ontology;
    private ItemFactory itemFactory;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException of problem reading/writing data
     */
    public RNAiConverter(ItemWriter writer) throws ObjectStoreException {
        super(writer);

        itemFactory = new ItemFactory(Model.getInstanceByName("genomic"), "-1_");

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "WormBase");
        getItemWriter().store(ItemHelper.convert(dataSource));

        dataSet = createItem("DataSet");
        dataSet.setAttribute("title", "WormBase RNAi Phenotype");
        dataSet.setReference("dataSource", dataSource.getIdentifier());
        getItemWriter().store(ItemHelper.convert(dataSet));

        org = createItem("Organism");
        org.setAttribute("taxonId", "6239");
        getItemWriter().store(ItemHelper.convert(org));

        ontology = createItem("Ontology");
        ontology.setAttribute("title", "WormBase phenotype codes");
        getItemWriter().store(ItemHelper.convert(ontology));
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

                getItemWriter().store(ItemHelper.convert(phenotype));
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

            getItemWriter().store(ItemHelper.convert(gene));
            getItemWriter().store(ItemHelper.convert(synonym));
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

            getItemWriter().store(ItemHelper.convert(phenotype));
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
            getItemWriter().store(ItemHelper.convert(pub));
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
            getItemWriter().store(ItemHelper.convert(screen));
        }
        return screen;
    }


    private String newId(String className) {
        Integer id = (Integer) ids.get(className);
        if (id == null) {
            id = new Integer(0);
            ids.put(className, id);
        }
        id = new Integer(id.intValue() + 1);
        ids.put(className, id);
        return id.toString();
    }

    private Item createItem(String className) {
        return itemFactory.makeItem(alias(className) + "_" + newId(className),
                                    GENOMIC_NS + className, "");
    }
}

