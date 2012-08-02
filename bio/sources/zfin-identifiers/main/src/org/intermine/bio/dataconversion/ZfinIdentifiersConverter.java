package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
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

/**
 * Data converter to load ZFIN Identifiers from:
 * /micklem/data/zfin-identifiers/current/ensembl_1_to_1.txt
 *
 * @author Fengyuan Hu
 */
public class ZfinIdentifiersConverter extends BioFileConverter
{
    private static final String DATASET_TITLE = "ZFIN";
    private static final String DATA_SOURCE_NAME = "ZFIN genes";

    private static final String GENE_PATTERN = "ZDB-GENE";
    private static final String FISH_TAXON = "7955";

    private Map<String, String> identifiersToGenes = new HashMap<String, String>();
    private Set<String> crossReferences = new HashSet<String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public ZfinIdentifiersConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * Read each line from flat file, create genes and cross references.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        // data is in format:
        // ZDBID  SYMBOL  Ensembl(Zv9)

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line.length < 3 || line[0].startsWith("#") || !line[0].startsWith(GENE_PATTERN)) {
                continue;
            }
            String primaryidentifier = line[0];
            String symbol = line[1];
            String crossReferenceIdentifier = line[2];

            String refId = identifiersToGenes.get(primaryidentifier);
            if (refId == null) {
                Item gene = createItem("Gene");
                refId = gene.getIdentifier();

                if (!StringUtils.isEmpty(primaryidentifier)) {
                    gene.setAttribute("primaryIdentifier", primaryidentifier);

                    if (!StringUtils.isEmpty(symbol)) {
                        gene.setAttribute("symbol", symbol);
                    }
                    if (!StringUtils.isEmpty(crossReferenceIdentifier)) {
                        createCrossReference(refId, crossReferenceIdentifier);
                    }
                }

                gene.setReference("organism", getOrganism(FISH_TAXON));

                identifiersToGenes.put(primaryidentifier, refId);
                store(gene);
            } else {
                createCrossReference(refId, crossReferenceIdentifier);
            }
        }
    }

    private void createCrossReference(String geneRefId, String identifier)
            throws ObjectStoreException {
        if (!StringUtils.isEmpty(identifier)) {
            String key = geneRefId + identifier;
            if (!crossReferences.contains(key)) {
                Item item = createItem("CrossReference");
                item.setAttribute("identifier", identifier);
                item.setReference("subject", geneRefId);
//                item.setReference("source", getDataSource(dataSource));
                crossReferences.add(key);
                store(item);
            }
        }
    }
}
