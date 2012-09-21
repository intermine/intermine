package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.StringEscapeUtils;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

/**
 * Class to represent a path-based query.
 *
 * @author Matthew Wakeling
 */
public class PathQuery implements Cloneable
{
    /** A Pattern that finds spaces in a String. */
    protected static final Pattern SPACE_SPLITTER = Pattern.compile(" ", Pattern.LITERAL);
    /** Version number for the userprofile and PathQuery XML format. */
    public static final int USERPROFILE_VERSION = 2;

    /** The lowest code value a constraint may be assigned. **/
    public static final char MIN_CODE = 'A';

    /** The highest code value a constraint may be assigned. **/
    public static final char MAX_CODE = 'Z';

    /** The maximum number of coded constraints a PathQuery may hold. **/
    public static final int MAX_CONSTRAINTS = MAX_CODE - MIN_CODE;

    private final Model model;
    private List<String> view = new ArrayList<String>();
    private List<OrderElement> orderBy = new ArrayList<OrderElement>();
    private Map<PathConstraint, String> constraints = new LinkedHashMap<PathConstraint, String>();
    private LogicExpression logic = null;
    private Map<String, OuterJoinStatus> outerJoinStatus
        = new LinkedHashMap<String, OuterJoinStatus>();
    private Map<String, String> descriptions = new LinkedHashMap<String, String>();
    private String description = null;

    /** Query title **/
    private String title = null;

    // Verification variables:
    private boolean isVerified = false;
    /** The root path of this query */
    private String rootClass = null;
    /** A Map from path to class name for all PathConstraintSubclass objects in the query. */
    private Map<String, String> subclasses = null;
    /** A Map from path to outer join group for all main paths in the query. */
    private Map<String, String> outerJoinGroups = null;
    /** A Map from outer join group to the set of constraint codes in that group. */
    private Map<String, Set<String>> constraintGroups = null;
    /** A Set of Strings describing all the loop constraints in the query, in order to check for
     * uniqueness */
    private Set<String> existingLoops = null;
    /** A boolean that determines if the constraints are broken enough to not bother validating the
     * constraint logic */
    private boolean doNotVerifyLogic = false;

    private static final String NO_VIEW_ERROR = "No columns selected for output";

    // See http://intrac.flymine.org/wiki/PathQueryRefactor

    /**
     * Constructor. Takes a Model object, to enable verification later.
     *
     * @param model a Model object
     */
    public PathQuery(Model model) {
        this.model = model;
    }

    /**
     * Constructor. Takes an existing PathQuery object, and copies all the data. Similar to the
     * clone method.
     *
     * @param o a PathQuery to copy
     */
    public PathQuery(PathQuery o) {
        model = o.model;
        view = new ArrayList<String>(o.view);
        orderBy = new ArrayList<OrderElement>(o.orderBy);
        constraints = new LinkedHashMap<PathConstraint, String>(o.constraints);
        if (o.logic != null) {
            logic = new LogicExpression(o.logic.toString());
        }
        outerJoinStatus = new LinkedHashMap<String, OuterJoinStatus>(o.outerJoinStatus);
        descriptions = new LinkedHashMap<String, String>(o.descriptions);
        description = o.description;
    }

    /**
     * Returns the Model object stored in this object.
     *
     * @return a Model
     */
    public Model getModel() {
        return model;
    }

    // ------------- View control methods ---------------

    /**
     * Add a single element to the view list. The element should be a normal path expression, with
     * dots separating the parts. Do not use colons to represent outer joins, and do not use
     * square brackets to represent subclass constraints. The path will not be verified until the
     * verifyQuery() method is called, but will be merely checked for format.
     *
     * @param viewPath the new path String to add to the view list
     * @throws NullPointerException if viewPath is null
     * @throws IllegalArgumentException if the viewPath contains colons or square brackets, or is
     * otherwise in a bad format
     */
    public synchronized void addView(String viewPath) {
        deVerify();
        checkPathFormat(viewPath);
        view.add(viewPath);
    }

    /**
     * Removes a single element from the view list. The element should be a normal path expression,
     * with dots separating the parts. Do not use colons to represent outer joins, and do not use
     * square brackets to represent subclass constraints. If there are multiple copies of the path
     * on the view list (which is an invalid query), then this method will remove all of them.
     *
     * @param viewPath the path String to remove from the view list
     * @throws NullPointerException if the viewPath is null
     * @throws NoSuchElementException if the viewPath is not already on the view list
     */
    public synchronized void removeView(String viewPath) {
        deVerify();
        checkPathFormat(viewPath);
        if (!view.contains(viewPath)) {
            throw new NoSuchElementException("Path \"" + viewPath + "\" is not in the view list: \""
                    + view + "\" - cannot remove it");
        }
        view.removeAll(Collections.singleton(viewPath));
    }

    /**
     * Clears the entire view list.
     */
    public synchronized void clearView() {
        deVerify();
        view.clear();
    }

    /**
     * Adds a group of elements to the view list. The elements should be normal path expressions,
     * with dots separating the parts. Do not use colons to represent outer joins, and do not use
     * square brackets to represent subclass constraints. The paths will not be verified until the
     * verifyQuery() method is called, but will merely be checked for format. The paths will be
     * added in the order of the iterator of the collection. If there is an error with any of the
     * elements of the collection, then none of the elements will be added and the query will be
     * unchanged.
     *
     * @param viewPaths a Collection of String paths to add to the view list
     * @throws NullPointerException if viewPaths is null or contains a null element
     * @throws IllegalArgumentException if a view path contains colons or square brackets, or is
     * otherwise in a bad format
     */
    public synchronized void addViews(Collection<String> viewPaths) {
        deVerify();
        try {
            for (String viewPath : viewPaths) {
                checkPathFormat(viewPath);
            }
            for (String viewPath : viewPaths) {
                addView(viewPath);
            }
        } catch (NullPointerException e) {
            NullPointerException e2 = new NullPointerException("While adding list to view: "
                    + viewPaths);
            e2.initCause(e);
            throw e2;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("While adding list to view: " + viewPaths, e);
        }
    }

    /**
     * Adds a group of elements to the view list. The elements should be normal path expressions,
     * with dots separating the parts. Do not use colons to represent outer joins, and do not use
     * square brackets to represent subclass constraints. The paths will not be verified until the
     * verifyQuery() method is called, but will merely be checked for format. The paths will be
     * added in the order of the arguments (varargs or array). If there is an error with any of the
     * elements of the array/varargs, then none of the elements will be added and the query will be
     * unchanged.
     *
     * @param viewPaths String paths to add to the view list
     * @throws NullPointerException if viewPaths is null or contains a null element
     * @throws IllegalArgumentException if a view path contains colons or square brackets, or is
     * otherwise in a bad format
     */
    public synchronized void addViews(String ... viewPaths) {
        deVerify();
        try {
            for (String viewPath : viewPaths) {
                checkPathFormat(viewPath);
            }
            for (String viewPath : viewPaths) {
                addView(viewPath);
            }
        } catch (NullPointerException e) {
            NullPointerException e2 = new NullPointerException("While adding array to view: "
                    + viewPaths);
            e2.initCause(e);
            throw e2;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("While adding array to view: " + viewPaths, e);
        }
    }

    /**
     * Adds a group of elements to the view list, given a space-separated list. The elements should
     * be normal path expressions, with dots separating the parts. Do not use colons to represent
     * outer joins, and do not use square brackets to represent subclass constraints. The paths
     * will not be verified until the verifyQuery() method is called, but will merely be checked
     * for format. The paths will be added preserving the order in the argument. The paths should be
     * separated by spaces in the argument, but not commas. If there is an error with any of the
     * elements in the argument, then none of the elements will be added and the query will be
     * unchanged.
     *
     * @param viewPaths String paths to add to the view list
     * @throws NullPointerException if viewPaths is null or contains a null element
     * @throws IllegalArgumentException if a view path contains colons or square brackets, or is
     * otherwise in a bad format
     */
    public synchronized void addViewSpaceSeparated(String viewPaths) {
        deVerify();
        try {
            String[] viewPathArray = SPACE_SPLITTER.split(viewPaths.trim());
            for (String viewPath : viewPathArray) {
                if (!"".equals(viewPath)) {
                    checkPathFormat(viewPath);
                }
            }
            for (String viewPath : viewPathArray) {
                if (!"".equals(viewPath)) {
                    addView(viewPath);
                }
            }
        } catch (NullPointerException e) {
            NullPointerException e2 = new NullPointerException("While adding space-separated list "
                    + "to view: \"" + viewPaths + "\"");
            e2.initCause(e);
            throw e2;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("While adding space-separated list to view: \""
                    + viewPaths + "\"", e);
        }
    }

    /**
     * Returns the current view list. This is an unmodifiable copy of the view list as it is at the
     * point of execution of this method. Changes in this query are not reflected in the result of
     * this method. The paths listed are normal path expressions without colons or square brackets.
     * The paths may not have been verified.
     *
     * @return a List of String paths
     */
    public synchronized List<String> getView() {
        return Collections.unmodifiableList(new ArrayList<String>(view));
    }

    // ---------------- Order By List Control -----------------

    /**
     * Adds an element to the order by list of this query. The element should be a normal path
     * expression, with dots separating the parts. Do not use colons to represent outer joins, and
     * do not use square brackets to represent subclass constraints. The path will not be verified
     * until the verifyQuery() method is called, but will merely be checked for format.
     *
     * @param orderPath the path expression to add to the order by list
     * @param direction the sort order
     * @throws NullPointerException if orderPath or direction is null
     * @throws IllegalArgumentException if the orderPath contains colons or square brackets, or is
     * otherwise in a bad format
     */
    public synchronized void addOrderBy(String orderPath, OrderDirection direction) {
        deVerify();
        addOrderBy(new OrderElement(orderPath, direction));
    }

    /**
     * Removes an element from the order by list of this query. The element should be a normal path
     * expression, with dots separating the parts. Do not use colons to represent outer joins, and
     * do not use square brackets to represent subclass constraints. If there are multiple copies
     * of the path on the order by list (which is an invalid query), then this method will remove
     * all of them.
     *
     * @param orderPath the path String to remove from the order by list
     * @throws NullPointerException if the orderPath is null
     * @throws NoSuchElementException if the orderPath is not already in the order by list
     */
    public synchronized void removeOrderBy(String orderPath) {
        deVerify();
        checkPathFormat(orderPath);
        boolean found = false;
        int i = 0;
        while (i < orderBy.size()) {
            if (orderPath.equals(orderBy.get(i).getOrderPath())) {
                orderBy.remove(i);
                found = true;
            } else {
                i++;
            }
        }
        if (!found) {
            throw new NoSuchElementException("Path \"" + orderPath + "\" is not in the order by "
                    + "list: \"" + orderBy + "\" - cannot remove it");
        }
    }

    /**
     * Clears the entire order by list.
     */
    public synchronized void clearOrderBy() {
        deVerify();
        orderBy.clear();
    }

    /**
     * Adds an element to the order by list of this query. The OrderElement will have already
     * checked the path for format, and the path will not be verified until the verifyQuery()
     * method is called.
     *
     * @param orderElement an OrderElement to add to the order by list
     * @throws NullPointerException if orderElement is null
     */
    public synchronized void addOrderBy(OrderElement orderElement) {
        deVerify();
        if (orderElement == null) {
            throw new NullPointerException("Cannot add a null OrderElement to the order by list");
        }
        orderBy.add(orderElement);
    }

    /**
     * Adds a group of elements to the order by list of this query. The elements will have already
     * checked the paths for format, but the paths will not be verified until the verifyQuery()
     * method is called. If there is an error with any of the elements of the collection, then none
     * of the elements will be added and the query will be unchanged.
     *
     * @param orderElements a Collection of OrderElement objects to add to the view list
     * @throws NullPointerException if orderElements is null or contains a null element
     */
    public synchronized void addOrderBys(Collection<OrderElement> orderElements) {
        deVerify();
        if (orderElements == null) {
            throw new NullPointerException("Cannot add null collection of OrderElements to order by"
                    + " list");
        }
        for (OrderElement orderElement : orderElements) {
            if (orderElement == null) {
                throw new NullPointerException("Cannot add null OrderElement (from collection \""
                        + orderElements + "\") to the order by list");
            }
        }
        for (OrderElement orderElement : orderElements) {
            addOrderBy(orderElement);
        }
    }

    /**
     * Adds a group of elements to the order by list of this query. The elements will have already
     * checked the paths for format, but the paths will not be verified until the verifyQuery()
     * method is called. If there is an error with any of the elements in this array/varargs, then
     * none of the elements will be added and the query will be unchanged.
     *
     * @param orderElements an array/varargs of OrderElement objects to add to the view list
     * @throws NullPointerException if orderElements is null or contains a null element
     */
    public synchronized void addOrderBys(OrderElement ... orderElements) {
        deVerify();
        if (orderElements == null) {
            throw new NullPointerException("Cannot add null array of OrderElements to order by "
                    + "list");
        }
        for (OrderElement orderElement : orderElements) {
            if (orderElement == null) {
                throw new NullPointerException("Cannot add null OrderElement (from array \""
                        + orderElements + "\") to the order by list");
            }
        }
        for (OrderElement orderElement : orderElements) {
            addOrderBy(orderElement);
        }
    }

    /**
     * Adds a group of elements to the order by list, given a space-separated list. The elements
     * should be normal path expressions, with dots separating the parts. Do not use colons to
     * represent outer joins, and do not use square brackets to represent subclass constraints. The
     * paths will not be verified until the verifyQuery() method is called, but will merely be
     * checked for format. The paths will be added preserving the order in the argument. Each
     * element should be a path expression followed by a space and then either "asc" or "desc" to
     * describe the direction of sorting, and the elements should be separated by spaces. If there
     * is an error with any of the elements in the argument, then none of the elements will be
     * added and the query will be unchanged.
     *
     * @param orderString the order elements in space-separated string form
     * @throws NullPointerException if orderString is null
     * @throws IllegalArgumentException if a path expression contains colons or square brackets, or
     * is otherwise in a bad format, or if there is not an even number of space-separated elements,
     * or if any even-numbered element is not either "asc" or "desc".
     */
    public synchronized void addOrderBySpaceSeparated(String orderString) {
        deVerify();
        try {
            String[] orderPathArray = SPACE_SPLITTER.split(orderString.trim());
            if (orderPathArray.length % 2 != 0) {
                throw new IllegalArgumentException("Order String must contain alternating paths and"
                        + " directions, so must have an even number of space-separated elements.");
            }
            List<OrderElement> toAdd = new ArrayList<OrderElement>();
            for (int i = 0; i < orderPathArray.length - 1; i += 2) {
                if ("asc".equals(orderPathArray[i + 1].toLowerCase())) {
                    toAdd.add(new OrderElement(orderPathArray[i], OrderDirection.ASC));
                } else if ("desc".equals(orderPathArray[i + 1].toLowerCase())) {
                    toAdd.add(new OrderElement(orderPathArray[i], OrderDirection.DESC));
                } else {
                    throw new IllegalArgumentException("Order direction \"" + orderPathArray[i + 1]
                            + "\" must be either \"asc\" or \"desc\"");
                }
            }
            addOrderBys(toAdd);
        } catch (NullPointerException e) {
            NullPointerException e2 = new NullPointerException("While adding space-separated list "
                    + "to order by: \"" + orderString + "\"");
            e2.initCause(e);
            throw e2;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("While adding space-separated list to order by: \""
                    + orderString + "\"");
        }
    }

    /**
     * Returns the current order by list. This is an unmodifiable copy of the order by list as it is
     * at the point of execution of this method. Changes in this query are not reflected in the
     * result of this method. The returned value is a List containing OrderElement objects, which
     * contain a String path expression without colons or square brackets, and an OrderDirection.
     * The paths may not have been verified.
     *
     * @return a List of OrderElement objects
     */
    public synchronized List<OrderElement> getOrderBy() {
        return Collections.unmodifiableList(new ArrayList<OrderElement>(orderBy));
    }

    // ------------------ Constraint Control ------------------

    /**
     * Adds a PathConstraint to this query. The PathConstraint will be attached to the path in the
     * constraint, which will have already been checked for format (no colons or square brackets),
     * but will not be verified until the verifyQuery() method is called. This method returns a
     * String code which is a single character that can be used in the constraint logic to logically
     * combine constraints. The constraint will be added to the existing constraint logic with the
     * AND operator - for any other arrangement, set the logic after calling this method. If the
     * constraint is already present in the query, then this method will do nothing.
     *
     * @param constraint the PathConstraint to add to this query
     * @return a String constraint code for use in the constraint logic
     * @throws NullPointerException if the constraint is null
     */
    public synchronized String addConstraint(PathConstraint constraint) {
        deVerify();
        if (constraint == null) {
            throw new NullPointerException("Cannot add a null constraint to this query");
        }
        if (constraints.containsKey(constraint)) {
            return constraints.get(constraint);
        }
        if (constraint instanceof PathConstraintSubclass) {
            // Subclass constraints don't get a code
            constraints.put(constraint, null);
            return null;
        }
        Set<String> usedCodes = new HashSet<String>(constraints.values());
        char charCode = 'A';
        String code = "A";
        while (usedCodes.contains(code)) {
            charCode++;
            code = "" + charCode;
        }
        if (logic == null) {
            logic = new LogicExpression(code);
        } else {
            logic = new LogicExpression("(" + logic.toString() + ") AND " + code);
        }
        constraints.put(constraint, code);
        return code;
    }

    /**
     * Adds a PathConstraints to this query, associated with a given constraint code. The
     * PathConstraint will be attached to the path in the constraint, which will have already been
     * checked for format (no colons or square brackets), but will not be verified until the
     * verifyQuery() method is called. If the given code is already in use by a different
     * constraint, or if the constraint already has a different code, then an exception is thrown.
     * The new constraint will be added to the existing constraint logic with the AND operator -
     * for any other arrangement, set the logic after calling this method. If the constraint is
     * already present in the query with the same constraint code, then this method will do nothing.
     *
     * @param constraint the PathConstraint to add to this query
     * @param code the constraint code to associate with this constraint. This must be a
     *             string consisting of one of the following characters "A","B","C","D","E","F","G",
     *             "H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z".
     * @throws NullPointerException if the constraint or the code is null
     * @throws IllegalStateException if the constraint is already associated with a different code,
     * or the code is already associated with a different constraint
     * @throws IllegalArgumentException if the code is in an inappropriate format - that is, if it
     * is not a single uppercase character
     */
    public synchronized void addConstraint(PathConstraint constraint, String code) {
        deVerify();
        if (constraint == null) {
            throw new NullPointerException("Cannot add a null constraint to this query");
        }
        if (constraint instanceof PathConstraintSubclass) {
            throw new IllegalArgumentException("Cannot associate a code with a subclass constraint."
                    + " Use the addConstraint(PathConstraint) method instead");
        }
        if (code == null) {
            throw new NullPointerException("Cannot use a null code for a constraint in this query");
        }
        if ((code.length() != 1) || (code.charAt(0) > MAX_CODE) || (code.charAt(0) < MIN_CODE)) {
            throw new IllegalArgumentException("The constraint code must be a single plain latin "
                    + "uppercase character");
        }
        if (constraints.containsKey(constraint)) {
            if (code.equals(constraints.get(constraint))) {
                // Trying to add an identical constraint at the same code.
                return;
            } else {
                throw new IllegalStateException("Given constraint is already associated with code "
                        + constraints.get(constraint) + " - cannot associate with code " + code
                        + "for query " + this.toString());
            }
        }
        Set<String> usedCodes = new HashSet<String>(constraints.values());
        if (usedCodes.contains(code)) {
            throw new IllegalStateException("Given code " + code + " from constraint "
                    + constraint + " conflicts with an existing constraint for query "
                    + this.toString());
        }
        if (logic == null) {
            logic = new LogicExpression(code);
        } else {
            logic = new LogicExpression("(" + logic.toString() + ") AND " + code);
        }
        constraints.put(constraint, code);
    }

    /**
     * Removes a constraint from this query. The PathConstraint should be a constraint that already
     * exists in this query. The constraint will also be removed from the constraint logic.
     *
     * @param constraint the PathConstraint to remove from this query
     * @throws NullPointerException if the constraint is null
     * @throws NoSuchElementException if the constraint is not present in the query
     */
    public synchronized void removeConstraint(PathConstraint constraint) {
        deVerify();
        if (constraint == null) {
            throw new NullPointerException("Cannot remove null constraint from this query");
        }
        if (constraints.containsKey(constraint)) {
            String code = constraints.remove(constraint);
            if (code != null) {
                if (logic.getVariableNames().size() > 1) {
                    logic.removeVariable(code);
                } else {
                    logic = null;
                }
            }
        } else {
            throw new NoSuchElementException("Constraint to remove is not present in query");
        }
    }

    /**
     * Replaces a constraint in the query with a different, carrying over the constraint code, and
     * preserving the constraint logic. The new PathConstraint will be attached to the path in the
     * constraint, which will have already been checked for format (no colons or square brackets),
     * but will not be verified until the verifyQuery() method is called. This method preserves the
     * order of constraints - that is, the replacement will be swapped in where the old constraint
     * was.
     *
     * @param old the old PathConstraint object
     * @param replacement the new PathConstraint object to replace it
     * @throws NullPointerException if old or replacement are null
     * @throws NoSuchElementException if the old PathConstraint is not already in the query
     * @throws IllegalArgumentException if the code from the old constraint is not appropriate to
     * the replacement constraint
     * @throws IllegalStateException if the replacement is already in the query
     */
    public synchronized void replaceConstraint(PathConstraint old, PathConstraint replacement) {
        deVerify();
        if (old == null) {
            throw new NullPointerException("Cannot replace a null constraint");
        }
        if (replacement == null) {
            throw new NullPointerException("Cannot replace a constraint with null");
        }
        if (!constraints.containsKey(old)) {
            throw new NoSuchElementException("Old constraint is not in the query");
        }
        if (constraints.containsKey(replacement)) {
            throw new IllegalStateException("Replacement constraint is already in the query");
        }
        String code = constraints.get(old);
        // Read the next line very carefully!
        if ((replacement instanceof PathConstraintSubclass) != (code == null)) {
            throw new IllegalArgumentException("Cannot replace a "
                    + old.getClass().getSimpleName() + " with a "
                    + replacement.getClass().getSimpleName());
        }
        Map<PathConstraint, String> temp = new LinkedHashMap<PathConstraint, String>(constraints);
        constraints.clear();
        for (Map.Entry<PathConstraint, String> entry : temp.entrySet()) {
            if (old.equals(entry.getKey())) {
                constraints.put(replacement, code);
            } else {
                constraints.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Clears the entire set of constraints from this query, and resets the constraint logic.
     */
    public synchronized void clearConstraints() {
        deVerify();
        constraints.clear();
        logic = null;
    }

    /**
     * Adds a collection of constraints to this query. The PathConstraints will be attached to the
     * paths in the constraints, which will have already been checked for format (no colons or
     * square brackets), but will not be verified until the verifyQuery() method is called. The
     * constraints will all be given codes, and added to the constraint logic with the default AND
     * operator. To discover the codes, use the getConstraints() method. If there is an error with
     * any of the elements in the collection, then none of the elements will be added and the
     * query will be unchanged.
     *
     * @param constraintList the PathConstraint objects to add to this query
     * @throws NullPointerException if constraints is null, or if it contains a null element
     */
    public synchronized void addConstraints(Collection<PathConstraint> constraintList) {
        deVerify();
        if (constraintList == null) {
            throw new NullPointerException("Cannot add null collection of PathConstraints to this "
                    + "query");
        }
        for (PathConstraint constraint : constraintList) {
            if (constraint == null) {
                throw new NullPointerException("Cannot add null PathConstraint (from collection \""
                        + constraintList + "\" to this query");
            }
        }
        for (PathConstraint constraint : constraintList) {
            addConstraint(constraint);
        }
    }

    /**
     * Adds a group of constraints to this query. The PathConstraints will be attached to the paths
     * in the constraints, which will have already been checked for format (no colons or square
     * brackets), but will not be verified until the verifyQuery() method is called. The constraints
     * will all be given codes, and added to the constraint logic with the default AND operator. To
     * discover the codes, use the getConstraints() method. If there is an error with any of the
     * elements in the array/varargs, then none of the elements will be added and the query will be
     * unchanged.
     *
     * @param constraintList the PathConstraint objects to add to this query
     * @throws NullPointerException if constraints is null, or if it contains a null element
     */
    public synchronized void addConstraints(PathConstraint ... constraintList) {
        deVerify();
        if (constraintList == null) {
            throw new NullPointerException("Cannot add null array of PathConstraints to this "
                    + "query");
        }
        for (PathConstraint constraint : constraintList) {
            if (constraint == null) {
                throw new NullPointerException("Cannot add null PathConstraint (from array \""
                        + constraintList + "\" to this query");
            }
        }
        for (PathConstraint constraint : constraintList) {
            addConstraint(constraint);
        }
    }

    /**
     * Returns a Map of all the constraints in this query, from PathConstraint to the constraint
     * code used in the constraint logic. This returns an unmodifiable copy of the data in the
     * query at the moment this method is executed, so further changes to the query are not
     * reflected in the returned value.
     *
     * @return a Map from PathConstraint to String constraint code (a single character)
     */
    public synchronized Map<PathConstraint, String> getConstraints() {
        Map<PathConstraint, String> retval = new LinkedHashMap<PathConstraint, String>(constraints);
        return retval;
    }

    public synchronized Map<PathConstraint, String> getRelevantConstraints() {
        return getConstraints(); // Simple alias. All constraints are relevant.
    }

    /**
     * Returns the PathConstraint associated with a given code.
     *
     * @param code a single uppercase character
     * @return a PathConstraint object
     * @throws NullPointerException if code is null
     * @throws NoSuchElementException if there is no PathConstraint for that code
     */
    public synchronized PathConstraint getConstraintForCode(String code) {
        for (Map.Entry<PathConstraint, String> entry : constraints.entrySet()) {
            if (code.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        throw new NoSuchElementException("No constraint is associated with code " + code
                + ", valid codes are " + constraints.values());
    }

    /**
     * Returns a list of PathConstraints applied to a given path or an empty list.
     *
     * @param path the path to fetch constraints for
     * @return a List of PathConstraints or an empty list
     */
    public synchronized List<PathConstraint> getConstraintsForPath(String path) {
        List<PathConstraint> retval = new ArrayList<PathConstraint>();
        for (PathConstraint con : constraints.keySet()) {
            if (con.getPath().equals(path)) {
                retval.add(con);
            }
        }
        return Collections.unmodifiableList(retval);
    }

    /**
     * Return the constraint codes used in the query, some constraint types (subclasses) don't
     * get assigned a code, these are not included.  This method returns all of the codes that
     * should be involved in the logic expression of the query.
     * @return the constraint codes used in this query
     */
    public synchronized Set<String> getConstraintCodes() {
        Set<String> codes = new HashSet<String>();
        for (String code : constraints.values()) {
            if (code != null) {
                codes.add(code);
            }
        }
        return codes;
    }

    // ------------------- Constraint Logic Control -------------------

    /**
     * Returns the current constraint logic. The logic is returned in groups, according to the outer
     * join layout of the query. Two codes in separate groups can only be combined with an AND
     * operation and OR operation.
     *
     * @return the current constraint logic
     */
    public synchronized String getConstraintLogic() {
        return (logic == null ? "" : logic.toString());
    }

    /**
     * Sets the current constraint logic.
     *
     * @param logic the constraint logic
     */
    public synchronized void setConstraintLogic(String logic) {
        // TODO method does not work properly allowing (A and B) or C on Outer Joins
        deVerify();
        if (constraints.isEmpty()) {
            this.logic = null;
        } else {
            this.logic = new LogicExpression(logic);
            for (String code : constraints.values()) {
                if (!this.logic.getVariableNames().contains(code)) {
                    this.logic = new LogicExpression("(" + this.logic.toString() + ") and " + code);
                }
            }
            this.logic.removeAllVariablesExcept(constraints.values());
        }
    }

    // --------------------- Outer Joined-ness Control --------------------

    /**
     * Returns the outer join status of the last part of a given path in this query. The given path
     * expression should not contain any colons to represent outer joins, and should not contain
     * any square brackets to represent subclass constraints.
     *
     * @param path a String path to check
     * @return an OuterJoinStatus object, or null if no information is held
     * @throws NullPointerException if path is null
     * @throws IllegalArgumentException if the path String contains colons or square brackets, or is
     * otherwise in a bad format
     */
    public synchronized OuterJoinStatus getOuterJoinStatus(String path) {
        checkPathFormat(path);
        return outerJoinStatus.get(path);
    }

    /**
     * Sets the outer join status of the last part of a given path in this query. The given path
     * expression should not contain any colons to represent outer joins, and should not contain
     * any square brackets to represent subclass constraints. To remove outer join status from a
     * path, call this method with a null status.
     *
     * @param path a String path to set
     * @param status an OuterJoinStatus object
     * @throws NullPointerException if path is null
     * @throws IllegalArgumentException if the path String contains colons or square brackets, or is
     * otherwise in a bad format
     */
    public synchronized void setOuterJoinStatus(String path, OuterJoinStatus status) {
        deVerify();
        checkPathFormat(path);
        if (status == null) {
            outerJoinStatus.remove(path);
        } else {
            outerJoinStatus.put(path, status);
        }
    }

    /**
     * Returns an unmodifiable Map which is a copy of the current outer join status of this query at
     * the time of execution of this method. Further changes to this object will not be reflected in
     * the object that was returned from this method.
     *
     * @return a Map from String path to OuterJoinStatus
     */
    public synchronized Map<String, OuterJoinStatus> getOuterJoinStatus() {
        return Collections.unmodifiableMap(
                new LinkedHashMap<String, OuterJoinStatus>(outerJoinStatus));
    }

    /**
     * Clears all outer join status data from this query.
     */
    public synchronized void clearOuterJoinStatus() {
        deVerify();
        outerJoinStatus.clear();
    }

    /**
     * Returns a Map from path to TRUE for all paths that are outer joined. That is, if the path is
     * an outer join (not referring to its parents - use isCompletelyInner() for that), then it is
     * present in this map mapped onto the value TRUE.
     *
     * @return a Map from String to Boolean TRUE
     */
    public synchronized Map<String, Boolean> getOuterMap() {
        Map<String, Boolean> retval = new HashMap<String, Boolean>();
        for (Map.Entry<String, OuterJoinStatus> stat : outerJoinStatus.entrySet()) {
            if (OuterJoinStatus.OUTER.equals(stat.getValue())) {
                retval.put(stat.getKey(), Boolean.TRUE);
            }
        }
        return retval;
    }

    // -------------------- Path Description Control --------------------

    /**
     * Returns the description for a given path, or null if no description is registered. The given
     * path expression should not contain any colons to represent outer joins, and should not
     * contain any square brackets to represent subclass constraints.
     *
     * @param path a String path to check
     * @return a String description
     * @throws NullPointerException if path is null
     * @throws IllegalArgumentException if the path String contains colons or square brackets, or is
     * otherwise in a bad format
     */
    public synchronized String getDescription(String path) {
        checkPathFormat(path);
        return descriptions.get(path);
    }

    /**
     * Sets the description for a given path. The given path expression should not contain any
     * colons to represent outer joins, and should not contain any square brackets to represent
     * subclass constraints. To clear the description on a path, call this method with a null
     * description.
     *
     * @param path the String path to set
     * @param description a String description or null
     * @throws NullPointerException if path is null
     * @throws IllegalArgumentException if the path String contains colons or square brackets, or is
     * otherwise in a bad format
     */
    public synchronized void setDescription(String path, String description) {
        deVerify();
        checkPathFormat(path);
        if (description == null) {
            descriptions.remove(path);
        } else {
            descriptions.put(path, description);
        }
    }

    /**
     * Returns an unmodifiable Map which is a copy of the current set of path descriptions of this
     * query at the time of execution of this method. Further changes to this object will not
     * be reflected in the object that was returned from this method.
     *
     * @return a Map from String path to description
     */
    public synchronized Map<String, String> getDescriptions() {
        return Collections.unmodifiableMap(new LinkedHashMap<String, String>(descriptions));
    }

    /**
     * Removes all path descriptions from this query.
     */
    public synchronized void clearDescriptions() {
        deVerify();
        descriptions.clear();
    }

    /**
     * Returns the path description for the given path. The description is computed from the set
     * descriptions of parent classes.
     *
     * @param path a String path with no square brackets or colons
     * @return a String description
     * @throws NullPointerException is path is null
     * @throws IllegalArgumentException if the path String contains colons or square brackets, or is
     * otherwise in a bad format
     */
    public synchronized String getGeneratedPathDescription(String path) {
        checkPathFormat(path);
        String retval = descriptions.get(path);
        if (retval == null) {
            int lastDot = path.lastIndexOf('.');
            if (lastDot == -1) {
                return path;
            } else {
                return getGeneratedPathDescription(path.substring(0, lastDot)) + " > "
                    + path.substring(lastDot + 1);
            }
        } else {
            return retval;
        }
    }


    /**
     * Returns the paths descriptions for the view.
     * @param pq
     * @return A list of column names
     */
    public List<String> getColumnHeaders() {
        List<String> columnNames = new ArrayList<String>();
        for (String viewString : getView()) {
            columnNames.add(getGeneratedPathDescription(viewString));
        }
        return columnNames;
    }

    // -------------------- Query description control --------------------
    // The two attributes description and title are used for display
    // in various queries contexts.

    /**
     * Sets the description for this PathQuery.
     *
     * @param description the new description, or null for none
     */
    public synchronized void setDescription(String description) {
        deVerify();
        this.description = description;
    }

    /**
     * Gets the description for this PathQuery.
     *
     * @return description
     */
    public synchronized String getDescription() {
        return description;
    }

    /**
     * Gets the title for this query.
     * @return The title of the query
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the name of the query.
     * @param title the new title, or null for none.
     */
    public void setTitle(String title) {
        deVerify();
        this.title = title;
    }

    // -------------------- Removals --------------------

    /**
     * Removes everything under a given path from the query, such that if the query was valid
     * before, it will be valid after this method.
     *
     * @param path everything under this path will be removed from the query
     * @throws NullPointerException is path is null
     * @throws IllegalArgumentException if the path String contains colons or square brackets, or is
     * otherwise in a bad format
     */
    public synchronized void removeAllUnder(String path) {
        checkPathFormat(path);
        deVerify();
        for (String v : getView()) {
            if (isPathUnder(path, v)) {
                removeView(v);
            }
        }
        for (OrderElement order : getOrderBy()) {
            if (isPathUnder(path, order.getOrderPath())) {
                removeOrderBy(order.getOrderPath());
            }
        }
        for (PathConstraint con : getConstraints().keySet()) {
            if (isPathUnder(path, con.getPath())) {
                removeConstraint(con);
            }
        }
        for (String join : getOuterJoinStatus().keySet()) {
            if (isPathUnder(path, join)) {
                setOuterJoinStatus(join, null);
            }
        }
        for (String desc : getDescriptions().keySet()) {
            if (isPathUnder(desc, path) && !isAnyViewWithPathUnder(desc)) {
                setDescription(desc, null);
            }
        }
    }

    private static boolean isPathUnder(String parent, String child) {
        if (parent.equals(child)) {
            return true;
        }
        return child.startsWith(parent + ".");
    }

    private boolean isAnyViewWithPathUnder(String parent) {
        for (String v : getView()) {
            if (parent.equals(v) || v.startsWith(parent + ".")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes everything from this query that is irrelevant, and therefore making the query
     * invalid. If the query is invalid for other reasons, then this method will either throw an
     * exception or ignore that part of the query, depending on the error, however the query is
     * unlikely to be made valid.
     *
     * @throws PathException if the query is invalid for a reason other than irrelevance
     */
    public synchronized void removeAllIrrelevant() throws PathException {
        deVerify();
        List<String> problems = new ArrayList<String>();
        // Validate subclass constraints and build subclass constraint map
        buildSubclassMap(problems);
        rootClass = null;
        Set<String> validMainPaths = new LinkedHashSet<String>();
        // Validate view paths
        validateView(problems, validMainPaths);
        // Validate constraints
        validateConstraints(problems, validMainPaths);
        // Now the validMainPaths set contains all the main (ie class) paths that are permitted in
        // this query.
        if (!(problems.isEmpty() || Arrays.asList(NO_VIEW_ERROR).equals(problems))) {
            throw new PathException(problems.toString(), null);
        }
        for (OrderElement order : getOrderBy()) {
            Path path = new Path(model, order.getOrderPath(), subclasses);
            if (path.endIsAttribute()) {
                path = path.getPrefix();
            }
            if (!validMainPaths.contains(path.getNoConstraintsString())) {
                removeOrderBy(order.getOrderPath());
            }
        }
        for (String join : getOuterJoinStatus().keySet()) {
            Path path = new Path(model, join, subclasses);
            if (path.endIsAttribute()) {
                path = path.getPrefix();
            }
            if (!validMainPaths.contains(path.getNoConstraintsString())) {
                setOuterJoinStatus(join, null);
            }
        }
        for (String desc : getDescriptions().keySet()) {
            Path path = new Path(model, desc, subclasses);
            if (path.endIsAttribute()) {
                path = path.getPrefix();
            }
            if (!validMainPaths.contains(path.getNoConstraintsString())) {
                setDescription(desc, null);
            }
        }
    }

    /**
     * Fixes up the order by list and the constraint logic, given the arrangement of outer joins in
     * the query.
     *
     * @return a List of messages about the changes that this method has made to the query
     * @throws PathException if the query is invalid in any way other than that which this method
     * will fix.
     */
    public synchronized List<String> fixUpForJoinStyle() throws PathException {
        deVerify();
        List<String> problems = new ArrayList<String>();
        // Validate subclass constraints and build subclass constraint map
        buildSubclassMap(problems);
        rootClass = null;
        Set<String> validMainPaths = new LinkedHashSet<String>();
        // Validate view paths
        validateView(problems, validMainPaths);
        // Validate constraints
        validateConstraints(problems, validMainPaths);
        // Now the validMainPaths set contains all the main (ie class) paths that are permitted in
        // this query.
        validateOuterJoins(problems, validMainPaths);
        calculateConstraintGroups(problems);
        if (!problems.isEmpty()) {
            throw new PathException(problems.toString(), null);
        }
        List<String> messages = new ArrayList<String>();
        for (OrderElement order : getOrderBy()) {
            // We cannot rely on the query being valid, as we are trying to make it valid!
            String orderString = order.getOrderPath();
            boolean outer = false;
            while (orderString.contains(".")) {
                if (OuterJoinStatus.OUTER.equals(outerJoinStatus.get(orderString))) {
                    outer = true;
                    break;
                }
                orderString = orderString.substring(0, orderString.lastIndexOf('.'));
            }
            if (outer) {
                removeOrderBy(order.getOrderPath());
                messages.add("Removed path " + order.getOrderPath() + " from ORDER BY because of "
                        + "outer joins");
            }
        }
        if (logic != null) {
            List<Set<String>> groups = new ArrayList<Set<String>>(constraintGroups.values());
            try {
                logic.split(groups);
            } catch (IllegalArgumentException e) {
                // The logic is invalid - we need to straighten it up.
                String oldLogic = logic.toString();
                logic = logic.validateForGroups(groups);
                messages.add("Changed constraint logic from " + oldLogic + " to " + logic.toString()
                        + " because of outer joins");
            }
        }
        return messages;
    }

    /**
     * Removes a subclass from the query, and removes any parts of the query that relied on it.
     * Returns a list of messages related to the extra things that had to be removed.
     *
     * @param path the path of the subclass constraint to remove
     * @return a list of messages
     * @throws PathException if the query is already invalid
     * @throws NullPointerException is path is null
     * @throws IllegalArgumentException if the path String contains colons or square brackets, or is
     * otherwise in a bad format
     */
    public synchronized List<String> removeSubclassAndFixUp(String path) throws PathException {
        checkPathFormat(path);
        List<String> problems = verifyQuery();
        if (!problems.isEmpty()) {
            throw new PathException("Query does not verify: " + problems, null);
        }
        deVerify();
        List<String> messages = new ArrayList<String>();
        PathConstraint toRemove = null;
        for (PathConstraint con : getConstraints().keySet()) {
            if (con instanceof PathConstraintSubclass) {
                if (con.getPath().equals(path)) {
                    toRemove = con;
                    break;
                }
            }
        }
        if (toRemove == null) {
            return messages;
        }
        removeConstraint(toRemove);
        buildSubclassMap(problems);
        // Remove things from view
        for (String viewPath : getView()) {
            try {
                @SuppressWarnings("unused")
                Path viewPathObj = new Path(model, viewPath, subclasses);
            } catch (PathException e) {
                // This one is now invalid. Remove
                removeView(viewPath);
                messages.add("Removed path " + viewPath + " from view, because you removed the "
                        + "subclass constraint that it depended on.");
            }
        }
        for (PathConstraint con : getConstraints().keySet()) {
            try {
                @SuppressWarnings("unused")
                Path constraintPath = new Path(model, con.getPath(), subclasses);
                if (con instanceof PathConstraintLoop) {
                    try {
                        @SuppressWarnings("unused")
                        Path loopPath = new Path(model, ((PathConstraintLoop) con).getLoopPath(),
                                subclasses);
                    } catch (PathException e) {
                        removeConstraint(con);
                        messages.add("Removed constraint " + con + " because you removed the "
                                + "subclass constraint it depended on.");
                    }
                }
            } catch (PathException e) {
                removeConstraint(con);
                messages.add("Removed constraint " + con + " because you removed the "
                        + "subclass constraint it depended on.");
            }
        }
        for (OrderElement order : getOrderBy()) {
            try {
                @SuppressWarnings("unused")
                Path orderPath = new Path(model, order.getOrderPath(), subclasses);
            } catch (PathException e) {
                removeOrderBy(order.getOrderPath());
                messages.add("Removed path " + order.getOrderPath() + " from ORDER BY, because you "
                        + "removed the subclass constraint it depended on.");
            }
        }
        for (String join : getOuterJoinStatus().keySet()) {
            try {
                @SuppressWarnings("unused")
                Path joinPath = new Path(model, join, subclasses);
            } catch (PathException e) {
                setOuterJoinStatus(join, null);
            }
        }
        for (String desc : getDescriptions().keySet()) {
            try {
                @SuppressWarnings("unused")
                Path descPath = new Path(model, desc, subclasses);
            } catch (PathException e) {
                setDescription(desc, null);
                messages.add("Removed description on path " + desc + ", because you removed the "
                        + "subclass constraint it depended on.");
            }
        }
        removeAllIrrelevant();

        return messages;
    }

    // -------------------- Other assorted stuff --------------------

    /**
     * Returns a deep copy of this object. The resulting object may be modified without impacting
     * this object.
     *
     * @return a PathQuery
     */
    @Override public PathQuery clone() {
        try {
            PathQuery retval = (PathQuery) super.clone();
            retval.view = new ArrayList<String>(retval.view);
            retval.orderBy = new ArrayList<OrderElement>(retval.orderBy);
            retval.constraints = new LinkedHashMap<PathConstraint, String>(retval.constraints);
            if (retval.logic != null) {
                retval.logic = new LogicExpression(retval.logic.toString());
            }
            retval.outerJoinStatus = new LinkedHashMap<String, OuterJoinStatus>(retval
                    .outerJoinStatus);
            retval.descriptions = new LinkedHashMap<String, String>(retval.descriptions);
            return retval;
        } catch (CloneNotSupportedException e) {
            throw new Error("Should never happen", e);
        }
    }

    /**
     * Produces a Path object from the given path String, using subclass information from the query.
     * Note that this method does not verify the query, but merely attempts to extract as much sane
     * subclass information as possible to construct the Path.
     *
     * @param path the String path
     * @return a Path object
     * @throws PathException if something goes wrong, or if the path is in an invalid format
     */
    public synchronized Path makePath(String path) throws PathException {
        Map<String, String> lSubclasses = new HashMap<String, String>();
        for (PathConstraint subclass : constraints.keySet()) {
            if (subclass instanceof PathConstraintSubclass) {
                lSubclasses.put(subclass.getPath(), ((PathConstraintSubclass) subclass).getType());
            }
        }
        return new Path(model, path, lSubclasses);
    }

    public synchronized void deVerify() {
        isVerified = false;
    }

    /**
     * Returns true if the query verifies correctly.
     *
     * @return a boolean
     */
    public boolean isValid() {
        return verifyQuery().isEmpty();
    }

    /**
     * Verifies the contents of this query against the model, and for internal integrity. Returns
     * a list of String problems that would need to be rectified for this query to pass validation
     * and be executed. If the return value is an empty List, then the query is valid.
     * <BR>
     * This method validates a few important characteristics about the query:
     * <UL><LI>All subclass constraints must be subclasses of the class they would otherwise be</LI>
     *     <LI>All paths must validate against the model</LI>
     *     <LI>All paths need to extend from the same root class</LI>
     *     <LI>Paths in the order by list, the outer join status, and the descriptions, must all be
     * attached to classes already defined by the view list and the constraints. Otherwise, it would
     * be possible to change the number of rows by changing the order</LI>
     *     <LI>All elements of the view list and the order by list must be attributes, and all
     * paths for outer join status and subclass constraints must not be attributes</LI>
     *     <LI>Subclass constraints cannot be on the root class of the query</LI>
     *     <LI>Loop constraints cannot cross an outer join</LI>
     *     <LI>Check constraint values against their types in the model and specific
     * characteristics</LI>
     *     <LI>Check constraint logic for sanity and that it can be split into separate ANDed outer
     * join sections</LI>
     * </UL>
     *
     * @return a List of problems
     */
    public synchronized List<String> verifyQuery() {
        List<String> problems = new ArrayList<String>();
        if (isVerified) {
            // Query is already verified correctly. Return no problems
            return problems;
        }
        // Validate subclass constraints and build subclass constraint map
        buildSubclassMap(problems);
        rootClass = null;
        Set<String> validMainPaths = new LinkedHashSet<String>();
        // Validate view paths
        validateView(problems, validMainPaths);
        // Validate constraints
        validateConstraints(problems, validMainPaths);
        // Now the validMainPaths set contains all the main (ie class) paths that are permitted in
        // this query.

        // Validate subclass constraints against validMainPaths
        for (PathConstraint constraint : constraints.keySet()) {
            if (constraint instanceof PathConstraintSubclass) {
                try {
                    Path path = new Path(model, constraint.getPath(), subclasses);
                    if (path.endIsAttribute()) {
                        // Should have already caught this problem above. Ignore and suppress
                        continue;
                    }
                } catch (PathException e) {
                    // Should have already been caught above. Ignore, and suppress further checking
                    continue;
                }
                if (!validMainPaths.contains(constraint.getPath())) {
                    problems.add("Subclass constraint on path " + constraint.getPath()
                            + " is not relevant to the query");
                }
            }
        }

        // Validate outer join paths
        validateOuterJoins(problems, validMainPaths);

        // Validate description paths
        for (String descPath : descriptions.keySet()) {
            try {
                Path path = new Path(model, descPath, subclasses);
                if (path.endIsAttribute()) {
                    path = path.getPrefix();
                }
                if (!validMainPaths.contains(path.getNoConstraintsString())) {
                    problems.add("Description on path " + descPath
                            + " is not relevant to the query");
                    continue;
                }
            } catch (PathException e) {
                problems.add("Path " + descPath + " for description is not in the model");
                continue;
            }
        }

        // Validate order by paths
        for (OrderElement orderPath : orderBy) {
            try {
                Path path = new Path(model, orderPath.getOrderPath(), subclasses);
                if (!path.endIsAttribute()) {
                    problems.add("Path " + orderPath.getOrderPath() + " in order by list must be "
                            + "an attribute");
                    continue;
                }
                if (!validMainPaths.contains(path.getPrefix().toStringNoConstraints())) {
                    problems.add("Order by element for path " + orderPath.getOrderPath()
                            + " is not relevant to the query");
                    continue;
                }
                if (!rootClass.equals(outerJoinGroups.get(path.getPrefix()
                        .getNoConstraintsString()))) {
                    problems.add("Order by element " + orderPath
                            + " is not in the root outer join group");
                }
            } catch (PathException e) {
                problems.add("Path " + orderPath.getOrderPath()
                        + " in order by list is not in the model");
                continue;
            }
        }

        calculateConstraintGroups(problems);

        if (logic != null) {
            if (!doNotVerifyLogic) {
                try {
                    logic.split(new ArrayList<Set<String>>(constraintGroups.values()));
                } catch (IllegalArgumentException e) {
                    problems.add("Logic expression is not compatible with outer join status: "
                            + e.getMessage());
                }
            }
        }

        if (problems.isEmpty()) {
            isVerified = true;
        }
        return problems;
    }

    private void calculateConstraintGroups(List<String> problems) {
        doNotVerifyLogic = false;
        // Put all constraints into groups
        constraintGroups = new LinkedHashMap<String, Set<String>>();
        for (Map.Entry<PathConstraint, String> constraintEntry : constraints.entrySet()) {
            if (constraintEntry.getValue() != null) {
                try {
                    Path path = new Path(model, constraintEntry.getKey().getPath(), subclasses);
                    if (path.getStartClassDescriptor().getUnqualifiedName().equals(rootClass)) {
                        if (path.endIsAttribute()) {
                            path = path.getPrefix();
                        }
                        String groupPath = outerJoinGroups.get(path.getNoConstraintsString());
                        if (groupPath != null) {
                            Set<String> group = constraintGroups.get(groupPath);
                            if (group == null) {
                                group = new HashSet<String>();
                                constraintGroups.put(groupPath, group);
                            }
                            group.add(constraintEntry.getValue());
                        }
                    } else {
                        doNotVerifyLogic = true;
                    }
                } catch (PathException e) {
                    // If this happens, then we have already noted the problem. Ignore.
                    doNotVerifyLogic = true;
                }
            }
            if (constraintEntry.getKey() instanceof PathConstraintLoop) {
                PathConstraintLoop loop = (PathConstraintLoop) constraintEntry.getKey();
                String aGroup = outerJoinGroups.get(loop.getPath());
                String bGroup = outerJoinGroups.get(loop.getLoopPath());
                // If one of these is null, then we must have recorded a problem above. Ignore.
                if ((aGroup != null) && (bGroup != null)) {
                    if (!aGroup.equals(bGroup)) {
                        problems.add("Loop constraint " + loop + " crosses an outer join");
                        continue;
                    }
                }
            }
        }
        for (String group : new HashSet<String>(outerJoinGroups.values())) {
            if (!constraintGroups.containsKey(group)) {
                constraintGroups.put(group, new HashSet<String>());
            }
        }
    }

    private void validateOuterJoins(List<String> problems, Set<String> validMainPaths) {
        for (String joinPath : outerJoinStatus.keySet()) {
            try {
                Path path = new Path(model, joinPath, subclasses);
                if (path.endIsAttribute()) {
                    problems.add("Outer join status on path " + joinPath
                            + " must not be on an attribute");
                    continue;
                }
                if (path.isRootPath()) {
                    problems.add("Outer join status cannot be set on root path " + joinPath);
                    continue;
                }
            } catch (PathException e) {
                problems.add("Path " + joinPath + " for outer join status is not in the model");
                continue;
            }
            if (!validMainPaths.contains(joinPath)) {
                problems.add("Outer join status path " + joinPath + " is not relevant to the "
                        + "query");
            }
        }
        // Calculate outer join groups from the validMainPaths list of paths
        outerJoinGroups = new LinkedHashMap<String, String>();
        for (String validPath : validMainPaths) {
            try {
                Path path = new Path(model, validPath, subclasses);
                while (isInner(path)) {
                    path = path.getPrefix();
                }
                outerJoinGroups.put(validPath, path.getNoConstraintsString());
            } catch (PathException e) {
                // Should never happen, as we have already checked in validateOuterJoins
                throw new Error(e);
            }
        }
    }

    private void validateConstraints(List<String> problems,
            Set<String> validMainPaths) {
        existingLoops = new HashSet<String>();
        // Validate constraint paths
        for (PathConstraint constraint : constraints.keySet()) {
            try {
                Path path = new Path(model, constraint.getPath(), subclasses);
                if (rootClass == null) {
                    rootClass = path.getStartClassDescriptor().getUnqualifiedName();
                } else {
                    String newRootClass = path.getStartClassDescriptor().getUnqualifiedName();
                    if (!rootClass.equals(newRootClass)) {
                        problems.add("Multiple root classes in query: " + rootClass + " and "
                                + newRootClass);
                        continue;
                    }
                }
                if (path.endIsAttribute()) {
                    addValidPaths(validMainPaths, path.getPrefix());
                } else {
                    addValidPaths(validMainPaths, path);
                }
                if (constraint instanceof PathConstraintAttribute) {
                    if (!path.endIsAttribute()) {
                        problems.add("Constraint " + constraint + " must be on an attribute");
                        continue;
                    }
                    Class<?> valueType = path.getEndType();
                    try {
                        TypeUtil.stringToObject(valueType,
                                ((PathConstraintAttribute) constraint).getValue());
                    } catch (Exception e) {
                        problems.add("Value in constraint " + constraint + " is not in correct "
                                + "format for type of "
                                + DynamicUtil.getFriendlyName(valueType));
                        continue;
                    }
                } else if (constraint instanceof PathConstraintNull) {
                    if (path.isRootPath()) {
                        problems.add("Constraint " + constraint
                                + " cannot be applied to the root path");
                        continue;
                    }
                    // TODO - make IS NULL work on references and collections.
                    //if (constraint.getOp().equals(ConstraintOp.IS_NULL)) {
                    //    if (!path.endIsAttribute()) {
                    //        problems.add("Constraint " + constraint
                    //                + " is invalid - can only set IS NULL on an attribute");
                    //        continue;
                    //    }
                    //}
                } else if (constraint instanceof PathConstraintBag) {
                    // We do not check that the bag exists here. Call getBagNames() and check
                    // elsewhere.
                    if (path.endIsAttribute()) {
                        problems.add("Constraint " + constraint
                                + " must not be on an attribute");
                        continue;
                    }
                } else if (constraint instanceof PathConstraintIds) {
                    if (path.endIsAttribute()) {
                        problems.add("Constraint " + constraint
                                + " must not be on an attribute");
                        continue;
                    }
                } else if (constraint instanceof PathConstraintMultitype) {
                    if (path.endIsAttribute()) {
                        problems.add("Constraint " + constraint + " must be on a class or reference");
                        continue;
                    }
                    for (String typeName: ((PathConstraintMultitype) constraint).getValues()) {
                        ClassDescriptor cd = model.getClassDescriptorByName(typeName);
                        if (cd == null) {
                            problems.add(String.format("Type '%s' named in [%s] is not in the model",
                                    typeName, constraint));
                        } else if (!cd.getAllSuperDescriptors().contains(path.getEndClassDescriptor())) {
                            problems.add(String.format("%s is not a subtype of %s, as required by %s",
                                    typeName, path.getEndClassDescriptor(), constraint));
                        }
                    }
                } else if (constraint instanceof PathConstraintRange) {
                    // Cannot verify these constraints until we try and make the query in the MainHelper.
                } else if (constraint instanceof PathConstraintMultiValue) {
                    if (!path.endIsAttribute()) {
                        problems.add("Constraint " + constraint + " must be on an attribute");
                        continue;
                    }
                    Class<?> valueType = path.getEndType();
                    for (String value : ((PathConstraintMultiValue) constraint).getValues()) {
                        try {
                            TypeUtil.stringToObject(valueType, value);
                        } catch (Exception e) {
                            problems.add("Value (" + value + ") in list in constraint "
                                    + constraint + " is not in correct format for type of "
                                    + DynamicUtil.getFriendlyName(valueType));
                            continue;
                        }
                    }
                } else if (constraint instanceof PathConstraintLoop) {
                    if (path.endIsAttribute()) {
                        problems.add("Constraint " + constraint
                                + " must not be on an attribute");
                        continue;
                    }
                    String loopPathString = ((PathConstraintLoop) constraint).getLoopPath();
                    try {
                        Path loopPath = new Path(model, loopPathString, subclasses);
                        if (loopPath.endIsAttribute()) {
                            problems.add("Loop path in constraint " + constraint
                                    + " must not be an attribute");
                            continue;
                        }
                        String newRootClass = loopPath.getStartClassDescriptor()
                            .getUnqualifiedName();
                        if (!rootClass.equals(newRootClass)) {
                            problems.add("Multiple root classes in query: " + rootClass + " and "
                                    + newRootClass);
                            continue;
                        }
                        addValidPaths(validMainPaths, loopPath);
                        if (constraint.getPath().equals(loopPathString)) {
                            problems.add("Path " + constraint.getPath()
                                    + " may not be looped back on itself");
                            continue;
                        }
                        if (model.isGeneratedClassesAvailable()) {
                            Class<?> aClass = path.getEndType();
                            Class<?> bClass = loopPath.getEndType();
                            if (!(aClass.isAssignableFrom(bClass)
                                    || bClass.isAssignableFrom(aClass))) {
                                problems.add("Loop constraint " + constraint
                                        + " must loop between similar types");
                                continue;
                            }
                        }
                        String loop = ((PathConstraintLoop) constraint).getDescriptiveString();
                        if (existingLoops.contains(loop)) {
                            problems.add("Cannot have two loop constraints between paths "
                                    + constraint.getPath() + " and " + loopPathString);
                            continue;
                        }
                        existingLoops.add(loop);
                    } catch (PathException e) {
                        problems.add("Path " + loopPathString + " in loop constraint from "
                                + constraint.getPath() + " is not in the model");
                        continue;
                    }
                } else if (constraint instanceof PathConstraintLookup) {
                    if (path.endIsAttribute()) {
                        problems.add("Constraint " + constraint
                                + " must not be on an attribute");
                    }
                } else if (constraint instanceof PathConstraintSubclass) {
                    // Do nothing
                } else {
                    problems.add("Unrecognised constraint type "
                            + constraint.getClass().getName());
                    continue;
                }
            } catch (PathException e) {
                if (!(constraint instanceof PathConstraintSubclass)) {
                    problems.add("Path " + constraint.getPath()
                            + " in constraint is not in the model");
                }
            }
        }
    }

    private Set<String> validateView(List<String> problems, Set<String> validMainPaths) {
        if (view.isEmpty()) {
            problems.add(NO_VIEW_ERROR);
        } else {
            for (String viewPath : view) {
                try {
                    Path path = new Path(model, viewPath, subclasses);
                    if (!path.endIsAttribute()) {
                        problems.add("Path " + viewPath + " in view list must be an attribute");
                        continue;
                    }
                    if (rootClass == null) {
                        rootClass = path.getStartClassDescriptor().getUnqualifiedName();
                    } else {
                        String newRootClass = path.getStartClassDescriptor().getUnqualifiedName();
                        if (!rootClass.equals(newRootClass)) {
                            problems.add("Multiple root classes in query: " + rootClass + " and "
                                    + newRootClass);
                            continue;
                        }
                    }
                    addValidPaths(validMainPaths, path.getPrefix());
                } catch (PathException e) {
                    problems.add("Path " + viewPath + " in view list is not in the model");
                }
            }
        }
        return validMainPaths;
    }

    private void buildSubclassMap(List<String> problems) {
        List<PathConstraintSubclass> subclassConstraints = new ArrayList<PathConstraintSubclass>();
        for (PathConstraint constraint : constraints.keySet()) {
            if (constraint instanceof PathConstraintSubclass) {
                subclassConstraints.add((PathConstraintSubclass) constraint);
            }
        }
        PathConstraintSubclass[] subclassConstraintArray = subclassConstraints.toArray(new
                PathConstraintSubclass[0]);
        Arrays.sort(subclassConstraintArray, new Comparator<PathConstraintSubclass>() {
                @Override
                public int compare(PathConstraintSubclass o1, PathConstraintSubclass o2) {
                    return o1.getPath().length() - o2.getPath().length();
                }
            });
        // subclassConstraintArray should now be in order of increasing length of path string, so it
        // should be fine to just build the subclass constraints map
        subclasses = new LinkedHashMap<String, String>();
        for (PathConstraintSubclass subclass : subclassConstraintArray) {
            if (subclasses.containsKey(subclass.getPath())) {
                problems.add("Cannot have multiple subclass constraints on path "
                        + subclass.getPath());
                continue;
            }
            Path subclassPath = null;
            try {
                subclassPath = new Path(model, subclass.getPath(), subclasses);
            } catch (PathException e) {
                problems.add("Path " + subclass.getPath() + " (from subclass constraint) is not in"
                        + " the model");
                continue;
            }
            if (subclassPath.isRootPath()) {
                problems.add("Root node " + subclass.getPath()
                        + " may not have a subclass constraint");
                continue;
            }
            if (subclassPath.endIsAttribute()) {
                problems.add("Path " + subclass.getPath() + " (from subclass constraint) must not "
                        + "be an attribute");
                continue;
            }

            ClassDescriptor subclassDesc = model.getClassDescriptorByName(subclass.getType());
            if (model.isGeneratedClassesAvailable()) {
                Class<?> parentClassType = subclassPath.getEndClassDescriptor().getType();
                Class<?> subclassType = (subclassDesc == null ? null : subclassDesc.getType());
                if (subclassType == null) {
                    problems.add("Subclass " + subclass.getType()
                            + " (for path " + subclass.getPath()
                            + ") is not in the model");
                    continue;
                }
                if (!parentClassType.isAssignableFrom(subclassType)) {
                    problems.add("Subclass constraint on path " + subclass.getPath() + " (type "
                            + DynamicUtil.getFriendlyName(parentClassType)
                            + ") restricting to type "
                            + DynamicUtil.getFriendlyName(subclassType)
                            + " is not possible, as it is "
                            + "not a subclass");
                    continue;
                }
            }
            subclasses.put(subclass.getPath(), subclass.getType());
        }
    }

    /**
     * Returns the root path for this query, if the query verifies correctly.
     *
     * @return a String path which is the root class
     * @throws PathException if the query does not verify
     */
    public synchronized String getRootClass() throws PathException {
        List<String> problems = verifyQuery();
        // For the purposes of this method, we will permit empty views.
        if (problems.isEmpty() || Arrays.asList(NO_VIEW_ERROR).equals(problems)) {
            return rootClass;
        }
        throw new PathException("Query does not verify: " + problems, null);
    }

    /**
     * Returns the subclass Map for this query, if the query verifies correctly.
     *
     * @return a Map from path String to subclass name, for all PathConstraintSubclass objects
     * @throws PathException if the query does not verify
     */
    public synchronized Map<String, String> getSubclasses() throws PathException {
        List<String> problems = verifyQuery();
        if (problems.isEmpty() || Arrays.asList(NO_VIEW_ERROR).equals(problems)) {
            return Collections.unmodifiableMap(new LinkedHashMap<String, String>(subclasses));
        }
        throw new PathException("Query does not verify: " + problems, null);
    }

    /**
     * Returns true if the query has no features yet.
     * @return whether or not this query is empty.
     */
    public synchronized boolean isEmpty() {
        return view.isEmpty() && constraints.isEmpty();
    }

    /**
     * Returns all bag names used in constraints on this query.
     *
     * @return the bag names used in this query or an empty set
     */
    public synchronized Set<String> getBagNames() {
        Set<String> bagNames = new HashSet<String>();
        for (PathConstraint constraint : constraints.keySet()) {
            if (constraint instanceof PathConstraintBag) {
                bagNames.add(((PathConstraintBag) constraint).getBag());
            }
        }
        return bagNames;
    }

    /**
     * Returns the outer join groups map for this query, if the query verifies correctly. This is a
     * Map from all the class paths in the query to the outer join group, represented by the path of
     * the root of the group.
     *
     * @return a Map from path String to the outer join group it is in
     * @throws PathException if the query does not verify
     */
    public synchronized Map<String, String> getOuterJoinGroups() throws PathException {
        List<String> problems = verifyQuery();
        if (problems.isEmpty() || Arrays.asList(NO_VIEW_ERROR).equals(problems)) {
            return Collections.unmodifiableMap(new LinkedHashMap<String, String>(outerJoinGroups));
        }
        throw new PathException("Query does not verify: " + problems, null);
    }

    /**
     * Returns the set of loop constraint descriptive strings, for the purpose of checking for
     * uniqueness.
     *
     * @return a Set of Strings
     * @throws PathException if the query does not verify
     */
    public synchronized Set<String> getExistingLoops() throws PathException {
        List<String> problems = verifyQuery();
        if (problems.isEmpty() || Arrays.asList(NO_VIEW_ERROR).equals(problems)) {
            return Collections.unmodifiableSet(new HashSet<String>(existingLoops));
        }
        throw new PathException("Query does not verify: " + problems, null);
    }

    /**
     * Returns the outer join group that the given path is in.
     *
     * @param stringPath a pathString
     * @return a String representing the outer join group that the path is in
     * @throws NullPointerException if pathString is null
     * @throws PathException if the query is invalid or the path is invalid
     * @throws NoSuchElementException is the path is not in the query
     */
    public String getOuterJoinGroup(String stringPath) throws PathException {
        if (stringPath == null) {
            throw new NullPointerException("stringPath is null");
        }
        Map<String, String> groups = getOuterJoinGroups();
        Path path = makePath(stringPath);
        if (path.endIsAttribute()) {
            path = path.getPrefix();
        }
        if (!groups.containsKey(path.getNoConstraintsString())) {
            throw new NoSuchElementException("Path " + stringPath + " is not in the query");
        }
        return groups.get(path.getNoConstraintsString());
    }

    /**
     * Returns true if a path string is in the root outer join group of this query.
     *
     * @param stringPath a path String
     * @return true if the given path is in the root outer join group, false if it contains outer
     * joins
     * @throws NullPointerException if pathString is null
     * @throws PathException if the query is invalid or the path is invalid
     * @throws NoSuchElementException if the path is not in the query
     */
    public boolean isPathCompletelyInner(String stringPath) throws PathException {
        String root = getRootClass();
        return root.equals(getOuterJoinGroup(stringPath));
    }

    /**
     * Returns the set of paths that could feasibly be loop constrained onto the given path, given
     * the current outer join situation. A candidate path must be a class path, of the same type,
     * and in the same outer join group. It must also not be already looped onto this path.
     *
     * @param stringPath a path String
     * @return a Set of path strings that could be looped onto the given path
     * @throws NullPointerException if stringPath is null
     * @throws IllegalArgumentException if stringPath refers to an attribute
     * @throws PathException if the query is invalid or stringPath is invalid
     */
    public synchronized Set<String> getCandidateLoops(String stringPath) throws PathException {
        if (stringPath == null) {
            throw new NullPointerException("stringPath is null");
        }
        Path path = makePath(stringPath);
        if (path.endIsAttribute()) {
            throw new IllegalArgumentException("stringPath \"" + stringPath
                    + "\" is an attribute, not a class");
        }
        String lRootClass = getRootClass();
        String rootOfStringPath = path.getStartClassDescriptor().getUnqualifiedName();
        if ((lRootClass != null) && (!lRootClass.equals(rootOfStringPath))) {
            throw new NoSuchElementException("Path " + stringPath + " is not in the query");
        }
        if (lRootClass == null) {
            outerJoinGroups.put(rootOfStringPath, rootOfStringPath);
        }
        Map<String, String> groups = new HashMap<String, String>(getOuterJoinGroups());
        Path groupPath = path;
        Set<String> toAdd = new HashSet<String>();
        while (!(groups.containsKey(groupPath.getNoConstraintsString()))) {
            toAdd.add(groupPath.toStringNoConstraints());
            if (groupPath.isRootPath()) {
                break;
            }
            groupPath = groupPath.getPrefix();
        }
        String group = groups.get(groupPath.getNoConstraintsString());
        for (String toAddElement : toAdd) {
            groups.put(toAddElement, group);
        }
        Class<?> type = path.getEndType();
        Set<String> lExistingLoops = getExistingLoops();
        Set<String> retval = new HashSet<String>();
        for (Map.Entry<String, String> entry : groups.entrySet()) {
            if (!entry.getKey().equals(stringPath)) {
                Path entryPath = makePath(entry.getKey());
                if (type.isAssignableFrom(entryPath.getEndType())
                    || entryPath.getEndType().isAssignableFrom(type)) {
                    if (group != null && group.equals(entry.getValue())) {
                        String desc = stringPath.compareTo(entry.getKey()) > 0
                                ? entry.getKey() + " -- " + stringPath
                                : stringPath + " -- " + entry.getKey();
                        if (!lExistingLoops.contains(desc)) {
                            retval.add(entry.getKey());
                        }
                    }
                }
            }
        }
        return retval;
    }

    /**
     * Returns the outer join constraint codes groups map for this query, if the query verifies
     * correctly.
     *
     * @return a Map from outer join group to the Set of constraint codes in the group
     * @throws PathException if the query does not verify
     */
    public synchronized Map<String, Set<String>> getConstraintGroups() throws PathException {
        List<String> problems = verifyQuery();
        if (problems.isEmpty() || Arrays.asList(NO_VIEW_ERROR).equals(problems)) {
            return Collections.unmodifiableMap(new LinkedHashMap<String, Set<String>>(
                        constraintGroups));
        }
        throw new PathException("Query does not verify: " + problems, null);
    }

    /**
     * Returns a List of logic Strings according to the different outer join sections of the query.
     *
     * @return a List of String
     * @throws PathException if the query does not verify
     */
    public synchronized List<String> getGroupedConstraintLogic() throws PathException {
        if (logic == null) {
            return Collections.emptyList();
        }
        Map<String, Set<String>> groups = getConstraintGroups();
        List<LogicExpression> grouped = logic.split(new ArrayList<Set<String>>(groups.values()));
        List<String> retval = new ArrayList<String>();
        for (LogicExpression group : grouped) {
            if (group != null) {
                retval.add(group.toString());
            }
        }
        return retval;
    }

    /**
     * Returns the constraint logic for the given outer join group, if the query verifies correctly.
     *
     * @param group an outer join group
     * @return the constraint logic for the constraints in that outer join group
     * @throws PathException if the query does not verify
     * @throws IllegalArgumentException if the group is not present in this query
     */
    public synchronized LogicExpression getConstraintLogicForGroup(String group)
        throws PathException {
        List<String> problems = verifyQuery();
        if (problems.isEmpty()) {
            if (logic == null) {
                return null;
            } else {
                Set<String> codes = constraintGroups.get(group);
                if (codes == null) {
                    throw new IllegalArgumentException("Outer join group " + group
                            + " does not seem to be in this query. Valid inputs are "
                            + constraintGroups.keySet());
                }
                if (codes.isEmpty()) {
                    return null;
                } else {
                    return logic.getSection(codes);
                }
            }
        }
        throw new PathException("Query does not verify: " + problems, null);
    }

    /**
     * Adds all the parts of a Path to a Set. Call this with only a non-attribute Path.
     *
     * @param validMainPaths a Set of Strings to add to
     * @param path a Path object
     */
    private static void addValidPaths(Set<String> validMainPaths, Path path) {
        Path pathToAdd = path;
        while (!pathToAdd.isRootPath()) {
            validMainPaths.add(pathToAdd.toStringNoConstraints());
            pathToAdd = pathToAdd.getPrefix();
        }
        validMainPaths.add(pathToAdd.toStringNoConstraints());
    }

    /**
     * Returns true if the given Path object represents a path that is inner-joined onto the parent
     * path in this query. This will return false for the root class. Do not call this method with
     * a Path that is an attribute.
     *
     * @param path a Path object
     * @return true if the join is inner, not outer and not the root
     * @throws IllegalArgumentException if the path is an attribute
     */
    private boolean isInner(Path path) {
        if (path.isRootPath()) {
            return false;
        }
        if (path.endIsAttribute()) {
            throw new IllegalArgumentException("Cannot call isInner() with a path that is an "
                    + "attribute");
        }
        OuterJoinStatus status = getOuterJoinStatus(path.getNoConstraintsString());
        if (OuterJoinStatus.INNER.equals(status)) {
            return true;
        } else if (OuterJoinStatus.OUTER.equals(status)) {
            return false;
        }
        // Fall back on defaults
        return true;
    }

    private static final Pattern PATH_MATCHER = Pattern.compile("([a-zA-Z0-9]+\\.)*[a-zA-Z0-9]+");

    /**
     * Verifies the format of a path for a query. Paths must fully match the regular expression
     * "([a-zA-Z0-9]+\.)*[a-zA-Z0-9]+"
     *
     * @param path a String path
     * @throws NullPointerException if path is null
     * @throws IllegalArgumentException if path contains colons or square brackets, or is otherwise
     * in a bad format
     */
    public static void checkPathFormat(String path) {
        if (path == null) {
            throw new NullPointerException("Path must not be null");
        }
        if (!PATH_MATCHER.matcher(path).matches()) {
            throw new IllegalArgumentException("Path \"" + path + "\" does not match regular "
                    + "expression \"([a-zA-Z0-9]+\\.)*[a-zA-Z0-9]+\"");
        }
    }

    /**
     * Get the PathQuery that should be executed.  This should be called by code creating an
     * ObjectStore query from a PathQuery.  For PathQuery the method returns this, subclasses can
     * override.  TemplateQuery removes optional constraints that have been switched off in the
     * returned query.
     * @return a version of the query to execute
     */
    public PathQuery getQueryToExecute() {
        return this;
    }

    /**
     * A method to sort constraints by a given lists, provided to allow TemplateQuery to set a
     * specific sort order that will be preserved in a round-trip to XML.  A list of constraints
     * is provided, the constraints map is updated to reflect that order.  The list does not need
     * to contain all constraints in the query - TemplateQuery only needs to order the editable
     * constraints.
     * @param listToSortBy a list to define the new constraint order
     */
    protected synchronized void sortConstraints(List<PathConstraint> listToSortBy) {
        ConstraintComparator comparator = new ConstraintComparator(listToSortBy);
        TreeMap<PathConstraint, String> orderedConstraints =
            new TreeMap<PathConstraint, String>(comparator);
        orderedConstraints.putAll(constraints);
        constraints = new LinkedHashMap<PathConstraint, String>(orderedConstraints);
    }

    private class ConstraintComparator implements Comparator<PathConstraint>
    {
        private final List<PathConstraint> listToSortBy;

        public ConstraintComparator (List<PathConstraint> listToSortBy) {
            this.listToSortBy = listToSortBy;
        }

        @Override
        public int compare(PathConstraint c1, PathConstraint c2) {
            // if neither in list we don't care how they compare, but want a consistent order
            if (!listToSortBy.contains(c1) && !listToSortBy.contains(c2)) {
                return -1;
            }
            // otherwise put lowest index first, if not in list indexOf() will return -1 so
            // constraints not in list will move to start
            return (listToSortBy.indexOf(c1) < listToSortBy.indexOf(c2)) ? -1 : 1;
        }
    }

    /**
     * Converts this object into a rudimentary String format, containing all the data.
     *
     * {@inheritDoc}
     */
    @Override public synchronized String toString() {
        return "PathQuery( view: " + view + ", orderBy: " + orderBy + ", constraints: "
            + constraints + ", logic: " + logic + ", outerJoinStatus: " + outerJoinStatus
            + ", descriptions: " + descriptions + ", description: " + description + ")";
    }

    /**
     * Convert a PathQuery to XML, using the default value of PathQuery.USERPROFILE_VERSION
     * @return This query as xml
     */
    public synchronized String toXml() {
        return this.toXml(PathQuery.USERPROFILE_VERSION);
    }

    protected void addJsonProperty(StringBuffer sb, String key, Object value) {
        if (value != null) {
            if (!sb.toString().endsWith("{")) {
                sb.append(",");
            }
            sb.append(formatKVPair(key, value));
        }
    }

    protected String formatKVPair(String key, Object value) {
        if (value instanceof List) {
            StringBuffer sb = new StringBuffer("[");
            boolean needsSep = false;
            for (Object obj: (List<?>) value) {
                if (needsSep) {
                    sb.append(",");
                }
                sb.append("\"" + StringEscapeUtils.escapeJava(obj.toString()) + "\"");
                needsSep = true;
            }
            sb.append("]");
            return "\"" + key + "\":" + sb.toString();
        } else if (value instanceof String) {
            String newValue = StringEscapeUtils.escapeJava((String) value);
            return "\"" + key + "\":\""  + newValue + "\"";
        }
        throw new IllegalArgumentException(value + " must be either String or a list of strings");
    }


    /**
     * toJson synonym for JSPs.
     *
     * @return This query as json.
     */
    public synchronized String getJson() {
        return toJson();
    }

    protected Map<String, Object> getHeadAttributes() {
        Map<String, Object> ret = new LinkedHashMap<String, Object>();
        ret.put("title", getTitle());
        ret.put("description", getDescription());
        ret.put("select", getView());

        // LOGIC - only if there is some. Just logic = A is dumb.
        String constraintLogic = getConstraintLogic();
        if (constraintLogic != null && constraintLogic.length() > 1) { 
            ret.put("constraintLogic", constraintLogic);
        }

        return ret;
    }

    /**
     * Convert this PathQuery to a JSON serialisation.
     *
     * @return This query as json.
     */
    public synchronized String toJson() {
        StringBuffer sb = new StringBuffer("{");

        sb.append(String.format("\"model\":{\"name\":\"%s\"}",
                    model.getName()));

        for (Entry<String, Object> attr: getHeadAttributes().entrySet()) {
            addJsonProperty(sb, attr.getKey(), attr.getValue());
        }


        // SORT ORDER
        List<OrderElement> order = getOrderBy();
        if (!order.isEmpty()) {
            sb.append(",\"orderBy\":[");
            for (Iterator<OrderElement> it = order.iterator(); it.hasNext();) {
                OrderElement oe = it.next();
                sb.append(String.format("{\"%s\":\"%s\"}", oe.getOrderPath(), oe.getDirection()));
                if (it.hasNext()) {
                    sb.append(",");
                }
            }
            sb.append("]");
        }


        // JOINS
        Map<String, OuterJoinStatus> ojs = getOuterJoinStatus();
        if (!ojs.isEmpty()) {
            StringBuilder sb2 = new StringBuilder();
            for (Iterator<Entry<String, OuterJoinStatus>> it = ojs.entrySet().iterator();
                it.hasNext();) {
                Entry<String, OuterJoinStatus> pair = it.next();
                if (pair.getValue() == OuterJoinStatus.OUTER) {
                    if (sb2.length() > 0) {
                        sb2.append(",");
                    }
                    sb2.append("\"" + pair.getKey() + "\"");
                }
            }
            if (sb2.length() != 0) {
                sb.append(",\"joins\":[" + sb2.toString() + "]");
            }
        }

        // CONSTRAINTS
        Map<PathConstraint, String> cons = getRelevantConstraints();
        if (!cons.isEmpty()) {
            sb.append(",\"where\":[");
            Iterator<Entry<PathConstraint, String>> it = cons.entrySet().iterator();
            while (it.hasNext()) {
                Entry<PathConstraint, String> pair = it.next();
                
                sb.append(constraintToJson(pair.getKey(), pair.getValue()));
                if (it.hasNext()) {
                    sb.append(",");
                }
            }
            sb.append("]");
        }
        sb.append("}");

        return sb.toString();
    }

    private String constraintToJson(PathConstraint constraint, String code) {
        String type = PathConstraint.getType(constraint);
        String path = constraint.getPath();

        if (type != null) {
            return String.format("{\"path\":\"%s\",\"type\":\"%s\"}", path, type);
        }

        String op = constraint.getOp().toString();

        String commonPrefix = "{\"path\":\"" + path + "\",\"op\":\"" + op + "\",\"code\":\""
                              + code + "\"";
        StringBuilder conb = new StringBuilder(commonPrefix);

        Collection<String> values = PathConstraint.getValues(constraint); // Serialise the Multi-Value list
        Collection<Integer> ids = PathConstraint.getIds(constraint); // Serialise the ID list.
        if (ids != null ) {
            conb.append(",\"ids\":[");
            Iterator<Integer> it = ids.iterator();
            while (it.hasNext()) {
                conb.append(String.valueOf(it.next()));
                if (it.hasNext()) {
                    conb.append(",");
                }
            }
            conb.append("]");
        } else if (values != null) {
            Iterator<String> it = values.iterator();
            conb.append(",\"values\":[");
            while (it.hasNext()) {
                conb.append("\"" + StringEscapeUtils.escapeJava(it.next()) + "\"");
                if (it.hasNext()) {
                    conb.append(",");
                }
            }
            conb.append("]");
        } else {
            String value = PathConstraint.getValue(constraint);
            String extraValue = PathConstraint.getExtraValue(constraint);

            if (value != null) {
                conb.append(",\"value\":\"" + StringEscapeUtils.escapeJava(value) + "\"");
            }
            if (extraValue != null) {
                conb.append(",\"extraValue\":\"" + StringEscapeUtils.escapeJava(extraValue) + "\"");
            }
        }
        conb.append("}");
        return conb.toString();
    }

    /**
     * Convert a PathQuery to XML.
     *
     * @param version the version number of the XML format
     * @return this template query as XML.
     */
    public synchronized String toXml(int version) {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            PathQueryBinding.marshal(this, "query", model.getName(), writer, version);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

        return sw.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof PathQuery) {
            return ((PathQuery) other).toXml().equals(this.toXml());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return toXml().hashCode();
    }
}
