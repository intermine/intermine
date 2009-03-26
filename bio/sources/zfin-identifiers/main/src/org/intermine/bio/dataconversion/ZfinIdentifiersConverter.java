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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
    //private static final Logger LOG = Logger.getLogger(ZfinIdentifiersConverter.class);
    protected String organismRefId;
    private Map<String, Item> genes = new HashMap();
    private Set<String> synonyms = new HashSet();
    private static final String ENSEMBL_FILE = "ensembl_1_to_1.txt";
    private static final String HISTORY_FILE = "zdb_history.txt";
    private static final String ALIASES_FILE = "aliases.txt";

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

        /* data is in one of three formats --

         [ALIASES.TXT]
         primaryIdentifier /t name /t symbol /t old name
         eg, ZDB-GENE-000329-4       notch homolog 2 notch2  sb:cb884

         [ZDB_HISTORY.TXT]
         primaryidentifier /t old primaryidentifier
         ZDB-GENE-000607-4       ZDB-GENE-010129-1

         [ENSEMBL_1_to_1.txt]
         #    ZDBID              SYMBOL  Ensembl(Zv7)
         ZDB-GENE-000112-47      ppardb  ENSDARG00000009473

        */

        String filename = null;

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            // the first column is primary identifier in all three files
            String primaryIdentifier = line[0];

            // identifier files contain non-genes.  we are only processing genes for now.
            if (!primaryIdentifier.contains("GENE")) {
                continue;
            }

            Item gene = getGene(primaryIdentifier);

            String refId = gene.getIdentifier();

            if (filename == null) {
                if (line.length == 2 && line[1].startsWith("ZDB-")) {
                    filename = HISTORY_FILE;
                } else if (line.length > 2) {
                    if (line[2].startsWith("ENSDARG")) {
                        filename = ENSEMBL_FILE;
                    } else {
                        filename = ALIASES_FILE;
                    }
                } else {
                    throw new RuntimeException("Bad file format.");
                }
            }

            if (filename.equals(ENSEMBL_FILE)) {

                String symbol = line[1];
                String secondaryIdentifier = line[2];

                if (!StringUtils.isEmpty(secondaryIdentifier)) {
                    gene.setAttribute("secondaryIdentifier", secondaryIdentifier);
                    setSynonym(refId, "identifier", secondaryIdentifier);
                }
                if (!StringUtils.isEmpty(symbol)) {
                    gene.setAttribute("symbol", symbol);
                    setSynonym(refId, "symbol", symbol);
                }

            } else if (filename.equals(HISTORY_FILE)) {
                String synonym = line[1];
                if (!StringUtils.isEmpty(synonym)) {
                    setSynonym(refId, "identifier", synonym);
                }
            } else {
                String name = line[1];
                String symbol = line[2];

                if (!StringUtils.isEmpty(name)) {
                    gene.setAttribute("name", name);
                    setSynonym(refId, "name", name);
                }
                if (!StringUtils.isEmpty(symbol)) {
                    gene.setAttribute("symbol", symbol);
                    setSynonym(refId, "symbol", symbol);
                }

                // loop through the rest of the synonyms
                for (int i = 3; i < line.length; i++) {
                    String synonym = line[i];
                    if (!StringUtils.isEmpty(synonym)) {
                        setSynonym(refId, "identifier", synonym);
                    }
                }
            }
        }
    }

    /**
     * After all files are processed, store genes.
     *
     * {@inheritDoc}
     */
    public void close()
    throws SAXException {
        for (Item gene : genes.values()) {
            try {
                store(gene);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
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
            item.setReference("organism", organismRefId);
            setSynonym(item.getIdentifier(), "identifier", primaryIdentifier);
        }
        return item;
    }

    private void setSynonym(String subjectRefId, String type, String value)
    throws SAXException {
        String key = subjectRefId + type + value;
        if (!synonyms.contains(key)) {
            Item synonym = createItem("Synonym");
            synonym.setAttribute("type", type);
            synonym.setAttribute("value", value);
            synonym.setReference("subject", subjectRefId);
            synonyms.add(key);
            try {
                store(synonym);
            } catch (ObjectStoreException e) {
                throw new SAXException(e);
            }
        }
    }
}
