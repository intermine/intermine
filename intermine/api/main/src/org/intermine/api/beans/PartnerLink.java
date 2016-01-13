package org.intermine.api.beans;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * A bean that contains the information to define a matching set of objects.
 * @author Alex Kalderimis
 *
 */
public class PartnerLink implements Serializable
{

    private String domain;
    private Set<ObjectDetails> objects = new HashSet<ObjectDetails>();

    /**
     * @return The domain.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @param domain the new domain value.
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * @return The objects details.
     */
    public Set<ObjectDetails> getObjects() {
        return objects;
    }

    /**
     * @param identifiers The new object details
     */
    public void setObjects(Set<ObjectDetails> identifiers) {
        this.objects = new HashSet<ObjectDetails>(identifiers);
    }

    // -- Object contract

    @Override
    public String toString() {
        return String.format(
            "PartnerLink(domain = %s, objects = %s)", domain, objects);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(domain).append(objects).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (other.getClass() != getClass()) {
            return false;
        }
        PartnerLink rhs = (PartnerLink) other;
        return new EqualsBuilder().append(domain, rhs.domain)
                                  .append(objects, rhs.objects)
                                  .isEquals();
    }

}
