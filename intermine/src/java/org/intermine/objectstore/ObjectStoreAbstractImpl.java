package org.flymine.objectstore;

/**
 * Abstract implementation of the ObjectStore interface. Used to provide uniformity
 * between different ObjectStore implementations.
 *
 * @author Andrew Varley
 */
public abstract class ObjectStoreAbstractImpl implements ObjectStore
{
    protected int maxOffset = Integer.MAX_VALUE;
    protected int maxLimit = Integer.MAX_VALUE;
    protected long maxTime = Long.MAX_VALUE;

    /**
     * No argument constructor
     */
    protected ObjectStoreAbstractImpl() {
    }

    /**
     * Checks the start and limit to see whether they are inside the
     * hard limits for this ObjectStore
     *
     * @param start the start row
     * @param limit the number of rows
     * @throws ObjectStoreLimitReachedException if the start is greater than the
     * maximum start allowed or the limit greater than the maximum
     * limit allowed
     */
    protected void checkStartLimit(int start, int limit) throws ObjectStoreLimitReachedException {
        if (start > maxOffset) {
            throw (new ObjectStoreLimitReachedException("offset parameter (" + start
                                            + ") is greater than permitted maximum ("
                                            + maxOffset + ")"));
        }
        if (limit > maxLimit) {
            throw (new ObjectStoreLimitReachedException("number of rows required (" + limit
                                            + ") is greater than permitted maximum ("
                                            + maxLimit + ")"));
        }
    }
}
