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

import org.intermine.webservice.server.core.Either.Left;
import org.intermine.webservice.server.core.Either.Right;

/**
 * @param <L> The type of lefts.
 * @param <R> The type of rights.
 * @author Alex Kalderimis
 **/
class DisjointList<L, R> extends ArrayList<Either<L, R>>
{

    /**
     * UID for Serializable
     */
    private static final long serialVersionUID = 2066865354564286318L;

    /**
     * Add a left to this list.
     * @param lefty The left thing.
     * @return whether a thing was added.
     */
    public boolean addLeft(L lefty) {
        super.add(new Left<L, R>(lefty));
        return true;
    }

    /**
     * Add a left to this list.
     * @param righty The right thing.
     * @return whether a thing was added.
     */
    public boolean addRight(R righty) {
        super.add(new Right<L, R>(righty));
        return true;
    }
}
