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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintLoop;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.SwitchOffAbility;
import org.intermine.template.TemplateQuery;

/**
 * Methods to generate summary information for a PathQuery for use in display of QueryBuilder
 * summary section.
 * @author Richard Smith
 *
 */
public final class QueryBuilderSummaryHelper
{
    private QueryBuilderSummaryHelper() {
    }

    /**
     * Create summary information of the paths currently in a query and their constraints for
     * display in the QueryBuilder summary section.  The list of SummaryPath objects collect
     * information for simple display in the JSP.
     * @param query the query to create summary information from
     * @return a list if summary information about paths on the query
     * @throws PathException if the PathQuery is invalid
     */
    public static List<SummaryPath> getDisplaySummary(PathQuery query)
        throws PathException {
        Set<SummaryPath> summaryPaths = new TreeSet<SummaryPath>();

        Set<String> constrainedPaths = new HashSet<String>();
        for (PathConstraint con : query.getConstraints().keySet()) {
            if (con instanceof PathConstraintSubclass) {
                // Do nothing
            } else if (con instanceof PathConstraintLoop) {
                // Put both paths in
                constrainedPaths.add(con.getPath());
                constrainedPaths.add(((PathConstraintLoop) con).getLoopPath());
            } else {
                constrainedPaths.add(con.getPath());
            }
        }

        List<String> lockedPaths = findLockedPaths(query);
        List<String> forcedInnerJoins = findForcedInnerJoins(query);

        Set<String> paths = new HashSet<String>();
        paths.addAll(constrainedPaths);
        paths.addAll(query.getView());
        paths.addAll(query.getOuterJoinGroups().keySet());

        for (String stringPath : paths) {
            Path path = query.makePath(stringPath);

            boolean isLocked = lockedPaths.contains(path.toStringNoConstraints());
            boolean isForcedInner = forcedInnerJoins.contains(path.toStringNoConstraints());
            SummaryPath summaryPath = new SummaryPath(path, isLocked, isForcedInner);

            for (PathConstraint con : query.getConstraintsForPath(path.toStringNoConstraints())) {
                boolean editable = false;
                String description = null;
                String switchable = SwitchOffAbility.LOCKED.toString().toLowerCase();
                if (query instanceof TemplateQuery) {
                    TemplateQuery template = (TemplateQuery) query;
                    editable = template.getEditableConstraints().contains(con);
                    description = template.getConstraintDescriptions().get(con);
                    SwitchOffAbility constraintSwitchOffAbility =
                        template.getConstraintSwitchOffAbility().get(con);
                    if (SwitchOffAbility.ON.equals(constraintSwitchOffAbility)) {
                        switchable = SwitchOffAbility.ON.toString().toLowerCase();
                    } else if (SwitchOffAbility.OFF.equals(constraintSwitchOffAbility)) {
                        switchable = SwitchOffAbility.OFF.toString().toLowerCase();
                    }
                }
                // subclass constraints aren't display
                if (!(con instanceof PathConstraintSubclass)) {
                    String code = query.getConstraints().get(con);
                    summaryPath.addSummaryConstraint(new SummaryConstraint(con, code, editable,
                            description, switchable));
                } else {
                    summaryPath.setSubclass(((PathConstraintSubclass) con).getType());
                }
            }
            summaryPaths.add(summaryPath);
        }
        return new ArrayList<SummaryPath>(summaryPaths);
    }

    /**
     * Get a list of paths that should not be removed from the query by the user. This is usually
     * because they are involved in a loop query constraint.
     *
     * @param pathQuery the PathQuery to process
     * @return list of paths (as Strings) that cannot be removed by the user
     * @throws PathException if something goes wrong
     */
    protected static List<String> findLockedPaths(PathQuery pathQuery) throws PathException {
        List<String> paths = new ArrayList<String>();
        for (PathConstraint con : pathQuery.getConstraints().keySet()) {
            if (con instanceof PathConstraintLoop) {
                List<String> constraintPaths = findParentPaths(con.getPath(), pathQuery);
                List<String> loopPaths = findParentPaths(((PathConstraintLoop) con).getLoopPath(),
                        pathQuery);
                loopPaths.removeAll(constraintPaths);
                paths.addAll(loopPaths);
            }
        }
        return paths;
    }

    /**
     * Returns a List of paths, being the given path and all its parents.
     *
     * @param pathString a path String
     * @param pathQuery a PathQuery object to use to create a Path object
     * @return a List of path Strings
     * @throws PathException if something goes wrong
     */
    protected static List<String> findParentPaths(String pathString, PathQuery pathQuery)
        throws PathException {
        Path path = pathQuery.makePath(pathString);
        List<String> retval = new ArrayList<String>();
        retval.add(path.getNoConstraintsString());
        while (!path.isRootPath()) {
            path = path.getPrefix();
            retval.add(path.getNoConstraintsString());
        }
        return retval;
    }

    /**
     * Get a list of paths that should be forced to inner join. This is usually because they are
     * involved in a loop query constraint.
     *
     * @param pathQuery the PathQuery containing the paths
     * @return a list of paths (as Strings) that must be inner joins
     * @throws PathException if something goes wrong
     * @throws IllegalArgumentException if a path that should be an inner join is not
     */
    protected static List<String> findForcedInnerJoins(PathQuery pathQuery) throws PathException {
        List<String> paths = new ArrayList<String>();
        for (PathConstraint con : pathQuery.getConstraints().keySet()) {
            if (con instanceof PathConstraintLoop) {
                List<String> constraintPaths = findParentPaths(con.getPath(), pathQuery);
                List<String> loopPaths = findParentPaths(((PathConstraintLoop) con).getLoopPath(),
                        pathQuery);
                // Having found the paths for both sides of the loop constraint, we want to find the
                // paths that are in only one of these lists.
                List<String> intersection = new ArrayList<String>(loopPaths);
                intersection.retainAll(constraintPaths);
                loopPaths.addAll(constraintPaths);
                loopPaths.removeAll(intersection);
                // loopPaths now contains the exclusive-or of the two lists, which is the list of
                // paths that cannot be made into outer joins
                paths.addAll(loopPaths);
            }
        }
        return paths;
    }
}
