package org.intermine.webservice.server.widget;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;


import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.widget.TableWidget;
import org.intermine.web.logic.widget.config.TableWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;

/**
 * Web service that returns a table widget for a given list.
 * URL examples: get a TableWidget
 * /service/list/table?list=copy&widget=interactions
 *
 * @author dbutano
 */
public class TableWidgetService extends WidgetService
{

    /**
     * Construct a TableWidgetService
     * @param im the intermine  API object
     */
    public TableWidgetService(InterMineAPI im) {
        super(im);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void execute() throws Exception {
        String bagName = request.getParameter("list");
        String widgetId = request.getParameter("widget");

        InterMineBag imBag = retrieveBag(bagName);
        addOutputListInfo(imBag);

        WebConfig webConfig = InterMineContext.getWebConfig();
        WidgetConfig widgetConfig = webConfig.getWidgets().get(widgetId);
        if (widgetConfig == null || !(widgetConfig instanceof TableWidgetConfig)) {
            throw new ResourceNotFoundException("Could not find a table widget called \""
                    + widgetId + "\"");
        }

        addOutputConfig(widgetConfig);

        TableWidget widget = null;
        try {
            Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();
            ((TableWidgetConfig) widgetConfig).setClassKeys(classKeys);
            widget = (TableWidget) widgetConfig.getWidget(imBag, null, im.getObjectStore(), null);
            addOutputInfo("columns", StringUtils.join(widget.getColumns().toArray(), ","));
        } catch (ClassCastException e) {
            throw new ResourceNotFoundException("Could not find a table widget called \""
                    + widgetId + "\"", e);
        }
        if (widget == null) {
            throw new InternalErrorException("Problem loading widget");
        }
        addOutputInfo("notAnalysed", Integer.toString(widget.getNotAnalysed()));
        addOutputPathQuery(widget, widgetConfig);
        addOutputResult(widget);
    }

    @Override
    protected void addOutputConfig(WidgetConfig config) {
        super.addOutputConfig(config);
        addOutputInfo("columnTitle", ((TableWidgetConfig) config).getColumnTitle());
    }

    /**
     * Add in the output the pathQuery and the pathConstraint that will be add to it
     * @param widget the table widget
     * @param config the table config
     */
    private void addOutputPathQuery(TableWidget widget, WidgetConfig config) {
        addOutputInfo("pathQuery", widget.getPathQuery().toJson());
        TableWidgetConfig tableWidgetConfig = (TableWidgetConfig) config;
        String pathStrings = tableWidgetConfig.getPathStrings();
        if (pathStrings.contains("[") && pathStrings.contains("]")) {
            pathStrings = pathStrings.substring(0, pathStrings.indexOf("["))
                          + pathStrings.substring(pathStrings.indexOf("]") + 1);
        }
        String prefix =  pathStrings + ".";
        addOutputInfo("pathConstraint", prefix + "id");
    }

    @Override
    protected WidgetResultProcessor getProcessor() {
        if (formatIsJSON()) {
            return TableJSONProcessor.instance();
        } else if (formatIsXML()) {
            return TableXMLProcessor.instance();
        } else {
            return FlatFileWidgetResultProcessor.instance();
        }
    }
}
