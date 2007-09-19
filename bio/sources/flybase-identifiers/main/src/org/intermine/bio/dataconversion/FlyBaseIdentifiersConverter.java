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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.intermine.dataconversion.DataConverter;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.TextFileUtil;
import org.intermine.xml.full.Item;

/**
 * DataConverter to load FlyBase CG, FBgn and symbol and secondary identifiers as
 * synonyms.
 *
 * @author Richard Smith
 */
public class FlyBaseIdentifiersConverter extends FileConverter
{
    protected Item dataSource, dmel, dpse;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     */
    public FlyBaseIdentifiersConverter(ItemWriter writer, Model model)
        throws ObjectStoreException, MetaDataException {
        super(writer, model);

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "FlyBase");
        store(dataSource);

        dmel = createItem("Organism");
        dmel.setAttribute("taxonId", "7227");
        store(dmel);

        dpse = createItem("Organism");
        dpse.setAttribute("taxonId", "7237");
        store(dpse);
    }


    /**
     * Read each line from flat file, create genes and synonyms.
     *
     * @see DataConverter#process
     */
    public void process(Reader reader) throws Exception {
        Iterator lineIter = TextFileUtil.parseTabDelimitedReader(reader);

        // data is in format
        // symbol | primary FBGN | secondary FBGNs | primary CG | secondary CGs

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line.length <= 1 || line[0].startsWith("#")) {
                continue;
            }

            if (line.length < 5) {
                throw new RuntimeException("Line does not have enough elements: "
                                           + Arrays.asList(line));
            }
            HashSet geneIds = new HashSet();
            String symbol = line[0];
            String fbgn = line[1];
            String cg = line[3];

            Item gene = createItem("Gene");
            if (cg != null && !cg.equals("")) {
                gene.setAttribute("identifier", cg);
                if (!geneIds.contains(cg)) {
                    createSynonym(gene, "identifier", cg);
                    geneIds.add(cg);
                }
            }
            if (fbgn != null && !fbgn.equals("")) {
                gene.setAttribute("organismDbId", fbgn);
                if (!geneIds.contains(fbgn)) {
                    createSynonym(gene, "identifier", fbgn);
                    geneIds.add(fbgn);
                }
            }
            if (symbol != null && !symbol.equals("")) {
                gene.setAttribute("symbol", symbol);
                if (!geneIds.contains(symbol)) {
                    createSynonym(gene, "symbol", symbol);
                    geneIds.add(symbol);
                }
            }

            ArrayList synonyms = new ArrayList(Arrays.asList(line[2].split(",")));
            synonyms.addAll(Arrays.asList((line[4].split(","))));
            Iterator iter = synonyms.iterator();
            while (iter.hasNext()) {
                String identifier = (String) iter.next();
                if (!identifier.equals("") && !geneIds.contains(identifier)) {
                    createSynonym(gene, "identifier", identifier);
                    geneIds.add(identifier);
                }
            }

            gene.setReference("organism", getOrganism(symbol));
            store(gene);

        }
    }

    private String getOrganism(String symbol) {
        if (symbol.startsWith("Dpse")) {
            return dpse.getIdentifier();
        } else {
            return dmel.getIdentifier();
        }
    }

    private void createSynonym(Item subject, String type, String value)
        throws ObjectStoreException {
        Item synonym = createItem("Synonym");
        synonym.setAttribute("type", type);
        synonym.setAttribute("value", value);
        synonym.setReference("source", dataSource.getIdentifier());
        synonym.setReference("subject", subject.getIdentifier());
        store(synonym);
    }
}
