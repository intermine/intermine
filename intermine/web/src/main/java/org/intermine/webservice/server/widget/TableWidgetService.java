package org.intermine.webservice.server.widget;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import org.intermine.webservice.server.exceptions.ServiceException;
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

    private final WidgetsRequestParser requestParser;

    /**
     * Construct a TableWidgetService
     * @param im the intermine  API object
     */
    public TableWidgetService(InterMineAPI im) {
        super(im);
        requestParser = new WidgetsRequestParser();
    }

    @Override
    protected void execute() {
        WidgetsServiceInput input = requestParser.getInput(request);

        InterMineBag imBag = retrieveBag(input.getBagName());
        addOutputListInfo(imBag);

        WebConfig webConfig = InterMineContext.getWebConfig();
        WidgetConfig widgetConfig = webConfig.getWidgets().get(input.getWidgetId());
        if (widgetConfig == null || !(widgetConfig instanceof TableWidgetConfig)) {
            throw new ResourceNotFoundException("Could not find a table widget called \""
                    + input.getWidgetId() + "\"");
        }

        addOutputConfig(widgetConfig);
        String ids = input.getIds();
        String populationIds = input.getPopulationIds();
        TableWidget widget = null;
        try {
            Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();
            TableWidgetConfig twc = (TableWidgetConfig) widgetConfig;
            twc.setClassKeys(classKeys);
            widget = twc.getWidget(imBag, null, im.getObjectStore(), input, ids, populationIds);
            widget.process();
            addOutputInfo("columns", StringUtils.join(widget.getColumns().toArray(), ","));
        } catch (ClassCastException e) {
            throw new ResourceNotFoundException("Could not find a table widget called \""
                    + input.getWidgetId() + "\"", e);
        }
        addOutputInfo("notAnalysed", Integer.toString(widget.getNotAnalysed()));
        addOutputPathQuery(widget, widgetConfig);
        try {
            addOutputResult(widget);
        } catch (Exception e) {
            throw new ServiceException("Could not get results.", e);
        }
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
