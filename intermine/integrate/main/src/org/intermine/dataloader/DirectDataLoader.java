package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.DynamicUtil;

/**
 * A DataLoader with helper methods for creating and storing objects using an IntegrationWriter.
 *
 * @author Kim Rutherford
 */

public class DirectDataLoader extends DataLoader
{
    private int idCounter = 0;
    private String sourceName;
    
    /**
     * Create a new DirectDataLoader using the given IntegrationWriter and source name.
     * @param iw an IntegrationWriter
     * @param sourceName the source name
     * 
     */
    public DirectDataLoader (IntegrationWriter iw, String sourceName) {
        super(iw);
        this.sourceName = sourceName;
    }


    /**
     * Store an object using the IntegrationWriter.
     * @param o the InterMineObject
     * @throws ObjectStoreException if there is a problem in the IntegrationWriter
     */
    public void store(InterMineObject o) throws ObjectStoreException {
        Source source = getIntegrationWriter().getMainSource(sourceName);
        Source skelSource = getIntegrationWriter().getSkeletonSource(sourceName);

        getIntegrationWriter().store(o, source, skelSource);
    }
    
    /**
     * Create a new object of the given class name and give it a unique ID.
     * @param className the class name
     * @return the new InterMineObject
     * @throws ClassNotFoundException if the given class doesn't exist
     */
    public InterMineObject createObject(String className) throws ClassNotFoundException {
        return createObject(Class.forName(className));
    }
    
    /**
     * Create a new object of the given class and give it a unique ID.
     * @param c the class
     * @return the new InterMineObject
     */
    public InterMineObject createObject(Class c) {
        InterMineObject o = (InterMineObject) DynamicUtil.createObject(Collections.singleton(c));
        o.setId(new Integer(idCounter));
        idCounter++;
        return o;
    }
}
