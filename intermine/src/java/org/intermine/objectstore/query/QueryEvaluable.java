package org.flymine.objectstore.query;

/**
 * An element that can be evaluated for comparison (one that represents an atomic type)
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public interface QueryEvaluable
{
    /**
     * Get Java type represented by this evaluable item
     *
     * @return class describing the type
     */
    public Class getType();
}
