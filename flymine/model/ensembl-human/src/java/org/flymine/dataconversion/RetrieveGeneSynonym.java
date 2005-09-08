package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ItemHelper;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;

/**
 * DataConverter to parse an RNAi data file into Items
 * @author Wenyan Ji
 */
public class RetrieveGeneSynonym extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";

    protected Map genes = new HashMap();
    protected Map synonyms = new HashMap();
    protected Item db;
    protected int id = 0;
    protected String synonymtype = null;


    /**
     * Set the value the Synonym.type field should have when retrieving Synonym.
     * @param synonymtype the Synonym type
     */

    public void setSynonymtype(String synonymtype) {
        this.synonymtype = synonymtype;
    }

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public RetrieveGeneSynonym(ItemWriter writer) throws ObjectStoreException {
        super(writer);

        db = createItem("DataSource");
        db.addAttribute(new Attribute("title", "ensembl"));
        writer.store(ItemHelper.convert(db));

    }


    /**
     * @see DataConverter#process
     */
    public void process(Reader reader) throws Exception {
        if (synonymtype == null) {
            throw new NullPointerException("synonymType not set");
        } else {

            BufferedReader br = new BufferedReader(reader);
            //intentionally throw away first line
            String line = br.readLine();

            while ((line = br.readLine()) != null) {
                String[] array = line.split("\t", -1); //keep trailing empty Strings
                if (array[0] != null && array[1].length() > 0) {
                    Item gene = getGene(array[0]);
                    Item synonym = getSynonym(array[1], synonymtype.concat("Id"),
                                   gene.getIdentifier());
                }
            }
        }
    }

    /**
     * @see FileConverter#close
     */
    public void close() throws ObjectStoreException {
        store(genes.values());
        store(synonyms.values());
    }


    /**
     * Return the Gene Item by organismDbId.
     * @param organismDbId set a map genes with organismDbId as key and geneItem as value
     * @throws ObjectStoreException if an error occurs while reading
     * @return gene item
     */
    protected Item getGene(String organismDbId) throws ObjectStoreException {
        Item item = (Item) genes.get(organismDbId);
        if (item == null) {
            item = createItem("Gene");
            item.addAttribute(new Attribute("organismDbId", organismDbId));
            genes.put(organismDbId, item);
        }
        return item;
    }


    /**
     * @param value set synonym map with value as key and synonym item as value
     * @param type synonym type
     * @param subjectId synonym subject reference
     * @throws ObjectStoreException if an error occurs while reading
     * @return synonym item
     */
    protected Item getSynonym(String value, String type, String subjectId )
        throws ObjectStoreException {
        Item item = (Item) synonyms.get(value);
        if (item == null) {
            item = createItem("Synonym");
            item.addAttribute(new Attribute("type", type));
            item.addAttribute(new Attribute("value", value));
            item.addReference(new Reference("source", db.getIdentifier()));
            item.addReference(new Reference("subject", subjectId));
            synonyms.put(value, item);
        }
        return item;
    }

    /**
     * Convenience method for creating a new Item
     * @param className the name of the class
     * @return a new Item
     */
    protected Item createItem(String className) {
        Item item = new Item();
        item.setIdentifier(alias(className) + "_" + (id++));
        item.setClassName(GENOMIC_NS + className);
        item.setImplementations("");
        return item;
    }

}
