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

/**
 * An interface for any object that can be shut down by the ShutdownHook.
 *
 * @author Matthew Wakeling
 */
public interface Shutdownable
{
    /**
     * Shuts down an instance. It is expected that this method will be called on JVM exit. To make
     * this happen, register the instance with ShutdownHook.
     */
    public void shutdown();
}
