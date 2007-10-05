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
public class RandomDataGenerator extends FileConverter
{
    protected static final String NS = "http://www.intermine.org/model/performancetest1#";

    protected static final Logger LOG = Logger.getLogger(RandomDataGenerator.class);

    protected ItemFactory itemFactory;
    protected Map ids = new HashMap();
    private int count;
    private int stringSize = 20;
    private int collectionSize = 20;
    private String className;
    private List<String> idsUsed = new ArrayList<String>();

    /**
     * Constructor.
     *
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the data model
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     */
    public RandomDataGenerator(ItemWriter writer, Model model)
        throws ObjectStoreException, MetaDataException {
        super(writer, model);

        itemFactory = new ItemFactory(model, "-1_");
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
     * Setter for string size.
     *
     * @param stringSize the size of generated strings
     */
    public void setStringSize(String stringSize) {
        this.stringSize = Integer.parseInt(stringSize);
    }

    /**
     * Setter for collection size.
     *
     * @param collectionSize the size of the generated collections
     */
    public void setCollectionSize(String collectionSize) {
        this.collectionSize = Integer.parseInt(collectionSize);
    }

    /**
     * Setter for class name.
     *
     * @param className the name of the class of which to create random objects
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Produce random data.
     *
     * @param inputFile an ignored Reader
     */
    public void process(Reader inputFile) throws Exception {
        // Discover field names and types
        Set<String> intFieldNames = new TreeSet<String>();
        Set<String> stringFieldNames = new TreeSet<String>();
        Set<String> dateFieldNames = new TreeSet<String>();
        Set<String> bigDecimalFieldNames = new TreeSet<String>();
        Set<String> longFieldNames = new TreeSet<String>();
        Set<String> itemFieldNames = new TreeSet<String>();
        Set<String> collectionFieldNames = new TreeSet<String>();
        Class clazz = Class.forName("org.intermine.model.performancetest1." + className);
        for (Map.Entry<String, TypeUtil.FieldInfo> entry : TypeUtil.getFieldInfos(clazz).entrySet()) {
            if ("id".equals(entry.getKey())) {
                // Do nothing
            } else if (Integer.TYPE.equals(entry.getValue().getType())) {
                intFieldNames.add(entry.getKey());
            } else if (String.class.equals(entry.getValue().getType())) {
                stringFieldNames.add(entry.getKey());
            } else if (Date.class.equals(entry.getValue().getType())) {
                dateFieldNames.add(entry.getKey());
            } else if (BigDecimal.class.equals(entry.getValue().getType())) {
                bigDecimalFieldNames.add(entry.getKey());
            } else if (Long.TYPE.equals(entry.getValue().getType())) {
                longFieldNames.add(entry.getKey());
            } else if (InterMineObject.class.isAssignableFrom(entry.getValue().getType())) {
                itemFieldNames.add(entry.getKey());
            } else if (Collection.class.isAssignableFrom(entry.getValue().getType())) {
                collectionFieldNames.add(entry.getKey());
            } else {
                throw new IllegalArgumentException("Unknown field type " + entry.getValue().getType() + " for field " + clazz.getName() + "." + entry.getKey());
            }
        }
        Random random = new Random();
        Set doneValues = new HashSet();
        long time = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            Item item = createItem(className);
            Random itemRandom;
            Integer firstInt;
            String firstString;
            Date firstDate;
            BigDecimal firstBigDecimal;
            Long firstLong;
            do {
                itemRandom = new Random(random.nextLong());
                firstInt = new Integer(itemRandom.nextInt());
                firstString = generateString(itemRandom);
                firstDate = new Date(itemRandom.nextLong());
                firstBigDecimal = generateBigDecimal(itemRandom);
                firstLong = new Long(itemRandom.nextLong());
            } while (doneValues.contains(firstInt) || doneValues.contains(firstString)
                    || doneValues.contains(firstDate) || doneValues.contains(firstBigDecimal)
                    || doneValues.contains(firstLong));
            idsUsed.add(item.getIdentifier());
            boolean firstField = true;
            for (String fieldName : intFieldNames) {
                if (firstField) {
                    item.setAttribute(fieldName, "" + firstInt);
                    doneValues.add(firstInt);
                } else {
                    item.setAttribute(fieldName, "" + itemRandom.nextInt());
                }
                firstField = false;
            }
            firstField = true;
            for (String fieldName : stringFieldNames) {
                if (firstField) {
                    item.setAttribute(fieldName, firstString);
                    doneValues.add(firstString);
                } else {
                    item.setAttribute(fieldName, generateString(itemRandom));
                }
                firstField = false;
            }
            firstField = true;
            for (String fieldName : dateFieldNames) {
                if (firstField) {
                    item.setAttribute(fieldName, "" + firstDate.getTime());
                    doneValues.add(firstDate);
                } else {
                    item.setAttribute(fieldName, "" + itemRandom.nextLong());
                }
                firstField = false;
            }
            firstField = true;
            for (String fieldName : bigDecimalFieldNames) {
                if (firstField) {
                    item.setAttribute(fieldName, "" + firstBigDecimal);
                    doneValues.add(firstBigDecimal);
                } else {
                    item.setAttribute(fieldName, "" + generateBigDecimal(itemRandom));
                }
                firstField = false;
            }
            firstField = true;
            for (String fieldName : longFieldNames) {
                if (firstField) {
                    item.setAttribute(fieldName, "" + firstLong);
                    doneValues.add(firstLong);
                } else {
                    item.setAttribute(fieldName, "" + itemRandom.nextLong());
                }
                firstField = false;
            }
            for (String fieldName : itemFieldNames) {
                item.setReference(fieldName, idsUsed.get(itemRandom.nextInt(idsUsed.size())));
            }
            for (String fieldName : collectionFieldNames) {
                Set<String> idsToUse = new HashSet<String>();
                for (int o = 0; o < collectionSize; o++) {
                    idsToUse.add(idsUsed.get(itemRandom.nextInt(idsUsed.size())));
                }
                item.setCollection(fieldName, new ArrayList(idsToUse));
            }
            getItemWriter().store(ItemHelper.convert(item));
        }
        long now = System.currentTimeMillis();
        LOG.info("Finished generating " + count + " objects at " + ((60000L * count)
                    / (now - time)) + " objects per minute (" + (now - time)
                + " ms total)");
    }

    private String generateString(Random random) {
        StringBuffer retval = new StringBuffer();
        for (int i = 0; i < stringSize; i++) {
            retval.append(((char) random.nextInt(26)) + 'a');
        }
        return retval.toString();
    }

    private BigDecimal generateBigDecimal(Random random) {
        StringBuffer retval = new StringBuffer();
        for (int i = 0; i < 20; i++) {
            retval.append(((char) random.nextInt(10)) + '0');
        }
        retval.append('.');
        for (int i = 0; i < 20; i++) {
            retval.append(((char) random.nextInt(10)) + '0');
        }
        return new BigDecimal(retval.toString());
    }

    protected String newId(String className) {
        Integer id = (Integer) ids.get(className);
        if (id == null) {
            id = new Integer(0);
            ids.put(className, id);
        }
        ids.put(className, new Integer(id.intValue() + 1));
        return id.toString();
    }

    public Item createItem(String className) {
        return itemFactory.makeItem(alias(className) + "_" + newId(className), NS + className, "");
    }
}
