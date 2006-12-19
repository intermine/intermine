package org.intermine.web.bag;

import java.util.Collection;
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
import org.intermine.web.ClassKeyHelper;

/**
 * @author Richard Smith
 *
 */
public class BagQueryHelper {

	
	public static BagQuery createDefaultBagQuery(String type, Map classKeys, 
			Model model, Set input) throws ClassNotFoundException {
		
		Class cls = Class.forName(model.getPackageName() + "." + type);
		if (!ClassKeyHelper.hasKeyFields(classKeys, type)) {
			throw new IllegalArgumentException("Internal error - no key fields found for type: " + type + ".");
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
			throw new IllegalArgumentException("Internal error - could not find any usable key fields for type: " + type + ".");
		}

		BagQuery bq = new BagQuery(q, "default", false);
		return bq;
	}
}
