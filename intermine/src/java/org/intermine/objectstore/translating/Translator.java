package org.flymine.objectstore.translating;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.flymine.objectstore.query.Query;
import org.flymine.model.FlyMineBusinessObject;

/**
 * Interface specifying operations required for inline translation of queries and data objects
 * by a translating ObjectStore
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public interface Translator
{
    /**
     * Translate a query
     * @param query the Query to translate
     * @return the translated query
     */
    public Query translateQuery(Query query);
    
    /**
    * Translate an object entering the ObjectStore
    * @param o the FlyMineBusinessObject to translate
    * @return the translated object
    */
    public FlyMineBusinessObject translateToDbObject(FlyMineBusinessObject o);
    
    /**
     * Translate an object exiting the ObjectStore
     * @param o the object to translate
     * @return the translated object
     */
    public FlyMineBusinessObject translateFromDbObject(FlyMineBusinessObject o);
}
