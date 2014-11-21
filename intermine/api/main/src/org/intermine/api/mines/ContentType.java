package org.intermine.api.mines;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * An enumeration of the content-types we know about.
 * @author Alex Kalderimis
 *
 */
public enum ContentType {
    XML("application/xml"), PlainText("text/plain"), JSON("application/json");

    private final String mimeType;

    /**
     * A content type is associated with a mime-type.
     * @param mimeType Which is passed to the constructor.
     */
    ContentType(String mimeType) {
        this.mimeType = mimeType;
    }

    /** @return and retrieved from the accessor **/
    public String getMimeType() {
       return mimeType;
    }
}
