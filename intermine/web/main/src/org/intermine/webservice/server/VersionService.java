package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.Constants;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.output.JSONFormatter;

/**
 * Service for returning the version of this service.
 * @author Alex Kalderimis
 *
 */
public class VersionService extends JSONService
{

    /**
     * Constructor
     * @param im The InterMine configuration object.
     */
    public VersionService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        String version = getVersion(request.getPathInfo());
        output.addResultItem(Collections.singletonList(version));
    }

    @Override
    protected int getDefaultFormat() {
        if (hasCallback()) {
            return WebService.JSONP_FORMAT;
        } else {
            return WebService.TEXT_FORMAT;
        }
    }

    @Override
    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attr = super.getHeaderAttributes();
        if (formatIsJSON()) {
            attr.put(JSONFormatter.KEY_INTRO, "\"version\":");
            attr.put(JSONFormatter.KEY_QUOTE, true);
        }

        return attr;
    }

    private String getVersion(String versionType) {
        if (versionType != null) {
            versionType = StringUtil.trimSlashes(versionType).toLowerCase();

            if (versionType.startsWith("release")) {
                return webProperties.getProperty("project.releaseVersion");
            } else if (versionType.startsWith("ws")) {
                return "" + Constants.WEB_SERVICE_VERSION;
            }
        }
        // for backwards compatibility default is the web service version
        return "" + Constants.WEB_SERVICE_VERSION;
    }

    @Override
    protected String parseFormatFromPathInfo() {
        String pi = request.getPathInfo();
        if (pi.endsWith("json")) {
            return "json";
        } else if (pi.endsWith("jsonp")) {
            return "jsonp";
        }
        return request.getParameter(WebServiceRequestParser.OUTPUT_PARAMETER);
    }

}
