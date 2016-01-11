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
 * Simple class wrapping content type information. At this moment only
 * TEXT_TAB, TEXT_XML, TEXT_PLAIN are useful content types. InterMine decides
 * which output format use according to the parameters in request
 * and not according to the content type header.
 * @author Jakub Kulaviak
 * @author Alex Kalderimis
 **/
public final class ContentType
{

    private String type;

    private String subType;

    private String charSet;

    private static final String UTF8_CHARSET = "UTF-8";

    /**
     * A ContentType constant that describes the generic text/xml content type.
     */
    public static final ContentType TEXT_XML =
        new ContentType("text", "xml", UTF8_CHARSET);

    /**
     * Content type for multi-part form requests.
     */
    public static final ContentType MULTI_PART_FORM =
        new ContentType("multipart", "form-data", UTF8_CHARSET);

    /**
     * A ContentType constant that describes the generic text/plain content type.
     */
    public static final ContentType TEXT_TAB =
        new ContentType("text", "tab-separated-values", UTF8_CHARSET);

    /**
     * A ContentType constant that describes the generic text/plain content type
     * when requesting counts.
     */
    public static final ContentType TEXT_COUNT = new ContentType("text", "count", UTF8_CHARSET);

    /**
     * A ContentType constant that describes the generic text/plain content type.
     */
    public static final ContentType TEXT_PLAIN = new ContentType("text", "plain", UTF8_CHARSET);

    /**
     * A ContentType constant that describes the JSON content type.
     */
    public static final ContentType APPLICATION_JSON =
        new ContentType("application", "json", UTF8_CHARSET);

    /**
     * A Content type that describes the JSON objects content type.
     */
    public static final ContentType APPLICATION_JSON_OBJ =
        new ContentType("application", "jsonobjects", UTF8_CHARSET);

    /**
     * A Content type that describes the JSON rows content type.
     */
    public static final ContentType APPLICATION_JSON_ROW =
        new ContentType("application", "jsonrows", UTF8_CHARSET);

    private ContentType(String type, String subType, String charSet) {
        this.type = type;
        this.subType = subType;
        this.charSet = charSet;
    }

    @Override
    public String toString() {
        return type + "/" + subType + ";char-set:" + charSet;
    }

    /**
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets main content type.
     * @param type content type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return sub type
     */
    public String getSubType() {
        return subType;
    }

    /**
     * @param subType content type sub type
     */
    public void setSubType(String subType) {
        this.subType = subType;
    }

    /**
     * @return character set of this content type.
     */
    public String getCharSet() {
        return charSet;
    }

    /**
     * Sets character set of this content type.
     * @param charSet character set
     */
    public void setCharSet(String charSet) {
        this.charSet = charSet;
    }
}
