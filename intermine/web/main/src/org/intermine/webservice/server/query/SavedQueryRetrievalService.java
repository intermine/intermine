package org.intermine.webservice.server.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.SavedQuery;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.core.Predicate;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.PlainFormatter;
import org.intermine.webservice.server.output.StreamedOutput;
import org.json.JSONException;
import org.json.JSONObject;

/** @author Alex Kalderimis **/
public class SavedQueryRetrievalService extends JSONService
{

    /** @param im The InterMine state object. **/
    public SavedQueryRetrievalService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected boolean canServe(Format format) {
        switch (format) {
            case XML:
            case JSON:
                return true;
            default:
                return false;
        }
    }

    @Override
    protected Output makeXMLOutput(PrintWriter out, String separator) {
        ResponseUtil.setXMLHeader(response, "saved-queries.xml");
        return new StreamedOutput(out, new PlainFormatter(), separator);
    }

    @Override
    protected void execute() {
        Profile p = getPermission().getProfile();

        Predicate<String> filter = getFilter(getOptionalParameter("filter", ""));
        Map<String, PathQuery> queries = getQueries(filter, p.getSavedQueries());
        if (Format.JSON == getFormat()) {
            sendJSON(queries);
        } else {
            sendXML(queries);
        }
    }

    private void sendXML(Map<String, PathQuery> queries) {
        int version = im.getProfileManager().getVersion();

        try {
            XMLStreamWriter writer
                = XMLOutputFactory.newInstance().createXMLStreamWriter(getRawOutput());

            writer.writeStartElement("saved-queries");
            for (Entry<String, PathQuery> pair: queries.entrySet()) {
                PathQueryBinding.marshal(pair.getValue(),
                        pair.getKey(),
                        pair.getValue().getModel().getName(),
                        writer, version);
            }
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new ServiceException("Serialization error.", e);
        }
    }

    private void sendJSON(Map<String, PathQuery> queries) {
        JSONObject result = new JSONObject();
        try {
            for (Entry<String, PathQuery> pair: queries.entrySet()) {
                result.put(pair.getKey(), new JSONObject(pair.getValue().toJson()));
            }
            output.addResultItem(Arrays.asList(result.toString(0)));
        } catch (JSONException e) {
            throw new ServiceException("Serialization error.", e);
        }
    }

    private Map<String, PathQuery> getQueries(
            Predicate<String> filter,
            Map<String, SavedQuery> allSaved) {
        Map<String, PathQuery> queries = new HashMap<String, PathQuery>();
        for (Entry<String, SavedQuery> pair: allSaved.entrySet()) {
            SavedQuery q = pair.getValue();
            PathQuery pq = q.getPathQuery();
            if (filter.call(pair.getKey())) {
                queries.put(pair.getKey(), pq);
            }
        }
        return queries;
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

    private class AlwaysTrue implements Predicate<String>
    {

        @Override
        public Boolean call(String subject) {
            return true;
        }
    }

    private class Contains implements Predicate<String>
    {

        private final String target;

        Contains(String target) {
            this.target = target.toLowerCase();
        }

        @Override
        public Boolean call(String subject) {
            return subject != null && subject.toLowerCase().contains(target);
        }
    }

    private class EndsWith implements Predicate<String>
    {

        private final String target;

        EndsWith(String target) {
            this.target = target.toLowerCase();
        }

        @Override
        public Boolean call(String subject) {
            return subject != null && subject.toLowerCase().endsWith(target);
        }
    }

    private class StartsWith implements Predicate<String>
    {

        private final String target;

        StartsWith(String target) {
            this.target = target.toLowerCase();
        }

        @Override
        public Boolean call(String subject) {
            return subject != null && subject.toLowerCase().startsWith(target);
        }
    }

    private class Matches implements Predicate<String>
    {

        private final String target;

        Matches(String target) {
            this.target = target;
        }

        @Override
        public Boolean call(String subject) {
            return subject != null && subject.equalsIgnoreCase(target);
        }
    }
}
