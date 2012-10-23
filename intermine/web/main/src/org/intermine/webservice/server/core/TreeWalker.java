package org.intermine.webservice.server.core;

/**
 * A type alias to reduce type stuttering.
 * @author Alex Kalderimis
 *
 * @param <L> The type of an individual leaf.
 * @param <R> The return type.
 */
public abstract class TreeWalker<L> extends EitherVisitor<L, DisjointRecursiveList<L>, Void> {

    @Override
    public Void visitLeft(L a) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitRight(DisjointRecursiveList<L> b) {
        // TODO Auto-generated method stub
        return null;
    }


}
