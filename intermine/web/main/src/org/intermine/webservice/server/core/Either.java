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
 * <h2> Disjoint type union </h2>
 * <p>
 * Disjoint type union to handle certain features of the way we present results
 * in outer-joined tables, but abstracted to be made more generally useful, and incorportating
 * the visitor pattern for value access.
 * </p>
 * <p>
 * Obviously the terminology is taken from the worlds of functional programming (Haskell, Scala,
 * <i>et al.</i>).
 * </p>
 *
 * @author Alex Kalderimis.
 *
 * @param <A> The type of Lefts.
 * @param <B> The type of Rights.
 */
public abstract class Either<A, B>
{
    private Either() {
        // Hidden
    }

    @Override
    public abstract int hashCode();

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof Either)) {
            return false;
        }
        @SuppressWarnings("unchecked") // Checked above in fact.
        final Either<A, B> rhs = (Either<A, B>) obj;
        return accept(new EqualityVisitor(rhs));
    }

    /**
     * How you go about accessing the values.
     * @param visitor The mapping function.
     * @param <T> The return type.
     * @return Whatever the mapping function returns.
     */
    public abstract <T> T accept(EitherVisitor<A, B, T> visitor);

    private final class EqualityVisitor extends EitherVisitor<A, B, Boolean>
    {
        private final class BEqualsB extends EitherVisitor<A, B, Boolean>
        {
            private final B b1;

            private BEqualsB(B b1) {
                this.b1 = b1;
            }

            @Override
            public Boolean visitLeft(A a) {
                return false;
            }

            @Override
            public Boolean visitRight(B b2) {
                return (b1 == null && b2 == null) || b1.equals(b2);
            }
        }

        private final class AEqualsA extends EitherVisitor<A, B, Boolean>
        {
            private final A a1;

            private AEqualsA(A a1) {
                this.a1 = a1;
            }

            @Override
            public Boolean visitLeft(A a2) {
                return (a1 == null && a2 == null) || a1.equals(a2);
            }

            @Override
            public Boolean visitRight(B b) {
                return false;
            }
        }

        private final Either<A, B> rhs;

        private EqualityVisitor(Either<A, B> rhs) {
            this.rhs = rhs;
        }

        @Override
        public Boolean visitLeft(final A a1) {
            return rhs.accept(new AEqualsA(a1));
        }

        @Override
        public Boolean visitRight(final B b1) {
            return rhs.accept(new BEqualsB(b1));
        }
    }

    /** @author Alex Kalderimis **/
    public static final class Left<A, B> extends Either<A, B>
    {

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((a == null) ? 0 : a.hashCode());
            return result;
        }

        private final A a;

        /**
         * Construct a left.
         * @param a The content of this Either.
         */
        public Left(A a) {
            this.a = a;
        }

        @Override
        public <T> T accept(EitherVisitor<A, B, T> visitor) {
            return visitor.visitLeft(a);
        }

        @Override
        public String toString() {
            return String.format("Left(%s)", a);
        }
    }

    /** @author Alex Kalderimis **/
    public static final class Right<A, B> extends Either<A, B>
    {
        private final B b;

        /**
         * Construct a right.
         * @param b the content of this Either.
         */
        public Right(B b) {
            this.b = b;
        }

        @Override
        public <T> T accept(EitherVisitor<A, B, T> visitor) {
            return visitor.visitRight(b);
        }

        @Override
        public String toString() {
            return String.format("Right(%s)", b);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((b == null) ? 0 : b.hashCode());
            return result;
        }
    }
}
