package org.intermine.web.logic.querybuilder;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

import org.intermine.pathquery.Path;

/**
 * Representation of a query path with additional information for display in the QueryBulder summary
 * section.  Holds information about constraints applied to the path, if the path is locked and if
 * the path is forced to be an inner join.
 *
 * @author Richard Smith
 *
 */
public class SummaryPath extends DisplayPath implements Comparable<SummaryPath>
{
    private List<SummaryConstraint> constraints = new ArrayList<SummaryConstraint>();
    private boolean isLocked = false;
    private boolean isForcedInnerJoin = false;
    private String subclass = null;

    /**
     * Construct a summary path with details of whether the path is locked and/or forced to be an
     * inner join.
     * @param path a path from the query to be displayed in the summary
     * @param isLocked true if the path cannot be removed from the query
     * @param isForcedInner true if this path must remain an inner join
     */
    public SummaryPath(Path path, boolean isLocked, boolean isForcedInner) {
        super(path);
        this.isLocked = isLocked;
        this.isForcedInnerJoin = isForcedInner;
    }

    /**
     * Add a constraint to that applies to this path.
     * @param con a constraint that applies to this path
     */
    protected void addSummaryConstraint(SummaryConstraint con) {
        constraints.add(con);
    }

    /**
     * Return summary representations of constraints on this path or an empty list if none exist.
     * @return constraints applied to this path
     */
    public List<SummaryConstraint> getConstraints() {
        return constraints;
    }

    /**
     * Returns the name of the subclass on this path, if there is one.
     *
     * @return a String
     */
    public String getSubclass() {
        return subclass;
    }

    /**
     * Sets the subclass on this path.
     *
     * @param subclass the new subclass String
     */
    public void setSubclass(String subclass) {
        this.subclass = subclass;
    }

    /**
     * Return true if this path is locked because it is involved in a loop constraint.  Locked paths
     * cannot be removed from the query unless the loop constraint is removed.  This means the
     * remove path icon should not be displayed.
     * @return true if this path is locked
     */
    public boolean isLocked() {
        return isLocked;
    }

    /**
     * Return true if this path is forced to be an inner join because it is involved in a loop
     * constraint.  This means the edit join style icon should not be displayed.
     * @return true if this path is forced to be an inner join
     */
    public boolean isForcedInnerJoin() {
        return isForcedInnerJoin;
    }


    /**
     * {@inheritDoc}
     */
    public int compareTo(SummaryPath other) {
        // We want the attributes to appear first, followed by the references and collections. If we
        // change the last dot to a plus on attributes, then natural sorting will take care of that.
        String thisPath = replaceAttributeDots(this.path);
        String otherPath = replaceAttributeDots(other.path);
        return thisPath.compareToIgnoreCase(otherPath);
    }

    // replace final dot in path with '+' if path represents and attribute, used for path ordering
    private String replaceAttributeDots(Path p) {
        String strPath = p.toStringNoConstraints();
        if (p.endIsAttribute()) {
            int lastIndex = strPath.lastIndexOf('.');
            strPath = strPath.substring(0, lastIndex) + "+" + strPath.substring(lastIndex + 1);
        }
        return strPath;
    }
}
