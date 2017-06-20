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

import java.util.LinkedList;
import java.util.List;

/**
 * Similar to Apache CollectionUtils - but with Generics.
 * @author Alex Kalderimis
 *
 */
public final class ListFunctions
{
    private ListFunctions() {
        // Hidden constructor.
    }

    /**
     * Transform a collection of things into a list of other things.
     * @param things The things we are transforming.
     * @param f The function that does the transformation.
     * @param <T> The type of the input things.
     * @param <R> The type of the output things.
     * @return A list of new things
     */
    public static <T, R> List<R> map(final Iterable<T> things, final Function<T, R> f) {
        List<R> returners = new LinkedList<R>();
        for (T thing: things) {
            returners.add(f.call(thing));
        }
        return returners;
    }
}
