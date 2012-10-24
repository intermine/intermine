package org.intermine.webservice.server.core;

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