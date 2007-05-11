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
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

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
 * DataConverter to load a flat file containing FlyBase gene ids, gene full names
 * and wild-type functions.
 *
 * @author Richard Smith
 */
public class FlyBaseGeneNamesConverter extends FileConverter
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
    public FlyBaseGeneNamesConverter(ItemWriter writer)
        throws ObjectStoreException, MetaDataException {
        super(writer);

        itemFactory = new ItemFactory(Model.getInstanceByName("genomic"), "-1_");

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "FlyBase");
        getItemWriter().store(ItemHelper.convert(dataSource));

        dmel = createItem("Organism");
        dmel.setAttribute("taxonId", "7227");
        getItemWriter().store(ItemHelper.convert(dmel));

        dpse = createItem("Organism");
        dpse.setAttribute("taxonId", "7237");
        getItemWriter().store(ItemHelper.convert(dpse));
    }


    /**
     * Read each line from flat file, create genes and synonyms.
     *
     * @see DataConverter#process
     */
    public void process(Reader reader) throws Exception {
        Iterator lineIter = TextFileUtil.parseTabDelimitedReader(reader);
        int lineNo = 0;

        // data is in format:
        // FBgn | symbol | current fullname | fullname synonyms | symbol synonyms

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            Set geneSynonyms = new HashSet();

            if (line.length <= 1 || line[0].startsWith("#")) {
                continue;
            }

            if (line.length < 2) {
                throw new RuntimeException("Line does not have enough elements: "
                                           + Arrays.asList(line) + " (line number "
                                           + lineNo);
            }
            String fbgn = line[0];
            String symbol = line[1];
            String name = line[2];

            Item organism = null;

            // symbols have format Dpse\x, Dyak\y, z where missing D???\ means Dmel
            if (symbol.indexOf("\\") < 0) {
                organism = dmel;
            } else if (symbol.startsWith("Dpse\\")) {
                organism = dpse;
            }

            // create id a name or a symbol
            if (organism != null && ((name != null && !name.equals(""))
                                     || (symbol != null && !symbol.equals("")))) {
                // set and create synonym for all fields, some genes only come from this source
                Item gene = createItem("Gene");
                gene.setAttribute("organismDbId", fbgn);
                createSynonym(gene, "identifier", fbgn);
                geneSynonyms.add(fbgn);

                if ((symbol != null) && !(symbol.equals(""))) {
                    gene.setAttribute("symbol", symbol);
                    createSynonym(gene, "symbol", symbol);
                    geneSynonyms.add(symbol);
                }

                if ((name != null) && !(name.equals(""))) {
                    gene.setAttribute("name", name);
                    createSynonym(gene, "name", name);
                    geneSynonyms.add(name);
                }

                if ((line.length >= 4) && line[3] != null && !(line[3].equals(""))) {
                    ArrayList synonyms = new ArrayList(Arrays.asList(line[3].split(",")));
                    Iterator iter = synonyms.iterator();
                    while (iter.hasNext()) {
                        String synonym = ((String) iter.next()).trim();
                        if (!geneSynonyms.contains(synonym)) {
                            createSynonym(gene, "name", synonym);
                            geneSynonyms.add(synonym);
                        }
                    }
                }

                gene.setReference("organism", organism.getIdentifier());
                getItemWriter().store(ItemHelper.convert(gene));
            }
            lineNo++;
        }
    }

    private void createSynonym(Item subject, String type, String value)
        throws ObjectStoreException {
        Item synonym = createItem("Synonym");
        synonym.setAttribute("type", type);
        synonym.setAttribute("value", value);
        synonym.setReference("source", dataSource.getIdentifier());
        synonym.setReference("subject", subject.getIdentifier());
        getItemWriter().store(ItemHelper.convert(synonym));
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
