package org.intermine.webservice.server;

public final class Formats {

    private Formats() {
        // Don't
    }

    /** The format for when no value is given **/
    public static final int EMPTY = -1;

    /** The Unknown format **/
    public static final int UNKNOWN = -2;

    /** XML format constant **/
    public static final int XML = 0;

    /** TSV format constant **/
    public static final int TSV = 1;

    /** HTML format constant **/
    public static final int HTML = 2;

    /** CSV format constant **/
    public static final int CSV = 3;

    /** Count format constant **/
    public static final int COUNT = 4;

    /** Text format constant **/
    public static final int TEXT = 5;

    // FORMAT CONSTANTS BETWEEN 20-40 ARE RESERVED FOR JSON FORMATS!!
    // Each json format should have a corresponding json-p format
    // with a value of format + 1. 

    /** Start of JSON format range **/
    public static final int JSON_RANGE_START = 20;

    /** End of JSON format range **/
    public static final int JSON_RANGE_END = 40;

    /** JSONP format constant **/
    public static final int JSON = 20;

    /** JSONP format constant **/
    public static final int JSONP = 21;

    /** JSON Object format constant **/
    public static final int JSON_OBJ = 22;

    /** JSONP Object format constant **/
    public static final int JSONP_OBJ = 23;

    /** JSON Table format constant **/
    public static final int JSON_TABLE = 24;

    /** JSONP Table format constant **/
    public static final int JSONP_TABLE = 25;

    /** JSON Row format constant **/
    public static final int JSON_ROW = 26;

    /** JSONP Row format constant **/
    public static final int JSONP_ROW = 27;

    /** JSON count format constant **/
    public static final int JSON_COUNT = 28;

    /** JSONP count format constant **/
    public static final int JSONP_COUNT = 29;


}
