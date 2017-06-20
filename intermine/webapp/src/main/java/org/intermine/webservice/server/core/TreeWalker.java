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
 * A type alias to reduce type stuttering.
 * @author Alex Kalderimis
 *
 * @param <L> The type of an individual leaf.
 */
public abstract class TreeWalker<L>
    extends EitherVisitor<L, DisjointRecursiveList<L>, Void>
{

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
