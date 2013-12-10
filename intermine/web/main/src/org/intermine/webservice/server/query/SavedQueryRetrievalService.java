package org.intermine.webservice.server.query;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.SavedQuery;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.core.Predicate;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.json.JSONException;
import org.json.JSONObject;

public class SavedQueryRetrievalService extends JSONService {

    public SavedQueryRetrievalService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws ServiceException {
        Profile p = getPermission().getProfile();
        JSONObject queries = new JSONObject();
        Predicate<String> filter = getFilter(getOptionalParameter("filter", ""));
        try {
            for (Entry<String, SavedQuery> pair: p.getSavedQueries().entrySet()) {
                SavedQuery q = pair.getValue();
                PathQuery pq = q.getPathQuery();
                if (filter.test(pair.getKey())) {
                    queries.put(pair.getKey(), new JSONObject(pq.toJson()));
                }
            }
            output.addResultItem(Arrays.asList(queries.toString(0)));
        } catch (JSONException e) {
            throw new ServiceException("Serialization error.", e);
        }
    }

    private Predicate<String> getFilter(String filter) {
        filter = filter.trim();
        if (StringUtils.isBlank(filter) || "*".equals(filter)) {
            return new AlwaysTrue();
        }
        String noStars = filter.replaceAll("(^\\*|\\*$)", "");
        if (StringUtils.isBlank(noStars)) {
            throw new BadRequestException("Illegal filter string");
        }
        if (filter.startsWith("*")) {
            if (filter.endsWith("*")) {
                return new Contains(noStars);
            } else {
                return new EndsWith(noStars);
            }
        } else {
            if (filter.endsWith("*")) {
                return new StartsWith(noStars);
            } else {
                return new Matches(noStars);
            }
        }
    }

    @Override
    public String getResultsKey() {
        return "queries";
    }

    private class AlwaysTrue implements Predicate<String> {

        @Override
        public boolean test(String subject) {
            return true;
        }
    }
 
    private class Contains implements Predicate<String> {

        private final String target;

        Contains(String target) {
            this.target = target.toLowerCase();
        }

        @Override
        public boolean test(String subject) {
            return subject != null && subject.toLowerCase().contains(target);
        }
    }

    private class EndsWith implements Predicate<String> {

        private final String target;

        EndsWith(String target) {
            this.target = target.toLowerCase();
        }

        @Override
        public boolean test(String subject) {
            return subject != null && subject.toLowerCase().endsWith(target);
        }
    }

    private class StartsWith implements Predicate<String> {

        private final String target;

        StartsWith(String target) {
            this.target = target.toLowerCase();
        }

        @Override
        public boolean test(String subject) {
            return subject != null && subject.toLowerCase().startsWith(target);
        }
    }

    private class Matches implements Predicate<String> {

        private final String target;

        Matches(String target) {
            this.target = target;
        }

        @Override
        public boolean test(String subject) {
            return subject != null && subject.equalsIgnoreCase(target);
        }
    }
}
