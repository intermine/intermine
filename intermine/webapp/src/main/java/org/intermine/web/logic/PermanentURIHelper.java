package org.intermine.web.logic;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.uri.InterMineLUI;
import org.intermine.api.uri.InterMineLUIConverter;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.util.URLGenerator;

import javax.servlet.http.HttpServletRequest;
import java.util.Properties;

public class PermanentURIHelper {
    private static final Logger LOGGER = Logger.getLogger(PermanentURIHelper.class);

    public static String getPermanentURI(HttpServletRequest request, Integer interMineId) {
        InterMineLUIConverter converter = new InterMineLUIConverter();
        InterMineLUI interMineLUI = null;
        String permanentURI = null;
        try {
            interMineLUI = converter.getInterMineLUI(interMineId);
        } catch (ObjectStoreException ex) {
            LOGGER.error("Problems retrieving identifier from InterMineObjectStore");
        }
        if (interMineLUI != null) {
            final Properties webProperties = InterMineContext.getWebProperties();
            String baseURI = webProperties.getProperty("identifier.uri.base");
            if (baseURI == null || StringUtils.isEmpty(baseURI)) {
                baseURI = new URLGenerator(request).getPermanentBaseURL();
            }
            if (!baseURI.endsWith("/")) {
                baseURI = baseURI + "/";
            }
            permanentURI = baseURI + interMineLUI.toString();
        }
        return permanentURI;
    }
}
