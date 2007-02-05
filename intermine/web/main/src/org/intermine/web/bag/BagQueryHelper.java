package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.util.SAXParser;
import org.intermine.web.ClassKeyHelper;

import java.io.InputStream;

import org.xml.sax.InputSource;

/**
 * @author Richard Smith
 *
 */
public class BagQueryHelper
{

    /**
     * Message associated with default bag query.
     */
    public static final String DEFAULT_MESSAGE = "searching key fields";

    public static BagQuery createDefaultBagQuery(String type, Map classKeys,
                                                 Model model, Collection input)
        throws ClassNotFoundException {

        Class cls = Class.forName(model.getPackageName() + "." + type);
        if (!ClassKeyHelper.hasKeyFields(classKeys, type)) {
            throw new IllegalArgumentException("Internal error - no key fields found for type: "
                                               + type + ".");
        }

        List lowerCaseInput = new ArrayList();
        Iterator inputIter = input.iterator();
        while (inputIter.hasNext()) {
            lowerCaseInput.add(((String) inputIter.next()).toLowerCase());
        }
        
        Query q = new Query();
        QueryClass qc = new QueryClass(cls);
        q.addFrom(qc);
        q.addToSelect(new QueryField(qc, "id"));

        ConstraintSet cs = new ConstraintSet(ConstraintOp.OR);
        q.setConstraint(cs);

        Collection keyFields = ClassKeyHelper.getKeyFields(classKeys, type);

        Iterator keyFieldIter = keyFields.iterator();
        while (keyFieldIter.hasNext()) {
            Set flds = (Set) keyFieldIter.next();
            if (flds.size() > 1) {
                continue;
            }
            FieldDescriptor fld = (FieldDescriptor) flds.iterator().next();
            if (!fld.isAttribute()) {
                continue;
            }

            QueryField qf = new QueryField(qc, fld.getName());
            QueryExpression qe = new QueryExpression(QueryExpression.LOWER, qf);
            // constrain field to be in a bag
            BagConstraint bc = new BagConstraint(qe, ConstraintOp.IN, lowerCaseInput);

            cs.addConstraint(bc);
            q.addToSelect(qf);
        }

        if (cs.getConstraints().size() == 0) {
            String message =
                "Internal error - could not find any usable key fields for type: " + type + ".";
            throw new IllegalArgumentException(message);
        }
        BagQuery bq = new BagQuery(q, DEFAULT_MESSAGE, false);
        return bq;
    }

    /**
     * Read the bag queries from the given stream.
     * @param model the Model to use to check the bag types
     * @param is the InputStream
     * @return a Map from type name to a List of BagQuerys to run for that type
     * @throws Exception if there is a problem parsing the bag-queries.xml
     */
    public static Map readBagQueries(Model model, InputStream is) throws Exception {
        BagQueryHandler handler = new BagQueryHandler(model);
        SAXParser.parse(new InputSource(is), handler);
        return handler.getBagQueries();
    }
}
