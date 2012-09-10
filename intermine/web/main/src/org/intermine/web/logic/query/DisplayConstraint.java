package org.intermine.web.logic.query;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.ConstraintValueParser;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathConstraintLoop;
import org.intermine.pathquery.PathConstraintMultiValue;
import org.intermine.pathquery.PathConstraintNull;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.SwitchOffAbility;
import org.intermine.util.StringUtil;
import org.intermine.web.autocompletion.AutoCompleter;
import org.intermine.web.logic.querybuilder.DisplayPath;



/**
 * Representation of a PathQuery constraint for use by JSP pages.  This object provides methods
 * needed to populate constraint editing boxes and dropdowns, find available bag names, etc.  Can
 * either represent a new constraint to be added with no values set or an existing constraint that
 * is being edited.
 *
 * Get methods return null if no values are available
 *
 * @author Richard Smith
 */
public class DisplayConstraint
{
    private Path path;
    private List<DisplayConstraintOption> validOps;
    private AutoCompleter ac;
    private ObjectStoreSummary oss;
    private String endCls;
    private String fieldName;
    private BagQueryConfig bagQueryConfig;
    private Map<String, List<FieldDescriptor>> classKeys;
    private BagManager bagManager;
    private Profile profile;
    private String constraintLabel;
    private List<DisplayConstraintOption> fixedOps;
    private PathConstraint con;
    private PathQuery query;
    private String code;
    private boolean editableInTemplate;
    private SwitchOffAbility switchOffAbility;
    private boolean isBagSelected;
    private String selectedBagValue;
    private ConstraintOp selectedBagOp;
    private List<Object> templateSummary;
    private boolean showExtraConstraint = false;

    /**
     * Construct for a new constraint that is being added to a query.
     * @param path The path that is being constrained
     * @param profile user editing the query, used to fetch available bags
     * @param query the PathQuery, in order to provide information on candidate loops
     * @param ac auto completer
     * @param oss summary data for the ObjectStore contents
     * @param bagQueryConfig addition details for needed for LOOKUP constraints
     * @param classKeys identifier field config, needed for LOOKUP constraints
     * @param bagManager provides access to saved bags
     */
    protected DisplayConstraint(Path path, Profile profile, PathQuery query, AutoCompleter ac,
            ObjectStoreSummary oss, BagQueryConfig bagQueryConfig,
            Map<String, List<FieldDescriptor>> classKeys, BagManager bagManager) {
        init(path, profile, query, ac, oss, bagQueryConfig, classKeys, bagManager);
    }

    /**
     * Construct for an existing constraint that is being edited.
     * @param path The path that is being constrained
     * @param con the constraint being edited
     * @param label text associated with this constraint, if a template query
     * @param code the code of this constraint in the query
     * @param editableInTemplate true if this is a template query and this constraint is editable
     * @param switchOffAbility if the contraint is on, off, locked
     * @param profile user editing the query, used to fetch available bags
     * @param query the PathQuery, in order to provide information on candidate loops
     * @param ac auto completer
     * @param oss summary data for the ObjectStore contents
     * @param bagQueryConfig addition details for needed for LOOKUP constraints
     * @param classKeys identifier field config, needed for LOOKUP constraints
     * @param bagManager provides access to saved bags
     */
    protected DisplayConstraint(Path path, PathConstraint con, String label, String code,
            boolean editableInTemplate, SwitchOffAbility switchOffAbility, Profile profile,
            PathQuery query, AutoCompleter ac,
            ObjectStoreSummary oss, BagQueryConfig bagQueryConfig,
            Map<String, List<FieldDescriptor>> classKeys, BagManager bagManager,
            List<Object> templateSummary) {
        init(path, profile, query, ac, oss, bagQueryConfig, classKeys, bagManager);
        this.con = con;
        this.constraintLabel = label;
        this.code = code;
        this.editableInTemplate = editableInTemplate;
        this.switchOffAbility = switchOffAbility;
        this.templateSummary = templateSummary;
    }

    private void init(Path path, Profile profile, PathQuery query, AutoCompleter ac,
            ObjectStoreSummary oss, BagQueryConfig bagQueryConfig,
            Map<String, List<FieldDescriptor>> classKeys, BagManager bagManager) {
        this.path = path;
        this.ac = ac;
        this.oss = oss;
        this.endCls = getEndClass(path);
        this.fieldName = getFieldName(path);
        this.bagQueryConfig = bagQueryConfig;
        this.classKeys = classKeys;
        this.profile = profile;
        this.query = query;
        this.bagManager = bagManager;
        this.isBagSelected = false;
        if (isExtraConstraint()) {
            this.showExtraConstraint = true;
        }
    }

    private String getEndClass(Path path) {
        if (path.isRootPath()) {
            return path.getStartClassDescriptor().getType().getSimpleName();
        } else {
            return path.getLastClassDescriptor().getType().getSimpleName();
        }
    }

    private String getFieldName(Path path) {
        if (!path.isRootPath()) {
            return path.getLastElement();
        }
        return null;
    }

    // TODO this should be in some common code
    private String constraintStringValue(PathConstraint con) {
        if (con instanceof PathConstraintAttribute) {
            return ((PathConstraintAttribute) con).getValue();
        } else if (con instanceof PathConstraintBag) {
            return ((PathConstraintBag) con).getBag();
        } else if (con instanceof PathConstraintLookup) {
            return ((PathConstraintLookup) con).getValue();
        } else if (con instanceof PathConstraintSubclass) {
            return ((PathConstraintSubclass) con).getType();
        } else if (con instanceof PathConstraintLoop) {
            return ((PathConstraintLoop) con).getLoopPath();
        } else if (con instanceof PathConstraintNull) {
            return ((PathConstraintNull) con).getOp().toString();
        }
        return null;
    }

    /**
     * If editing an existing constraint get the code for this constraint in the query, return null
     * if creating a new constraint.
     * @return the constraint code or null
     */
    public String getCode() {
        return code;
    }

    /**
     * Return true if editing an existing template constraint and that constraint is editable.
     * @return true if an editable template constraint, or null
     */
    public boolean isEditableInTemplate() {
        return editableInTemplate;
    }

    /**
     * Get a representation of the path that is being constraint.  DisplayPath provides convenience
     * methods for use in JSP.
     * @return the path being constrained
     */
    public DisplayPath getPath() {
        return new DisplayPath(path);
    }

    /**
     * If editing an existing constraint, return the selected value.  Otherwise return null.  If
     * an attribute constraint this will be the user entered.  If a bag constraint, the selected
     * bag name, etc. If an attribute constraint, but the use bag is setted, this will be the
     * selectedBagValue setted
     * @return the selected value or null
     */
    public String getSelectedValue() {
        if (isBagSelected) {
            return selectedBagValue;
        }
        if (con != null) {
            return constraintStringValue(con);
        }
        return null;
    }

    /**
     * 
     */
    public String getOriginalValue() {
        if (con != null) {
            return constraintStringValue(con);
        }
        return null;
    }

    /**
     * Returns the value collection if the constraint is a multivalue, otherwise return null.
     *
     * @return a Collection of Strings
     */
    public Collection<String> getMultiValues() {
        if (isMultiValueSelected()) {
            return ((PathConstraintMultiValue) con).getValues();
        }
        return null;
    }

    /**
     * If the constraint is a multivalue, returns the value collection
     * represented as string separated by ', ', otherwise return an empty String.
     *
     * @return a String representing the multivalues of constraint
     */
    public String getMultiValuesAsString() {
        String multiValuesAsString = "";
        if (getMultiValues() != null) {
            for (String value : getMultiValues()) {
                multiValuesAsString += value + ", ";
            }
            multiValuesAsString = multiValuesAsString.substring(0,
                                  multiValuesAsString.lastIndexOf(","));
        }
        return multiValuesAsString;
    }

    /**
     * Return true if editing an existing constraint and a bag has been selected.
     * @return true if a bag has been selected
     */
    public boolean isBagSelected() {
        if (isBagSelected) {
            return isBagSelected;
        } else {
            return (con != null && con instanceof PathConstraintBag);
        }
    }

    /**
     * Set if the bag is selected, used by the method isBagSelected that returns true,
     * even if the constraint is an attribute constraint
     * @param isBagSelected true if a bag has been selected
     */
    public void setBagSelected(boolean isBagSelected) {
        this.isBagSelected = isBagSelected;
    }

    /**
     * Return true if editing an existing constraint and 'has a value' or 'has no value' has been
     * selected.
     * @return true if a null constraint was selected
     */
    public boolean isNullSelected() {
        return (con != null && con instanceof PathConstraintNull);
    }

    /**
     * Return true if editing an existing having the attribute type boolean or Boolean
     * @return true if the type is the primitive boolean or the object java.lang.Boolean
     */
    public boolean isBoolean() {
        String type = getPath().getType();
        return ("boolean".equals(type) || "Boolean".equals(type));
    }

    /**
     * Return true if editing an existing constraint and an attribute value or LOOKUP constraint
     * was selected.
     * @return true if an attribute/LOOKUP constraint was selected
     */
    public boolean isValueSelected() {
        if (con != null) {
            return !(isBagSelected() || isNullSelected() || isLoopSelected());
        }
        return false;
    }

    /**
     * Return true if editing an existing constraint and a loop value has been
     * selected.
     * @return true if a loop constraint was selected
     */
    public boolean isLoopSelected() {
        return (con != null && con instanceof PathConstraintLoop);
    }

    /**
     * Return true if editing an existing constraint and a multivalue has been
     * selected.
     * @return true if a multivalue constraint was selected
     */
    public boolean isMultiValueSelected() {
        return (con != null && con instanceof PathConstraintMultiValue);
    }

    /**
     * Return the last class in the path and fieldname as the title for the constraint.
     * @return the title of this constraint
     */
    public String getTitle() {
        return endCls + (fieldName == null ? "" : " " + fieldName);
    }

    public String getEndClassName() {
        return endCls;
    }

    /**
     * Return the label associated with a constraint if editing a template query constraint.
     * @return the constraint label
     */
    public String getDescription() {
        return constraintLabel;
    }

    /**
     * Return a help message to display alongside the constraint, this will examine the constraint
     * type and generate and appropriate message, e.g. list the key fields for LOOKUP constraints
     * and explain the use of wildcards.  Returns null when there is no appropriate help.
     * @return the help message or null
     */
    public String getHelpMessage() {
        return DisplayConstraintHelpMessages.getHelpMessage(this);
    }

    /**
     * If the bag is selected, return the value setted with the method setSelectedBagOp
     * If editing an existing constraint return the operation used.
     * Otherwise return null.
     * @return the selected constraint op or null
     */
    public DisplayConstraintOption getSelectedOp() {
        if (isBagSelected) {
            return new DisplayConstraintOption(selectedBagOp.toString(),
                                               selectedBagOp.getIndex());
        }
        if (con != null) {
            ConstraintOp selectedOp = con.getOp();
            if (selectedOp != null) {
                return new DisplayConstraintOption(selectedOp.toString(), selectedOp.getIndex());
            }
        }
        return null;
    }

    /**
     * Set the seletedBagOp
     * @param selectedBagOp the constraint op returned by the method getSelectedOp()
     * if the bag is selected
     */
    public void setSelectedBagOp(ConstraintOp selectedBagOp) {
        this.selectedBagOp = selectedBagOp;
    }

    /**
     * Set the seletedBagValue returned bye the getSelectedValue if the bag is selected
     * @param selectedBagValue string to set the selectedBagValue
     */
    public void setSelectedBagValue(String selectedBagValue) {
        this.selectedBagValue = selectedBagValue;
    }

    /**
     * If editing an existing LOOKUP constraint return the value selected for the extra constraint
     * field.  Otherwise return null
     * @return the LOOKUP constraint extra value or null
     */
    public String getSelectedExtraValue() {
        if (con instanceof PathConstraintLookup) {
            return ((PathConstraintLookup) con).getExtraValue();
        }
        return null;
    }

    /**
     * Given the path being constrained return the valid constraint operations.  If constraining an
     * attribute the valid ops depend on the type being constraint - String, Integer, Boolean, etc.
     * @return the valid constraint operations
     */
    public List<DisplayConstraintOption> getValidOps() {
        if (validOps != null) {
            return validOps;
        }
        validOps = new ArrayList<DisplayConstraintOption>();
        if (con instanceof PathConstraintBag) {
            for  (ConstraintOp op : PathConstraintBag.VALID_OPS) {
                validOps.add(new DisplayConstraintOption(op.toString(), op.getIndex()));
            }
        } else if (con  instanceof PathConstraintSubclass) {
            return validOps;
        } else if (con  instanceof PathConstraintLoop) {
            List<DisplayConstraintOption> loopQueryOps = getLoopQueryOps();
            for  (DisplayConstraintOption dco : loopQueryOps) {
                validOps.add(dco);
            }
        } else if (path.endIsAttribute()) {
            List<ConstraintOp> allOps = SimpleConstraint.validOps(path.getEndType());
            // TODO This was in the constraint jsp:
            // <c:if test="${!(editingNode.type == 'String' && (op.value == '<='
                                                             //|| op.value == '>='))}">
            // TODO this should show different options if a dropdown is to be used
            boolean existPossibleValues =
                (getPossibleValues() != null && getPossibleValues().size() > 0) ? true : false;
            for (ConstraintOp op : allOps) {
                if (existPossibleValues
                    || (!op.getIndex().equals(ConstraintOp.MATCHES.getIndex())
                        && !op.getIndex().equals(ConstraintOp.DOES_NOT_MATCH.getIndex()))
                ) {
                    validOps.add(new DisplayConstraintOption(op.toString(), op.getIndex()));
                }
            }
            if (existPossibleValues) {
                for (ConstraintOp op : PathConstraintMultiValue.VALID_OPS) {
                    validOps.add(new DisplayConstraintOption(op.toString(),
                        op.getIndex()));
                }
            }
        } else if (isLookup()) {
            // this must be a LOOKUP constraint
            ConstraintOp lookup = ConstraintOp.LOOKUP;
            validOps.add(new DisplayConstraintOption(lookup.toString(), lookup.getIndex()));
        }

        return validOps;
    }

    /**
     * Returns the set of operators valid for loop constraints.
     *
     * @return a List of DisplayConstraintOption objects
     */
    public List<DisplayConstraintOption> getLoopQueryOps() {
        return Arrays.asList(new DisplayConstraintOption(ConstraintOp.EQUALS.toString(),
                    ConstraintOp.EQUALS.getIndex()),
                new DisplayConstraintOption(ConstraintOp.NOT_EQUALS.toString(),
                    ConstraintOp.NOT_EQUALS.getIndex()));
    }

    /**
     * Return true if this constraint should be a LOOKUP, true if constraining a class (ref/col)
     * instead of an attribute and that class has class keys defined.
     * @return true if this constraint should be a LOOKUP
     */
    public boolean isLookup() {
        return !path.endIsAttribute() && ClassKeyHelper.hasKeyFields(classKeys, endCls);
    }

    /**
     * Return the LOOKUP constraint op.
     * @return the LOOKUP constraint op
     */
    // TOOO do we need this?  validOps should contain correct value
    public DisplayConstraintOption getLookupOp() {
        ConstraintOp lookup = ConstraintOp.LOOKUP;
        return new DisplayConstraintOption(lookup.toString(), lookup.getIndex());
    }

    /**
     * Return the autocompleter for this path if one is available.  Otherwise return null.
     * @return an autocompleter for this path or null
     */
    public AutoCompleter getAutoCompleter() {
        if (ac != null && ac.hasAutocompleter(endCls, fieldName)) {
            return ac;
        }
        return null;
    }


    /**
     * Values to populate a dropdown for the path if possible values are available.
     * @return possible values to populate a dropdown
     */
    public List<Object> getPossibleValues() {
        String className = "";
        if (path.isRootPath()) {
            className = path.getStartClassDescriptor().getType().getCanonicalName();
        } else {
            className = path.getLastClassDescriptor().getType().getCanonicalName();
        }

        // if this is a template, it may have been summarised so we have a restricted set if values
        // for particular paths (the TemplateSummariser runs queries to work out exact values
        // constraints could take given the other constraints in the query.
        if (templateSummary != null && !templateSummary.isEmpty()) {
            return templateSummary;
        }

        // otherwise, we may have possible values from the ObjectStoreSummary
        List<Object> fieldValues = oss.getFieldValues(className, fieldName);
        if (fieldValues != null) {
            if (fieldValues.size() == 1 && fieldValues.get(0) == null) {
                return null;
            }
        }
        if (path.endIsAttribute()) {
            Class<?> type = path.getEndType();
            if (Date.class.equals(type)) {
                List<Object> fieldValueFormatted = new ArrayList<Object>();
                if (fieldValues != null) {
                    for (Object obj : fieldValues) {
                        fieldValueFormatted.add(ConstraintValueParser.format((String) obj));
                    }
                }
                return fieldValueFormatted;
            }
        }
        return fieldValues;
    }

    /**
     * If a dropdown is available for a constraint fewer operations are possible, return the list
     * of operations.
     * @return  the constraint ops available when selecting values from a dropdown
     */
    // TODO Do we need this, could getValildOps return the correct ops if a dropdown is available
    public List<DisplayConstraintOption> getFixedOps() {
        if (fixedOps != null) {
            return fixedOps;
        }

        if (getPossibleValues() != null) {
            fixedOps = new ArrayList<DisplayConstraintOption>();
            for (ConstraintOp op : SimpleConstraint.fixedEnumOps(path.getEndType())) {
                fixedOps.add(new DisplayConstraintOption(op.toString(), op.getIndex()));
            }
        }
        return fixedOps;
    }

    /**
     * Return true if this is a LOOKUP constraint and an extra constraint should be available.
     * @return true if an extra constraint option is available
     */
    public boolean isExtraConstraint() {
        if (isLookup() && bagQueryConfig != null) {
            String extraValueFieldName = bagQueryConfig.getConnectField();
            ClassDescriptor cld = (path.isRootPath()) ? path.getStartClassDescriptor()
                                   : path.getLastClassDescriptor();
            ReferenceDescriptor fd = cld.getReferenceDescriptorByName(extraValueFieldName, true);
            return fd != null;
        } else {
            return false;
        }
    }

    public boolean isShowExtraConstraint() {
        return showExtraConstraint;
    }

    public void setShowExtraConstraint(boolean showExtraConstraint) {
        this.showExtraConstraint = showExtraConstraint;
    }

    public String getExtraValueFieldClass() {
        if (isExtraConstraint()) {
            return bagQueryConfig.getExtraConstraintClassName();
        }
        return null;
    }

    public String getExtraConnectFieldPath() {
        if (isExtraConstraint()) {
            return path.toStringNoConstraints() + "." + bagQueryConfig.getConnectField();
        }
        return null;
    }

    /**
     * If a LOOKUP constraint and an extra constraint is available for this path, return a list of
     * the possible values for populating a dropdown.  Otherwise return null.
     * @return a list of possible extra constraint values
     */
    public List<Object> getExtraConstraintValues() {
        if (isExtraConstraint()) {
            String extraValueFieldName = bagQueryConfig.getConstrainField();
            return oss.getFieldValues(bagQueryConfig.getExtraConstraintClassName(),
                    extraValueFieldName);
        }
        return null;
    }

    /**
     * If a LOOKUP constraint and an extra value constraint is available return the classname of
     * the extra constraint so it can be displayed.  Otherwise return null.
     * @return the extra constraint class name or null
     */
    public String getExtraConstraintClassName() {
        if (isExtraConstraint()) {
            String[] splitClassName = bagQueryConfig.getExtraConstraintClassName().split("[.]");
            return splitClassName[splitClassName.length - 1];
            //return bagQueryConfig.getExtraConstraintClassName();
        }
        return null;
    }

    /**
     * Return the key fields for this path as a formatted string, for use in LOOKUP help message.
     * @return a formatted string listing key fields for this path
     */
    public String getKeyFields() {
        if (ClassKeyHelper.hasKeyFields(classKeys, endCls)) {
            return StringUtil.prettyList(ClassKeyHelper.getKeyFieldNames(classKeys, endCls), true);
        }
        return null;
    }

    /**
     * Get a list of public and user bag names available and currentfor this path.
     * If none available return null.
     * @return a list of available bag names or null
     */
    public List<String> getBags() {
        if (ClassKeyHelper.hasKeyFields(classKeys, endCls)
            /*&& !ClassKeyHelper.isKeyField(classKeys, endCls, fieldName)*/) {
            Map<String, InterMineBag> bags =
                bagManager.getCurrentBagsOfType(profile, endCls);
            if (!bags.isEmpty()) {
                List<String> bagList = new ArrayList<String>(bags.keySet());
                Collections.sort(bagList);
                return bagList;
            }
        }
        return null;
    }

    /**
     * Return the valid constraint ops when constraining on a bag.
     * @return the possible bag constraint operations
     */
    public List<DisplayConstraintOption> getBagOps() {
        List<DisplayConstraintOption> bagOps = new ArrayList<DisplayConstraintOption>();
        for (ConstraintOp op : BagConstraint.VALID_OPS) {
            bagOps.add(new DisplayConstraintOption(op.toString(), op.getIndex()));
        }
        return bagOps;
    }

    /**
     * Returns the bag type that the constraint can be constrained to.
     * If there aren't bags return null
     *
     * @return a String
     */
    public String getBagType() {
        if (getBags() != null) {
            return endCls;
        } else {
            return null;
        }
    }

    /**
     * Returns the constraint type selected.
     *
     * @return a String representing the constraint type selected
     */
    public String getSelectedConstraint() {
        if (isBagSelected()) {
            return "bag";
        } else if (isNullSelected()) {
            return "empty";
        } else if (isLoopSelected()) {
            return "loopQuery";
        }
        return "attribute";
    }

    /**
     * Returns the set of paths that could feasibly be loop constrained onto the constraint's path,
     * given the query's outer join situation. A candidate path must be a class path, of the same
     * type, and in the same outer join group.
     *
     * @return a Set of String paths that could be loop joined
     * @throws PathException if something goes wrong
     */
    public Set<String> getCandidateLoops() throws PathException {
        if (path.endIsAttribute()) {
            return Collections.emptySet();
        } else {
            if (con instanceof PathConstraintLoop) {
                Set<String> retval = new LinkedHashSet<String>();
                retval.add(((PathConstraintLoop) con).getLoopPath());
                retval.addAll(query.getCandidateLoops(path.getNoConstraintsString()));
                return retval;
            } else {
                return query.getCandidateLoops(path.getNoConstraintsString());
            }
        }
    }

    /**
     * Return true if the constraint is locked, it should'n be enabled or disabled.
     * @return true if the constraint is locked
     */
    public boolean isLocked() {
        if (switchOffAbility == null || switchOffAbility == SwitchOffAbility.LOCKED) {
            return true;
        }
        return false;
    }

    /**
     * Return true if the constraint is enabled, false if it is disabled or locked.
     * @return true if the constraint is enabled,false if it is disabled or locked
     */
    public boolean isEnabled() {
        if (switchOffAbility == SwitchOffAbility.ON) {
            return true;
        }
        return false;
    }

    /**
     * Return true if the constraint is disabled, false if it is enabled or locked.
     * @return true if the constraint is disabled,false if it is enabled or locked
     */
    public boolean isDisabled() {
        if (switchOffAbility == SwitchOffAbility.OFF) {
            return true;
        }
        return false;
    }

    /**
     * Return the value on, off, locked depending on the constraint SwitchOffAbility .
     * @return switchable property (on, off, locked)
     */
    public String getSwitchable() {
        if (SwitchOffAbility.ON.equals(switchOffAbility)) {
            return SwitchOffAbility.ON.toString().toLowerCase();
        } else if (SwitchOffAbility.OFF.equals(switchOffAbility)) {
            return SwitchOffAbility.OFF.toString().toLowerCase();
        } else {
            return SwitchOffAbility.LOCKED.toString().toLowerCase();
        }
    }

    /**
     * Set the switchOffAbility
     * @param switchOffAbility value
     */
    public void setSwitchOffAbility(SwitchOffAbility switchOffAbility) {
        this.switchOffAbility = switchOffAbility;
    }

    /**
     * Return true if the input field can be displayed, method for use in JSP
     * @return true if the input is displayed
     */
    public boolean isInputFieldDisplayed() {
        if (con != null) {
            int selectedOperator = getSelectedOp().getProperty();
            if (selectedOperator == ConstraintOp.MATCHES.getIndex()
                    || selectedOperator == ConstraintOp.DOES_NOT_MATCH.getIndex()
                    || selectedOperator == ConstraintOp.LOOKUP.getIndex()
                    || selectedOperator == ConstraintOp.CONTAINS.getIndex()
                    || selectedOperator == ConstraintOp.DOES_NOT_CONTAIN.getIndex()) {
                return true;
            }
            if (selectedOperator == ConstraintOp.ONE_OF.getIndex()
                    || selectedOperator == ConstraintOp.NONE_OF.getIndex()) {
                if (con instanceof PathConstraintBag) {
                    return true;
                }
                return false;
            }
            if (getPossibleValues() != null && getPossibleValues().size() > 0) {
                return false;
            }
            return true;
        }
        if (getPossibleValues() != null && getPossibleValues().size() > 0) {
            return false;
        }
        return true;
    }

    /**
     * Return true if the drop-down containing the possibleValues can be displayed,
     * method for use in JSP
     * @return true if the drop-down is displayed
     */
    public boolean isPossibleValuesDisplayed() {
        if (con != null) {
            if (getSelectedOp() == null) {
                return false;
            }
            int selectedOperator = getSelectedOp().getProperty();
            if (selectedOperator == ConstraintOp.MATCHES.getIndex()
                    || selectedOperator == ConstraintOp.DOES_NOT_MATCH.getIndex()
                    || selectedOperator == ConstraintOp.CONTAINS.getIndex()
                    || selectedOperator == ConstraintOp.DOES_NOT_CONTAIN.getIndex()
                    || selectedOperator == ConstraintOp.LOOKUP.getIndex()
                    || selectedOperator == ConstraintOp.ONE_OF.getIndex()
                    || selectedOperator == ConstraintOp.NONE_OF.getIndex()) {
                return false;
            }
            if (getPossibleValues() != null && getPossibleValues().size() > 0) {
                return true;
            }
            return false;
        }
        if (getPossibleValues() != null && getPossibleValues().size() > 0) {
            return true;
        }
        return false;
    }

    /**
     * Return true if the multi-select containing the possibleValue can be displayed,
     * method for use in JSP
     * @return true if the multi-select is displayed
     */
    public boolean isMultiValuesDisplayed() {
        if (con != null) {
            int selectedOperator = getSelectedOp().getProperty();
            if (selectedOperator == ConstraintOp.ONE_OF.getIndex()
                    || selectedOperator == ConstraintOp.NONE_OF.getIndex()) {
                return true;
            }
            return false;
        } return false;
    }

    /**
     * Representation of a constraint operation to populate a dropdown.  Label is value to be
     * displayed in the dropdown, property is the index of the constraint that will be selected.
     * @author Richard Smith
     *
     */
    public class DisplayConstraintOption
    {
        private String label;
        private Integer property;

        /**
         * Construct with the constraint lable and index
         * @param label the value to be shown in dropdown
         * @param property the constraint index to be added to form on selection
         */
        public DisplayConstraintOption(String label, Integer property) {
            this.label = label;
            this.property = property;
        }

        /**
         * Get the value to be displayed in the dropdown for this operation.
         * @return the display value
         */
        public String getLabel() {
            return label;
        }

        /**
         * Get the constraint index to be put in form when this op is selected.
         * @return the constraint index
         */
        public Integer getProperty() {
            return property;
        }
    }
}
