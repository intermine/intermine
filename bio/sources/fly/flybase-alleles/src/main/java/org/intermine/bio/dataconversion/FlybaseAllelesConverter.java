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
 * Load allele disease annotation
 *
 * @author Julie Sullivan
 */
public class FlybaseAllelesConverter extends BioFileConverter
{


    private static final String DATASET_TITLE = "FlyBase";
    private static final String DATA_SOURCE_NAME = "FlyBase Human disease model data set";
    private Map<String, Item> alleles = new HashMap<String, Item>();
    private Map<String, String> diseases = new HashMap<String, String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public FlybaseAllelesConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close()  {
        for (Item allele : alleles.values()) {
            try {
                store(allele);
            } catch (ObjectStoreException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     *
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line.length < 6) {
                continue;
            }

            String alleleIdentifier = line[0];
            String symbol = line[1];
            Item allele = getAllele(alleleIdentifier, symbol);

            String qualifier = line[2];
            //String diseaseName = line[3];
            String diseaseIdentifier = line[4];
            String evidence = line[5];

            String doTerm = getDisease(diseaseIdentifier);

            Item evidenceTerm = createItem("DOEvidence");
            if (StringUtils.isNotEmpty(evidence)) {
                evidenceTerm.setAttribute("evidence", evidence);
            }
            store(evidenceTerm);

            Item doAnnotation = createItem("DOAnnotation");
            doAnnotation.setReference("subject", allele);
            allele.addToCollection("doAnnotation", doAnnotation);
            if (StringUtils.isNotEmpty(qualifier)) {
                doAnnotation.setAttribute("qualifier", qualifier);
            }
            doAnnotation.setReference("ontologyTerm", doTerm);
            store(doAnnotation);
        }
    }

    private Item getAllele(String primaryIdentifier, String symbol) throws ObjectStoreException {
        Item item = alleles.get(primaryIdentifier);
        if (item == null) {
            item = createItem("Allele");
            item.setAttribute("primaryIdentifier", primaryIdentifier);
            item.setAttribute("symbol", symbol);
            alleles.put(primaryIdentifier, item);
        }
        return item;
    }

    private String getDisease(String identifier) throws ObjectStoreException {
        String refId = diseases.get(identifier);
        if (refId == null) {
            Item item = createItem("DOTerm");
            item.setAttribute("identifier", identifier);
            store(item);
            refId = item.getIdentifier();
            diseases.put(identifier, refId);
        }
        return refId;
    }
}
