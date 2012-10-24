package org.intermine.webservice.server.core;

import java.util.LinkedList;
import java.util.List;

/**
 * Similar to Apache CollectionUtils - but with Generics.
 * @author Alex Kalderimis
 *
 */
public class ListFunctions {

    public static <T, R> List<R> map(final Iterable<T> things, final F<T, R> f) {
        List<R> returners = new LinkedList<R>();
        for (T thing: things) {
            returners.add(f.call(thing));
        }
        return returners;
    }
}
