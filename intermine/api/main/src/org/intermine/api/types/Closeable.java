package org.intermine.api.types;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Things that have a close method.
 * @author Alex Kalderimis
 *
 */
public interface Closeable
{
    /**
     * Call this to dispose of this object.
     */
    void close();
}
