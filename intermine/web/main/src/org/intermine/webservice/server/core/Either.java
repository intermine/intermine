package org.intermine.webservice.server.core;

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
    private Either() {}

    public abstract <T> T accept(EitherVisitor<A, B, T> visitor);

    public static final class Left<A, B> extends Either<A, B> {

        private final A a;

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
    
    public static final class Right<A, B> extends Either<A, B> {
        private final B b;
        
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
    }
}
