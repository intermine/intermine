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

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;
import org.xml.sax.SAXException;

/**
 * DataConverter to load ZFIN gene identifiers from text files
 *
 * @author Julie Sullivan
 */
public class ZfinIdentifiersConverter extends BioFileConverter
{
    protected String organismRefId;
    private Map<String, Item> genes = new HashMap();
    private Map<String, String> synonyms = new HashMap();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws ObjectStoreException if an error occurs in storing
     */
    public ZfinIdentifiersConverter(ItemWriter writer, Model model)
        throws ObjectStoreException {
        super(writer, model, "ZFIN", "ZFIN data set");

        // create and store organism
        Item organism = createItem("Organism");
        organism.setAttribute("taxonId", "7955");
        store(organism);
        organismRefId = organism.getIdentifier();
    }


    /**
     * Read each line from flat file, create genes and synonyms.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        /* data is in format:
         ALIASES.TXT
         primaryIdentifier /t tab delimited list of synonyms
         eg, ZDB-GENE-000329-4       notch homolog 2 notch2  sb:cb884

         ZDB_HISTORY.TXT
         primaryidentifier /t old primaryidentifier
         ZDB-GENE-000607-4       ZDB-GENE-010129-1

         ensembl_1_to_1.txt
         #    ZDBID              SYMBOL  Ensembl(Zv7)
          ZDB-GENE-000112-47      ppardb  ENSDARG00000009473

        */

        boolean processingEnsembl = false;

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            // if the first line starts with a hash, we are processing the ensembl file
            if (line[0].startsWith("#")) {
                processingEnsembl = true;
                // skip first line
                continue;
            }

            // the first column is primary identifier in all three files
            String primaryIdentifier = line[0];
            Item gene = getGene(primaryIdentifier);
            gene.setReference("organism", organismRefId);
            String refId = gene.getIdentifier();

            if (processingEnsembl) {
                String symbol = line[1];
                String secondaryIdentifier = line[2];

                if (secondaryIdentifier != null && !secondaryIdentifier.equals("")) {
                    gene.setAttribute("secondaryIdentifier", secondaryIdentifier);
                    setSynonym(refId, "identifier", secondaryIdentifier);
                }
                if (symbol != null && !symbol.equals("")) {
                    gene.setAttribute("symbol", symbol);
                    setSynonym(refId, "symbol", symbol);
                }
            } else {
                // loop through the rest of the synonyms
                for (int i = 1; i < line.length; i++) {
                    String synonym = line[i];
                    if (!StringUtils.isEmpty(synonym)) {
                        setSynonym(refId, "identifier", synonym);
                    }
                }
            }
        }
    }

    private Item getGene(String primaryIdentifier)
    throws SAXException {
        Item item = genes.get(primaryIdentifier);
        if (item == null) {
            item = createItem("Gene");
            item.setAttribute("primaryIdentifier", primaryIdentifier);
            genes.put(primaryIdentifier, item);
            setSynonym(item.getIdentifier(), "identifier", primaryIdentifier);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }
        return item;
    }

    private void setSynonym(String subjectRefId, String type, String value)
    throws SAXException {
        String key = subjectRefId + type + value;
        String refId = synonyms.get(key);
        if (refId == null) {
            Item synonym = createItem("Synonym");
            synonym.setAttribute("type", type);
            synonym.setAttribute("value", value);
            synonym.setReference("subject", subjectRefId);
            synonyms.put(key, refId);
            try {
                store(synonym);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }
    }
}
