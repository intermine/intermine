package org.intermine.webservice.client.services;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.intermine.webservice.client.core.ContentType;
import org.intermine.webservice.client.core.Request;
import org.intermine.webservice.client.core.RequestImpl;
import org.intermine.webservice.client.core.Service;

import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.util.HttpConnection;

import org.intermine.webservice.client.widget.Widget;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * A class for managing widget operations.
 *
 * This service provides information directly, and manages interactions
 * for other services.
 *
 * Normally the user should obtain an instance of this class through the
 * ServiceFactory class. Any authentication provided to that parent class
 * will be used in all requests that need authentication by the WidgetService.
 *
 * The services provided include:
 * <ul>
 *   <li>Getting information about the available widgets</li>
 * </ul>
 *
 * @author Alex Kalderimis
 * @version 1
 **/
public class WidgetService extends Service
{
    private static final String SERVICE_RELATIVE_URL = "/widgets";

    /**
     * Use {@link ServiceFactory} instead of constructor for creating this service .
     * @param rootUrl root URL
     * @param applicationName application name
     */
    public WidgetService(String rootUrl, String applicationName) {
        super(rootUrl, SERVICE_RELATIVE_URL, applicationName);
    }

    /** A request implementation for use in this service **/
    private static final class WidgetRequest extends RequestImpl
    {
        private WidgetRequest(String url) {
            super(RequestType.GET, url, ContentType.APPLICATION_JSON);
        }
    }

    /**
     * @return Get the available widgets.
     */
    public List<Widget> getWidgets() {
        Request request = new WidgetRequest(getUrl());
        return processWidgetRequest(request);
    }

    /**
     * @param name The name of the widget you want.
     * @return A widget by name, or null.
     */
    public Widget getWidget(String name) {
        List<Widget> widgets = getWidgets();
        for (Widget w: widgets) {
            if (w.getName().equals(name)) {
                return w;
            }
        }
        return null;
    }

    /**
     * @return All chart widgets.
     */
    public List<Widget> getChartWidgets() {
        List<Widget> widgets = getWidgets();
        List<Widget> chartWidgets = new LinkedList<Widget>();
        for (Widget w: widgets) {
            if (w.isChart()) {
                chartWidgets.add(w);
            }
        }
        return chartWidgets;
    }

    /**
     * @return All enrichment widgets.
     */
    public List<Widget> getEnrichmentWidgets() {
        List<Widget> widgets = getWidgets();
        List<Widget> enrichmentWidgets = new LinkedList<Widget>();
        for (Widget w: widgets) {
            if (w.isEnrichment()) {
                enrichmentWidgets.add(w);
            }
        }
        return enrichmentWidgets;
    }

    private List<Widget> processWidgetRequest(Request request) {
        HttpConnection connection = executeRequest(request);
        String body = connection.getResponseBodyAsString();
        try {
            JSONObject resultSet = new JSONObject(body);
            if (!resultSet.isNull("error")) {
                throw new ServiceException(resultSet.getString("error"));
            }
            JSONArray widgets = resultSet.getJSONArray("widgets");
            int length = widgets.length();
            List<Widget> ret = new ArrayList<Widget>();
            try {
                for (int i = 0; i < length; i++) {
                    ret.add(parseWidget(getFactory(),
                                widgets.getJSONObject(i)));
                }
            } catch (JSONException e) {
                throw new ServiceException("Error processing request: "
                        + request + ", Incorrect JSON returned: '"
                        + body + "'", e);
            }
            return ret;
        } catch (JSONException e) {
            throw new ServiceException("Error processing request: "
                    + request + ", error parsing widget data: '" + body + "'", e);
        }
    }

    private Widget parseWidget(ServiceFactory factory, JSONObject json)
        throws JSONException {
        String name = json.getString("name");
        String title = json.getString("title");
        String description = json.getString("description");
        String widgetType = json.getString("widgetType");
        String chartType = json.optString("chartType", null);
        List<String> targets = new LinkedList<String>();
        JSONArray targetArray = json.getJSONArray("targets");
        int length = targetArray.length();
        for (int i = 0; i < length; i++) {
            targets.add(targetArray.getString(i));
        }
        List<String> filters = new LinkedList<String>();
        JSONArray filterArray = json.getJSONArray("filters");
        length = filterArray.length();
        for (int i = 0; i < length; i++) {
            filters.add(filterArray.getString(i));
        }
        JSONObject labels = json.optJSONObject("labels");
        String xLabel = labels == null ? null : labels.getString("x");
        String yLabel = labels == null ? null : labels.getString("y");
        return new Widget(factory, name, title, description,
                widgetType, chartType, targets, filters, xLabel, yLabel);
    }
}
