package org.flymine.objectstore.query.presentation;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.Iterator;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.flymine.objectstore.query.*;
import org.flymine.FlyMineException;

/**
 * Static methods to assist with query generation from front end.
 *
 * @author Richard Smith
 */
public class QueryCreator
{
    protected static final String DATE_FORMAT = "dd/MM/yyyy";

    /**
     * Add a QueryClass to the from list of query and add constraints
     * generated from a map of fields/values.
     *
     * @param q a query to add QueryClass and constraints to
     * @param clsName name of class to add to query
     * @param fieldValues map of fieldname/value to build constraints from
     * @param fieldOps map of fieldname/operation to build constraints from
     * @throws FlyMineException if an error occurs
     * @throws NullPointerException if any of the parameters are null
     */
    public static void addToQuery(Query q, String clsName, Map fieldValues, Map fieldOps)
        throws FlyMineException {
        if (q == null) {
            throw new NullPointerException("Query parameter is null");
        } else if (clsName == null || clsName.equals("")) {
            throw new NullPointerException("clsName parameter is null");
        } else if (fieldValues == null) {
            throw new NullPointerException("fieldValues parameter is null");
        } else if (fieldOps == null) {
            throw new NullPointerException("fieldOps parameter is null");
        } 

        try {
            QueryClass qc = new QueryClass(Class.forName(clsName));
            q.addFrom(qc);
            addConstraint(q, generateConstraints(qc, fieldValues, fieldOps));
        } catch (Exception e) {
            throw new FlyMineException("Problem occurred adding class (" + clsName
                                + ") to query: " + e);
        }
    }

    /**
     * Generate ConstraintSet of SimpleConstraints for a QueryClass from a map of field/value pairs
     *
     * @param qc QueryClass to constrain
     * @param fieldValues map of fieldname/value to build constraints from
     * @param fieldOps map of fieldname/operation to build constraints from
     * @return a populated ConstraintSet
     * @throws Exception if it goes wrong
     */
    protected static ConstraintSet generateConstraints(QueryClass qc, Map fieldValues,
                                                       Map fieldOps) throws Exception {
        ConstraintSet constraints = new ConstraintSet(ConstraintSet.AND);
        Iterator iter = fieldValues.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry fieldEntry = (Map.Entry) iter.next();
            String fieldValue = (String) fieldEntry.getValue();
            if (!"".equals(fieldValue)) {
                String fieldName = (String) fieldEntry.getKey();
                int fieldOp = Integer.parseInt((String) fieldOps.get(fieldName));
                QueryField qf = new QueryField(qc, fieldName);
                QueryValue qv = createQueryValue(qf.getType(), fieldValue);
                SimpleConstraint sc = new SimpleConstraint(qf, fieldOp, qv);
                constraints.addConstraint(sc);
            }
        }
        return constraints;
    }

    /**
     * Adds the constraints in a ConstraintSet to those present in a Query
     *
     * @param q the query in question
     * @param constraints the new constraints
     */
    protected static void addConstraint(Query q, ConstraintSet constraints) {
        if (constraints == null) {
            throw new NullPointerException("constraints cannot be null");
        }
        if (constraints.getConstraints().size() > 0) {
            Constraint c = q.getConstraint();
            if (c == null) {
                q.setConstraint(constraints);
            }  else if (c instanceof ConstraintSet) {
                ((ConstraintSet) c).addConstraint(constraints);
            }  else { // any other type of constraint
                constraints.addConstraint(c);
                q.setConstraint(constraints);
            }
        }
    }

    /**
     * Create a QueryValue by parsing a string for the appropriate class type
     * (common java.lang complex types and java.util.Date supported).
     *
     * @param type java type of the QueryValue to be created
     * @param value string to be parsed for value
     * @return a new QueryValue
     * @throws IllegalArgumentException if type is not supported
     * @throws ParseException if an error occurs parsing date string
     */
    protected static QueryValue createQueryValue(Class type, String value)
        throws IllegalArgumentException, ParseException {
        QueryValue qv = null;
        if (type.equals(Integer.class)) {
            qv = new QueryValue(new Integer(Integer.parseInt(value)));
        } else if (type.equals(Float.class)) {
            qv = new QueryValue(new Float(Float.parseFloat(value)));
        } else if (type.equals(Double.class)) {
            qv = new QueryValue(new Double(Double.parseDouble(value)));
        } else if (type.equals(Long.class)) {
            qv = new QueryValue(new Long(Long.parseLong(value)));
        } else if (type.equals(Short.class)) {
            qv = new QueryValue(new Short(Short.parseShort(value)));
        } else if (type.equals(Boolean.class)) {
            qv = new QueryValue(Boolean.valueOf(value));
        } else if (type.equals(Date.class)) {
            qv = new QueryValue(new SimpleDateFormat(DATE_FORMAT).parse(value));
        } else if (type.equals(String.class)) {
            qv = new QueryValue(value);
        } else {
            throw new IllegalArgumentException("Invalid type for QueryValue: " + type);
        }
        return qv;
    }
}
