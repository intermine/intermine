package org.intermine.objectstore.flymine;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.ref.WeakReference;

/**
 * This class supports ObjectStoreWriterFlyMineImpl, and prints out statistics on exit, if the
 * ObjectStoreFlyMineImpl is still in memory.
 *
 * @author Matthew Wakeling
 */
public class StatsShutdownHook extends Thread
{
    private WeakReference wr;

    /**
     * Creates a StatsShutdownHook for a given ObjectStoreWriterFlyMineImpl.
     *
     * @param osw an ObjectStoreWriterFlyMineImpl
     */
    public StatsShutdownHook(ObjectStoreWriterFlyMineImpl osw) {
        wr = new WeakReference(osw);
    }

    /**
     * @see Thread#run
     */
    public void run() {
        ObjectStoreWriterFlyMineImpl osw = (ObjectStoreWriterFlyMineImpl) wr.get();
        if (osw != null) {
            osw.shutdown();
        }
    }
}
