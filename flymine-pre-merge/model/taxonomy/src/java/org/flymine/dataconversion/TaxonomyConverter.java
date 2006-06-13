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
import java.io.IOException;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.ItemHelper;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;

/**
 * DataConverter to parse a taxonomy data file into Items
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class TaxonomyConverter extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";
    protected int id = 0;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public TaxonomyConverter(ItemWriter writer) throws ObjectStoreException {
        super(writer);
    }

    /**
     * @see FileConverter#process
     */
    public void process(Reader reader) throws ObjectStoreException, IOException {
        BufferedReader br = new BufferedReader(reader);
        String line, taxonId = null, rank = null;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("ID")) {
                taxonId = split(line);
            }
            if (line.startsWith("RANK")) {
                rank = split(line);
            }
            if (line.startsWith("SCIENTIFIC NAME") && "species".equals(rank)) {
                Item organism = newItem("Organism");
                organism.addAttribute(new Attribute("taxonId", taxonId));
                organism.addAttribute(new Attribute("name", split(line)));
                writer.store(ItemHelper.convert(organism));
            }
        }
    }

    /**
     * Return the substring following the first colon of a String
     * @param line the String
     * @return the remainder of the String
     */
    protected String split(String line) {
        return line.substring(line.indexOf(":") + 1).trim();
    }

    /**
     * Convenience method for creating a new Item
     * @param className the name of the class
     * @return a new Item
     */
    protected Item newItem(String className) {
        Item item = new Item();
        item.setIdentifier(alias(className) + "_" + (id++));
        item.setClassName(GENOMIC_NS + className);
        item.setImplementations("");
        return item;
    }
}
