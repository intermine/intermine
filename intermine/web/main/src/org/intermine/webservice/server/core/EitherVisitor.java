package org.intermine.webservice.server.core;

public abstract class EitherVisitor<A, B, T>
{
    public abstract T visitLeft(A a);
    
    public abstract T visitRight(B b);

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

    public <R> EitherVisitor<A, B, R> then(final F<T, R> fn) {
        final EitherVisitor<A, B, T> outer = this;
        return new EitherVisitor<A, B, R>() {
            public R visitLeft(A a) {
                return fn.f(outer.visitLeft(a));
            }
            public R visitRight(B b) {
                return fn.f(outer.visitRight(b));
            }
        };
    }
}
