package org.intermine.webservice.server.core;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * The type of functions that take no input and produce a value.
 * @author Alex Kalderimis
 *
 * @param <T> The type of thing this will produce.
 */
public interface Producer<T>
{

    /**
     * @return A T of some kind.
     */
    T produce();
}
