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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.flymine.metadata.Model;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.model.fulldata.Item;
import org.flymine.model.fulldata.Attribute;
import org.flymine.model.fulldata.Reference;
import org.flymine.model.fulldata.ReferenceList;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.proxy.ProxyReference;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryCast;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryExpression;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryFunction;
import org.flymine.objectstore.query.QueryNode;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.Constraint;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.objectstore.query.BagConstraint;
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.SingletonResults;
import org.flymine.objectstore.translating.Translator;
import org.flymine.ontology.OntologyUtil;
import org.flymine.util.DynamicUtil;
import org.flymine.util.TypeUtil;
import org.flymine.util.StringUtil;

import org.apache.log4j.Logger;

/**
 * Translator that translates fulldata Items to business objects
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class ItemToObjectTranslator extends Translator
{
    protected static final Logger LOG = Logger.getLogger(ItemToObjectTranslator.class);

    protected Model model;
    protected SortedMap idToNamespace = new TreeMap();
    protected Map namespaceToId = new HashMap();
    
    /**
     * Constructor
     * @param model the Model used in business object creation
     * @param os an ObjectStore containing the Item data that we will be translating. Note that
     * the contents of the objectstore must remain constant for IDs to not clash - newly written
     * objects will not be accessible by their ID. This translator assumes that the Item identifier
     * field contains some string, an underscore, then some number with no leading zeros.
     * @throws ObjectStoreException if the initialisation query fails
     */
    public ItemToObjectTranslator(Model model, ObjectStore os) throws ObjectStoreException {
        this.model = model;
        Query q = new Query();
        QueryClass qc = new QueryClass(Item.class);
        q.addFrom(qc);
        QueryField qf = new QueryField(qc, "identifier");
        QueryExpression qe1 = new QueryExpression(qf, QueryExpression.INDEX_OF,
                new QueryValue("_"));
        QueryExpression qe2 = new QueryExpression(qe1, QueryExpression.SUBTRACT,
                new QueryValue(new Integer(1)));
        QueryExpression qe3 = new QueryExpression(qf, new QueryValue(new Integer(1)), qe2);
        QueryExpression qe4 = new QueryExpression(qe1, QueryExpression.ADD,
                new QueryValue(new Integer(1)));
        QueryExpression qe5 = new QueryExpression(qf, QueryExpression.SUBSTRING, qe4);
        QueryCast qca = new QueryCast(qe5, Integer.class);
        QueryFunction qfu = new QueryFunction(qca, QueryFunction.MAX);
        q.addToSelect(qe3);
        q.addToSelect(qfu);
        q.addToGroupBy(qe3);
        q.setDistinct(false);
        if (os != null) {
            List res = os.execute(q);
            int offset = 0;
            Iterator iter = res.iterator();
            while (iter.hasNext()) {
                ResultsRow row = (ResultsRow) iter.next();
                String namespace = (String) row.get(0);
                idToNamespace.put(new Integer(offset), namespace);
                namespaceToId.put(namespace, new Integer(offset));
                LOG.error("Assigning namespace " + namespace + " to " + offset);
                int highest = ((Integer) row.get(1)).intValue();
                offset += highest;
            }
        }
    }

    private String idToIdentifier(Integer id) {
        if (id == null) {
            return null;
        }
        String namespace = (String) idToNamespace.get(id);
        if (namespace != null) {
            return namespace + "_0";
        } else {
            Integer baseInteger = (Integer) idToNamespace.headMap(id).lastKey();
            namespace = (String) idToNamespace.get(baseInteger);
            int base = baseInteger.intValue();
            return namespace + "_" + (id.intValue() - base);
        }
    }

    private Integer identifierToId(String identifier) {
        if (identifier == null) {
            return null;
        }
        String namespace = identifier.substring(0, identifier.indexOf("_"));
        int base = ((Integer) namespaceToId.get(namespace)).intValue();
        Integer retval = new Integer(base + Integer.parseInt(identifier.substring(identifier
                        .indexOf("_") + 1)));
        return retval;
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
                       && ((SimpleConstraint) constraint).getArg2() instanceof QueryValue
                       && ((QueryField) ((SimpleConstraint) constraint).getArg1())
                           .getFromElement() == qn
                       && ((QueryField) ((SimpleConstraint) constraint).getArg1())
                       .getFieldName().equals("id")) {
                SimpleConstraint sc =
                    new SimpleConstraint(new QueryField(qc, "identifier"),
                            ConstraintOp.EQUALS, 
                            new QueryValue(idToIdentifier((Integer) (((QueryValue)
                                        ((SimpleConstraint) constraint).getArg2())).getValue())));
                q.setConstraint(sc);
            } else {
                throw new ObjectStoreException("Query cannot be translated: " + query);
            }
        }
        q.setDistinct(query.isDistinct());
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

        obj.setId(identifierToId(item.getIdentifier()));
        
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
                Integer identifier = identifierToId(ref.getRefId());
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
     * Convert a set of Integers to a set of String using idToIdentifier()
     * @param integers a set of Integers
     * @return the corresponding set of Strings
     */
    protected Set toStrings(Set integers) {
        Set strings = new HashSet();
        for (Iterator i = integers.iterator(); i.hasNext();) {
            strings.add(idToIdentifier((Integer) i.next()));
        }
        return strings;
    }

    /**
     * Convert a set of Strings to a set of Integers using identifierToId()
     * @param strings a set of Strings
     * @return the corresponding set of Integers
     */
    protected Set toIntegers(Set strings) {
        Set integers = new HashSet();
        for (Iterator i = strings.iterator(); i.hasNext();) {
            integers.add(identifierToId((String) i.next()));
        }
        return integers;
    }
}
