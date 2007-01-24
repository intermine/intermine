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

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.util.SAXParser;
import org.intermine.web.ClassKeyHelper;
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
            // constrain field to be in a bag
            BagConstraint bc = new BagConstraint(qf, ConstraintOp.IN, input);
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

    public static Map readBagQueries(Model model, InputStream is) throws Exception {
        BagQueryHandler handler = new BagQueryHandler(model);
        SAXParser.parse(new InputSource(is), handler);
        return handler.getBagQueries();
    }
}
