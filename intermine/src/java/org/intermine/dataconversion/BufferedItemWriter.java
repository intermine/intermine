package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flymine.model.fulldata.Item;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.util.ObjectPipe;

import org.apache.log4j.Logger;

/**
 * Stores Items in another ItemStore. This class decouples the thread that calls methods in this
 * class from the actual act of storing the Items, improving performance. Actual storing is done in
 * a separate Thread. This class uses an ObjectPipe to communicate the Items to be stored to the
 * Thread that performs the store. The store() and storeAll() methods should never take much time to
 * return to the caller unless the buffer is full, however the close() method will block until all
 * the Items are fully stored, or until an exception is thrown.
 *
 * @author Matthew Wakeling
 */
public class BufferedItemWriter implements ItemWriter
{
    protected static final Logger LOG = Logger.getLogger(BufferedItemWriter.class);
    private ObjectPipe pipe = new ObjectPipe(PIPE_LENGTH);
    private ItemWriter iw;
    private int batchCounter = 0;
    private List batch = new ArrayList();
    private ObjectStoreException problem = null;
    private boolean finished = false;
    private static final int BATCH_SIZE = 1000;
    private static final int PIPE_LENGTH = 5;
    
    /**
     * Constructs the ItemWriter with another ItemWriter.
     *
     * @param iw the ItemWriter in which to store the Items
     */
    public BufferedItemWriter(ItemWriter iw) {
        this.iw = iw;
        PipeReader pipeReader = new PipeReader();
        Thread pipeReaderThread = new Thread(pipeReader, "BufferedItemWriter.PipeReader thread");
        pipeReaderThread.start();
    }

    /**
     * @see ItemWriter#store
     */
    public void store(Item item) throws ObjectStoreException {
        batch.add(item);
        batchCounter++;
        if (batchCounter >= BATCH_SIZE) {
            pipe.put(batch);
            batch = new ArrayList();
            batchCounter = 0;
            checkException();
        }
    }

    /**
     * @see ItemWriter#storeAll
     */
    public void storeAll(Collection items) throws ObjectStoreException {
        if (batchCounter > 0) {
            pipe.put(batch);
            batch = new ArrayList();
            batchCounter = 0;
        }
        pipe.put(batch);
        checkException();
    }

    
    /**
     * @see ItemWriter#close
     */
    public void close() throws ObjectStoreException {
        if (batchCounter > 0) {
            pipe.put(batch);
            batchCounter = 0;
        }
        pipe.finish();
        synchronized (this) {
            while (!finished) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
        checkException();
    }

    private synchronized void checkException() throws ObjectStoreException {
        if (problem != null) {
            throw problem;
        }
    }

    private class PipeReader implements Runnable
    {
        public PipeReader() {
        }
        
        public void run() {
            while (pipe.hasNext()) {
                Object nextInPipe = pipe.next();
                if (problem == null) {
                    try {
                        Collection col = (Collection) nextInPipe;
                        iw.storeAll(col);
                    } catch (ObjectStoreException e) {
                        synchronized (BufferedItemWriter.this) {
                            problem = e;
                        }
                    } catch (Exception e) {
                        synchronized (BufferedItemWriter.this) {
                            problem = new ObjectStoreException(e);
                        }
                    }
                }
            }
            try {
                iw.close();
            } catch (ObjectStoreException e) {
                synchronized (BufferedItemWriter.this) {
                    if (problem == null) {
                        problem = e;
                    }
                }
            }
            synchronized (BufferedItemWriter.this) {
                finished = true;
                BufferedItemWriter.this.notifyAll();
            }
        }
    }
}
