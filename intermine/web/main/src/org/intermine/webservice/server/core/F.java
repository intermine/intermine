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

public abstract class F<A, B>
{
    public abstract B call(A a);

    public static <B> F constant(B val) {
        return new Constant(val);
    }

    public <C> F<A, C> compose(final F<B, C> outer) {
        final F<A, B> inner = this;
        return new F<A, C>() {
            public C call(A a) {
                return outer.call(inner.call(a));
            }
        };
    }

    public static class Constant<X, R> extends F<X, R>
    {
        private final R value;
        
        protected Constant(R val) {
            this.value = val;
        }

        public R call(X x) {
            return value;
        }
    }
}
