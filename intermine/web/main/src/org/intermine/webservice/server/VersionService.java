package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2013 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.Constants;
import org.intermine.webservice.server.core.JSONService;

import static org.apache.commons.lang.StringUtils.defaultString;
import static org.apache.commons.lang.StringUtils.lowerCase;
import static org.intermine.util.StringUtil.trimSlashes;

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
        // Serve the webservice version by default, which provides information about
        // server capabilities, rather than the release version, which
        // provides information about the data available.
        String versionType = lowerCase(trimSlashes(defaultString(request.getPathInfo(), "ws")));

        if (versionType.startsWith("release")) {
            this.addResultValue(webProperties.getProperty("project.releaseVersion"), false);
        } else {
            this.addResultValue(Constants.WEB_SERVICE_VERSION, false);
        }
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
        return format == Format.JSON || format == Format.TEXT; 
    }

}
