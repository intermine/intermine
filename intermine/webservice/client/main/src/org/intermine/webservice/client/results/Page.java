package org.intermine.webservice.client.results;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Object representing a page parameter.
 *
 * A description of a web-service request page has two properties, start and size.
 * The default page is one starting at the beginning and with the maximum
 * available size.
 *
 * @author Alex Kalderimis
 *
 */
public final class Page
{
    /** The default page size (from the beginning to the maximum page size) **/
    public static final Page DEFAULT = new Page(0);
    private final int start;
    private final Integer size;

    /**
     * Construct a new page, specifying a start point and a size.
     * @param start The index of the first result to retrieve.
     * @param size The maximum size of the page of results you want back.
     */
    public Page(int start, int size) {
        this.start = start;
        this.size = size;
    }

    /**
     * Construct a new page, specifying a start point.
     * @param start The index of the first result to retrieve.
     */
    public Page(int start) {
        this.start = start;
        this.size = null;
    }

    /**
     * Get the start index of this page.
     * @return The index.
     */
    public int getStart() {
        return start;
    }

    /**
     * Get the maximum size of this page.
     * @return the size.
     */
    public Integer getSize() {
        return size;
    }
}
