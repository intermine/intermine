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
 * A type for a function from A -> B.
 * @author Alex Kalderimis
 *
 * @param <A> The input type.
 * @param <B> The output type.
 */
public abstract class F<A, B> implements Function<A, B>
{
    /**
     * The body of the function
     * @param a The parameter.
     * @return Something.
     **/
    public abstract B call(A a);

    /**
     * Construct a constant function that discards its input
     * and always returns the same value.
     * @param <B> The type of the thing.
     * @param val The thing to return.
     * @return That same thing.
     */
    public static <B> F<Object, B> constant(B val) {
        return new Constant<Object, B>(val);
    }

    /**
     * Compose two functions f and g producing a function h such that h(x) = f(g(x)).
     *
     * @param outer The outer function, which consumes the result of this function.
     * @param <C> The type of the final result.
     * @return The result of applying this function to the result of that function.
     */
    public <C> F<A, C> compose(final F<B, C> outer) {
        final F<A, B> inner = this;
        return new F<A, C>() {
            public C call(A a) {
                return outer.call(inner.call(a));
            }
        };
    }

    /**
     * An implementation of a constant function.
     * @author Alex Kalderimis
     *
     * @param <X> The input type - ignored.
     * @param <R> The output type.
     */
    public static class Constant<X, R> extends F<X, R>
    {
        private final R value;

        /**
         * Construct a constant function.
         * @param val The value we will return.
         */
        protected Constant(R val) {
            this.value = val;
        }

        /**
         * Call this function, returning the value supplied to the constructor.
         * @param x The input to this function - ignored.
         * @return The value supplied to the constructor.
         */
        public R call(X x) {
            return value;
        }
    }
}
