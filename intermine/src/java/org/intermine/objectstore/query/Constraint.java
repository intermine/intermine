package org.flymine.objectstore.query;

/**
 * Abstract reperesentation of a query constraint.
 *
 * @author Richard Smith
 * @author Mark Woddbridge
 */

public interface Constraint 
{
    /**
     * Set whether constraint is negated.  Negated reverses the logic of the constraint
     * i.e equals be becomes not equals.
     *
     * @param negated true if constraint logic to be reversed
     */
    public void setNegated(boolean negated);
    
    /**
     * Test if constraint logic has been reversed
     *
     * @return true if constraint is negated
     */
    public boolean isNegated();
    
}
