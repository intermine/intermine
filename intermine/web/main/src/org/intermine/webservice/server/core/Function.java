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
 * An interface for defining a thing that can take something and return
 * something else.
 *
 * @author Alex Kalderimis
 *
 * @param <A> The input type.
 * @param <B> The output type.
 */
public interface Function<A, B>
{

    /**
     * Take an A and return a B.
     * @param a The input.
     * @return a B of some kind.
     */
    B call(A a);
}
