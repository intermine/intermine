package org.intermine.dataconversion;

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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.metadata.Model;
import org.intermine.metadata.MetaDataException;
import org.intermine.model.InterMineObject;
import org.intermine.util.TypeUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.ItemFactory;

import org.apache.log4j.Logger;

/**
 * DataConverter to produce random tgt-item data.
 *
 * @author Matthew Wakeling
 */
public class ComplexGenerator extends FileConverter
{
    protected static final String NS = "http://www.intermine.org/model/performancetest1#";

    protected static final Logger LOG = Logger.getLogger(ComplexGenerator.class);

    protected ItemFactory itemFactory;
    protected Map ids = new HashMap();
    private int count;

    /**
     * Constructor.
     *
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     */
    public ComplexGenerator(ItemWriter writer)
        throws ObjectStoreException, MetaDataException {
        super(writer);

        itemFactory = new ItemFactory(Model.getInstanceByName("performancetest1"), "-1_");
    }

    /**
     * Setter for object count.
     *
     * @param count the number of objects to create
     */
    public void setCount(String count) {
        this.count = Integer.parseInt(count);
    }

    /**
     * Produce random data.
     *
     * @param inputFile an ignored Reader
     */
    public void process(Reader inputFile) throws Exception {
        List<Item> attributes = new ArrayList();
        long time = System.currentTimeMillis();
        for (int i = 0; i < count / 10; i++) {
            Item master = createItem("ComplexMaster");
            Item ref1 = createItem("ComplexAttribute");
            ref1.setAttribute("att", "" + getInteger());
            attributes.add(ref1);
            master.setReference("ref1", ref1.getIdentifier());
            Item ref2 = createItem("ComplexAttribute");
            ref2.setAttribute("att", "" + getInteger());
            attributes.add(ref2);
            master.setReference("ref2", ref2.getIdentifier());
            List<String> idsToUse = new ArrayList();
            for (int o = 0; o < 7; o++) {
                Item col = createItem("ComplexAttribute");
                attributes.add(col);
                col.setAttribute("att", "" + getInteger());
                idsToUse.add(col.getIdentifier());
            }
            master.setCollection("col", idsToUse);
            getItemWriter().store(ItemHelper.convert(master));
        }
        for (Item item : attributes) {
            getItemWriter().store(ItemHelper.convert(item));
        }
        long now = System.currentTimeMillis();
        LOG.info("Finished generating " + count + " objects at " + ((60000L * count)
                    / (now - time)) + " objects per minute (" + (now - time)
                + " ms total)");
    }

    private Random random = new Random();
    private Set doneValues = new HashSet();

    private Integer getInteger() {
        Integer retval;
        do {
            retval = new Integer(random.nextInt());
        } while (doneValues.contains(retval));
        doneValues.add(retval);
        return retval;
    }

    private String newId(String className) {
        Integer id = (Integer) ids.get(className);
        if (id == null) {
            id = new Integer(0);
            ids.put(className, id);
        }
        ids.put(className, new Integer(id.intValue() + 1));
        return id.toString();
    }

    private Item createItem(String className) {
        return itemFactory.makeItem(alias(className) + "_" + newId(className), NS + className, "");
    }
}
