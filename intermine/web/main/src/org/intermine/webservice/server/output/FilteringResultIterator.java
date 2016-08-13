package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.ObjectUtils;
import org.intermine.api.results.ResultElement;
import org.intermine.objectstore.query.Results;

/**
 * A result iterator that skips things we aren't interested in.
 * @author Alex Kalderimis
 *
 */
public class FilteringResultIterator implements Iterator<List<ResultElement>>
{

    private static Logger logger = Logger.getLogger(FilteringResultIterator.class);

    private int counter = 0;
    private int start = 0;
    private Integer end = null;
    private String filterTerm = null;
    private List<ResultElement> nextRow = null;

    private final Iterator<Object> subIter;

    /**
     * Construct a new iterator which does no filtering.
     * @param res The results to wrap.
     */
    public FilteringResultIterator(Results res) {
        this.subIter = res.iterator();
    }

    /**
     * Construct a new iterator which filters to things that match a filter term
     * and only returns results in a given window.
     * @param res The results to wrap.
     * @param start The index of the first result to return.
     * @param size The maximum number of results to return.
     * @param filterTerm A filter term.
     */
    public FilteringResultIterator(Results res, int start, int size, String filterTerm) {
        this(res);
        this.start = start;
        this.end = start + size;
        this.filterTerm = ObjectUtils.toString(filterTerm).toLowerCase();
        logger.debug(
                "START: " + start
                + ", END: " + (start + size)
                + ", FILTER: " + this.filterTerm);
    }

    @Override
    public boolean hasNext() {
        scrollToStart();
        if (nextRow != null) {
            return true;
        }
        if (counter >= start && (end == null || counter < end)) {
            if (StringUtils.isBlank(filterTerm)) {
                return subIter.hasNext();
            } else {
                try {
                    nextRow = next();
                } catch (NoSuchElementException e) {
                    return false;
                }
                return nextRow != null;
            }
        }
        return false;
    }

    @Override
    public List<ResultElement> next() {
        if (nextRow != null) {
            List<ResultElement> ret = nextRow;
            nextRow = null;
            return ret;
        }
        scrollToStart();
        if (end != null && counter > end) {
            throw new NoSuchElementException();
        }

        List<Object> l = null;
        while (l == null) {
            l = getNextInternal();
        }
        counter++;

        List<ResultElement> ret = new ArrayList<ResultElement>();
        for (Object o: l) {
            ResultElement re = new ResultElement(o);
            ret.add(re);
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    private void scrollToStart() {
        while (counter < start && subIter.hasNext()) {
            // throw away values we are not interested in.
            List<Object> l = (List<Object>) subIter.next();
            if (StringUtils.isBlank(filterTerm)) {
                counter++;
            } else {
                boolean contained = false;
                for (Object o: l) {
                    String n = ObjectUtils.toString(o).toLowerCase();
                    if (StringUtils.contains(n, filterTerm)) {
                        contained = true;
                    }
                }
                if (contained) {
                    counter++;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> getNextInternal() {
        List<Object> l = (List<Object>) subIter.next();
        if (StringUtils.isBlank(filterTerm)) {
            return l;
        } else {
            Object o = l.get(0);
            String n = (o == null) ? "" : o.toString().toLowerCase();
            if (n.indexOf(filterTerm) >= 0) {
                return l;
            } else {
                return null;
            }
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
