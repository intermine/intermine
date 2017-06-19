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

import java.util.ArrayList;
import java.util.List;


/**
 * Translates strings between codes used in ConstraintOp class and abbreviations or
 * full code names of operations.
 * @author Jakub Kulaviak
 **/
public abstract class CodeTranslator
{

    private static List<Operation> operations = new ArrayList<Operation>();

    static {
        operations.add(new Operation("=", "eq", "equals"));
        operations.add(new Operation("!=", "ne", "notequals"));
        operations.add(new Operation("<", "lt", "lessthan"));
        operations.add(new Operation("<=", "le", "lessthanequals"));
        operations.add(new Operation(">", "gt", "greaterthan"));
        operations.add(new Operation(">=", "ge", "greaterthanequals"));
    }

    private CodeTranslator() {
    }

    /**
     * Returns corresponding code of translated operation.
     * @param translated code, abbreviation or fullName of operation
     * @return translated code or original string if no translation found
     */
    public static String getCode(String translated) {
        Operation op = getOperation(translated);
        if (op != null) {
            return op.getCode();
        } else {
            return translated;
        }
    }

    /**
     * Returns corresponding abbreviation of translated operation.
     * @param translated code, abbreviation or fullName of operation
     * @return abbreviation or original string if no translation found
     */
    public static String getAbbreviation(String translated) {
        Operation op = getOperation(translated);
        if (op != null) {
            return op.getAbbreviation();
        } else {
            return translated;
        }
    }

    /**
     * Returns corresponding fullName of translated operation.
     * @param translated code, abbreviation or fullName of operation
     * @return translated code or original string if no translation found
     */
    public static String getFullName(String translated) {
        Operation op = getOperation(translated);
        if (op != null) {
            return op.getFullName();
        } else {
            return translated;
        }
    }

    private static Operation getOperation(String translated) {
        for (Operation op : operations) {
            if (op.isEquivalent(translated)) {
                return op;
            }
        }
        return null;
    }

    private static class Operation
    {

        private final String code;

        private final String abbrev;

        private final String fullName;

        public Operation(String code, String abbrev, String fullName) {
            this.code = code;
            this.abbrev = abbrev;
            this.fullName = fullName;
        }

        /**
         * @param other code, abbreviation or full name of other operation
         * @return true if other operation equals this operation
         */
        public boolean isEquivalent(String other) {
            if (code != null  && code.equalsIgnoreCase(other)) {
                return true;
            }
            if (abbrev != null && abbrev.equalsIgnoreCase(other)) {
                return true;
            }
            if (fullName != null && fullName.equalsIgnoreCase(other)) {
                return true;
            }
            return false;
        }

        public String getCode() {
            return code;
        }

        public String getAbbreviation() {
            return abbrev;
        }

        public String getFullName() {
            return fullName;
        }
    }

}


