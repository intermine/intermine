package org.flymine.sql.precompute;

/**
 * This object is an abstract superclass for a Best Query tracker. Queries can be added to these
 * objects, and they will keep track of them.
 *
 * @author Matthew Wakeling
 */
public abstract class BestQuery
{
    /**
     * Allows a Query to be added to this tracker.
     *
     * @param obj a Query to be added to the tracker
     */
    public abstract void add(Query obj);
}
