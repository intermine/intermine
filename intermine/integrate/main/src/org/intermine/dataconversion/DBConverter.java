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

import org.intermine.metadata.Model;
import org.intermine.sql.Database;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;

/**
 * A DataConverter that reads from a Database and writes to a ItemWriter
 * @author Kim Rutherford
 */
public abstract class DBConverter extends DataConverter
{
    private final Database database;
    private final Model tgtModel;
    private final ItemFactory itemFactory;
    
    /**
     * Constructor
     * @param writer an ItemWriter used to handle the resultant Items
     * @param tgtModel the Model used by the object store we will write to with the ItemWriter
     * @param database the database to read from
     */
    public DBConverter(Database database, Model tgtModel, ItemWriter writer) {
        super(writer);
        this.database = database;
        this.tgtModel = tgtModel;
        itemFactory = new ItemFactory(tgtModel);
    }

    /**
     * Query from database and write items to the writer.
     * @throws Exception if there is a problem while processing
     */
    public abstract void process() throws Exception;

    /**
     * Make a new Item with the given className.
     * @param className a name of a Class in the Model that was passed to the constructor
     * @return a new Item
     */
    public Item makeItem(String className) {
        return itemFactory.makeItemForClass(tgtModel.getNameSpace() + className);
    }
    
    /**
     * Get the Database that was passed to the constructor.
     * @return the Database
     */
    public Database getDatabase() {
        return database;
    }

}
