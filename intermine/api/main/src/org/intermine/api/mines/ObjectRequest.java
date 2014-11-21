package org.intermine.api.mines;

import org.apache.commons.lang.builder.HashCodeBuilder;

public class ObjectRequest {

    private final String domain, identifier;

    /**
     * Define a new object request.
     * @param domain The domain over which these identifiers have validity.
     * @param identifier The identifier.
     */
    public ObjectRequest(String domain, String identifier) {
        this.domain = domain;
        this.identifier = identifier;
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @return the domain
     */
    public String getDomain() {
        return domain;
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder();
        hcb.append(domain).append(identifier);
        return hcb.toHashCode();
    }

    @Override
    public String toString() {
        return String.format("ObjectRequest(domain = %s, identifier = %s)", domain, identifier);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof ObjectRequest)) {
            return false;
        }
        ObjectRequest oor = (ObjectRequest) other;
        if (identifier == null && oor.identifier != null) {
            return false;
        }
        if (identifier != null && !identifier.equals(oor.identifier)) {
            return false;
        }
        if (domain == null && oor.domain != null) {
            return false;
        }
        if (domain != null && !domain.equals(oor.domain)) {
            return false;
        }
        return true;
    }
}
