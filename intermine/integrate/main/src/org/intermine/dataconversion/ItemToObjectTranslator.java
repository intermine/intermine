package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import org.intermine.metadata.Model;
import org.intermine.metadata.MetaDataException;
import org.intermine.model.InterMineObject;
import org.intermine.model.fulldata.Item;
import org.intermine.model.fulldata.Attribute;
import org.intermine.model.fulldata.Reference;
import org.intermine.model.fulldata.ReferenceList;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCast;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.translating.Translator;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil.FieldInfo;

import org.apache.log4j.Logger;

/**
 * Translator that translates fulldata Items to business objects
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class ItemToObjectTranslator extends Translator
{
    private static final Logger LOG = Logger.getLogger(ItemToObjectTranslator.class);

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
        try {
            if (os != null) {
                List res = os.execute(q);
                int offset = 0;
                Iterator iter = res.iterator();
                while (iter.hasNext()) {
                    ResultsRow row = (ResultsRow) iter.next();
                    String namespace = (String) row.get(0);
                    idToNamespace.put(new Integer(offset), namespace);
                    namespaceToId.put(namespace, new Integer(offset));
                    int highest = ((Integer) row.get(1)).intValue();
                    offset += highest + 1;
                }
            }
        } catch (Exception e) {
            throw new ObjectStoreException(e);
        }
        LOG.info("Namespace map: " + namespaceToId);
    }

    /**
     * Turn an object id into an item identifier.
     * @param id an InterMineObject id
     * @return the corresponding item identifier
     */
    public String idToIdentifier(Integer id) {
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

    /**
     * Turn an item identifier into an object id.
     * @param identifier an item identifier
     * @return the corresponding InterMineObject id
     */
    public Integer identifierToId(String identifier) {
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
     * {@inheritDoc}
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
              && qn instanceof QueryClass)) {
              //&& ((QueryClass) qn).getType().equals(InterMineObject.class))) {
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

        // if not querying for InterMineObject constrain Item className
        if (!((QueryClass) qn).getType().equals(InterMineObject.class)) {
            String clsName = ((QueryClass) qn).getType().getName();
            String clsURI = model.getNameSpace() + TypeUtil.unqualifiedName(clsName);
            SimpleConstraint clsNameConstraint = new SimpleConstraint(
                                new QueryField(qc, "className"), ConstraintOp.EQUALS,
                                new QueryValue(clsURI));
            if (q.getConstraint() != null) {
                ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                cs.addConstraint(q.getConstraint());
                cs.addConstraint(clsNameConstraint);
                q.setConstraint(cs);
            } else {
                q.setConstraint(clsNameConstraint);
            }
        }

        q.setDistinct(query.isDistinct());
        return q;
    }

    /**
     * {@inheritDoc}
     */
    public InterMineObject translateToDbObject(InterMineObject o) {
        return o;
    }

    /**
     * {@inheritDoc}
     */
    public InterMineObject translateFromDbObject(InterMineObject o)
        throws MetaDataException {
        if (!(o instanceof Item)) {
            return o;
        }

        Item item = (Item) o;
        int itemSize = 100;
        Iterator iter = item.getAttributes().iterator();
        try {
            while (iter.hasNext()) {
                itemSize += ((Attribute) iter.next()).getValue().length() + 50;
            }
        } catch (NullPointerException e) {
            LOG.error("An Attribute caused a NullPointerException!", e);
            throw e;
        }
        iter = item.getCollections().iterator();
        while (iter.hasNext()) {
            itemSize += ((ReferenceList) iter.next()).getRefIds().length() + 50;
        }
        itemSize += item.getReferences().size() * 50;
        if (itemSize > 1000000) {
            LOG.info("Translating large object " + item.getIdentifier() + " ("
                    + identifierToId(item.getIdentifier()) + ") - classname = "
                    + item.getClassName() + ", size = " + itemSize);
        }
        InterMineObject obj = (InterMineObject)
            DynamicUtil.instantiateObject(
                OntologyUtil.generateClassNames(item.getClassName(), model),
                OntologyUtil.generateClassNames(item.getImplementations(), model));

        obj.setId(identifierToId(item.getIdentifier()));

        try {
            for (Iterator i = item.getAttributes().iterator(); i.hasNext();) {
                Attribute attr = (Attribute) i.next();
                FieldInfo info = TypeUtil.getFieldInfo(obj.getClass(), attr.getName());
                if (info == null) {
                    String message = "Attribute not found in model: "
                                                + DynamicUtil.decomposeClass(obj.getClass())
                                                + "." + attr.getName() + " - Attribute id = "
                                                + attr.getId() + ", attributes = "
                                                + item.getAttributes();
                    LOG.error(message);
                    throw new MetaDataException(message);
                }
                Class attrClass = info.getType();
                if (!attr.getName().equalsIgnoreCase("id")) {
                    Object value = TypeUtil.stringToObject(attrClass, attr.getValue());
                    TypeUtil.setFieldValue(obj, attr.getName(), value);
                }
            }

            for (Iterator i = item.getReferences().iterator(); i.hasNext();) {
                Reference ref = (Reference) i.next();
                Integer identifier = identifierToId(ref.getRefId());
                String refName = ref.getName();
                if (Character.isLowerCase(refName.charAt(1))) {
                    refName = StringUtil.decapitalise(refName);
                }
                if (TypeUtil.getFieldInfo(obj.getClass(), refName) != null) {
                    TypeUtil.setFieldValue(obj, refName, new ProxyReference(os, identifier,
                                InterMineObject.class));
                } else {
                    String message = "Reference not found in model: "
                        + DynamicUtil.decomposeClass(obj.getClass())
                        + "." + ref.getName();
                    LOG.error(message);
                    throw new MetaDataException(message);
                }
            }

            for (Iterator i = item.getCollections().iterator(); i.hasNext();) {
                ReferenceList refs = (ReferenceList) i.next();
                QueryClass qc = new QueryClass(InterMineObject.class);
                QueryField qf = new QueryField(qc, "id");
                BagConstraint bc =
                    new BagConstraint(qf, ConstraintOp.IN,
                        toIntegers(new HashSet(StringUtil.tokenize(refs.getRefIds()))));
                Query q = new Query();
                q.addToSelect(qc);
                q.addFrom(qc);
                q.setConstraint(bc);
                // TODO rules about case changes should be centralised
                String refsName = refs.getName();
                if (Character.isLowerCase(refsName.charAt(1))) {
                    refsName = StringUtil.decapitalise(refsName);
                }
                if (TypeUtil.getFieldInfo(obj.getClass(), refsName) != null) {
                    TypeUtil.setFieldValue(obj, refsName, os.executeSingleton(q));
                } else {
                    String message = "Collection not found in model: "
                        + DynamicUtil.decomposeClass(obj.getClass())
                        + "." + refsName + " fileInfos: " + TypeUtil.getFieldInfos(obj.getClass());
                    LOG.error(message);
                    throw new MetaDataException(message);
                }
            }
        } catch (MetaDataException e) {
            LOG.error("Broken with: " + DynamicUtil.decomposeClass(obj.getClass())
                      + item.getIdentifier(), e);
            throw e;
        } catch (Exception e) {
            LOG.error("Broken with: " + DynamicUtil.decomposeClass(obj.getClass())
                      + item.getIdentifier(), e);
            throw new RuntimeException(e);
        }
        return obj;
    }

    /**
     * Convert a set of Integers to a set of String using idToIdentifier()
     * @param integers a set of Integers
     * @return the corresponding set of Strings
     */
    protected Collection toStrings(Collection integers) {
        Collection strings = new ArrayList();
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
    protected Collection toIntegers(Collection strings) {
        Collection integers = new ArrayList();
        for (Iterator i = strings.iterator(); i.hasNext();) {
            integers.add(identifierToId((String) i.next()));
        }
        return integers;
    }
}
