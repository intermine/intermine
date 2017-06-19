package org.intermine.web.logic.template;

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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.intermine.metadata.ConstraintOp;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathConstraintMultiValue;
import org.intermine.pathquery.PathConstraintNull;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.SwitchOffAbility;
import org.intermine.template.TemplateQuery;
import org.intermine.template.TemplateValue;
import org.intermine.webservice.server.CodeTranslator;

/**
 * A utility class with static methods for dealing with templates.
 * @author Alex Kalderimis
 *
 */
public final class Templates
{

    private Templates() {
        // Don't.
    }

    private static final String OPERATION_PARAMETER = "op";
    private static final String EXTRA_PARAMETER = "extra";
    private static final String VALUE_PARAMETER = "value";
    private static final String ID_PARAMETER = "constraint";
    private static final String CODE_PARAMETER = "code";

    private static ConstraintOp getConstraintOp(String parName, String parValue)
        throws TemplateValueParseException {
        ConstraintOp ret = ConstraintOp.getConstraintOp(CodeTranslator.getCode(parValue));
        if (parValue != null && ret == null) {
            throw new TemplateValueParseException (
                    "Problem with parameter '" + parName
                    + "': '" + parValue + "' is not a valid operator.");
        }
        return ret;
    }

    private static boolean isPresent(String value) {
        return (value != null && value.length() > 0);
    }

    private static boolean isPresent(ConstraintOp op) {
        return (op != null);
    }

    /**
     * Given a HTTP request, parse out the template values.
     *
     * A template value is expected to be encoded such as:
     * <pre><code>
     *   constraintX=Gene
     *   opX=LOOKUP
     *   valueX=eve
     *   extraX=D.%20melanogaster
     * </code></pre>
     * where X is an integer from 1 - 25. The only element that may be omitted is
     * the "extra" parameter where none is expected.
     *
     * @param request HTTP request by user
     * @return map of constraints and values to be used to populate template.
     * @throws TemplateValueParseException if the request parameters are bad.
     */
    public static Map<String, List<ConstraintInput>> parseConstraints(HttpServletRequest request)
        throws TemplateValueParseException {
        // Maximum number of constraints is determined by the valid code range
        // on PathQueries.
        Map<String, List<ConstraintInput>> ret = new HashMap<String, List<ConstraintInput>>();
        Set<String> processedIds = new HashSet<String>();
        for (int i = 1; i <= PathQuery.MAX_CONSTRAINTS; i++) {

            String idParameter = ID_PARAMETER + i;
            String id = request.getParameter(idParameter);
            processedIds.add(idParameter);

            String opParameter = OPERATION_PARAMETER + i;
            String opString = request.getParameter(opParameter);
            ConstraintOp op = getConstraintOp(opParameter, opString);

            String valueParameter = VALUE_PARAMETER + i;
            String[] values = request.getParameterValues(valueParameter);
            String value = null;
            List<String> multivalues = null;
            if (values != null) {
                value = values[0];
                multivalues = Arrays.asList(values);
            }

            String extraParameter = EXTRA_PARAMETER + i;
            String extraValue = request.getParameter(extraParameter);

            String codeParameter = CODE_PARAMETER + i;
            String code = request.getParameter(codeParameter);

            if (opString != null && opString.length() > 0 && op == null) {
                throw new TemplateValueParseException("invalid parameter: '"
                    + opParameter + "' with value '" + opString + "': "
                    + "This must be valid operation code. "
                    + "Special characters must be encoded in request. "
                    + " See help for 'url encoding'.");
            }

            if (isPresent(op) || isPresent(value) || isPresent(id)
                    || isPresent(extraValue) || isPresent(code)
                    || multivalues != null) {
                String problemIntro =
                    "parameters were provided for constraint " + i;
                if (!isPresent(id)) {
                    throw new TemplateValueParseException(problemIntro
                        + " but no path was provided to identify the "
                        + "constraint. Missing parameter: '"
                        + idParameter + "'.");
                }
                if (!isPresent(op)) {
                    throw new TemplateValueParseException(problemIntro
                        + " but the operation was not specified."
                        + " Missing parameter '" + opParameter + "'.");
                }
                if (!PathConstraintNull.VALID_OPS.contains(op)
                    && (request.getParameterValues(valueParameter) == null)) {
                    throw new TemplateValueParseException(problemIntro
                        + " but no values were provided, and " + op
                        + " requires at least one value. Missing"
                        + " parameter '" + valueParameter + "'.");
                }
                if (!PathConstraintMultiValue.VALID_OPS.contains(op)
                    && multivalues != null && multivalues.size() > 1) {
                    throw new TemplateValueParseException(
                        " An operation was provided ('" + op + "') "
                        + " that expected at most one value, but "
                        + multivalues.size()
                        + " values were provided using the parameter '"
                        + valueParameter + "'.");
                }

                ConstraintInput load = new ConstraintInput(idParameter,
                        id, code, op, value, multivalues, extraValue);
                if (ret.get(id) == null) {
                    ret.put(id, new ArrayList<ConstraintInput>());
                }
                ret.get(id).add(load);
            }
        }
        // Make sure there aren't any extra parameters hanging around.
        // Use the id parameters (eg. constraint1, constraint2, ...) as a proxy
        // for the whole constraint.
        Set<String> allIdParameters = new HashSet<String>();
        for (Enumeration<?> e = request.getParameterNames(); e.hasMoreElements();) {
            String next = (String) e.nextElement();
            if (next.startsWith("constraint")) {
                allIdParameters.add(next);
            }
        }
        allIdParameters.removeAll(processedIds);
        if (allIdParameters.size() > 0) {
            throw new TemplateValueParseException("Maximum number of template parameters ("
                    + PathQuery.MAX_CONSTRAINTS
                    + ") exceeded. "
                    + "The extra values were :"
                    + allIdParameters);
        }
        return ret;
    }

    /**
     * An exception that we throw when we can't parse a template.
     * @author Alex Kalderimis
     *
     */
    public static class TemplateValueParseException extends Exception
    {
        private static final long serialVersionUID = -6128402589193631537L;

        /**
         * Construct an exception with a message
         * @param message The message, obvs.
         */
        public TemplateValueParseException(String message) {
            super(message);
        }
    }


    /**
     * Creates a map from input to be used later to populate the template.
     *
     * @param template template
     * @param input values from URL
     * @return map from constraints to values
     * @throws TemplateValueParseException if the input is bad.
     */
    public static Map<String, List<TemplateValue>> getValuesFromInput(TemplateQuery template,
            TemplateResultInput input) throws TemplateValueParseException {
        Map<String, List<TemplateValue>> values = new HashMap<String, List<TemplateValue>>();
        for (String path : template.getEditablePaths()) {
            List<PathConstraint> constraintsForPath = template.getEditableConstraints(path);
            List<ConstraintInput> inputsForPath = new ArrayList<ConstraintInput>();
            if (input.getConstraints().get(path) != null) {
                inputsForPath.addAll(input.getConstraints().get(path));
            }

            // too many inputs for path
            if (constraintsForPath.size() < inputsForPath.size()) {
                throw new TemplateValueParseException("There were more constraints specified "
                        + " in the request than there are editable constraints for path "
                        + path + ".");
            }

            if (constraintsForPath.size() == 1) {
                // one constraint and at most one input
                PathConstraint con = constraintsForPath.get(0);
                ConstraintInput conInput = null;
                if (!inputsForPath.isEmpty()) {
                    conInput = inputsForPath.get(0);
                }
                checkAndAddValue(values, template, con, conInput, null);
            } else {
                // more than one constraint so we need to look at codes
                for (PathConstraint con : constraintsForPath) {
                    ConstraintInput foundConInput = null;
                    String code = template.getConstraints().get(con);
                    for (ConstraintInput conInput : inputsForPath) {
                        if (StringUtils.isBlank(conInput.getCode())) {
                            String err = "There are multiple editable constraints"
                                + "for path " + path + " but codes weren't set.  If there is"
                                + " more than one constraint on a path you need to specify the"
                                + " corresponding constraint codes.";
                            throw new TemplateValueParseException(err);
                        }
                        if (conInput.getCode().equals(code)) {
                            if (foundConInput != null) {
                                String err = "There was more than one constraint"
                                    + " specified with code: " + conInput.getCode()
                                    + " in the request for path: " + path + "  You should only"
                                    + " provide one value per code.";
                                throw new TemplateValueParseException(err);
                            }
                            foundConInput = conInput;
                        }
                    }
                    // foundConInput may be null but that's ok if the constraint is optional
                    checkAndAddValue(values, template, con, foundConInput, code);
                }
            }
        }
        return values;
    }

    private static void checkAndAddValue(Map<String, List<TemplateValue>> values,
                                           TemplateQuery template,
                                           PathConstraint con,
                                           ConstraintInput conInput,
                                           String code)
        throws TemplateValueParseException {
        if (conInput != null) {
            if (template.isRequired(con)) {
                addToValuesMap(values, createTemplateValue(con, conInput, SwitchOffAbility.LOCKED));
            } else {
                addToValuesMap(values, createTemplateValue(con, conInput, SwitchOffAbility.ON));
            }
        } else if (template.isRequired(con)) {
            throw new TemplateValueParseException("There isn't a specified constraint value "
                    + "and operation for path " + con.getPath()
                    + ((code != null) ? " and code " + code : "")
                    + " in the request; this constraint is required.");
        } else {
            // no value was provided but the constraint was optional so we can do nothing
        }
    }

    private static void addToValuesMap(Map<String, List<TemplateValue>> valMap,
            TemplateValue newValue) {
        String path = newValue.getConstraint().getPath();
        List<TemplateValue> values = valMap.get(path);
        if (values == null) {
            values = new ArrayList<TemplateValue>();
            valMap.put(path, values);
        }
        values.add(newValue);
    }

    private static TemplateValue createTemplateValue(PathConstraint con, ConstraintInput input,
            SwitchOffAbility switchOffAbility) {
        TemplateValue value;
        if (PathConstraintBag.VALID_OPS.contains(input.getConstraintOp())) {
            value = new TemplateValue(con, input.getConstraintOp(), input.getValue(),
                TemplateValue.ValueType.BAG_VALUE, switchOffAbility);
        } else if (con instanceof PathConstraintLookup) {
            value = new TemplateValue(con, input.getConstraintOp(), input.getValue(),
                    TemplateValue.ValueType.SIMPLE_VALUE, input.getExtraValue(), switchOffAbility);
        } else if (con instanceof PathConstraintBag) {
            value = new TemplateValue(con, input.getConstraintOp(), input.getValue(),
                    TemplateValue.ValueType.BAG_VALUE, switchOffAbility);
        } else {
            if (PathConstraintMultiValue.VALID_OPS.contains(input.getConstraintOp())) {
                value = new TemplateValue(con, input.getConstraintOp(),
                        TemplateValue.ValueType.SIMPLE_VALUE,
                        input.getMultivalues(), switchOffAbility);
            } else if (input.getValue() != null) {
                value = new TemplateValue(con, input.getConstraintOp(), input.getValue(),
                    TemplateValue.ValueType.SIMPLE_VALUE, switchOffAbility);
            } else {
                // For unary (null) constraints.
                value = new TemplateValue(con, input.getConstraintOp(),
                    TemplateValue.ValueType.SIMPLE_VALUE, switchOffAbility);
            }
        }
        return value;
    }

}
