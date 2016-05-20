package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.intermine.metadata.Model;
import org.intermine.metadata.ConstraintOp;
import org.intermine.pathquery.OrderElement;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathConstraintLoop;
import org.intermine.pathquery.PathConstraintNull;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;

/**
 * Class for updating a pathquery with a new model
 * @author Daniela BUtano
 *
 */
public class PathQueryUpdate
{
    protected PathQuery pathQuery;
    protected PathQuery newPathQuery;
    protected Model oldModel;
    protected boolean isUpdated = false;

    /**
     * Default constructor
     */
    public PathQueryUpdate() {
    }

    /**
     * Construct with pathquery and former model
     * @param pathQuery the epath query
     * @param oldModel the former model
     */
    public PathQueryUpdate(PathQuery pathQuery, Model oldModel) {
        this.pathQuery = pathQuery;
        this.oldModel = oldModel;
        this.newPathQuery = new PathQuery(pathQuery.getModel());
    }

    /**
     * Return a new pathquery updated to the new model
     * @return the new pathquery
     */
    public PathQuery getUpdatedPathQuery() {
        return newPathQuery;
    }

    /**
     * Return true if the pathquery has been updated
     * @return true if the pathquery has been updated, false otherwise
     */
    public boolean isUpdated() {
        return isUpdated;
    }

    /**
     * Update the path query
     * @param renamedClasses the renamed classes
     * @param renamedFields the renamed fields
     * @return the list of problems
     * @throws PathException if there is problem in some paths
     */
    public synchronized List<String> update(Map<String, String> renamedClasses,
        Map<String, String> renamedFields) throws PathException {
        // Update view paths
        updateView(renamedClasses, renamedFields);
        // Update constraints
        updateConstraints(renamedClasses, renamedFields);
        // Update outer join paths
        updateOuterJoins(renamedClasses, renamedFields);
        // Update description paths
        updateDescriptionsPath(renamedClasses, renamedFields);
        // Update order by paths
        updateOrderByPath(renamedClasses, renamedFields);

        List<String> problems = newPathQuery.verifyQuery();
        return problems;
    }

    private void updateView (Map<String, String> renamedClasses, Map<String, String> renamedFields)
        throws PathException {
        for (String viewPath : pathQuery.getView()) {
            viewPath = getPathUpdated(viewPath, renamedClasses, renamedFields);
            newPathQuery.addView(viewPath);
        }
    }

    /**
     * Update the constraint with the renamed classes or/and fields
     * @param renamedClasses the renamed classes
     * @param renamedFields the renamed fields
     * @throws PathException if there is problem in some paths
     */
    protected void updateConstraints (Map<String, String> renamedClasses,
        Map<String, String> renamedFields) throws PathException {
        String path, newPath;
        for (PathConstraint pathConstraint : pathQuery.getConstraints().keySet()) {
            path = pathConstraint.getPath();
            newPath = getPathUpdated(path, renamedClasses, renamedFields);
            if (!newPath.equals(path)) {
                newPathQuery.addConstraint(createPathConstraint(pathConstraint, newPath));
            } else {
                newPathQuery.addConstraint(pathConstraint);
            }
        }
    }

    /**
     * Create a pathconstraint with the same operator and value of path contraint given in input
     * and with the path given in input
     * @param pathConstraint the source path contraint
     * @param newPath the new path
     * @return the path constraint created
     */
    protected PathConstraint createPathConstraint(PathConstraint pathConstraint, String newPath) {
        PathConstraint newPathConstraint = null;
        ConstraintOp op = pathConstraint.getOp();
        if (pathConstraint instanceof PathConstraintAttribute) {
            newPathConstraint = new PathConstraintAttribute(newPath, op,
                                ((PathConstraintAttribute) pathConstraint).getValue());
        } else if (pathConstraint instanceof PathConstraintBag) {
            newPathConstraint = new PathConstraintBag(newPath, op,
                                ((PathConstraintBag) pathConstraint).getBag());
        } else if (pathConstraint instanceof PathConstraintLookup) {
            newPathConstraint = new PathConstraintLookup(newPath,
                                ((PathConstraintLookup) pathConstraint).getValue(),
                                ((PathConstraintLookup) pathConstraint).getExtraValue());
        } else if (pathConstraint instanceof PathConstraintSubclass) {
            newPathConstraint = new PathConstraintSubclass(newPath,
                    ((PathConstraintSubclass) pathConstraint).getType());
        } else if (pathConstraint instanceof PathConstraintLoop) {
            newPathConstraint = new PathConstraintLoop(newPath, op,
                     ((PathConstraintLoop) pathConstraint).getLoopPath());
        } else if (pathConstraint instanceof PathConstraintNull) {
            newPathConstraint = new PathConstraintNull(newPath, op);
        }
        return newPathConstraint;
    }

    private void updateOuterJoins(Map<String, String> renamedClasses,
        Map<String, String> renamedFields) throws PathException {
        Map<String, OuterJoinStatus> outerJoinStatus = pathQuery.getOuterJoinStatus();
        String newJoinPath;
        for (String joinPath : outerJoinStatus.keySet()) {
            newJoinPath = getPathUpdated(joinPath, renamedClasses, renamedFields);
            newPathQuery.setOuterJoinStatus(newJoinPath, outerJoinStatus.get(joinPath));
        }
    }

    private void updateDescriptionsPath(Map<String, String> renamedClasses,
        Map<String, String> renamedFields) throws PathException {
        Map<String, String> descriptions = pathQuery.getDescriptions();
        String newDescriptionPath;
        for (String descPath : descriptions.keySet()) {
            newDescriptionPath = getPathUpdated(descPath, renamedClasses, renamedFields);
            newPathQuery.setDescription(newDescriptionPath, descriptions.get(descPath));
        }
    }

    private void updateOrderByPath(Map<String, String> renamedClasses,
        Map<String, String> renamedFields) throws PathException {
        String orderPath, newOrderPath;
        for (OrderElement orderElement : pathQuery.getOrderBy()) {
            orderPath = orderElement.getOrderPath();
            newOrderPath = getPathUpdated(orderPath, renamedClasses, renamedFields);
            if (!newOrderPath.equals(orderPath)) {
                newPathQuery.addOrderBy(new OrderElement(newOrderPath,
                                        orderElement.getDirection()));
            } else {
                newPathQuery.addOrderBy(orderElement);
            }
        }
    }

    /**
     * Update the path given in input with the renamed classes and fields
     * @param path the original path to update
     * @param renamedClasses the renamed classes
     * @param renamedFields the renamed fields
     * @return the updated path
     * @throws PathException if there is problem in some paths
     */
    protected String getPathUpdated(String path, Map<String, String> renamedClasses,
        Map<String, String> renamedFields) throws PathException {
        Path p = new Path(oldModel, path);
        String pathUpdated = path;
        for (String key : renamedFields.keySet()) {
            int dotIndex = key.indexOf(".");
            String cls = key.substring(0, dotIndex);
            String prevField = key.substring(dotIndex + 1);
            List<Integer> elementContainingField = p.getElementsContainingField(cls, prevField);
            if (!elementContainingField.isEmpty()) {
                for (int index : elementContainingField) {
                    p.getElements().set(index, renamedFields.get(key));
                }
                pathUpdated = p.toStringNoConstraints();
                isUpdated = true;
            }
        }
        for (Entry<String, String> entry : renamedClasses.entrySet()) {
            String cls = entry.getKey();
            String newClass = entry.getValue();
            if (p.startContainsClass(cls)) {
                pathUpdated = pathUpdated.replace(cls, newClass);
                isUpdated = true;
                break;
            }
        }
        return pathUpdated;
    }
}
