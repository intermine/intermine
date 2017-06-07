package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.metadata.TypeUtil;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.InlineResultsTable;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Display all objects mentioned by publication
 *
 * @author Julie Sullivan
 */
public class PublicationAnnotationsDisplayer extends ReportDisplayer
{

    protected static final Logger LOG = Logger.getLogger(PublicationAnnotationsDisplayer.class);

    /**
     * Construct with config and the InterMineAPI.
     *
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public PublicationAnnotationsDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        Map<String, InlineResultsTable> result = new HashMap<String, InlineResultsTable>();
        InterMineObject object = reportObject.getObject();
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        String params = config.getParameterString();
        if (StringUtils.isEmpty(params)) {
            // don't know which collections to display, although I suppose we could show genes
            LOG.error("Parameters not configured for publications displayer, not showing any.");
            return;
        }
        Profile profile = SessionMethods.getProfile(session);
        PathQueryExecutor executor = im.getPathQueryExecutor(profile);
        for (String path : params.split("[, ]+")) {
            String type = getType(path);
            List<Class<?>> lc = getClass(type, im.getModel());
            if (lc.isEmpty()) {
                // invalid class name
                LOG.error("Invalid class name: " + type);
                continue;
            }
            PathQuery q = new PathQuery(im.getModel());
            q.addView(type + ".id");
            q.addConstraint(Constraints.eq(path + ".publications.id", object.getId().toString()));
            if (!q.isValid()) {
                // try again
                q = new PathQuery(im.getModel());
                q.addView(type + ".id");
                q.addConstraint(Constraints.eq(path + ".publication.id",
                        object.getId().toString()));
                if (!q.isValid()) {
                    //  no publications collection found
                    LOG.error("No publications reference or collection found: " + path);
                    continue;
                }
            }
            ExportResultsIterator values;
            try {
                values = executor.execute(q);
            } catch (ObjectStoreException e) {
                throw new RuntimeException(e);
            }
            Collection<InterMineObject> results = new HashSet<InterMineObject>();
            int count = formatResults(results, values);
            InlineResultsTable t = new InlineResultsTable(results, im.getModel(),
                    SessionMethods.getWebConfig(request), im.getClassKeys(), count,
                    false, lc);
            if (count > 0) {
                result.put(type, t);
            }
        }
        request.setAttribute("results", result);
    }

    private List<Class<?>> getClass(String type, Model model) {
        ArrayList<Class<?>> lc = new ArrayList<Class<?>>();
        Class<?> c;
        try {
            c = TypeUtil.getClass(type, model);
            lc.add(c);
        } catch (ClassNotFoundException e) {
            // return empty list
        }
        return lc;
    }

    private String getType(String path) {
        if (path.contains(".")) {
            return path.split("\\.")[0];
        }
        return path;
    }

    private int formatResults(Collection<InterMineObject> results,
            ExportResultsIterator it) {
        int i = 0;
        while (it.hasNext()) {
            List<ResultElement> row = it.next();
            ResultElement e = (ResultElement) row.get(0);
            InterMineObject o = (InterMineObject) e.getObject();
            results.add(o);
            i++;
        }
        return i;
    }
}
