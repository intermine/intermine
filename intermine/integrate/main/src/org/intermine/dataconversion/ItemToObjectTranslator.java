package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.metadata.StringUtil;
import org.intermine.metadata.TypeUtil;
import org.intermine.metadata.TypeUtil.FieldInfo;
import org.intermine.metadata.Util;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.model.fulldata.Attribute;
import org.intermine.model.fulldata.Item;
import org.intermine.model.fulldata.Reference;
import org.intermine.model.fulldata.ReferenceList;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ClobAccess;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.FromElement;
import org.intermine.objectstore.query.PendingClob;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCast;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.translating.Translator;
import org.intermine.util.DynamicUtil;

/**
 * Translator that translates fulldata Items to business objects
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class ItemToObjectTranslator extends Translator
{
    private static final Logger LOG = Logger.getLogger(ItemToObjectTranslator.class);

    protected Model model;
    protected SortedMap<Integer, String> idToNamespace = new TreeMap<Integer, String>();
    protected Map<String, Integer> namespaceToId = new HashMap<String, Integer>();

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
                Results res = os.execute(q, 1000, false, false, false);
                int offset = 0;
                @SuppressWarnings("unchecked") Collection<ResultsRow<Object>> tmpRes =
                    (Collection) res;
                for (ResultsRow<Object> row : tmpRes) {
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
        String namespace = idToNamespace.get(id);
        if (namespace != null) {
            return namespace + "_0";
        } else {
            Integer baseInteger = idToNamespace.headMap(id).lastKey();
            namespace = idToNamespace.get(baseInteger);
            int base = baseInteger.intValue();
            return namespace + "_" + (id.intValue() - base);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object translateIdToIdentifier(Integer id) {
        return idToIdentifier(id);
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
        int index = identifier.indexOf("_");
        if (index == -1) {
            throw new RuntimeException("illegal identifier (\"" + identifier + "\") for item");
        }
        String namespace = identifier.substring(0, index);
        Integer objectId = namespaceToId.get(namespace);
        if (objectId == null) {
            throw new RuntimeException("namespace \"" + namespace + "\" not found");
        }
        int base = objectId.intValue();
        Integer retval = new Integer(base + Integer.parseInt(identifier.substring(index + 1)));
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query translateQuery(Query query) throws ObjectStoreException {
        if (query.getOrderBy().size() > 0 || query.getGroupBy().size() > 0) {
            throw new ObjectStoreException("Query cannot be translated: " + query);
        }

        List<QuerySelectable> select = query.getSelect();
        Set<FromElement> from = query.getFrom();
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
                && "id".equals(((QueryField) ((BagConstraint) constraint).getQueryNode())
                        .getFieldName())) {
                @SuppressWarnings("unchecked") Collection<Integer> bag =
                    (Collection<Integer>) ((BagConstraint) constraint).getBag();
                BagConstraint bc = new BagConstraint(new QueryField(qc, "identifier"),
                        ConstraintOp.IN, toStrings(bag));
                q.setConstraint(bc);
            } else if (constraint instanceof SimpleConstraint
                       && constraint.getOp() == ConstraintOp.EQUALS
                       && ((SimpleConstraint) constraint).getArg1() instanceof QueryField
                       && ((SimpleConstraint) constraint).getArg2() instanceof QueryValue
                       && ((QueryField) ((SimpleConstraint) constraint).getArg1())
                           .getFromElement() == qn
                       && "id".equals(((QueryField) ((SimpleConstraint) constraint).getArg1())
                               .getFieldName())) {
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
            String clsURI = TypeUtil.unqualifiedName(clsName);
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
    @Override
    public Object translateToDbObject(Object o) {
        return o;
    }

    private long timeSpentSizing = 0;
    private long timeSpentCreate = 0;
    private long timeSpentAttributes = 0;
    private int objectCount = 0;

    /**
     * {@inheritDoc}
     */
    @Override
    public Object translateFromDbObject(Object o) throws MetaDataException {
        long time1 = System.currentTimeMillis();
        if (!(o instanceof Item)) {
            return o;
        }

        Item item = (Item) o;
        int itemSize = 100;
        for (Attribute a : item.getAttributes()) {
            String value = a.getValue();
            if (value != null) {
                itemSize += value.length() + 50;
            } else {
                itemSize += 50;
            }
        }
        for (ReferenceList r : item.getCollections()) {
            itemSize += r.getRefIds().length() + 50;
        }
        itemSize += item.getReferences().size() * 50;
        if (itemSize > 1000000) {
            LOG.info("Translating large object " + item.getIdentifier() + " ("
                    + identifierToId(item.getIdentifier()) + ") - classname = "
                    + item.getClassName() + ", size = " + itemSize);
        }
        long time2 = System.currentTimeMillis();
        timeSpentSizing += time2 - time1;
        FastPathObject obj;

        // make sure only have one class as Dynamic objects are no longer supported
        Set<String> classes = new HashSet<String>();
        classes.add(item.getClassName());
        classes.addAll(Arrays.asList(item.getImplementations().split(" ")));
        if (classes.size() == 1) {
            try {
                Class<? extends FastPathObject> cls =
                        Class.forName(classes.iterator().next()).asSubclass(FastPathObject.class);
                obj = DynamicUtil.createObject(cls);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("class \"" + item.getClassName() + "\" does not exist\n"
                        + "Problem found while loading Item with identifier "
                        + item.getIdentifier(), e);
            }
        } else {
            throw new RuntimeException("Item " + item.getIdentifier() + " has multiple classes/"
                    + "implementations but dynamic objects are no longer supported.");
        }

        try {
            obj.setFieldValue("id", identifierToId(item.getIdentifier()));
        } catch (IllegalArgumentException e) {
            // that's not good
        }
        time1 = System.currentTimeMillis();
        timeSpentCreate += time1 - time2;

        try {
            for (Attribute attr : item.getAttributes()) {
                FieldInfo info = TypeUtil.getFieldInfo(obj.getClass(), attr.getName());
                if (info == null) {
                    String message = "Attribute not found in class: "
                        + Util.getFriendlyName(obj.getClass()) + "." + attr.getName()
                          + "\nProblem found while loading Item with identifier "
                          + item.getIdentifier() + " and attribute name " + attr.getName();
                    LOG.error(message);
                    throw new MetaDataException(message);
                }
                Class<?> attrClass = info.getType();
                if (!"id".equalsIgnoreCase(attr.getName())) {
                    Object value = null;
                    if (ClobAccess.class.equals(attrClass)) {
                        if (attr.getValue() != null) {
                            value = new PendingClob(attr.getValue());
                        }
                    } else {
                        value = TypeUtil.stringToObject(attrClass, attr.getValue());
                    }
                    if (value == null) {
                        throw new IllegalArgumentException("An attribute (name " + attr.getName()
                                + ") for item with id " + item.getIdentifier() + " was null");
                    }
                    obj.setFieldValue(attr.getName(), value);
                }
            }

            for (Reference ref : item.getReferences()) {
                Integer identifier;
                try {
                    identifier = identifierToId(ref.getRefId());
                } catch (RuntimeException e) {
                    throw new RuntimeException("Failed to find referenced Item with identifier "
                            + ref.getRefId() + " in object store from Item with identifier "
                            + item.getIdentifier() + " and reference name " + ref.getName(), e);
                }
                String refName = ref.getName();
                if (refName == null) {
                    throw new RuntimeException("Item with identifier " + item.getIdentifier()
                            + " has a reference with a null name");
                }
                if ("".equals(refName)) {
                    throw new RuntimeException("Item with identifier " + item.getIdentifier()
                            + " has a reference with an empty name");
                }
                if (Character.isLowerCase(refName.charAt(1))) {
                    refName = StringUtil.decapitalise(refName);
                }
                if (TypeUtil.getFieldInfo(obj.getClass(), refName) != null) {
                    obj.setFieldValue(refName, new ProxyReference(os, identifier,
                                InterMineObject.class));
                } else {
                    String message = "Reference not found in class: "
                        + Util.getFriendlyName(obj.getClass()) + "." + ref.getName()
                          + " while translating Item with identifier " + item.getIdentifier();
                    LOG.error(message);
                    throw new MetaDataException(message);
                }
            }

            for (ReferenceList refs : item.getCollections()) {
                QueryClass qc = new QueryClass(InterMineObject.class);
                QueryField qf = new QueryField(qc, "id");
                BagConstraint bc;
                try {
                    bc = new BagConstraint(qf, ConstraintOp.IN,
                        toIntegers(new HashSet<String>(StringUtil.tokenize(refs.getRefIds()))));
                } catch (Exception e) {
                    throw new RuntimeException("failed to find some referenced Items from "
                            + "identifiers " + refs.getRefIds() + " in object store from Item "
                            + "with identifier " + item.getIdentifier() + " and collection name "
                            + refs.getName(), e);
                }

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
                    obj.setFieldValue(refsName, os.executeSingleton(q));
                } else {
                    String message = "Collection not found in class: "
                        + Util.getFriendlyName(obj.getClass()) + "." + refsName
                          + " while translating Item with identifier " + item.getIdentifier();
                    LOG.error(message);
                    throw new MetaDataException(message);
                }
            }
        } catch (MetaDataException e) {
            LOG.error("Broken with: " + Util.getFriendlyName(obj.getClass())
                      + item.getIdentifier(), e);
            throw e;
        } catch (Exception e) {
            LOG.error("Broken with: " + Util.getFriendlyName(obj.getClass())
                      + item.getIdentifier(), e);
            throw new RuntimeException(e);
        }
        time2 = System.currentTimeMillis();
        timeSpentAttributes += time2 - time1;
        objectCount++;
        if (objectCount % 10000 == 0) {
            LOG.info("Translated " + objectCount + " objects. Time spent: sizing: "
                    + timeSpentSizing + ", Create object: " + timeSpentCreate
                    + ", Copy fields: " + timeSpentAttributes);
        }
        return obj;
    }

    /**
     * Convert a set of Integers to a set of String using idToIdentifier()
     * @param integers a set of Integers
     * @return the corresponding set of Strings
     */
    protected Collection<String> toStrings(Collection<Integer> integers) {
        Collection<String> strings = new ArrayList<String>();
        for (Integer i : integers) {
            strings.add(idToIdentifier(i));
        }
        return strings;
    }

    /**
     * Convert a set of Strings to a set of Integers using identifierToId()
     * @param strings a set of Strings
     * @return the corresponding set of Integers
     */
    protected Collection<Integer> toIntegers(Collection<String> strings) {
        Collection<Integer> integers = new ArrayList<Integer>();
        for (String s : strings) {
            integers.add(identifierToId(s));
        }
        return integers;
    }
}
