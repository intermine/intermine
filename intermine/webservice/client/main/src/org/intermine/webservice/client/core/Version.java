package org.intermine.webservice.client.core;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Simple class representing service version.
 * @author Jakub Kulaviak
 **/
public class Version
{
    private String version;

    /**
     * Constructor.
     * @param version version
     */
    public Version(String version) {
        this.version = version;
    }

    /**
     * @return version
     */
    String getVersion() {
        return version;
    }

    /**
     * @param version version
     */
    void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns string representation of Version that is send with
     * request.
     * @return a string representation of the version.
     */
    @Override
    public String toString() {
        return getVersion();
    }
}
