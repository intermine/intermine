package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.fail;

public class ObjectStoreWriterTests
{
    private boolean finished = false;
    private Throwable failureException = null;

    @Test
    public void testRapidShutdown() throws Exception {
        Thread t = new Thread(new ShutdownThread());
        t.start();
        synchronized (this) {
            try {
                wait(5000);
            } catch (InterruptedException e) {
            }
            Assert.assertTrue(finished);
            if (failureException != null) {
                throw new Exception(failureException);
            }
        }
    }

    public synchronized void signalFinished(Throwable e) {
        finished = true;
        failureException = e;
        notifyAll();
    }

    private class ShutdownThread implements Runnable {
        public void run() {
            try {
                ObjectStoreWriterInterMineImpl w = (ObjectStoreWriterInterMineImpl)ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
                Connection c = w.getConnection();
                try {
                    w.close();
                    fail("Expected an ObjectStoreException");
                } catch (ObjectStoreException e) {
                    Assert.assertEquals("Closed ObjectStoreWriter while it is being used. Note this writer will be automatically closed when the current operation finishes", e.getMessage());
                }
                w.releaseConnection(c);
                signalFinished(null);
            } catch (Throwable e) {
                System.out.println("Error in ShutdownThread: " + e);
                signalFinished(e);
            }
        }
    }
}
