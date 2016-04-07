package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.apache.commons.lang.StringUtils.defaultString;
import static org.apache.commons.lang.StringUtils.lowerCase;
import static org.intermine.metadata.StringUtil.trimSlashes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.Constants;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.output.HTMLTableFormatter;

/**
 * Service for returning the version of this service.
 * @author Alex Kalderimis
 *
 */
public class VersionService extends JSONService
{
    private String versionType;

    /**
     * Constructor
     * @param im The InterMine configuration object.
     */
    public VersionService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        // Serve the webservice version by default, which provides information about
        // server capabilities, rather than the release version, which
        // provides information about the data available.

        if (versionType.startsWith("release")) {
            addResultValue(webProperties.getProperty("project.releaseVersion"), false);
        } else if (versionType.startsWith("intermine")) {
            addResultValue(Constants.INTERMINE_VERSION, false);
        } else {
            addResultValue(Constants.WEB_SERVICE_VERSION, false);
        }
    }

    @Override
    protected void initState() {
        super.initState();
        versionType = lowerCase(trimSlashes(defaultString(request.getPathInfo(), "ws")));
    }

    @Override
    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = super.getHeaderAttributes();
        if (Format.HTML == getFormat()) {
            List<String> headers = new ArrayList<String>();
            String header = null;
            if (versionType.startsWith("release")) {
                header = "Release";
            } else if (versionType.startsWith("intermine")) {
                header = "InterMine version";
            } else {
                header = "API Version";
            }
            headers.add(header);
            attributes.put(HTMLTableFormatter.KEY_COLUMN_HEADERS, headers);
        }
        return attributes;
    }

    @Override
    protected Format getDefaultFormat() {
        if (hasCallback()) {
            return Format.JSON;
        } else {
            return Format.TEXT;
        }
    }

    @Override
    protected String getResultsKey() {
        return "version";
    }

    @Override
    protected boolean canServe(Format format) {
        return format == Format.JSON
            || format == Format.HTML
            || format == Format.TEXT;
    }

}
