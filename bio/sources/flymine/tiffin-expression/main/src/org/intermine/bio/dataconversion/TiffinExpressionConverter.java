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

import java.io.BufferedReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * DataConverter to parse Tiffin expression data file into Items.
 * @author Kim Rutherford
 */
public class TiffinExpressionConverter extends BioFileConverter
{
    protected static final Logger LOG = Logger.getLogger(TiffinExpressionConverter.class);
    protected Item pub, orgDrosophila;
    private String soterm;
    private Map<String, Item> termItems = new HashMap<String, Item>();

    /**
     * Construct a new instance of HomophilaCnoverter.
     *
     * @param model the Model
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public TiffinExpressionConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model, "Sanger Institute", "Tiffin");

        orgDrosophila = createItem("Organism");
        orgDrosophila.setAttribute("taxonId", "7227");
        store(orgDrosophila);

        pub = createItem("Publication");
        pub.setAttribute("pubMedId", "17238282");
        store(pub);

        soterm = BioStoreHook.getSoTerm(this, null, "DNA_motif", getSequenceOntologyRefId());

    }

    private static final Pattern MOTIF_NAME_PATTERN = Pattern.compile("(TIFDMEM\\d+)\\.\\d+\\s*");
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("(.*)\\s+\\(P=(.*)\\)\\s*");

    /**
     * Process the Tiffin expression results
     * @param reader the Reader
     * @see DataConverter#process()
     * @throws Exception if something goes wrong
     */
    @Override
    public void process(Reader reader) throws Exception {
        BufferedReader br = new BufferedReader(reader);
        Item currentMotif = null;
        String line = null;
        Map<String, Item> motifs = new HashMap<String, Item>();

        while ((line = br.readLine()) != null) {
            Matcher motifNameMatcher = MOTIF_NAME_PATTERN.matcher(line);
            if (motifNameMatcher.matches()) {
                String currentMotifIdentifier = motifNameMatcher.group(1);
                currentMotif = createItem("Motif");
                currentMotif.setAttribute("primaryIdentifier", currentMotifIdentifier);
                currentMotif.setReference("sequenceOntologyTerm", soterm);
                //store(currentMotif);
                motifs.put(currentMotifIdentifier, currentMotif);
            } else {
                Matcher expressionMatcher = EXPRESSION_PATTERN.matcher(line);
                if (expressionMatcher.matches()) {
                    String expressionDescription = expressionMatcher.group(1);
                    Item expressionItem = getExpressionTerm(expressionDescription);
                    if (currentMotif == null) {
                        throw new RuntimeException("internal error \"currentMotif\" shouldn't "
                                                   + "be null at this point");
                    }
                    currentMotif.addToCollection("expressionTerms", expressionItem);
                    //expressionItem.addToCollection("motifs", currentMotif);
                } else {
                    if (line.trim().length() > 0) {
                        throw new RuntimeException("failed to parse this line: " + line);
                    }
                }
            }
        }
        storeAll(termItems);
        storeAll(motifs);
    }

    private Item getExpressionTerm(String expressionDescription) {
        if (termItems.containsKey(expressionDescription)) {
            return termItems.get(expressionDescription);
        }
        Item expressionTermItem = createItem("MRNAExpressionTerm");
        expressionTermItem.setAttribute("name", expressionDescription);
        expressionTermItem.setAttribute("type", "ImaGO");
        termItems.put(expressionDescription, expressionTermItem);
        return expressionTermItem;
    }

    private void storeAll(Map<String, Item> map) throws Exception {
        for (Item item: map.values()) {
            store(item);
        }
    }
}

