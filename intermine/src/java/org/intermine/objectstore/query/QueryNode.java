package org.flymine.objectstore.query;

/**
 * An element that can appear in the SELECT, ORDER BY or GROUP BY clause of a query
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public interface QueryNode
{
    /**
     * Get Java type represented by this evaluable item
     *
     * @return class describing the type
     */
    public Class getType();
}
