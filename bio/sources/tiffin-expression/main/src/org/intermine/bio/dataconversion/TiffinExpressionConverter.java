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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.intermine.dataconversion.DataConverter;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;

import java.io.BufferedReader;
import java.io.Reader;

import org.apache.log4j.Logger;

/**
 * DataConverter to parse Tiffin expression data file into Items.
 * @author Kim Rutherford
 */
public class TiffinExpressionConverter extends FileConverter
{
    protected static final Logger LOG = Logger.getLogger(TiffinExpressionConverter.class);

    private Map<String, Item> geneItems = new HashMap<String, Item>();
    private Map<String, Item> termItems = new HashMap<String, Item>();

    Item orgDrosophila;
    private Item dataSet;
    private Item pub;

    /**
     * Construct a new instance of HomophilaCnoverter.
     *
     * @param model the Model
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public TiffinExpressionConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model);

        orgDrosophila = createItem("Organism");
        orgDrosophila.addAttribute(new Attribute("taxonId", "7227"));
        store(orgDrosophila);

        pub = createItem("Publication");
        pub.addAttribute(new Attribute("pubMedId", "17238282"));
        store(pub);
    }

    private static final Pattern MOTIF_NAME_PATTERN = Pattern.compile("(TIFDMEM\\d+)\\.\\d+");
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("(.*)\\W+\\(P=(.*)\\)");

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

        while ((line = br.readLine()) != null) {
            Matcher motifNameMatcher = MOTIF_NAME_PATTERN.matcher(line);
            if (motifNameMatcher.matches()) {
                String currentMotifIdentifier = motifNameMatcher.group(1);
                currentMotif = createItem("Motif");
                currentMotif.setAttribute("identifier", currentMotifIdentifier);
                store(currentMotif);
            } else {
                Matcher expressionMatcher = EXPRESSION_PATTERN.matcher(line);
                if (expressionMatcher.matches()) {
                    String expressionDescription = expressionMatcher.group(1);
                    Item expressionItem = getExpressionTerm(expressionDescription);
                    expressionItem.setReference("motif", currentMotif);
                    store(expressionItem);
                } else {
                    if (line.trim().length() > 0) {
                        throw new RuntimeException("failed to parse this line: " + line);
                    }
                }
            }
        }
    }

    private Item getExpressionTerm(String expressionDescription) throws ObjectStoreException {
        if (termItems.containsKey(expressionDescription)) {
            return termItems.get(expressionDescription);
        } else {
            Item expressionTermItem = createItem("TiffinExpressionTerm");
            expressionTermItem.setAttribute("name", expressionDescription);
            termItems.put(expressionDescription, expressionTermItem);
            return expressionTermItem;
        }
    }
}

