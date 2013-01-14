package org.intermine.webservice.server.query;

/*
 * Copyright (C) 2002-2013 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager.ApiPermission.Level;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

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
    private Map<String, InterMineBag> lists;
    private Set<String> knownBags;
    private Map<String, PathQuery> toSave;
    private List<String> problems;
    private Profile profile;
    private Set<String> saved;

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
        return f == Format.JSON || f == Format.TEXT;
    }

    @Override
    protected void initState() {
        toSave = new HashMap<String, PathQuery>();
        problems = new ArrayList<String>();
        saved = new HashSet<String>();
    }

    @Override
    protected void postInit() {
        profile = getPermission().getProfile();
        BagManager bagManager = im.getBagManager();
        lists = bagManager.getBags(profile);
        knownBags = lists.keySet();
    }

    @Override
    protected void validateState() {
        if (!isAuthenticated() || getPermission().getLevel() == Level.RO) {
            throw new ServiceForbiddenException("Not authenticated");
        }
    }

    @Override
    protected void execute() throws Exception {

        String queriesXML = getXML();
        int version = getVersion();

        Reader r = new StringReader(queriesXML);
        Map<String, PathQuery> queries = PathQueryBinding.unmarshalPathQueries(r, version);

        for (String name: queries.keySet()) {
            PathQuery pq = queries.get(name);
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
                    problems.add(name + " references the following missing lists: " + missingBags);
                }
            }
        }
        if (toSave.size() != queries.size()) {
            throw new BadRequestException("Errors with queries. " + StringUtils.join(problems, "\n"));
        }

        try {
            profile.saveQueries(toSave, saved);
        } catch (Exception e) {
            for (String name: saved) {
                profile.deleteQuery(name);
            }
            throw new ServiceException("Failed to save queries", e);
        }
    }

    protected String getXML() throws IOException {
        String contentType = StringUtils.defaultString(request.getContentType(), "").trim();

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
}
