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

import java.util.Map.Entry;

/**
 * A tuple of order two.
 * @author Alex Kalderimis
 *
 * This is also an implementation of Map.Entry, with the exception that setValue
 * throws an error.
 *
 * @param <A> The first element in the pair is an A.
 * @param <B> The second element in the pair is a B.
 */
public final class Pair<A, B> implements Entry<A, B>
{
    final A a;
    final B b;

    /**
     * Construct a pair of two things
     * @param a Thing the first
     * @param b Thing the second
     */
    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public A getKey() {
        return a;
    }

    @Override
    public B getValue() {
        return b;
    }

    @Override
    public B setValue(B value) {
        throw new UnsupportedOperationException("Pairs are final");
    }

    @Override
    public String toString() {
        return String.format("org.intermine.webservice.core.Pair(%s => %s)", a, b);
    }
}
