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

import java.io.File;
import java.io.Reader;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.collections.map.MultiKeyMap;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * Parent class for DataConverters that read from files
 * @author Mark Woodbridge
 */
public abstract class FileConverter extends DataConverter
{
    private File currentFile;
    private MultiKeyMap itemsMap;

    /**
     * Constructor
     * @param writer the Writer used to output the resultant items
     * @param model the data model
     */
    public FileConverter(ItemWriter writer, Model model) {
        super(writer, model);
        itemsMap = new MultiKeyMap();
    }
    
    /**
     * Perform the currentFile conversion
     * @param reader BufferedReader used as input
     * @throws Exception if an error occurs during processing
     */
    public abstract void process(Reader reader) throws Exception;

    /**
     * Perform any necessary clean-up after post-conversion
     * @throws Exception if an error occurs
     */
    public void close() throws Exception {
        // empty
    }

    /**
     * Set the current File that is being processed.  Called by FileConverterTask.execute().
     * @param currentFile the current File that is being processed
     */
    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
    }
    
    /**
     * Return the File that is currently being converted.
     * @return the current File
     */
    public File getCurrentFile() {
        return currentFile;
    }
    
    /**
     * Get an item and if it doesn't already exist create it and store it
     * @param className the name of the class
     * @param attributeName the name of the attribute to set
     * @param identifier the identifier
     * @return an Item
     */
    public Item getAndStoreItemOnce(String className, String attributeName, String identifier) {
        MultiKey key = new MultiKey(className, identifier);
        Item item = (Item) itemsMap.get(key);
        if (itemsMap.get(key) == null) {
            item = (Item) itemsMap.get(key);
            item = createItem(className);
            item.setAttribute(attributeName, identifier);
            itemsMap.put(key, item);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("error while storing: " + identifier, e);
            }
        }   
        return item;
    }
    
}
