package org.intermine.webservice.server.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

class DisjointRecursiveList<T> {
    List<Either<T, DisjointRecursiveList<T>>> items;

    DisjointRecursiveList() {
        items = new ArrayList<Either<T, DisjointRecursiveList<T>>>();
    }

    void addList(DisjointRecursiveList<T> subs) {
        items.add(new Either.Right<T, DisjointRecursiveList<T>>(subs));
    }

    void addNode(T node) {
        items.add(new Either.Left<T, DisjointRecursiveList<T>>(node));
    }

    @Override
    public String toString() {
        return String.valueOf(items);
    }
    
    public List<T> flatten() {
        final List<T> flattened = new LinkedList<T>();
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
    
    void forEach(EitherVisitor<T, DisjointRecursiveList<T>, Void> visitor) {
        for (Either<T, DisjointRecursiveList<T>> item: items) {
            item.accept(visitor);
        }
    }
    
    public abstract static class Eacher<T> extends EitherVisitor<T, DisjointRecursiveList<T>, Void> {
        
    }
}