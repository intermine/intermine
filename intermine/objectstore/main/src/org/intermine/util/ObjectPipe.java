package org.intermine.util;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * This is a rather simple implementation of a queue designed for passing objects from one
 * Thread to another to form a pipeline. The implementation allows a certain number of objects
 * to be buffered between the two Threads. The writing thread will wait when the buffer is full,
 * and the reading thread will wait while the buffer is empty. DO NOT allow the threads to wait
 * for each other in any way apart from through this object, otherwise deadlocks will occur.
 *
 * @author Matthew Wakeling
 */
public class ObjectPipe implements Iterator
{
    private LinkedList list = new LinkedList();
    private boolean finished = false;
    private int maxBuffer = 1024;
    private int putters = 0;

    /**
     * Construct an ObjectPipe with a default maximum buffer size.
     */
    public ObjectPipe() {
    }

    /**
     * Construct an ObjectPipe with a certain maximum buffer size.
     *
     * @param maxBuffer the maximum buffer size, in objects
     */
    public ObjectPipe(int maxBuffer) {
        if (maxBuffer < 1) {
            throw new IllegalArgumentException("Illegal value for maxBuffer: " + maxBuffer);
        }
        this.maxBuffer = maxBuffer;
    }

    /**
     * Adds an Object onto the end of the queue.
     *
     * @param o an Object
     */
    public synchronized void put(Object o) {
        putters++;
        if (finished) {
            throw new IllegalArgumentException("Can't put onto a finished ObjectPipe");
        }
        while (list.size() >= maxBuffer) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        list.add(o);
        notifyAll();
        putters--;
    }

    /**
     * Adds a whole Collection of Objects onto the end of the queue, in the order that the
     * Collection's Iterator presents them.
     * Note that this method will add all of the objects if at the beginning the buffer is
     * not full, even if it over-fills the buffer. This is not a problem, as no data is
     * lost, and all the objects were taking up memory anyway. This also helps with performance,
     * because it means that the threads will be switched between with larger granularity.
     *
     * @param col a Collection
     */
    public synchronized void putAll(Collection col) {
        putters++;
        if (finished) {
            throw new IllegalArgumentException("Can't putAll onto a finished ObjectPipe");
        }
        while (list.size() >= maxBuffer) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        list.addAll(col);
        notifyAll();
        putters--;
    }

    /**
     * Marks the ObjectPipe as finished - that is, no further Objects will be put into the
     * ObjectPipe. The hasNext() method will return false and the next() method will throw a
     * NoSuchElementException once the buffer is emptied, instead of blocking for input.
     * This method will wait until no other Threads are executing in put() or putAll().
     */
    public synchronized void finish() {
        if (finished) {
            throw new IllegalArgumentException("Can't finish a finished ObjectPipe");
        }
        while (putters > 0) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        finished = true;
        notifyAll();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean hasNext() {
        while ((!finished) && list.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        return !list.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Object next() throws NoSuchElementException {
        while ((!finished) && list.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        notifyAll();
        return list.removeFirst();
    }

    /**
     * {@inheritDoc}
     */
    public void remove() {
        throw new UnsupportedOperationException("Remove is not supported on an ObjectPipe");
    }
}
