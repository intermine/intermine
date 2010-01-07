package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.SAXParser;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Item;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * BioGrid human data converter.
 *
 * @author Dominik Grimm
 */
public class BioGridHumanConverter extends BioFileConverter
{
    private Map<String, String> pubs = new HashMap<String, String>();
    protected IdResolverFactory resolverFactory;

    /**
     * Create a new BioGridHumanConverter object.
     * @param writer the ItemWriter to write Items to
     * @param model the Model to use when making Items
     */
    public BioGridHumanConverter(ItemWriter writer, Model model) {
        super(writer, model, "BioGRID", "BioGRID data set");
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        BioGridHumanHandler handler = new BioGridHumanHandler(getItemWriter());

        try {
            handler.parseTabbedFile(reader);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private class BioGridHumanHandler
    {
        private ItemWriter writer;
        private BufferedReader in;
        private Map<String, Item> genes = new HashMap<String, Item>();
        private Map<String, String> organismMap = new HashMap<String, String>();
        private List<String> values = new Vector<String>();
        private List<List<String>> valueRows = new Vector<List<String>>();
        private Set<String> storedItems = new HashSet<String>();

        /**
         * Constructor
         * @param writer the ItemWriter used to handle the resultant items
         */
        public BioGridHumanHandler(ItemWriter writer) {
            this.writer = writer;
        }

        public void parseTabbedFile(Reader reader) throws IOException, ObjectStoreException {

            in = new BufferedReader(reader);

            String delimiter = "\\t";

            String readString;

            //read fields and values and filter by gene interactions
            while ((readString = in.readLine()) != null) {
                String[] tmp = readString.split(delimiter);

                if (tmp[6].equals("Phenotypic Enhancement")
                   || tmp[6].equals("Phenotypic Suppression")) {
                    values = new Vector<String>();

                    for (int i = 0; i < tmp.length; i++) {
                        switch(i) {
                            case 2: { //Gene 1
                                values.add(tmp[i]);
                                break;
                            }
                            case 3: { //Gene 2
                                values.add(tmp[i]);
                                break;
                            }
                            case 6: { //GeneInteraction.type
                                values.add(tmp[i]);
                                break;
                            }
                            case 7: { //GeneInteractionExperiment.name
                                values.add(tmp[i]);
                                break;
                            }
                            case 8: { //PupMed Ids; seperated by ;
                                values.add(tmp[i]);
                                break;
                            }
                            case 9: { //Organism 1
                                values.add(tmp[i]);
                                break;
                            }
                            case 10: { //Organism 2
                                values.add(tmp[i]);
                                break;
                            }
                        }
                    }
                    valueRows.add(values);
                }
            }
            storeInteractingGenes();
        }

        private void storeInteractingGenes() throws ObjectStoreException {

            for (int i = 0; i < valueRows.size(); i++) {

                values = valueRows.get(i);

                    String organismARefId, organismBRefId;

                    if (organismMap.get(values.get(5)) == null) {
                        Item organism = createItem("Organism");

                        organism.setAttribute("taxonId", values.get(5));

                        organismARefId = organism.getIdentifier();

                        organismMap.put(values.get(5), organismARefId);

                        store(organism);
                    } else {
                        organismARefId = organismMap.get(values.get(5));
                    }

                    if (organismMap.get(values.get(6)) == null) {
                        Item organism2 = createItem("Organism");

                        organism2.setAttribute("taxonId", values.get(6));

                        organismBRefId = organism2.getIdentifier();

                        organismMap.put(values.get(6), organismARefId);

                        store(organism2);
                    } else {
                        organismBRefId = organismMap.get(values.get(6));
                    }

                    Item experiment = createItem("GeneticInteractionExperiment");

                    experiment.setAttribute("name", values.get(3));

                    String[] publications = values.get(4).split(";");

                    for (int k = 0; k < publications.length; k++) {
                        String itemId = pubs.get(publications[k]);
                        if (itemId == null) {
                                Item pub = createItem("Publication");
                                pub.setAttribute("pubMedId", publications[k]);
                                itemId = pub.getIdentifier();
                                pubs.put(publications[k], itemId);
                                store(pub);
                        }
                        experiment.setReference("publication", itemId);
                    }

                    store(experiment);

                    Item gene = getGene(values.get(0));

                    Item gene2 = getGene(values.get(1));

                    Item interaction = createItem("GeneticInteraction");

                    interaction.setAttribute("type", values.get(2));

                    interaction.setReference("gene", gene.getIdentifier());
                    interaction.setReference("experiment", experiment.getIdentifier());

                    interaction.addToCollection("interactingGenes", gene2);

                    /* store all interaction-related items */
                    if (!storedItems.contains(gene.getAttribute("symbol").getValue())) {
                        gene.setReference("organism", organismARefId);
                        store(gene);
                        storedItems.add(gene.getAttribute("symbol").getValue());
                    }
                    if (!storedItems.contains(gene2.getAttribute("symbol").getValue())) {
                        gene2.setReference("organism", organismBRefId);
                        store(gene2);
                        storedItems.add(gene2.getAttribute("symbol").getValue());
                    }

                    //interaction.addToCollection(masterList.get("dataset"));
                    store(interaction);
            }
        }

        private Item getGene(String identifier) {
            Item item = genes.get(identifier);
            if (item == null) {
                item = createItem("Gene");
                item.setAttribute("symbol", identifier);
                genes.put(identifier, item);
            }
            return item;
        }

    }
}
