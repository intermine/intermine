package org.intermine.webservice.client.results;

/*
 * Copyright (C) 2002-2016 FlyMine
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
 * This class also has facilities for advancing between pages.
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
        if (size < 1) {
            throw new IllegalArgumentException("size must be >= 1, not " + size);
        }
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

    /**
     * Advance to a new page my a given number of steps.
     * If this is the first page, and <code>2</code> is given as
     * an argument, an object representing the third page will be returned.
     *
     * The new page will have the same size, but an adjusted starting point.
     * The adjusted starting point will never be negative.
     *
     * Negative arguments can be given to go back. An argument of 0 will return
     * the caller.
     *
     * @param steps The number of pages to advance. <code>1</code> gives the next page.
     * @return A new page, or this page, if the argument is <code>0</code>.
     */
    public Page advance(int steps) {
        if (steps == 0) {
            return this;
        } else {
            int newStart = getStart() + (steps * getSize());
            if (newStart < 0) {
                return first();
            }
            return new Page(newStart, getSize());
        }
    }

    /**
     * Get the first page. The new page will have the same size as
     * this page, but a starting position of <code>0</code>.
     * @return The first page.
     */
    public Page first() {
        return new Page(0, getSize());
    }

    /**
     * Get the next page. The new page will have the same size as
     * this page, but a starting position of <code>start + size</code>.
     * @return The next page.
     */
    public Page next() {
        return advance(1);
    }

    /**
     * Get the last page. The new page will have the same size
     * as this page, but a starting position such that
     * <code>start + size >= total</code>.
     *
     * If this page has no size (ie. it is an open-ended page)
     * then the caller will be returned.
     *
     * @param total The total size of the result set.
     * @return A page that includes the last result row.
     */
    public Page last(int total) {
        if (size == null) {
            return this;
        } else {
            int newStart = getStart();
            while ((newStart + size) < total) {
                newStart += size;
            }
            return new Page(newStart, size);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof Page)) {
            return false;
        }
        Page otherPage = (Page) other;
        return start == otherPage.getStart()
                && (size == null) ? otherPage.getSize() == null : size.equals(otherPage.getSize());
    }

    @Override
    public int hashCode() {
        int h = 0;
        h += 31 * start;
        if (size != null) {
            h += 31 * size;
        }
        return h;
    }

}
