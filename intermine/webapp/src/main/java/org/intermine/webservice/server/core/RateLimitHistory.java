package org.intermine.webservice.server.core;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A class for making sure that certain resources don't get hammered.
 * @author Alex Kalderimis
 */
public class RateLimitHistory implements Predicate<String>
{

    private final Map<String, List<Long>> historyOfRequests = new HashMap<String, List<Long>>();

    private int periodLength;
    private int maxRequests;

    /**
     * Constructor
     * @param periodInSeconds The period we take into consideration.
     * @param maxRequestsPerPeriod The maximum number of requests in any given period.
     */
    public RateLimitHistory(int periodInSeconds, int maxRequestsPerPeriod) {
        periodLength = periodInSeconds;
        maxRequests = maxRequestsPerPeriod;
    }

    private synchronized List<Long> getRequestHistory(String id) {
        List<Long> requestHistory = new ArrayList<Long>();
        if (id != null) {
            if (historyOfRequests.containsKey(id)) {
                requestHistory = historyOfRequests.get(id);
                // Cull all records of requests made outside time period.
                for (Iterator<Long> it = requestHistory.iterator(); it.hasNext();) {
                    Long timestamp = it.next();
                    if (timestamp == null
                            || System.currentTimeMillis() - timestamp > (periodLength * 1000)) {
                        it.remove();
                    }
                }
            } else {
                historyOfRequests.put(id, requestHistory);
            }
        }
        return requestHistory;
    }

    /**
     * Check that this requester is within their limit.
     * @param id The ID to key their requests against.
     * @return true or false.
     */
    public synchronized boolean isWithinLimit(String id) {
        List<Long> requests = getRequestHistory(id);
        return requests.size() < maxRequests;
    }

    @Override
    public Boolean call(String id) {
        return isWithinLimit(id);
    }

    /**
     * Record that a request was made.
     * @param id The id to key this request against.
     */
    public void recordRequest(String id) {
        List<Long> requests = getRequestHistory(id);
        synchronized (requests) {
            requests.add(Long.valueOf(System.currentTimeMillis()));
        }
    }
}
