package org.intermine.api.mines;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;

/**
 * A mine that refers to the local application instance.
 * @author Alex Kalderimis
 *
 */
public class LocalMine implements ConfigurableMine
{
    private final InterMineAPI api;
    private final String id;
    private final String name;
    private final String url;
    private final String release;
    private final Set<String> defaultValues = new LinkedHashSet<String>();
    private String logo;
    private String bgcolor;
    private String frontcolor;
    private String description;

    /**
     * Construct a mine that refers to the local application instance.
     * @param api The InterMine API of this application
     * @param props The web-properties.
     */
    public LocalMine(InterMineAPI api, Properties props) {
        if (api == null) {
            throw new NullPointerException("api is null");
        }
        if (props == null) {
            throw new NullPointerException("props is null");
        }
        this.name = props.getProperty("project.title");
        if (name == null) {
            throw new NullPointerException("id is null");
        }
        this.url = props.getProperty("webapp.baseurl") + "/" + props.getProperty("webapp.path");
        this.release = props.getProperty("project.releaseVersion");
        this.api = api;
        this.id = this.name.toLowerCase().replaceAll("\\s", "");
    }

    @Override
    public void configure(Properties props) {
        logo = props.getProperty("logo");
        defaultValues.addAll(Arrays.asList(props.getProperty("defaultValues").split(",")));
        bgcolor = props.getProperty("bgcolor");
        frontcolor = props.getProperty("frontcolor");
        description = props.getProperty("description");
    }

    /**
     * @return The identifier of this mine.
     */
    public String getID() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getLogo() {
        return logo;
    }

    @Override
    public String getBgcolor() {
        return bgcolor;
    }

    @Override
    public String getFrontcolor() {
        return frontcolor;
    }

    @Override
    public String getReleaseVersion() {
        return release;
    }

    @Override
    public Model getModel() {
        return api.getModel();
    }

    @Override
    public Set<String> getDefaultValues() {
        return defaultValues;
    }

    @Override
    public String getDefaultValue() {
        if (defaultValues != null && !defaultValues.isEmpty()) {
            return defaultValues.iterator().next();
        }
        return null;
    }

    @Override
    public List<List<Object>> getRows(PathQuery query) {
        List<List<Object>> rows = new ArrayList<List<Object>>();
        if (!query.isValid()) {
            throw new IllegalArgumentException("query is not valid: "  + query.verifyQuery());
        }
        PathQueryExecutor runner = api.getPathQueryExecutor();
        ExportResultsIterator it;
        try {
            it = runner.execute(query);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Could not run query", e);
        }
        int rowWidth = query.getView().size();
        while (it.hasNext()) {
            List<ResultElement> row = it.next();
            List<Object> parsed = new ArrayList<Object>(rowWidth);
            for (ResultElement e: row) {
                Object o = e.getField();
                if (o instanceof CharSequence) {
                    // CharSequence subclasses can cause issues on serialisation - stringify them.
                    o = o.toString();
                }
                parsed.add(o);
            }
            rows.add(parsed);
        }
        return rows;
    }

    @Override
    public List<List<Object>> getRows(String xml) {
        PathQuery pq = PathQueryBinding.unmarshalPathQuery(
                new StringReader(xml), PathQuery.USERPROFILE_VERSION, getModel());
        return getRows(pq);
    }

}
