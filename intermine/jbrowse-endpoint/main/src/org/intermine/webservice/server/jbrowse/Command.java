package org.intermine.webservice.server.jbrowse;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.intermine.webservice.server.jbrowse.Commands.Action;

public class Command {

    private final Action action;
    private final String domain, featureType;
    private final Segment segment;
    private final Map<String, String> parameters;

    public Command(Action a, String domain, String fType, Segment s, Map<String, String> params) {
        this.action = a;
        this.domain = domain;
        this.featureType = fType;
        this.segment = s;
        this.parameters = new HashMap<String, String>(params);
    }
    public Action getAction() {
        return action;
    }

    public String getDomain() {
        return domain;
    }

    public Segment getSegment() {
        return segment;
    }

    public String getType(String ifNull) {
        if (featureType == null) return ifNull;
        return featureType;
    }

    public String getParameter(String key) {
        return parameters.get(key);
    }

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
