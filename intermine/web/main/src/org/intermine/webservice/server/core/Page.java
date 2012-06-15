package org.intermine.webservice.server.core;

/**
 * Parameter class for passing start and limit arguments to
 * iterators.
 * @author Alex Kalderimis
 *
 */
public class Page
{
    private final int start;
    private final Integer size;

    /**
     * Construct a new page.
     * @param start The index of the first row to return.
     * @param size The maximum number of rows to return.
     */
    public Page(int start, Integer size) {
        this.start = start;
        this.size = size;
    }

    /**
     * Construct a new page, going from a certain row to the end
     * of the result set.
     * @param start The index of the first result to return.
     */
    public Page(int start) {
        this(start, null);
    }

    /** 
     * Get the index of the first row that is requested.
     * @return The index of the first row to return.
     */
    public int getStart() {
        return start;
    }

    /**
     * @return The requested size, or NULL if all results are
     * requested.
     */
    public Integer getSize() {
        return size;
    }

    /**
     * @return The index of the last result to be returned, or NULL
     * if all results are requested.
     */
    public Integer getEnd() {
        if (size == null) {
            return null;
        } else {
            return start + size;
        }
    }
    
    /**
     * @param index An index to test.
     * @return Whether or not the given index lies within the page.
     */
    public boolean withinRange(int index) {
        Integer end = getEnd();
        if (end != null && index >= end) {
            return false;
        }
        return index >= start;
    }
}
