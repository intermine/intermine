package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.metadata.ConstraintOp;
import org.intermine.pathquery.ConstraintValueParser;
import org.intermine.pathquery.ParseValueException;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.session.SessionMethods;

/**
 * The form used for editing QueryBuilder constraints
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class QueryBuilderConstraintForm extends ActionForm
{
    private static final long serialVersionUID = 1L;
    protected String bagOp, bagValue;
    protected String attributeOp, attributeValue, attributeOptions, extraValue;
    protected String multiValueAttribute;
    protected String subclassValue;
    protected String loopQueryOp, loopQueryValue;
    protected String joinType;
    protected String useJoin;

    protected String path;
    protected String operator = "and";
    protected String nullConstraint;
    protected String rangeOp;
    protected String rangeConstraint;
    protected Set<String> ranges;
    private String editingConstraintCode = null;

    // template builder elements

    protected boolean editable;
    protected String templateLabel;
    protected String switchable;

    /**
     * Set the code of an existing constraint that is being edited.
     * @param code the code of the constraint in the query
     */
    public void setEditingConstraintCode(String code) {
        this.editingConstraintCode = code;
    }

    /**
     * Get the constraint that is being edited or null if creating a new constraint.
     * @return the constraint being edited or null
     */
    public String getEditingConstraintCode() {
        return this.editingConstraintCode;
    }

    /**
     * Gets the value of loopQueryOp
     * @return the value of loopQueryOp
     */
    public String getLoopQueryOp()  {
        return loopQueryOp;
    }

    /**
     * Sets the value of loopQueryOp
     * @param loopOp Value to assign to loopQueryOp
     */
    public void setLoopQueryOp(String loopOp) {
        this.loopQueryOp = loopOp;
    }

    /**
     * Gets the value of loopQueryValue
     * @return the value of loopQueryValue
     */
    public String getLoopQueryValue()  {
        return loopQueryValue;
    }

    /**
     * Sets the value of loopQueryValue
     * @param loopQuery value to assign to loopQueryValue
     */
    public void setLoopQueryValue(String loopQuery) {
        this.loopQueryValue = loopQuery;
    }

    /**
     * Gets the value of bagOp
     * @return the value of bagOp
     */
    public String getBagOp()  {
        return bagOp;
    }

    /**
     * Sets the value of bagOp
     * @param bagOp Value to assign to bagOp
     */
    public void setBagOp(String bagOp) {
        this.bagOp = bagOp;
    }

    /**
     * Gets the value of bagValue
     * @return the value of bagValue
     */
    public String getBagValue()  {
        return bagValue;
    }

    /**
     * Sets the value of bagValue
     * @param bagValue value to assign to bagValue
     */
    public void setBagValue(String bagValue) {
        this.bagValue = bagValue;
    }

    /**
     * Gets the value of attributeOp
     * @return the value of attributeOp
     */
    public String getAttributeOp()  {
        return attributeOp;
    }

    /**
     * Sets the value of attributeOp
     * @param attributeOp value to assign to attributeOp
     */
    public void setAttributeOp(String attributeOp) {
        this.attributeOp = attributeOp;
    }

    /**
     * Gets the value of attributeValue
     * @return the value of attributeValue
     */
    public String getAttributeValue()  {
        return attributeValue;
    }

    /**
     * Sets the value of attributeValue
     * @param attributeValue value to assign to attributeValue
     */
    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    /**
     * Gets the value of extraValue
     * @return the value of extraValue
     */
    public String getExtraValue() {
        return extraValue;
    }

    /**
     * Sets the value of extraValue
     * @param extraValue the value to assign to extraValue
     */
    public void setExtraValue(String extraValue) {
        this.extraValue = extraValue;
    }

    /**
     * Returs the value of multiValueAttribute
     * @return a String rapresenting the value of multiValueAttribute
     */
    public String getMultiValueAttribute() {
        return multiValueAttribute;
    }

    /**
     * Sets the value of multiValueAttribute, a string representing the values selected
     * by the user separated by a comma.
     *
     * @param multiValueAttribute the value to assign to multiValueAttribute
     */
    public void setMultiValueAttribute(String multiValueAttribute) {
        this.multiValueAttribute = multiValueAttribute;
    }

    /**
     * Gets the value of attributeValue
     * @return the value of attributeValue
     */
    public String getAttributeOptions()  {
        return attributeOptions;
    }

    /**
     * Sets the value of attributeOptions
     * @param attributeOptions value to assign to attributeOptions
     */
    public void setAttributeOptions(String attributeOptions) {
        this.attributeOptions = attributeOptions;
    }

    /**
     * Gets the value of subclassValue
     * @return the value of subclassValue
     */
    public String getSubclassValue()  {
        return subclassValue;
    }

    /**
     * Sets the value of subclassValue
     * @param subclassValue value to assign to subclassValue
     */
    public void setSubclassValue(String subclassValue) {
        this.subclassValue = subclassValue;
    }

    /**
     * Gets the value of path
     * @return the value of path
     */
    public String getPath()  {
        return path;
    }

    /**
     * Sets the value of path
     * @param path value to assign to path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Get the operator.
     * @return "and" or "or"
     */
    public String getOperator() {
        return operator;
    }

    /**
     * Set the operator, "and" or "or"
     * @param operator the operator
     */
    public void setOperator(String operator) {
        this.operator = operator;
    }

    /**
     * Get the null/not null constraint value. Returned value will be
     * either "NULL" or "NotNULL".
     * @return null/not null constraint value
     */
    public String getNullConstraint() {
        return nullConstraint;
    }

    /**
     * Set the null/not null constraint. Parameter should be
     * either "NULL" or "NotNULL".
     * @param nullConstraint null/not null constraint
     */
    public void setNullConstraint(String nullConstraint) {
        this.nullConstraint = nullConstraint;
    }

    /**
     * Set the range constraint value. Will be one or more ranges separated by commas
     *
     * @param rangeConstraint the range constraint value
     */
    public void setRangeConstraint(String rangeConstraint) {
        this.rangeConstraint = rangeConstraint.trim();
    }

    /**
     * Get the range constraint value. Will be one or more ranges separated by commas
     *
     * @return the range constraint value
     */
    public String getRangeConstraint() {
        return rangeConstraint;
    }

    /**
     * @return the ranges to constrain by, e.g. 2R:123..456
     */
    public Set<String> getRanges() {
        return ranges;
    }

    /**
     * @param range the range to constrain by, e.g. 2R:123..456
     */
    public void addRange(String range) {
        ranges.add(range);
    }

    /**
     * @return the operator for this range constraint, e.g. OVERLAPS
     */
    public String getRangeOp() {
        return rangeOp;
    }

    /**
     * @param rangeOp the operator for this range constraint, e.g. OVERLAPS
     */
    public void setRangeOp(String rangeOp) {
        this.rangeOp = rangeOp;
    }

    /**
     * Get the template label.
     * @return the template label
     */
    public String getTemplateLabel() {
        return templateLabel;
    }

    /**
     * Set the template label.
     * @param templateLabel the template label
     */
    public void setTemplateLabel(String templateLabel) {
        this.templateLabel = templateLabel;
    }

    /**
     * @return the joinType
     */
    public String getJoinType() {
        return joinType;
    }

    /**
     * @param joinType the joinType to set
     */
    public void setJoinType(String joinType) {
        this.joinType = joinType;
    }

    /**
     * @return the useJoin
     */
    public String getUseJoin() {
        return useJoin;
    }

    /**
     * @param useJoin the useJoin to set
     */
    public void setUseJoin(String useJoin) {
        this.useJoin = useJoin;
    }

    /**
     * Get the editable flag (when building a template).
     * @return whether this constraint is editable
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Set the editable flag (when building a template).
     * @param editable whether or not this constraint should be editable
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     * Get the switchable value (on, off, locked).
     * @return switchable
     */
    public String getSwitchable() {
        return switchable;
    }

    /**
     * @param switchable the switchable values (on, off, locked) to set
     */
    public void setSwitchable(String switchable) {
        this.switchable = switchable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionErrors validate(ActionMapping mapping,
                                 HttpServletRequest request) {
        HttpSession session = request.getSession();

        PathQuery query = SessionMethods.getQuery(session);

        ActionErrors errors = new ActionErrors();
        if (request.getParameter("attribute") != null) {
            try {
                Path pathObj = query.makePath(path);
                Class<?> fieldClass;
                if (pathObj.endIsAttribute()) {
                    fieldClass = pathObj.getEndType();
                } else {
                    fieldClass = String.class;
                }
                int attributeOperator = Integer.parseInt(attributeOp);
                if (attributeOperator == ConstraintOp.NONE_OF.getIndex()
                    || attributeOperator == ConstraintOp.ONE_OF.getIndex()) {
                    if ("".equals(multiValueAttribute)) {
                        errors.add(ActionErrors.GLOBAL_MESSAGE, new ActionMessage("errors.message",
                            "No input given, please supply a valid expression"));
                    }
                } else {
                    parseValue(attributeValue, fieldClass, errors);
                }
            } catch (PathException e) {
                errors.add(ActionErrors.GLOBAL_MESSAGE, new ActionMessage("errors.message",
                            e.getMessage()));
            }
        }
        return errors;
    }

    /**
     * Parse an attribute value
     * @param value the value as a String
     * @param type the type of the parsed value
     * @param errors ActionErrors to which any parse errors are added
     * @return the parsed value
     */
    public static Object parseValue(String value, Class<?> type, ActionMessages errors) {
        try {
            return ConstraintValueParser.parse(value, type);
        } catch (ParseValueException ex) {
            errors.add(ActionErrors.GLOBAL_MESSAGE,
                    new ActionMessage("errors.message", ex.getMessage()));
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset(ActionMapping mapping,
                      HttpServletRequest request) {
        bagOp = null;
        bagValue = null;
        attributeOp = null;
        attributeValue = null; // can be a checkbox
        multiValueAttribute = null;
        subclassValue = null;
        path = null;
        nullConstraint = "NULL";
        templateLabel = "";
        editable = false;
        operator = SessionMethods.getDefaultOperator(request.getSession());
        joinType = "inner";
        useJoin = null;
        editingConstraintCode = null;
        ranges = new HashSet<String>();
        rangeOp = null;
        rangeConstraint = null;
    }
}
