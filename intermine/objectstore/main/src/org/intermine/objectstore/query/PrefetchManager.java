package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.intermine.objectstore.ObjectStoreException;

/**
 * A manager for the prefetch mechanism for the Results object.
 *
 * @author Matthew Wakeling
 */
public class PrefetchManager
{
    private static final Logger LOG = Logger.getLogger(PrefetchManager.class);
    /** Pending set of requests - always accessed inside a synchronise on sync. */
    protected static Set pending = new HashSet();
    /** Set of requests currently being serviced. This Set is not accessed inside a block
     * synchronised on any global object, so it must be able to handle concurrent access. */
    protected static Set serviced = Collections.synchronizedSet(new HashSet());
    protected static int serviceThreads = 0;
    private static Object sync = new Object();

    protected static final int LOADING = 3;

    /*
     * This class provides methods for cancelling requests, so here is an explanation of how this
     * magic works.
     *
     * The ObjectStore provides a query cancellation system, where you register your thread as
     * having a request ID, which is any object you choose, and then you run your query as normal.
     * Another thread can come along and pass that request ID to the objectstore, telling it to
     * cancel the associated query. The Thread making the request may throw an exception, and
     * all subsequent actions will throw exceptions until the request ID is deregistered from the
     * ObjectStore.
     *
     * This PrefetchManager messes all of this up, because it uses extra threads to perform
     * queries, so a thread can be waiting in the PrefetchManager for another thread to finish
     * a query. The PrefetchManager maps user threads onto action threads - sometimes they will
     * be the same, but other times they will not.
     *
     * The PrefetchManager works with PrefetchManager.Request objects, which represent a unit of
     * work to perform. These can be used as request ID objects to hand to the ObjectStore. So,
     * Threads should register request ID objects with this PrefetchManager, in the same way as
     * with the ObjectStore. When a cancel request comes in, the PrefetchManager will then be able
     * to match that against a Request object. A request ID matches a PrefetchManager.Request
     * object if a Thread with that request ID is currently inside the PrefetchManager.doRequest()
     * method. There are several scenarios:
     * 1. The thread is doing the work itself, and it is the only one that needs that data.
     *       -> dead simple, just cancel the request.
     * 2. The thread is doing the work itself, but there are other threads waiting for the same
     *    data.
     *       -> Policy decision to be made. Probably cancel the request, and let the other threads
     *          start again.
     * 3. The thread is waiting for another thread to finish the work, and there are no other
     *    threads waiting for the data.
     *       -> In this case, the thread that is doing the work is guaranteed to be a prefetch
     *          thread, so it can just be cancelled.
     * 4. The thread is waiting for another thread to finish the work, but there are other threads
     *    waiting for the same data. Another thread waiting for the data will be any thread that
     *    is inside the doRequest() method with that PrefetchManager.Request object, regardless of
     *    whether that thread has registered a request ID or not.
     *      -> The thread that is doing the work may or may not be a prefetch thread. In either
     *         case, the waiting thread should be kicked out of the doRequest() method without
     *         jeopardising the thread that is performing the work.
     */

    /**
     * Adds a request to the Set of pending requests, and wakes up a Thread to handle it.
     *
     * @param result a Results object that is making the request
     * @param batchNo the batch number to be fetched
     */
    public static void addRequest(Results result, int batchNo) {
        Request request = new Request(result, batchNo);
        synchronized (sync) {
            synchronized (result) {
                // Synchronise on BOTH locks, so we can muck about with anything.
                if (!result.batches.containsKey(new Integer(batchNo))) {
                    // The request has not been done.
                    if (!serviced.contains(request)) {
                        // And it isn't currently being serviced.
                        //if (!pending.contains(request)) {
                        //    LOG.debug("addRequest - adding request:                          "
                        //            + request);
                        //}
                        pending.add(request);
                        if ((pending.size() + serviced.size()) > (serviceThreads * LOADING)) {
                            // There are too many requests for the servicing threads.
                            Thread newThread = new ServiceThread();
                            newThread.setDaemon(true);
                            newThread.setName("PrefetchManager ServiceThread");
                            newThread.start();
                            serviceThreads++;
                            LOG.info("addRequest - creating new ServiceThread. We now have "
                                    + serviceThreads);
                        } else {
                            // There may or may not be a service thread waiting. If not, a service
                            // thread will soon finish a request.
                            sync.notify();
                        }
                    //} else {
                        //LOG.debug("addRequest - the request is currently being serviced: "
                        //        + request);
                    }
                //} else {
                    //LOG.debug("addRequest - the request has already been done:       " + request);
                }
            }
        }
    }

    /**
     * Returns when the given request is completed. If the given request is not already being
     * serviced, then this method will start servicing the request in the current thread.
     *
     * @param result a Results object that is making the request
     * @param batchNo the batch number to be fetched
     * @return a List containing the contents of the batch
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     * @throws IndexOutOfBoundsException if the batch is off the end of the results
     */
    public static List doRequest(Results result, int batchNo) throws ObjectStoreException {
        return doRequest(new Request(result, batchNo));
    }

    /**
     * Returns the batch described by the request. If the batch is not already available, then the
     * current thread fetches it.
     *
     * @param request a Request object
     * @return a List containing the contents of the batch
     * @throws ObjectStoreException if an error occurs in the underlying ObjectStore
     * @throws IndexOutOfBoundsException if the batch is off the end of the results
     */
    protected static List doRequest(Request request) throws ObjectStoreException {
        boolean needToWait = false;
        List retval = null;

        synchronized (sync) {
            synchronized (request.result) {
                // Synchronise on BOTH locks, so we can muck about with anything.
                // Note, that to avoid deadlocks, we should never lock these two in the opposite
                // order, or call wait on either of these locks where a notify needs to obtain
                // the other one. We shouldn't call wait in here anyway really.
                // We need both locks, because we need to exclude the possibility that someone
                // finishes a request between us checking to see if it is already fetched, and
                // checking if we need to wait for someone to finish fetching it.
                retval = (List) request.result.batches.get(new Integer(request.batchNo));
                if (retval != null) {
                    // The batch has already been fetched.
                    //LOG.debug("doRequest - the request has already been done:        " + request);
                    return retval;
                }
                if (pending.contains(request)) {
                    // The request is pending.
                    // If it is pending, then we wish it to be serviced by US. Therefore, we
                    // need to move it to serviced state and do it.
                    // The request is not being serviced. We take over.
                    serviced.add(request);
                    pending.remove(request);
                    //LOG.debug("doRequest - the request was pending:                  " + request);
                } else if (serviced.contains(request)) {
                    // The request is being serviced. We just need to wait.
                    needToWait = true;
                    //LOG.debug("doRequest - the request is currently being serviced:  " + request);
                } else {
                    // The request has never been seen before. Therefore, we should add it to the
                    // serviced set, and handle it.
                    serviced.add(request);
                    //LOG.debug("doRequest - new request:                              " + request);
                }
            }
        }

        // At this stage, there is a limited set of states that we can be in. Either
        // 1. We need to wait for another thread to finish servicing the request. In this case, we
        //     know for sure that our request was in serviced when we left the synchronised block.
        //     However, since this is a GAP between synchronising, another thread could finish the
        //     request before we synchronise again. Therefore, we should check after we have
        //     synchronised again.
        // 2. We need to do the request ourselves. In this case, we set the PrefetchManager state
        //     so that anything else to do with this request waits for us rather than changing the
        //     state while we were in the synchronised block.

        if (needToWait) {
            synchronized (request.result) {
                // We are synchronised. Now, no thread can report that the request is finished until
                // we release the lock.
                // First, check that someone didn't finish during the GAP.
                if (serviced.contains(request)) {
                    // Noone finished, so we can wait for that to happen.
                    //LOG.debug("doRequest - waiting for another thread to finish:     " + request);
                    try {
                        request.result.wait();
                    } catch (InterruptedException e) {
                        // Ignore interruption.
                    }
                }
                // At this point, either our wait was interrupted by notifyAll(), or someone has
                // removed the request from serviced. Either way, we can't be absolutely sure that
                // result.batches contains the batch we want, for several reasons. So we recurse.
                // Reasons:
                // 1. request.result.batches may be a WeakHashMap, in which case the batch may have
                //     been removed from it by the garbage collector since it was added.
                // 2. The object we are waiting for is a result. A notify that we might have
                //     received could have been for a different request.
                // 3. The request is a dud, so the thread that was servicing the request received
                //     an exception. Under this circumstance, that thread should still remove the
                //     request from the serviced set, and notify other threads waiting for the
                //     request. This allows threads waiting to do the operation themselves, and
                //     therefore return back the correct exception.
            }
            retval = doRequest(request);
        } else {
            try {
                //LOG.debug("doRequest - servicing request:                        " + request);
                // Now, we can service this request in a normal manner, outside all locks.
                retval = request.result.fetchBatchFromObjectStore(request.batchNo);
            } finally {
                // And then report that it is finished, inside a lock, even if we did get an
                // exception.
                reportDone(request);
            }
        }

        return retval;
    }

    /**
     * Allows a system to report that it has finished servicing a particular request.
     *
     * @param request the request that has been done
     */
    protected static void reportDone(Request request) {
        synchronized (request.result) {
            //LOG.debug("reportDone - finished request:                        " + request);
            serviced.remove(request);
            request.result.notifyAll();
        }
    }

    /**
     * Returns a request for a thread to service.
     *
     * @return a request to service
     */
    protected static Request getRequest() {
        Request retval;
        synchronized (sync) {
            while (pending.isEmpty()) {
                // There are no requests to get - wait for a notify.
                //LOG.debug("getRequest - waiting for a request");
                try {
                    sync.wait();
                } catch (InterruptedException e) {
                    // Ignore interruption.
                }
            }
            // Get a request from the pending set. We know there is something in there, because we
            // just got false from pending.isEmpty, and we have the lock on sync, and nothing
            // touches pending unless they have that lock.
            retval = (Request) pending.iterator().next();
            //LOG.debug("getRequest - got request:                             " + retval);
            // Now we have a request, we could synchronise by its result - other things look
            // for the request in serviced - namely:
            // 1. addRequest doesn't add a request to pending if it already in serviced. However,
            //     that doesn't matter, because it is already synchronised on sync. Otherwise, you
            //     might have had the situation where a request appears in both pending and serviced
            //     sets.
            // 2. doRequest also moves items from pending to serviced, but that doesn't matter
            //     because it is also synchronised on sync. Otherwise you could have two threads
            //     servicing the same request, which could possibly leave other requests not being
            //     serviced while a service thread sleeps.
            // 3. doRequest decides whether to wait on whether there is a request in serviced, but
            //     this also does not matter, as it can only be broken by a thread removing a
            //     request from serviced outside a synchronised block.
            // 4. reportDone removes an entry from serviced, which does not matter, because it will
            //     only break something that is broken by things being removed from serviced outside
            //     a sync lock.
            // Basically, we never check to see if the batch has already been fetched before
            // checking to see if anyone is fetching it - we only put the request in serviced.
            // Therefore, we do not need to synchronise by request.result here.

            serviced.add(retval);
            pending.remove(retval);
        }
        return retval;
    }

    private static class Request
    {
        private Results result;
        private int batchNo;

        public Request(Results result, int batchNo) {
            this.result = result;
            this.batchNo = batchNo;
        }

        public int hashCode() {
            return 2 * result.query.hashCode() + 3 * batchNo;
        }

        public boolean equals(Object obj) {
            return (result == ((Request) obj).result) && (((Request) obj).batchNo == batchNo);
        }

        public String toString() {
            return "Result " + result.query.hashCode() + ", batch " + batchNo;
        }
    }

    private static class ServiceThread extends Thread
    {
        public void run() {
            try {
                while (true) {
                    Request request = PrefetchManager.getRequest();
                    //LOG.debug("ServiceThread.run - servicing request                 " + request);
                    try {
                        // Now, we can service this request in a normal manner, outside all locks.
                        List batch = request.result.fetchBatchFromObjectStore(request.batchNo);
                    } catch (Exception e) {
                        LOG.warn("ServiceThread.run - Received exception                " + request
                                + " " + e);
                        // We don't care about any exception - we NEED this thread to keep running.
                        // Otherwise, things go pear-shaped.
                    } finally {
                        // And then report that it is finished, inside a lock, even if we did get an
                        // exception.
                        reportDone(request);
                    }
                }
            } finally {
                // Like I said, we REALLY NEED to know if something has gone wrong here, as it is a
                // BIG BAD BUG.
                LOG.error("ServiceThread died unexpectedly. PrefetchManager may stop working");
                // And decrement the count of available ServiceThreads.
                PrefetchManager.serviceThreads--;
            }
        }
    }
}

