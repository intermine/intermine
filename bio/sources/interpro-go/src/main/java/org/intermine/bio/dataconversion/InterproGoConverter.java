package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2020 FlyMine
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
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;
import org.xml.sax.SAXException;


/**
 * Loads go annotation from interpro2go goa file
 * @author Julie Sullivan
 */
public class InterproGoConverter extends BioFileConverter
{

    protected static final Logger LOG = Logger.getLogger(InterproGoConverter.class);
    private static final String DATASET_TITLE = "InterPro domain GO annotations";
    private static final String DATA_SOURCE_NAME = "InterPro";
    private static final String INTERPRO_PREFIX = "InterPro:";
    private Map<String, Item> proteinDomains = new HashMap<String, Item>();
    private Map<String, String> goTerms = new HashMap<String, String>();


    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public InterproGoConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE, null, false);
    }

    /**
     *
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        //InterPro:IPR000003 Retinoid X receptor > GO:DNA binding ; GO:0003677

        Iterator<String[]> lineIter = FormattedTextParser.parseDelimitedReader(reader, ';');

        while (lineIter.hasNext()) {
            String[] line = lineIter.next();

            if (line.length != 2) {
                LOG.error("bad line: " + line[0]);
                continue;
            }

            String firstChunk = line[0];
            String goTermIdentifier = line[1];

            String[] bits = firstChunk.split(" ");
            String interproIdentifier = bits[0];

            // chop off InterPro: prefix
            interproIdentifier = interproIdentifier.substring(INTERPRO_PREFIX.length());

            // create interpro Id
            Item proteinDomain = getDomain(interproIdentifier);

            // create go term
            String goTermRefId = getGoTerm(goTermIdentifier.trim());

            // create Go annotation
            Item goAnnotation = createItem("GOAnnotation");
            goAnnotation.setReference("subject", proteinDomain);
            goAnnotation.setReference("ontologyTerm", goTermRefId);

            proteinDomain.addToCollection("goAnnotation", goAnnotation);

            try {
                store(goAnnotation);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }

        for (Item proteinDomain : proteinDomains.values()) {
            try {
                store(proteinDomain);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }
    }

    private Item getDomain(String identifier) {
        Item item = proteinDomains.get(identifier);
        if (item == null) {
            item = createItem("ProteinDomain");
            item.setAttribute("primaryIdentifier", identifier);
            proteinDomains.put(identifier, item);
        }
        return item;
    }

    private String getGoTerm(String identifier)
        throws SAXException {
        String refId = goTerms.get(identifier);
        if (refId == null) {
            Item item = createItem("GOTerm");
            item.setAttribute("identifier", identifier);
            refId = item.getIdentifier();
            goTerms.put(identifier, refId);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }
        return refId;
    }
}

