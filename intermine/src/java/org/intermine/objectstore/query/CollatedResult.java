package org.flymine.objectstore.query;

import org.flymine.objectstore.*;

/**
 * A CollatedResult wraps the result of a Query along with another
 * Query that can be used by the ObjectStore to retrieve all the
 * objects that this result refers to.
 *
 * In the case of an aggregate this would be (e.g.) the count, along
 * with a query to retrieve the objects counted.
 *
 * CollatedResult is a Results object, which itself is a List, so we have all the
 * usual methods of interacting with the Results.
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Andrew Varley
 */
public class CollatedResult extends Results
{
    private Object result;

    /**
     * Construct a collated result
     *
     * @param result the result
     * @param q the Query to use to retrieve matches
     * @param os the ObjectStore to use to run the Query
     */
    public CollatedResult(Object result, Query q, ObjectStore os) {
        super(q, os);
        if (result == null) {
            throw new NullPointerException("result must not be null");
        }
        this.result = result;
    }

    /**
     * Get the result
     *
     * @return the result
     */
    public Object getResult() {
        return result;
    }

}
