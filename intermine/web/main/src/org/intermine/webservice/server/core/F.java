package org.intermine.webservice.server.core;

public abstract class F<A, B>
{
    public abstract B f(A a);
    
    
    public static <B> F constant(B val) {
        return new Constant(val);
    }
    public static class Constant<X, R> extends F<X, R>
    {
        private final R value;
        
        protected Constant(R val) {
            this.value = val;
        }

        public R f(X x) {
            return value;
        }
    }
}
