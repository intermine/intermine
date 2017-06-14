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
 * The type of functions that test things.
 * @author Alex Kalderimis
 *
 * @param <T> The type of the thing to test.
 **/
public interface Predicate<T> extends Function<T, Boolean>
{

    /**
     * Apply some test to some kind of thing.
     * @param subject The thing to test
     * @return true or false
     **/
    Boolean call(T subject);
}
