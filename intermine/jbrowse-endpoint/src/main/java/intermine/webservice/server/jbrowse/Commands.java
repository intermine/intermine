package org.intermine.webservice.server.jbrowse;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Alex
 *
 */
public final class Commands
{
    /**
     * @author Alex
     */
    public enum Action {
        /**
         * stats
         */
        STATS,
        /**
         * reference
         */
        REFERENCE,
        /**
         * features
         */
        FEATURES,
        /**
         * densities
         */
        DENSITIES
    }

    private Commands() {
        // don't
    }

    /**
     *  return null if not a suitable command.
     *
     * Interprets commands such as: /7227/features/X?start=100&end=200&type=Gene
     * as /(domain)/(action)/(section)?start=(start)&end=(end)&type=(featureType)
     * See: http://gmod.org/wiki/JBrowse_Configuration_Guide
     * #Writing_JBrowse-compatible_Web_Services
     *
     * @param pathInfo path info
     * @param parameters params
     * @return command
     */
    public static Command getCommand(
            String pathInfo,
            Map<String, String> parameters) {
        if (pathInfo == null) {
            return null;
        }

        Integer start = getIntegerParam(parameters, "start");
        Integer end = getIntegerParam(parameters, "end");

        String[] parts = StringUtils.split(pathInfo.substring(1), "/");
        if (parts.length < 3 || parts.length > 4) {
            return null;
        }

        String domain = parts[0];
        String actionName = parts[1];
        String section = parts[2];
        String realSection = (parts.length == 4) ? parts[3] : null;
        String featureType = parameters.get("type");
        if ("stats".equals(actionName) && !"global".equals(section)) {
            section = realSection;
        }

        Segment segment = Segment.makeSegment(section, start, end);
        Action action = null;

        if ("stats".equals(actionName)) {
            action = ("regionFeatureDensities".equals(parts[2])) ? Action.DENSITIES : Action.STATS;
        } else if ("features".equals(actionName)) {
            if ("true".equals(parameters.get("reference"))) {
                action = Action.REFERENCE;
            } else {
                action = Action.FEATURES;
            }
        }
        if (action == null) {
            return null;
        } else {
            return new Command(action, domain, featureType, segment, parameters);
        }
    }

    private static Integer getIntegerParam(Map<String, String> params, String key) {
        String numStr = params.get(key);
        if (numStr == null || "null".equalsIgnoreCase(numStr)) {
            return null;
        }
        return Integer.valueOf(numStr);
    }

}
