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
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Arrays;

import org.intermine.util.TextFileUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.metadata.Model;
import org.intermine.metadata.MetaDataException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.ItemFactory;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.FileConverter;

/**
 * DataConverter to load FlyBase CG, FBgn and symbol and secondary identifiers as
 * synonyms.
 *
 * @author Richard Smith
 */
public class FlyBaseIdentifiersConverter extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";

    protected Item dataSource, dmel, dpse;
    protected ItemFactory itemFactory;
    protected Map ids = new HashMap();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     */
    public FlyBaseIdentifiersConverter(ItemWriter writer)
        throws ObjectStoreException, MetaDataException {
        super(writer);

        itemFactory = new ItemFactory(Model.getInstanceByName("genomic"), "-1_");

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "FlyBase");
        writer.store(ItemHelper.convert(dataSource));

        dmel = createItem("Organism");
        dmel.setAttribute("taxonId", "7227");
        writer.store(ItemHelper.convert(dmel));

        dpse = createItem("Organism");
        dpse.setAttribute("taxonId", "7237");
        writer.store(ItemHelper.convert(dpse));
    }


    /**
     * Read each line from flat file, create genes and synonyms.
     *
     * @see DataConverter#process
     */
    public void process(Reader reader) throws Exception {
        String arrayName;
        Iterator lineIter = TextFileUtil.parseTabDelimitedReader(reader);
        int lineNo = 0;
        boolean readingData = false;

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
            // get the name of the array.  TODO also version?
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
            writer.store(ItemHelper.convert(gene));

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
        writer.store(ItemHelper.convert(synonym));
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
