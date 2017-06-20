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

import java.util.ArrayList;
import java.util.List;

/**
 * A list that can have either a simple element of type <code>T</code> in it,
 * or lists of elements of that type.
 *
 * @author Alex Kalderimis
 *
 * @param <T> The type of elements in this list.
 */
class DisjointRecursiveList<T>
{

    DisjointList<T, DisjointRecursiveList<T>> items;

    /** Construct a new disjoint list **/
    DisjointRecursiveList() {
        items = new DisjointList<T, DisjointRecursiveList<T>>();
    }

    /**
     * Add a list
     * @param subs The list to add.
     */
    void addList(DisjointRecursiveList<T> subs) {
        items.addRight(subs);
    }

    /** Add a node
     * @param node the node to add.
     */
    void addNode(T node) {
        items.addLeft(node);
    }

    @Override
    public String toString() {
        return String.valueOf(items);
    }

    /**
     * Flatten this list into a list of elements.
     * @return The flattened list.
     */
    public List<T> flatten() {
        final List<T> flattened = new ArrayList<T>();
        forEach(new Eacher<T>() {
            @Override
            public Void visitLeft(T a) {
                flattened.add(a);
                return null;
            }
            @Override
            public Void visitRight(DisjointRecursiveList<T> b) {
                flattened.addAll(b.flatten());
                return null;
            }

        });
        return flattened;
    }

    /**
     * Map this list from a recursive list with terminal nodes of type T to a list
     * with terminal nodes of type X. The resulting list will have the same shape as this
     * list, but each node will be transformed.
     * @param mapFn A function that maps from <code>T -> X</code>
     * @param <X> The type of the elements in the resulting list.
     * @return A disjoint recursive list where terminal nodes are of type X.
     */
    <X> DisjointRecursiveList<X> fmap(final F<T, X> mapFn) {
        final DisjointRecursiveList<X> retVal = new DisjointRecursiveList<X>();

        forEach(new EitherVisitor<T, DisjointRecursiveList<T>, Void>() {
            @Override
            public Void visitLeft(T a) {
                retVal.addNode(mapFn.call(a));
                return null;
            }
            @Override
            public Void visitRight(DisjointRecursiveList<T> b) {
                retVal.addList(b.fmap(mapFn));
                return null;
            }
        });

        return retVal;
    }

    /**
     * Iterate over each element in this list.
     * @param visitor The iteration function.
     */
    void forEach(EitherVisitor<T, DisjointRecursiveList<T>, Void> visitor) {
        for (Either<T, DisjointRecursiveList<T>> item: items) {
            item.accept(visitor);
        }
    }

    /**
     * Type alias to prevent stuttering.
     * @author Alex Kalderimis
     *
     * @param <T> The type of elements in the list.
     */
    public abstract static class Eacher<T>
        extends EitherVisitor<T, DisjointRecursiveList<T>, Void>
    {

    }
}
