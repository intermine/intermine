package org.intermine.api.bag;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.api.config.ClassKeyHelper;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.MultipleInBagConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryEvaluable;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.util.SAXParser;
import org.intermine.util.TypeUtil;
import org.xml.sax.InputSource;

/**
 * @author Richard Smith
 *
 */
public final class BagQueryHelper
{
    private BagQueryHelper() {
        // nothing to do
    }

    /**
     * Message associated with default bag query.
     */
    public static final String DEFAULT_MESSAGE = "searching key fields";

    /**
     * Create a BagQuery that constrains the class key fields of the given type.
     * @param type the class to query for
     * @param bagQueryConfig The BagQueryConfig object used to get the extra class and field to
     * constrain when making the query (eg. constrain Bioentiry.organism.name = "something")
     * @param classKeys the class keys map
     * @param input the input strings/identifiers
     * @param model the Model to pass to the BagQuery constructor
     * @return a BagQuery that queries for objects of class given of type where any of the class
     * key fields match any of the input identifiers
     * @throws ClassNotFoundException if the type isn't in the model
     */
    public static Query createDefaultBagQuery(String type,
            BagQueryConfig bagQueryConfig,
            Model model, Map<String, List<FieldDescriptor>> classKeys,
            Collection<String> input) throws ClassNotFoundException {

        Class<?> cls = Class.forName(type);
        if (!ClassKeyHelper.hasKeyFields(classKeys, type)) {
            throw new IllegalArgumentException("Internal error - no key fields found for type: "
                                               + type + ".");
        }

        Map<Class<?>, Collection<Object>> bags = new HashMap<Class<?>, Collection<Object>>();
        Map<Class<?>, List<QueryEvaluable>> evaluables =
            new HashMap<Class<?>, List<QueryEvaluable>>();

        Query q = new Query();
        QueryClass qc = new QueryClass(cls);
        q.addFrom(qc);
        q.addToSelect(new QueryField(qc, "id"));

        for (FieldDescriptor fld : ClassKeyHelper.getKeyFields(classKeys, type)) {
            if (!fld.isAttribute()) {
                continue;
            }
            QueryEvaluable field = new QueryField(qc, fld.getName());
            Class<?> attType = field.getType();
            List<QueryEvaluable> qes = evaluables.get(attType);
            Collection<Object> bag = bags.get(attType);
            if (qes == null) {
                qes = new ArrayList<QueryEvaluable>();
                bag = new ArrayList<Object>();
                if (String.class.equals(attType)) {
                    for (String o : input) {
                        bag.add(o.toLowerCase());
                    }
                } else {
                    for (String o : input) {
                        try {
                            bag.add(TypeUtil.stringToObject(attType, o));
                        } catch (Exception e) {
                            // Ignore - this value does not parse to this type
                        }
                    }
                }
                evaluables.put(attType, qes);
                bags.put(attType, bag);
            }
            q.addToSelect(field);
            if (String.class.equals(attType)) {
                field = new QueryExpression(QueryExpression.LOWER, field);
            }
            qes.add(field);
        }

        if (evaluables.size() > 1) {
            ConstraintSet cs = new ConstraintSet(ConstraintOp.OR);
            for (Class<?> attType : evaluables.keySet()) {
                List<QueryEvaluable> qes = evaluables.get(attType);
                Collection<Object> bag = bags.get(attType);
                if (qes.size() > 1) {
                    cs.addConstraint(new MultipleInBagConstraint(bag, qes));
                } else {
                    cs.addConstraint(new BagConstraint(qes.get(0), ConstraintOp.IN, bag));
                }
            }
            q.setConstraint(cs);
        } else if (evaluables.isEmpty()) {
            throw new IllegalArgumentException("Internal Error - could not find any usable key "
                    + "fields for type " + type + ".");
        } else {
            for (Class<?> attType : evaluables.keySet()) {
                List<QueryEvaluable> qes = evaluables.get(attType);
                Collection<Object> bag = bags.get(attType);
                if (qes.size() > 1) {
                    q.setConstraint(new MultipleInBagConstraint(bag, qes));
                } else {
                    q.setConstraint(new BagConstraint(qes.get(0), ConstraintOp.IN, bag));
                }
            }
        }
        return q;
    }

    /**
     * Read the bag query configuration from the given stream.
     * @param model the Model to use to check the bag types
     * @param is the InputStream
     * @return the BagQueryConfig object for this webapp
     * @throws Exception if there is a problem parsing the bag-queries.xml
     */
    public static BagQueryConfig readBagQueryConfig(Model model, InputStream is) throws Exception {
        BagQueryHandler handler = new BagQueryHandler(model);
        SAXParser.parse(new InputSource(is), handler);
        return handler.getBagQueryConfig();
    }
}
