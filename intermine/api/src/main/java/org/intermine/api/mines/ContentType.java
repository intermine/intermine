package org.intermine.api.mines;

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
 * An enumeration of the content-types we know about.
 * @author Alex Kalderimis
 *
 */
public enum ContentType {
    /** The XML content type **/
    XML("application/xml", "xml"),
    /** The plain text content type **/
    PlainText("text/plain", "text"),
    /** The JSON content type **/
    JSON("application/json", "json");

    private final String mimeType, format;

    /**
     * A content type is associated with a mime-type.
     * @param mimeType Which is passed to the constructor.
     * @param format The value which the 'format' query string parameter should have.
     */
    ContentType(String mimeType, String format) {
        this.mimeType = mimeType;
        this.format = format;
    }

    /** @return The mimetype associated with this content type **/
    public String getMimeType() {
        return mimeType;
    }

    /** @return The format parameter value associated with this content type **/
    public String getFormat() {
        return format;
    }
}
