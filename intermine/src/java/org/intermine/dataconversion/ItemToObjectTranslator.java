package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.flymine.metadata.Model;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.model.fulldata.Item;
import org.flymine.model.fulldata.Attribute;
import org.flymine.model.fulldata.Reference;
import org.flymine.model.fulldata.ReferenceList;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.proxy.ProxyReference;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryNode;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.Constraint;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.objectstore.query.BagConstraint;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.SingletonResults;
import org.flymine.objectstore.translating.Translator;
import org.flymine.ontology.OntologyUtil;
import org.flymine.util.DynamicUtil;
import org.flymine.util.TypeUtil;
import org.flymine.util.StringUtil;

/**
 * Translator that translates fulldata Items to business objects
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class ItemToObjectTranslator extends Translator
{
    protected Model model;
    
    /**
     * Constructor
     * @param model the Model used in business object creation
     */
    public ItemToObjectTranslator(Model model) {
        this.model = model;
    }
    
    /**
     * @see Translator#translateQuery
     */
    public Query translateQuery(Query query) throws ObjectStoreException {
        if (query.getOrderBy().size() > 0 || query.getGroupBy().size() > 0) {
            throw new ObjectStoreException("Query cannot be translated: " + query);
        }

        List select = query.getSelect();
        Set from = query.getFrom();
        QueryNode qn = (QueryNode) select.get(0);
        if (!(select.size() == 1
              && from.size() == 1
              && qn == from.iterator().next()
              && qn instanceof QueryClass
              && ((QueryClass) qn).getType().equals(FlyMineBusinessObject.class))) {
            throw new ObjectStoreException("Query cannot be translated: " + query);
        }
        
        Query q = new Query();
        QueryClass qc = new QueryClass(Item.class);
        q.addToSelect(qc);
        q.addFrom(qc);
        
        Constraint constraint = query.getConstraint();
        if (constraint != null) {
            if (constraint instanceof BagConstraint
                && constraint.getOp() == ConstraintOp.IN
                && ((BagConstraint) constraint).getQueryNode() instanceof QueryField
                && ((QueryField) ((BagConstraint) constraint).getQueryNode()).getFromElement() == qn
                && ((QueryField) ((BagConstraint) constraint).getQueryNode())
                .getFieldName().equals("id")) {
                BagConstraint bc =
                    new BagConstraint(new QueryField(qc, "identifier"),
                                      ConstraintOp.IN,
                                      toStrings(((BagConstraint) constraint).getBag()));
                q.setConstraint(bc);
            } else if (constraint instanceof SimpleConstraint
                       && constraint.getOp() == ConstraintOp.EQUALS
                       && ((SimpleConstraint) constraint).getArg1() instanceof QueryField
                       && ((QueryField) ((SimpleConstraint) constraint).getArg1())
                           .getFromElement() == qn
                       && ((QueryField) ((SimpleConstraint) constraint).getArg1())
                       .getFieldName().equals("id")) {
                SimpleConstraint sc =
                    new SimpleConstraint(new QueryField(qc, "identifier"),
                         ConstraintOp.EQUALS, 
                          new QueryValue(((QueryValue) ((SimpleConstraint) constraint)
                                                         .getArg2()).getValue().toString()));
                q.setConstraint(sc);
            } else {
                throw new ObjectStoreException("Query cannot be translated: " + query);
            }
        }
        
        return q;
    }
    
    /**
     * @see Translator#translateToDbObject
     */
    public FlyMineBusinessObject translateToDbObject(FlyMineBusinessObject o) {
        return o;
    }
    
    /**
     * @see Translator#translateFromDbObject
     */
    public FlyMineBusinessObject translateFromDbObject(FlyMineBusinessObject o) {
        if (!(o instanceof Item)) {
            return o;
        }
        
        Item item = (Item) o;
        FlyMineBusinessObject obj = (FlyMineBusinessObject)
            DynamicUtil.instantiateObject(
                OntologyUtil.generateClassNames(item.getClassName(), model),
                OntologyUtil.generateClassNames(item.getImplementations(), model));

        obj.setId(Integer.valueOf(item.getIdentifier()));
        
        try {
            for (Iterator i = item.getAttributes().iterator(); i.hasNext();) {
                Attribute attr = (Attribute) i.next();
                Class attrClass = TypeUtil.getFieldInfo(obj.getClass(), attr.getName()).getType();
                if (!attr.getName().equalsIgnoreCase("id")) {
                    Object value = TypeUtil.stringToObject(attrClass, attr.getValue());
                    TypeUtil.setFieldValue(obj, attr.getName(), value);
                }
            }
            
            for (Iterator i = item.getReferences().iterator(); i.hasNext();) {
                Reference ref = (Reference) i.next();
                Integer identifier = new Integer(ref.getRefId());
                TypeUtil.setFieldValue(obj, ref.getName(), new ProxyReference(os, identifier));
            }
            
            for (Iterator i = item.getCollections().iterator(); i.hasNext();) {
                ReferenceList refs = (ReferenceList) i.next();
                QueryClass qc = new QueryClass(FlyMineBusinessObject.class);
                QueryField qf = new QueryField(qc, "id");
                BagConstraint bc =
                    new BagConstraint(qf, ConstraintOp.IN,
                        toIntegers(new HashSet(StringUtil.tokenize(refs.getRefIds()))));
                Query q = new Query();
                q.addToSelect(qc);
                q.addFrom(qc);
                q.setConstraint(bc);
                TypeUtil.setFieldValue(obj, refs.getName(), new SingletonResults(q, os,
                            os.getSequence()));
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return obj;
    }

    /**
     * Convert a set of Integers to a set of String using toString()
     * @param integers a set of Integers
     * @return the corresponding set of Strings
     */
    protected Set toStrings(Set integers) {
        Set strings = new HashSet();
        for (Iterator i = integers.iterator(); i.hasNext();) {
            strings.add(i.next().toString());
        }
        return strings;
    }

    /**
     * Convert a set of Strings to a set of Integers using valueOf()
     * @param strings a set of Strings
     * @return the corresponding set of Integers
     */
    protected Set toIntegers(Set strings) {
        Set integers = new HashSet();
        for (Iterator i = strings.iterator(); i.hasNext();) {
            integers.add(Integer.valueOf(i.next().toString()));
        }
        return integers;
        }
}
