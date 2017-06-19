package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum Format {

    /** The format for when no value is given **/
    EMPTY(null),
    /** The format for when the user will accept whatever. **/
    DEFAULT("*/*"),
    /** The Unknown format **/
    UNKNOWN(null),
    /** The HTML format **/
    HTML("text/html"),
    /** Plain Text **/
    TEXT("text/plain"),
    /** XML **/
    XML("application/xml"),
    /** Comma-separated-values **/
    CSV("text/comma-separated-values"),
    /** Tab-separated-values **/
    TSV("text/tab-separated-values"),
    /** Vanilla-JSON **/
    JSON("application/json"),
    /** JSON Object format constant **/
    OBJECTS("application/json;format=objects"),
    /** JSON Table format constant **/
    TABLE("application/json;format=table"),
    /** JSON Row format constant **/
    ROWS("application/json;format=rows");

    /** The basic formats: HTML, TEXT, XML, JSON **/
    public static final Set<Format> BASIC_FORMATS = new HashSet<Format>(Arrays.asList(
        HTML, TEXT, XML, JSON
    ));

    /** The JSON formats: JSON, OBJECTS, TABLE, ROWS **/
    public static final Set<Format> JSON_FORMATS = new HashSet<Format>(Arrays.asList(
        JSON, OBJECTS, TABLE, ROWS
    ));

    /** Flat file formats: TSV, CSV **/
    public static final Set<Format> FLAT_FILES = new HashSet<Format>(Arrays.asList(
        TSV, CSV
    ));

    private final String contentType;

    private Format(String contentType) {
        this.contentType = contentType;
    }

    /** @return the content type **/
    public String getContentType() {
        return contentType;
    }

    @Override
    public String toString() {
        return super.toString() + ((contentType == null) ? "" : (" (" + contentType + ") "));
    }

}
