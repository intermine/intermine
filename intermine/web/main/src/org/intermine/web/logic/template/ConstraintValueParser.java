package org.intermine.web.logic.template;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.intermine.pathquery.Constraint.ISO_DATE_FORMAT;

import java.util.Date;
import java.util.Locale;

import org.intermine.objectstore.query.ConstraintOp;

import org.intermine.util.TypeUtil;
import org.intermine.util.Util;

import java.text.ParseException;

/**
 * Parser for parsing constraint value.
 * @author Jakub Kulaviak
 **/
public class ConstraintValueParser
{

    /**
     * @param value parsed value
     * @param type Java type, it is type of returned object
     * @param constraintOp operation connected with this value
     * @param locale locale used for parsing date, floats ...
     * @return converted object
     * @throws ParseValueException if value can not be converted to required type
     */
    public Object parse(String value, Class type, ConstraintOp constraintOp,
            Locale locale) throws ParseValueException {
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
                throw new ParseValueException("Please supply a valid wildcard expression.");
            } else {
                String trimmedValue = value.trim();
                if (constraintOp == ConstraintOp.EQUALS
                    || constraintOp == ConstraintOp.NOT_EQUALS
                    || constraintOp == ConstraintOp.MATCHES
                    || constraintOp == ConstraintOp.DOES_NOT_MATCH
                    || constraintOp == ConstraintOp.CONTAINS) {
                    parsedValue = Util.wildcardUserToSql(trimmedValue);
                } else {
                    parsedValue = trimmedValue;
                }
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
}
