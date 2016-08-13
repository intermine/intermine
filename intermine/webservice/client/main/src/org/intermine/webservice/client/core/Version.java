package org.intermine.webservice.client.core;

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
 * Simple class representing service version.
 * @author Jakub Kulaviak
 **/
public class Version
{
    private String version;

    /**
     * Constructor.
     * @param major major release version - increment for backwards incompatible changes.
     * @param minor minor release version - increment for new features
     * @param point point release version - increment for every change.
     */
    public Version(int major, int minor, int point) {
        this.version = String.format("%d.%02d.%02d", major, minor, point);
    }

    /**
     * @return version
     */
    String getVersion() {
        return version;
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
