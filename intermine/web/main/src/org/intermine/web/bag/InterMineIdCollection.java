package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

/**
 * A collection of InterMine object ids.
 * @author Kim Rutherford
 */
public interface InterMineIdCollection
{
    /**
     * Add an id to this Collection.
     * @param id intermine id
     */
    public void addId(int id);
    
    /**
     * Remove an id.
     * @param id an intermine id
     */
    public void removeId(int id);
    
    /**
     * Return a List of InterMineObjects corresponding to the ids in the Collection.
     * @return List of InterMineObjects
     */
    public List toObjectList();
}
