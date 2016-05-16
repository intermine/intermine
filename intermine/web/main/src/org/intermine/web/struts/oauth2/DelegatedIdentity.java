package org.intermine.web.struts.oauth2;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * A simple struct class for containing information about a delegated identity.
 *
 * A delegated identity is defined by a provider's name, a unique id and possibly
 * an email and name.
 *
 * @author Alex Kalderimis
 *
 */
public final class DelegatedIdentity
{

    private final String provider, id, email, name;

    /**
     * Construct a new delegated identity.
     *
     * @param provider The identity of the provider.
     * @param id The id of the user - an opaque string.
     * @param email An optional email.
     * @param name An optional name.
     */
    public DelegatedIdentity(String provider, String id, String email, String name) {
        if (provider == null || id == null) {
            throw new NullPointerException("provider and id must have values");
        }
        this.provider = provider;
        this.id = id;
        this.email = email;
        this.name = name;
    }

    /** @return the name of this identity. **/
    public String getName() {
        return name;
    }

    /** @return the email of this identity. **/
    public String getEmail() {
        return email;
    }

    /** @return the id of this identity. **/
    public String getId() {
        return id;
    }

    /** @return the provider of this identity. **/
    public String getProvider() {
        return provider;
    }

    @Override
    public String toString() {
        return String.format(
                "<%s-identity id=%s email=%s name=%s>",
                provider.toLowerCase(), id, email, name);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + provider.hashCode();
        result = prime * result + id.hashCode();
        result = prime * result + ((email == null) ? 0 : email.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        DelegatedIdentity other = (DelegatedIdentity) obj;
        if (!provider.equals(other.provider)) {
            return false;
        }
        if (!id.equals(other.id)) {
            return false;
        }
        if (email == null) {
            if (other.email != null) {
                return false;
            }
        } else if (!email.equals(other.email)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

}
