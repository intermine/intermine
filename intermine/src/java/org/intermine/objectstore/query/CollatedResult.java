package org.flymine.objectstore.query;

import java.util.List;
import java.util.ArrayList;

/**
 * A CollatedResult wraps the result of a Query with its associated objects.
 * In the case of an aggregate this would be (e.g.) the count, along with the objects counted.
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class CollatedResult
{
    private Object result;
    private List matches = new ArrayList(); // @element-type SingletonResult

    /**
     * @return the result
     */    
    public Object getResult() {
        return result;
    }
    
    /**
     * @return the objects that "match" the result
     */    
    public List getMatches() {
        return matches;
    }
}
