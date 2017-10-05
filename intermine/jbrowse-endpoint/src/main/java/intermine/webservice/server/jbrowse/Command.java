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

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.intermine.webservice.server.jbrowse.Commands.Action;

/**
 * @author Alex
 */
public class Command
{

    private final Action action;
    private final String domain, featureType;
    private final Segment segment;
    private final Map<String, String> parameters;

    /**
     * @param a action
     * @param domain domain
     * @param fType feature type
     * @param s segment
     * @param params parameters
     */
    public Command(Action a, String domain, String fType, Segment s, Map<String, String> params) {
        this.action = a;
        this.domain = domain;
        this.featureType = fType;
        this.segment = s;
        this.parameters = new HashMap<String, String>(params);
    }

    /**
     * @return action
     */
    public Action getAction() {
        return action;
    }

    /**
     * @return domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @return segment
     */
    public Segment getSegment() {
        return segment;
    }

    /**
     * @param ifNull if null
     * @return featureType
     */
    public String getType(String ifNull) {
        if (featureType == null) {
            return ifNull;
        }
        return featureType;
    }

    /**
     * @param key key
     * @return parameter
     */
    public String getParameter(String key) {
        return parameters.get(key);
    }

    /**
     * @param key key
     * @param ifNull if null
     * @return parameter
     */
    public String getParameter(String key, String ifNull) {
        String got = parameters.get(key);
        return (got != null) ? got : ifNull;
    }

    @Override
    public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(action)
                                    .append(domain)
                                    .append(featureType)
                                    .append(segment)
                                    .append(parameters).toHashCode();
    }
}
