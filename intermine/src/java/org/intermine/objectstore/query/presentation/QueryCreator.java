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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.flymine.objectstore.query.*;

/**
 *
 * @author Richard Smith
 */

public class QueryCreator
{
    private static final String DATE_FORMAT = "dd/MM/yyyy";
    private Query q;

    /**
     *
     * @param q query
     */
    public QueryCreator(Query q) {
        this.q = q;
    }

    /**
     * Returns a ConstraintSet for the query, either existing or newly created.
     * If the query has a constraint that is not a ConstraintSet will return
     * a new ConstraintSet with the original constraint added to it.
     *
     * @return the query ConstraintSet
     */
    private ConstraintSet getConstraintSet() {
        ConstraintSet constraints;
        Constraint c = q.getConstraint();
        if (c == null) {
            constraints = new ConstraintSet(ConstraintSet.AND);
        }  else if (c instanceof ConstraintSet) {
            constraints = (ConstraintSet) c;
        } else {
            constraints = new ConstraintSet(ConstraintSet.AND);
            constraints.addConstraint(c);
        }
        return constraints;
    }


    /**
     *
     * @param clsName class name
     * @param fields field map
     * @throws Exception if it goes wrong
     */
    public void generateConstraints(String clsName, Map fields)
        throws Exception {

        QueryClass qc = new QueryClass(Class.forName(clsName));
        q.addFrom(qc);
        ConstraintSet constraints = getConstraintSet();

        Iterator iter = fields.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry fieldEntry = (Map.Entry) iter.next();
            if (!(((String) fieldEntry.getValue()).equals(""))) {
                QueryField qf = new QueryField(qc, (String) fieldEntry.getKey());
                QueryValue qv = createQueryValue(qf.getType(), (String) fieldEntry.getValue());
                SimpleConstraint sc = new SimpleConstraint(qf, SimpleConstraint.EQUALS, qv);
                constraints.addConstraint(sc);
            }
        }
        q.setConstraint(constraints);
    }


    private QueryValue createQueryValue(Class type, String value)
        throws IllegalArgumentException, ParseException {

        QueryValue qv = null;

        if (type.equals(Integer.class)) {
            qv = new QueryValue(new Integer(Integer.parseInt(value)));
        } else if (type.equals(Float.class)) {
            qv = new QueryValue(new Float(Float.parseFloat(value)));
        } else if (type.equals(Double.class)) {
            qv = new QueryValue(new Double(Double.parseDouble(value)));
        } else if (type.equals(Boolean.class)) {
            qv = new QueryValue(Boolean.valueOf(value));
        } else if (type.equals(Date.class)) {
            DateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
            qv = new QueryValue(formatter.parse(value));
        } else if (type.equals(String.class)) {
            qv = new QueryValue(value);
        } else {
            throw new IllegalArgumentException("Invalid type for QueryValue: " + type);
        }
        return qv;
    }
}
