package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

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
    private String sourceType;

    /**
     * Create a new DirectDataLoader using the given IntegrationWriter and source name.
     * @param iw an IntegrationWriter
     * @param sourceName the source name
     * @param sourceType the source type
     */
    public DirectDataLoader (IntegrationWriter iw, String sourceName, String sourceType) {
        super(iw);
        this.sourceName = sourceName;
        this.sourceType = sourceType;
    }


    /**
     * Store an object using the IntegrationWriter.
     * @param o the InterMineObject
     * @throws ObjectStoreException if there is a problem in the IntegrationWriter
     */
    public void store(InterMineObject o) throws ObjectStoreException {
        Source source = getIntegrationWriter().getMainSource(sourceName, sourceType);
        Source skelSource = getIntegrationWriter().getSkeletonSource(sourceName, sourceType);

        getIntegrationWriter().store(o, source, skelSource);
    }

    /**
     * Create a new object of the given class name and give it a unique ID.
     * @param className the class name
     * @return the new InterMineObject
     * @throws ClassNotFoundException if the given class doesn't exist
     */
    @SuppressWarnings("unchecked")
    public InterMineObject createObject(String className) throws ClassNotFoundException {
        return createObject((Class<? extends InterMineObject>) Class.forName(className));
    }

    /**
     * Create a new object of the given class and give it a unique ID.
     * @param c the class
     * @param <C> the type of the class
     * @return the new InterMineObject
     */
    public <C extends InterMineObject> C createObject(Class<C> c) {
        C o = DynamicUtil.simpleCreateObject(c);
        o.setId(new Integer(idCounter));
        idCounter++;
        return o;
    }
}
