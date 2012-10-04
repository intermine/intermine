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

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.web.logic.widget.Widget;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.output.JSONFormatter;

/**
 * @author dbutano
 */
public abstract class WidgetService extends JSONService
{

    public WidgetService(InterMineAPI im) {
        super(im);
    }

    protected InterMineBag retrieveBag(String bagName) {
        if ("".equals(bagName)) {
            return null;
        }
        Profile profile = getPermission().getProfile();
        InterMineBag imBag = im.getBagManager().getBag(profile, bagName);
        if (imBag == null) {
            throw new BadRequestException("You do not have access to a bag named" + bagName);
        }
        return imBag;
    }

    protected void addOutputAttribute(String label, String value) {
        if (value != null && !"".equals(value.trim())){
            addOutputInfo(label, value);
        }
    }

    protected void addOutputListInfo(InterMineBag imBag) {
        addOutputInfo("type", imBag.getType());
        addOutputInfo("list", imBag.getName());
        addOutputInfo("requestedAt", new Date().toGMTString());
    }

    protected void addOutputConfig(WidgetConfig config) {
        addOutputAttribute("title", config.getTitle());
        addOutputAttribute("description", config.getDescription());
    }

    protected void addOutputFilter(WidgetConfig widgetConfig, String filterSelectedValue,
        InterMineBag imBag) {
        addOutputAttribute("filterLabel", widgetConfig.getFilterLabel());
        String filters = widgetConfig.getFiltersValues(im.getObjectStore(), imBag);
        if (filters != null && !"".equals(filters)) {
            addOutputAttribute("filters", filters);
            addOutputAttribute("filterSelectedValue", filterSelectedValue);
        }
    }

    protected void addOutputResult(Widget widget) throws Exception {
        WidgetResultProcessor processor = getProcessor();
        Iterator<List<Object>> it = widget.getResults().iterator();
        while (it.hasNext()) {
            List<Object> row = it.next();
            List<String> processed = processor.formatRow(row);
            if (!formatIsFlatFile() && it.hasNext()) {
                processed.add("");
            }
            output.addResultItem(processed);
        }
    }

    protected abstract WidgetResultProcessor getProcessor();

    @Override
    protected Map<String, Object> getHeaderAttributes() {
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.putAll(super.getHeaderAttributes());
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"results\":[");
            attributes.put(JSONFormatter.KEY_OUTRO, "]");
        }
        return attributes;
    }
}
