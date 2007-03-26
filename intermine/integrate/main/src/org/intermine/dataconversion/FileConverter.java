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
import java.util.Iterator;
import java.util.Collection;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.Item;

/**
 * Parent class for DataConverters that read from files
 * @author Mark Woodbridge
 */
public abstract class FileConverter extends DataConverter
{
    protected String param1;
    protected String param2;
    private File currentFile;

    /**
     * Constructor
     * @param writer the Writer used to output the resultant items
     */
    public FileConverter(ItemWriter writer) {
        super(writer);
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
     * Set some param1 to some String value.
     * @param param1 value for param1
     */
    public void setParam1(String param1) {
        this.param1 = param1;
    }

    /**
     * Set some param2 to some String value.
     * @param param2 value for param1
     */
    public void setParam2(String param2) {
        this.param2 = param2;
    }

    /**
     * Store a single Item
     * @param item the Item
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected void store(Item item) throws ObjectStoreException {
        writer.store(ItemHelper.convert(item));
    }
    
    /**
     * Store a Collection of Items
     * @param c the Collection
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected void store(Collection c) throws ObjectStoreException {
        for (Iterator i = c.iterator(); i.hasNext(); ) {
            writer.store(ItemHelper.convert((Item) i.next()));
        }
    }
}
