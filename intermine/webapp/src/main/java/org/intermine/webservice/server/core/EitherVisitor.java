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
 * A mapping function capable of accessing a disjoint sum-type (an Either A B).
 * @param <T> The return type.
 * @param <A> The left type.
 * @param <B> The right type.
 * @author Alex Kalderimis
 **/
public abstract class EitherVisitor<A, B, T> implements Function<Either<A, B>, T>
{
    /**
     * Override this to access the left hand side.
     * @param a The content of the left.
     * @return A T of some kind
     */
    public abstract T visitLeft(A a);

    /**
     * Override this to access the right hand side.
     * @param b The content of the right
     * @return A T of some kind
     */
    public abstract T visitRight(B b);

    @Override
    public T call(Either<A, B> either) {
        return either.accept(this);
    }

    /**
     * Compose this visitor with another visitor which handles the either
     * after this one has had a go at it, presumably for side-effects like
     * logging and what-not.
     * @param next The visitor to handle the either after this one.
     * @param <R> The return type.
     * @return An R of some kind.
     */
    public <R> EitherVisitor<A, B, R> and(final EitherVisitor<A, B, R> next) {
        final EitherVisitor<A, B, T> outer = this;
        return new EitherVisitor<A, B, R>() {
            public R visitLeft(A a) {
                outer.visitLeft(a);
                return next.visitLeft(a);
            }
            public R visitRight(B b) {
                outer.visitRight(b);
                return next.visitRight(b);
            }
        };
    }

    /**
     * Compose this visitor with a function which consumes the output of this
     * visitor.
     * @param fn The function to pass the result to.
     * @param <R> The return type.
     * @return An R of some kind.
     */
    public <R> EitherVisitor<A, B, R> then(final Function<T, R> fn) {
        final EitherVisitor<A, B, T> outer = this;
        return new EitherVisitor<A, B, R>() {
            public R visitLeft(A a) {
                return fn.call(outer.visitLeft(a));
            }
            public R visitRight(B b) {
                return fn.call(outer.visitRight(b));
            }
        };
    }
}
