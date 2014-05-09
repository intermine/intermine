package org.intermine.webservice.server.core;

public interface Predicate<T> {

    public boolean test(T subject);
}
