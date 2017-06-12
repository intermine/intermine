package org.intermine.api.mines;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.metadata.InterMineModelParser;
import org.intermine.metadata.Model;
import org.intermine.metadata.ModelParser;
import org.intermine.metadata.ModelParserException;
import org.intermine.pathquery.PathQuery;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * An object that represents an InterMine application hosted somewhere else.
 * @author Alex Kalderimis
 *
 */
public class RemoteMine implements ConfigurableMine
{

    private static final String AN_ARG_IS_NULL
        = "all arguments must have values, got id = %s, requester = %s";
    private static final long MAX_UPDATE_INTERVAL = 24L * 60L * 60L * 1000L; // At least once a day.
    private static final String RELEASE_VERSION_URL = "/service/version/release";
    private static final String MODEL_URL           = "/service/model";
    private static final String QUERY_RESULTS_PATH  = "/service/query/results";

    private static final Logger LOG = Logger.getLogger(RemoteMine.class);

    private final String id;
    private final MineRequester requester;
    private final long updateInterval;
    private String name;
    private String url;
    private String logo;
    private Set<String> defaultValues = new HashSet<String>();
    private String bgcolor;
    private String frontcolor;
    private String description;
    private String release;
    private Model model;

    private long lastReleaseUpdate = System.currentTimeMillis() - MAX_UPDATE_INTERVAL;
    private long lastModelUpdate = System.currentTimeMillis() - MAX_UPDATE_INTERVAL;

    /**
     * Create a representation of a remotely accessible mine.
     * @param id The identifier of the mine.
     * @param requester Something to use to make requests with.
     * @param updateInterval The maximum number of seconds we can accept stale data for.
     */
    public RemoteMine(String id, MineRequester requester, int updateInterval) {
        if (id == null || requester == null) {
            throw new NullPointerException(String.format(AN_ARG_IS_NULL, id, requester));
        }
        this.updateInterval = Math.min(MAX_UPDATE_INTERVAL, updateInterval * 1000L);
        this.id = id;
        this.requester = requester;
    }

    @Override
    public void configure(Properties props) throws ConfigurationException {
        name = props.getProperty("name");
        url = props.getProperty("url");
        logo = props.getProperty("logo");
        defaultValues.addAll(Arrays.asList(props.getProperty("defaultValues", "").split(",")));
        bgcolor = props.getProperty("bgcolor");
        frontcolor = props.getProperty("frontcolor");
        description = props.getProperty("description");
        if (StringUtils.isBlank(name)) {
            throw new ConfigurationException("name is blank");
        }
        if (StringUtils.isBlank("url")) {
            throw new ConfigurationException("url is blank");
        }
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
        long now = System.currentTimeMillis();
        if (lastReleaseUpdate + updateInterval <= now) {
            updateRelease();
            lastReleaseUpdate = now;
        }
        return release;
    }

    @Override
    public Model getModel() {
        long now = System.currentTimeMillis();
        if (lastModelUpdate + updateInterval <= now) {
            updateModel();
            lastModelUpdate = now;
        }
        return model;
    }

    private void updateRelease() {
        BufferedReader r = null;
        try {
            r = requester.requestURL(getUrl() + RELEASE_VERSION_URL, ContentType.PlainText);
            release = IOUtils.toString(r);
        } catch (IOException e) {
            LOG.error("Error updating release version", e);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e) {
                    LOG.error("Error closing connection", e);
                }
            }
        }
    }

    private void updateModel () {
        BufferedReader r = null;
        try {
            r = requester.requestURL(getUrl() + MODEL_URL, ContentType.XML);
            ModelParser m = new InterMineModelParser();
            model = m.process(r);
        } catch (ModelParserException e) {
            LOG.error("Error reading remote model", e);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e) {
                    LOG.error("Error closing connection", e);
                }
            }
        }
    }

    @Override
    public Set<String> getDefaultValues() {
        return defaultValues;
    }

    @Override
    public String getDefaultValue() {
        for (String value : defaultValues) {
            return value;
        }
        return null;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public List<List<Object>> getRows(PathQuery query) {
        String xml = query.toXml();
        return getRows(xml);
    }

    @Override
    public List<List<Object>> getRows(String xml) {
        String params;
        try {
            params = "query=" + URLEncoder.encode("" + xml, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Could not encode query", e);
        }
        String reqUrl = getUrl() + QUERY_RESULTS_PATH + "?" + params;
        BufferedReader reader = null;
        List<List<Object>> ret = null;
        try {
            reader = requester.requestURL(reqUrl, ContentType.JSON);
            if (reader == null) {
                throw new RuntimeException("reader is null");
            }
            JSONTokener jreader = new JSONTokener(reader);
            Object nextValue = jreader.nextValue();
            if (!(nextValue instanceof JSONObject)) {
                throw new RuntimeException("Unexpected value: " + nextValue);
            }
            JSONObject response = (JSONObject) nextValue;
            JSONArray results = response.getJSONArray("results");
            int c = results.length();
            ret = new ArrayList<List<Object>>(c);

            for (int i = 0; i < c; i++) {
                JSONArray row = results.getJSONArray(i);
                List<Object> parsed = new ArrayList<Object>();
                for (int j = 0; j < row.length(); j++) {
                    Object o = row.get(j);
                    if (JSONObject.NULL.equals(o)) {
                        parsed.add(null);
                    } else {
                        parsed.add(o);
                    }
                }
                ret.add(parsed);
            }

        } catch (JSONException e) {
            throw new RuntimeException("Error getting rows " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOG.error("Error closing reader", e);
                }
            }
        }
        return ret;
    }

}
