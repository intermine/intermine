package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.intermine.metadata.TypeUtil;

/**
 * Parser for parsing constraint value.
 * @author Jakub Kulaviak
 **/
public final class ConstraintValueParser
{
    private static final Logger LOG = Logger.getLogger(ConstraintValueParser.class);

    private ConstraintValueParser() {
    }

    /**
     * A date format for ISO dates.
     */
    public static final DateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    static {
        Calendar gregorianCalendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
        ISO_DATE_FORMAT.setCalendar(gregorianCalendar);
    }

    /**
     * Parses a value.
     *
     * @param value parsed value
     * @param type Java type, it is type of returned object
     * @return converted object
     * @throws ParseValueException if value can not be converted to required type
     */
    public static Object parse(String value, Class<?> type)
        throws ParseValueException {
        Object parsedValue = null;

        if (value == null || value.length() == 0) {
            throw new ParseValueException("No input given, please supply a valid expression");
        }

        if (Date.class.equals(type)) {
            try {
                parsedValue = ISO_DATE_FORMAT.parse(value);
            } catch (ParseException e) {
                throw new ParseValueException(value + " is not a valid date - example: "
                        + ISO_DATE_FORMAT.format(new Date()));
            }
        } else if (String.class.equals(type)) {
            if (value.length() == 0) {
                // Is the expression valid? We need a non-zero length string at least
                throw new ParseValueException("Please supply a valid expression.");
            } else {
                parsedValue = value.trim();
            }
        } else {
            try {
                parsedValue = TypeUtil.stringToObject(type, value);
                if (parsedValue instanceof String) {
                    parsedValue = ((String) parsedValue).trim();
                }
            } catch (NumberFormatException e) {
                throw new ParseValueException(value + " is not a valid number.");
            }
        }
        return parsedValue;
    }

    /**
     * Converts a Date to ISO date format.
     *
     * @param value a date in String format "EEE MMM d HH:mm:ss Z yyyy"
     * @return the date converted to format "yyyy-MM-dd"
     */
    public static String format(String value) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            Date date = sdf.parse(value);
            return ISO_DATE_FORMAT.format(date);
        } catch (ParseException e) {
            LOG.log(Priority.ERROR, "The date" + value + " is not a valid date", e);
        }
        return "";
    }
}
