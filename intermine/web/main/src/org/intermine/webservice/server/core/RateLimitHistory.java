package org.intermine.webservice.server.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RateLimitHistory {
    private final Map<String, List<Long>> historyOfRequests = new HashMap<String, List<Long>>();

    private int periodLength;
    private int maxRequests;

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
                    if (timestamp == null || System.currentTimeMillis() - timestamp > (periodLength * 1000)) {
                        it.remove();
                    }
                }
            } else {
                historyOfRequests.put(id, requestHistory);
            }
        }
        return requestHistory;
    }

    public synchronized boolean isWithinLimit(String id) {
        List<Long> requests = getRequestHistory(id);
        return requests.size() < maxRequests;
    }

    public void recordRequest(String id) {
        List<Long> requests = getRequestHistory(id);
        synchronized (requests) {
            requests.add(Long.valueOf(System.currentTimeMillis()));
        }
    }
}
