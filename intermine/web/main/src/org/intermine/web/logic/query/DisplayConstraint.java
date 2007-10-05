package org.intermine.web.logic.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.ClassKeyHelper;

/**
 * When an constraint is being applied to an attribute, an instance of this class is used
 * to present extra, editor-related information to the JSP such valid operators,
 * enumerations etc.
 *
 * @author Thomas Riley
 */
public class DisplayConstraint
{
    private static final Logger LOG = Logger.getLogger(DisplayConstraint.class);
    /** . */
    protected PathNode node;
    /** The related model. */
    protected Model model;
    /** The list of valid operators. */
    private Map<Integer, String> validOps;
    /** List of fixed operator indices. */
    private List<Integer> fixedOps;
    /** List of possible attribute values. */
    private List optionsList;
    /** The object store summary. */
    protected ObjectStoreSummary oss;
    /** The classKeys Map. */
    protected Map classKeys;

    /**
     * Creates a new instance of DisplayConstraint.
     *
     * @param node the node representing a class attribute
     * @param model the associated model
     * @param oss the object store summary
     * @param optionsList a List of possible values, or null (to fall back to oss)
     * @param classKeys the ClassKeys Map
     */
    public DisplayConstraint(PathNode node, Model model, ObjectStoreSummary oss,
        List optionsList, Map classKeys) {
        this.node = node;
        this.model = model;
        this.oss = oss;
        this.optionsList = optionsList;
        this.classKeys = classKeys;
    }

    /**
     * Get a map of valid operators for the constraint. Maps an operator index Integer to
     * an operator name. Creates the map lazily.
     *
     * @return Map from index integer to name of valid operator for the constraint
     */
    public Map getValidOps() {
        if (validOps != null) {
            return validOps;
        }

        if (node.isAttribute()) {
            Class nodeType = MainHelper.getClass(node.getType());
            List<ConstraintOp> allOps = SimpleConstraint.validOps(nodeType);

            List<ConstraintOp> simpleConstraintOps = new ArrayList<ConstraintOp>(allOps);
            if (String.class.equals(nodeType)) {
                simpleConstraintOps.remove(ConstraintOp.MATCHES);
                simpleConstraintOps.remove(ConstraintOp.DOES_NOT_MATCH);
                // We're slightly abusing "CONTAINS" here because in the object store it only
                // means "contained in a bag".  Here we're using it to mean "contained as a
                // substring" 
                int notEqualsIndex = simpleConstraintOps.indexOf(ConstraintOp.NOT_EQUALS);
                simpleConstraintOps.add(notEqualsIndex + 1, ConstraintOp.CONTAINS);
            }
            validOps = MainHelper.mapOps(simpleConstraintOps);
        } else {
            validOps = Collections.singletonMap(new Integer(18), ConstraintOp.LOOKUP.toString());
        }

        return validOps;
    }

    /**
     * Get a List of simple constraint operator indices that can only apply to an
     * argument selected from a fixed list of values, the values being provided by
     * <code>getOptionsList</code>.
     *
     * @return  indices of operators that should only be applied to values in the options list
     */
    public List<Integer> getFixedOpIndices() {
        if (fixedOps != null) {
            return fixedOps;
        }

        fixedOps = new ArrayList<Integer>();
        List newOptionsList = new ArrayList();

        String parentType = node.getParentType();
        if ((parentType != null) || (!node.isAttribute())) {
            Class parentClass;
            String fieldName;
            try {
                if (node.isAttribute()) {
                    parentClass = MainHelper.getClass(parentType, model);
                    fieldName = node.getFieldName();
                } else {
                    parentClass = MainHelper.getClass(node.getType(), model);
                    Collection keyFieldNames = ClassKeyHelper
                                .getKeyFieldNames(classKeys, node.getType());
                    if (keyFieldNames == null || keyFieldNames.size() == 0) {
                        return new ArrayList();
                    }
                    fieldName = (String) keyFieldNames.iterator().next();
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("unexpected exception", e);
            }
            List fieldValues = oss.getFieldValues(parentClass.getName(), fieldName);
            if (fieldValues != null && node.getType() != null) {
                newOptionsList.addAll(fieldValues);
                Class fieldClass = TypeUtil.getFieldInfo(parentClass, fieldName).getType();
                Iterator iter = SimpleConstraint.fixedEnumOps(fieldClass).iterator();

                // This is commented out so that no edit box appears next to the dropdown
                // for string constraints.  A proper solution is needed - see #1398.
                //if (!String.class.equals(parentClass)) {
                    // for Strings always allow the user to type in a pattern because EQUALS
                    // constraints are automatically converted to MATCHES constraints if the
                    // pattern contains a wildcard
                    while (iter.hasNext()) {
                        fixedOps.add(((ConstraintOp) iter.next()).getIndex());
                    }
                //}

            }
        }

        if (optionsList == null) {
            optionsList = newOptionsList;
        }
        return fixedOps;
    }

    /**
     * Will return an empty list or alternatively, a list of all possible values of the field that
     * the constraint is attached to. This allows the user interface to present a list of possible
     * attribute values.
     *
     * @return  list of all possible attribute values for the node or an empty list
     */
    public List getOptionsList() {
        if (optionsList == null) {
            getFixedOpIndices();
        }

        return optionsList;
    }
}
