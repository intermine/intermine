package org.flymine.objectstore.query;

import java.util.List;
import java.util.AbstractList;
import java.util.ArrayList;

import org.flymine.objectstore.ObjectStore;

/**
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class Results extends AbstractList
{
    protected Query query;
    protected ObjectStore os;
    protected int batchsize;
    protected int lookahead;
    protected List resultsRows = new ArrayList(); // @element-type ResultRow

    /**
     * @param start the start index
     * @param end the end index
     * @return the relevant objects
     */    
    public List range(int start, int end) {
        return null;
    }

    /**
     * @param start the start index
     * @param end the end index
     */    
    public void fetchRows(int start, int end) {
    }
    
    /**
     * @param index of the object required
     * @return the relevant object
     */    
    public Object get(int index) {
        return null;
    }
    
    /**
     * @return the number of rows in this results item
     */    
    public int size() {
        return 0;
    }
}
