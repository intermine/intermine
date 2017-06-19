package org.intermine.web.logic.query;

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
 * Class to generate help messages appropriate to a given constraint for display in the QueryBuilder
 * and template form.
 * @author Richard Smith
 *
 */
public final class DisplayConstraintHelpMessages
{

    private DisplayConstraintHelpMessages() {
        // Hidden constructor.
    }

    /**
     * Generate a context sensitive help message for a constraint.  The message will explain all
     * available options - e.g. if a dropdown will explain how to enter multiple values, if a
     * string field how to use wildcards.
     * @param con the constraint to generate help for
     * @return a context sensitive help message
     */
    public static String getHelpMessage(DisplayConstraint con) {
        StringBuffer sb = new StringBuffer();

        if (con.isLookup()) {
            sb.append("Search multiple fields including: " + con.getKeyFields()
                    + " You can enter multiple values separated by comma, use * as a wildcard.");

            if (con.getExtraConstraintValues() != null) {
                sb.append(" You can choose to limit the " + con.getPath().getType()
                        + " to a particular " + con.getExtraConstraintClassName() + ".");
            }
            sb.append(getBagMessage(con));
        } else if (con.isNullSelected()) {
            sb.append("Select a value.");
        } else if (con.getBags() != null) {
            sb.append("Select a value.");
            sb.append(getBagMessage(con));
        } else if (con.getPossibleValues() != null && !con.getPossibleValues().isEmpty()) {
            sb.append("Choose a value from the dropdown.  To choose multiple values set the"
                    + " operation to ONE OF or NONE OF.");
            if (con.getPath().isAttribute() && "String".equals(con.getPath().getType())) {
                sb.append(" To type text set the operation to LIKE or NOT LIKE");
                sb.append(", you can use * as a wildcard");
            }
            sb.append(".");
            sb.append(getBagMessage(con));
        }
        if (sb.length() == 0) {
            return null;
        }
        return sb.toString();
    }

    private static String getBagMessage(DisplayConstraint con) {
        if (con.getBags() != null) {
            return " Or use the checkbox and constrain the " + con.getBagType()
                + " to be in a saved list.";
        }
        return "";
    }
}
