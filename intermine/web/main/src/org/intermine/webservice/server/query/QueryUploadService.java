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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager.ApiPermission.Level;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;
import org.intermine.webservice.server.output.Formatter;
import org.intermine.webservice.server.output.JSONFormatter;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;

/**
 * A service to enable queries to be uploaded programmatically.
 * @author Alexis Kalderimis
 *
 */
public class QueryUploadService extends WebService
{

    /** The key for the queries parameter **/
    public static final String QUERIES_PARAMETER = "xml";
    /** The key for the version parameter **/
    public static final String VERSION_PARAMETER = "version";
    private static final Logger LOG = Logger.getLogger(QueryUploadService.class);
    private Map<String, InterMineBag> lists;
    private final Set<String> knownBags = new HashSet<String>();
    private final List<String> problems = new ArrayList<String>();
    protected Profile profile;

    /**
     * Constructor.
     * @param im A reference to the API configuration and settings bundle.
     */
    public QueryUploadService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected Format getDefaultFormat() {
        return Format.TEXT;
    }

    @Override
    protected boolean canServe(Format f) {
        return f == Format.JSON
             || f == Format.XML
             || f == Format.TEXT
             || f == Format.TSV
             || f == Format.CSV;
    }

    @Override
    protected void postInit() {
        profile = getPermission().getProfile();
        BagManager bagManager = im.getBagManager();
        lists = bagManager.getBags(profile);
        knownBags.addAll(lists.keySet());
        output.setHeaderAttributes(getHeaderAttributes());
    }

    private Map<String, Object> getHeaderAttributes() {
        Map<String, Object> headerAttributes = new HashMap<String, Object>();
        switch (getFormat()) {
            case JSON:
                headerAttributes.put(JSONFormatter.KEY_INTRO, "\"queries\":{");
                headerAttributes.put(JSONFormatter.KEY_OUTRO, "},");
                break;
            default:
                break;
        }
        return headerAttributes;
    }

    @Override
    protected void validateState() {
        if (getPermission().getLevel() == Level.RO) {
            throw new ServiceForbiddenException("Access denied.");
        }
    }

    @Override
    protected void execute() throws Exception {

        String queriesXML = getXML();
        Map<String, PathQuery> toSave = new HashMap<String, PathQuery>();
        int version = getVersion();

        Reader r = new StringReader(queriesXML);
        Map<String, PathQuery> queries;
        try {
            queries = PathQueryBinding.unmarshalPathQueries(r, version);
        } catch (Exception e) {
            throw new BadRequestException("Could not de-serialize queries: " + e.getMessage());
        }

        for (String name: queries.keySet()) {
            PathQuery pq = queries.get(name);
            pq.setTitle(name);
            if (!pq.isValid()) {
                problems.add(name + ": " + formatMessage(pq.verifyQuery()));
            } else {
                Set<String> missingBags = new HashSet<String>();
                for (String bag: pq.getBagNames()) {
                    if (!knownBags.contains(bag)) {
                        missingBags.add(bag);
                    }
                }
                if (missingBags.isEmpty()) {
                    toSave.put(name, pq);
                } else {
                    problems.add(name
                            + " references the following missing lists: " + missingBags);
                }
            }
        }
        if (toSave.size() != queries.size()) {
            throw new BadRequestException(
                    "Errors with queries. " + StringUtils.join(problems, "\n"));
        }

        try {
            Map<String, String> saved = saveQueries(toSave);
            Iterator<Entry<String, String>> it = saved.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, String> pair = it.next();
                addResultItem(pair, it.hasNext());
            }
        } catch (Exception e) {
            throw new ServiceException("Failed to save queries", e);
        }
    }

    /**
     * Save the given queries.
     * @param toSave The queries to save.
     * @return A mapping from given name to the name they were actually saved under.
     */
    protected Map<String, String> saveQueries(Map<String, PathQuery> toSave) {
        return profile.saveQueries(toSave);
    }

    private void addResultItem(Entry<String, String> mapping, boolean hasMore) {
        switch (getFormat()) {
            case JSON:
                List<String> line = Arrays.asList(
                        String.format("\"%s\":\"%s\"",
                                StringEscapeUtils.escapeJava(mapping.getKey()),
                                StringEscapeUtils.escapeJava(mapping.getValue())));
                if (hasMore) {
                    line.add("");
                }
                output.addResultItem(line);
                break;
            case TEXT:
                output.addResultItem(
                        Arrays.asList(
                                String.format("%s successfully saved as %s",
                                        mapping.getKey(), mapping.getValue())));
                break;
            default:
                output.addResultItem(Arrays.asList(mapping.getKey(), mapping.getValue()));
        }
    }

    /**
     * Get the XML which represents the queries to be saved.
     * @return The XML.
     * @throws IOException If we can't read from the request.
     */
    protected String getXML() throws IOException {
        String contentType = StringUtils.defaultString(request.getContentType(), "").trim();
        if (contentType.contains(";")) {
            String[] parts = contentType.split(";", 2);
            contentType = parts[0].trim();
        }

        LOG.debug("Reading queries from " + contentType + " data");
        String queriesXML;
        if ("application/xml".equals(contentType) || "text/xml".equals(contentType)) {
            InputStream in = request.getInputStream();
            queriesXML = IOUtils.toString(in);
        } else {
            queriesXML = getRequiredParameter("xml");
        }
        return queriesXML;
    }

    private String formatMessage(List<String> msgs) {
        StringBuilder sb = new StringBuilder();
        for (String msg : msgs) {
            sb.append(msg);
            if (!msg.endsWith(".")) {
                sb.append(".");
            }
        }
        return sb.toString();
    }

    private Integer getVersion() {
        return getIntParameter(VERSION_PARAMETER, PathQuery.USERPROFILE_VERSION);
    }

    @Override
    protected Output makeXMLOutput(PrintWriter out, String separator) {
        ResponseUtil.setXMLHeader(response, "uploaded-queries.xml");
        try {
            return new StreamedOutput(out, new XMLFormatter(out), separator);
        } catch (XMLStreamException e) {
            throw new ServiceException(e);
        } catch (FactoryConfigurationError e) {
            throw new ServiceException(e);
        }
    }

    private static class XMLFormatter extends Formatter
    {

        private XMLStreamWriter writer;

        XMLFormatter(PrintWriter output) throws XMLStreamException {
            this.writer = XMLOutputFactory.newInstance().createXMLStreamWriter(output);
        }

        @Override
        public String formatHeader(Map<String, Object> attributes) {
            try {
                writer.writeStartElement("report");
            } catch (XMLStreamException e) {
                throw new ServiceException(e);
            }
            return "";
        }

        @Override
        public String formatResult(List<String> resultRow) {
            try {
                writer.writeStartElement("entry");

                writer.writeStartElement("input");
                writer.writeCharacters(resultRow.get(0));
                writer.writeEndElement();

                writer.writeStartElement("saved-as");
                writer.writeCharacters(resultRow.get(1));
                writer.writeEndElement();

                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throw new ServiceException(e);
            }
            return "";
        }

        @Override
        public String formatFooter(String errorMessage, int errorCode) {
            try {
                if (errorMessage != null && errorCode >= 400) {
                    writer.writeStartElement("error");
                    writer.writeAttribute("code", String.valueOf(errorCode));

                    writer.writeStartElement("message");
                    writer.writeCharacters(errorMessage);
                    writer.writeEndElement();

                    writer.writeEndElement();
                }
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throw new ServiceException(e);
            }
            return "";
        }
    }
}
