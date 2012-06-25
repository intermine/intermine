package org.intermine.webservice.server.query;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.SavedQuery;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;

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

    /**
     * The usage string to school recalcitrant users with.
     */
    public static final String USAGE =
          "\nQuery Upload Service:\n"
        + "==========================\n"
        + "Parameters:\n"
        + QUERIES_PARAMETER + ": XML representation of template(s)\n"
        + VERSION_PARAMETER + ": (optional) XML version number\n"
        + "NOTE: all template upload requests must be authenticated.\n";

    /**
     * Constructor.
     * @param im A reference to the API configuration and settings bundle.
     */
    public QueryUploadService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        if (!isAuthenticated()) {
            throw new BadRequestException("Not authenticated" + USAGE);
        }
        String queriesXML = request.getParameter(QUERIES_PARAMETER);
        if (queriesXML == null || "".equals(queriesXML)) {
            throw new BadRequestException("No XML data." + USAGE);
        }
        Profile profile = getPermission().getProfile();
        BagManager bagManager = this.im.getBagManager();

        Map<String, InterMineBag> lists = bagManager.getUserAndGlobalBags(profile);

        int version = getVersion(request);
        Reader r = new StringReader(queriesXML);

        Map<String, PathQuery> queries;
        Map<String, SavedQuery> toSave = new HashMap<String, SavedQuery>();
        try {
            queries = PathQueryBinding.unmarshalPathQueries(r, version);
        } catch (RuntimeException e) {
            throw new ServiceException("Error parsing queries", e);
        }

        for (String name: queries.keySet()) {
            PathQuery pq = queries.get(name);
            if (!pq.isValid()) {
                this.output.addResultItem(Arrays.asList(name,
                    formatMessage(pq.verifyQuery())));
            } else {
                Set<String> missingBags = new HashSet<String>();
                for (String bag: pq.getBagNames()) {
                    if (!lists.keySet().contains(bag)) {
                        missingBags.add(bag);
                    }
                }
                if (missingBags.isEmpty()) {
                    toSave.put(name, new SavedQuery(name, new Date(), pq));
                } else {
                    this.output.addResultItem(Arrays.asList(name,
                        "The following lists referred to by " + name
                        + " either don't exist or cannot be accessed"
                        + " by this user account: " + missingBags));
                }
            }
        }
        if (toSave.size() != queries.size()) {
            throw new BadRequestException("Errors with queries");
        }
        for (String name: toSave.keySet()) {
            try {
                profile.saveQuery(name, toSave.get(name));
                this.output.addResultItem(Arrays.asList(name, "Success"));
            } catch (RuntimeException e) {
                throw new ServiceException("Failed to save template: " + name, e);
            }
        }
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

    private int getVersion(HttpServletRequest request) {
        String versionString = request.getParameter(VERSION_PARAMETER);
        if (versionString == null || "".equals(versionString)) {
            return PathQuery.USERPROFILE_VERSION;
        }
        try {
            int version = Integer.parseInt(versionString);
            return version;
        } catch (NumberFormatException e) {
            throw new ServiceException("Version provided in request (" + versionString
                    + ") can not be parsed to an integer");
        }
    }
}
