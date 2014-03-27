package org.intermine.webservice.server.core;

/*
 * Copyright (C) 2002-2014 FlyMine
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

class DisjointList<L, R> extends ArrayList<Either<L, R>> {
    public boolean addLeft(L lefty) {
        super.add(new Left<L, R>(lefty));
        return true;
    }
    public boolean addRight(R righty) {
        super.add(new Right<L, R>(righty));
        return true;
    }
}