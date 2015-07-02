package org.intermine.api.bag;

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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.StringUtil;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.FromElement;
import org.intermine.objectstore.query.MultipleInBagConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCast;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCloner;
import org.intermine.objectstore.query.QueryEvaluable;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.iql.IqlQuery;

/**
 * A class encapsulating a query used to create a bag from a collection of input identifiers.
 * @author Richard Smith
 */
public class BagQuery
{
    private String message, queryString, packageName;
    private boolean matchesAreIssues;
    private final BagQueryConfig bagQueryConfig;
    private final Model model;
    private boolean isDefaultQuery;
    private Map<String, List<FieldDescriptor>> classKeys;
    private String type;

    /**
     * Create a new BagQuery object.
     * @param bagQueryConfig the configuration to use
     * @param model the Model for the query
     * @param queryString the query IQL
     * @param message the message from the bag-queries.xml describing this query
     * @param packageName the package name
     * @param matchesAreIssues true if matches for this bag query should be treated as issues (aka
     * low quality matches)
     */
    public BagQuery(BagQueryConfig bagQueryConfig, Model model, String queryString,
                    String message, String packageName, boolean matchesAreIssues) {
        if (bagQueryConfig == null) {
            throw new IllegalArgumentException("bagQueryConfig argument cannot be null");
        }
        this.bagQueryConfig = bagQueryConfig;
        this.model = model;
        this.queryString = queryString;
        this.message = message;
        this.matchesAreIssues = matchesAreIssues;
        this.packageName = packageName;
        this.isDefaultQuery = false;
    }

    /**
     * Create a new BagQuery object for the default query - search for all key fields
     * of the given class.
     * @param bagQueryConfig the configuration to use
     * @param model the Model for the query
     * @param classKeys map of class key fields
     * @param type the qualified class name of the type to build query for
     */
    public BagQuery(BagQueryConfig bagQueryConfig, Model model,
            Map<String, List<FieldDescriptor>> classKeys, String type) {
        if (bagQueryConfig == null) {
            throw new IllegalArgumentException("bagQueryConfig argument cannot be null");
        }
        this.bagQueryConfig = bagQueryConfig;
        this.model = model;
        this.queryString = "";
        this.isDefaultQuery = true;
        this.classKeys = classKeys;
        this.type = type;
        this.message = BagQueryHelper.DEFAULT_MESSAGE;
        this.matchesAreIssues = false;
    }

    /**
     * Return the Query that was passed to the constructor.
     * @param bag the collection to use to constrain the query
     * @param extraFieldValue the value used if any extra constraint is configured
     * @return the Query
     * @throws ClassNotFoundException if class given by type not found
     */
    public Query getQuery(Collection<String> bag, String extraFieldValue)
        throws ClassNotFoundException {
        List<String> lowerCaseBag = new ArrayList<String>();
        for (String o : bag) {
            o = o.toLowerCase();
            lowerCaseBag.add(o);
        }

        // if this should be the default query using class key fields, create it now
        if (isDefaultQuery) {
            Query q = BagQueryHelper.createDefaultBagQuery(type, bagQueryConfig, model,
                                                           classKeys, lowerCaseBag);
            return addExtraConstraint(q, extraFieldValue);
        }
        List<Object> bags = new ArrayList<Object>();
        int bagCount = StringUtil.countOccurances("?", queryString);
        for (int i = 0; i < bagCount; i++) {
            bags.add(lowerCaseBag);
        }
        IqlQuery q = new IqlQuery(queryString, packageName, bags);
        return addExtraConstraint(q.toQuery(), extraFieldValue);

    }

    /**
     * Return a Query to fetch bag contents for wildcards.
     *
     * @param bag the Collection of strings to use to constrain the query
     * @param extraFieldValue the value used if any extra constraint is configured
     * @return the Query
     * @throws ClassNotFoundException if class specified by type not found
     */
    public Query getQueryForWildcards(Collection<String> bag, String extraFieldValue)
        throws ClassNotFoundException {
        Set<String> empty = Collections.emptySet();
        Query q = QueryCloner.cloneQuery(getQuery(empty, extraFieldValue));
        Map<QueryEvaluable, ConstraintSet> nodes = new LinkedHashMap<QueryEvaluable,
                ConstraintSet>();
        if (q.getConstraint() instanceof BagConstraint) {
            ConstraintSet cs = new ConstraintSet(ConstraintOp.OR);
            nodes.put((QueryEvaluable) ((BagConstraint) q.getConstraint()).getQueryNode(), cs);
            q.setConstraint(cs);
        } else if (q.getConstraint() instanceof MultipleInBagConstraint) {
            ConstraintSet cs = new ConstraintSet(ConstraintOp.OR);
            MultipleInBagConstraint c = (MultipleInBagConstraint) q.getConstraint();
            for (QueryEvaluable qe : c.getEvaluables()) {
                nodes.put(qe, cs);
            }
            q.setConstraint(cs);
        } else {
            traverseConstraint(q.getConstraint(), nodes);
        }
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("Query " + q + " does not contain any"
                    + " BagConstraints");
        }
        for (String string : bag) {
            String wildcardSql = string.replace('*', '%').toLowerCase();
            for (Map.Entry<QueryEvaluable, ConstraintSet> entry : nodes.entrySet()) {
                if (entry.getKey().getType().equals(String.class)) {
                    entry.getValue().addConstraint(new SimpleConstraint(entry.getKey(),
                                ConstraintOp.MATCHES, new QueryValue(wildcardSql)));
                } else {
                    entry.getValue().addConstraint(new SimpleConstraint(new QueryCast(
                                    entry.getKey(), String.class), ConstraintOp.MATCHES,
                                new QueryValue(wildcardSql)));

                }
            }
        }
        return q;
    }

    /**
     * Finds all BagConstraints in a Constraint, and places the QueryEvaluable and the containing
     * ConstraintSet into the given Map.
     *
     * @param con the Constraint to search
     * @param nodes the Map to put results into
     */
    public void traverseConstraint(Constraint con, Map<QueryEvaluable, ConstraintSet> nodes) {
        if (con instanceof ConstraintSet) {
            ConstraintSet cs = (ConstraintSet) con;
            Set<Constraint> constraints = new HashSet<Constraint>(cs.getConstraints());
            for (Constraint c : constraints) {
                if (c instanceof BagConstraint) {
                    ((ConstraintSet) con).removeConstraint(c);
                    if (cs.getOp().equals(ConstraintOp.OR)) {
                        nodes.put((QueryEvaluable) ((BagConstraint) c).getQueryNode(), cs);
                    } else {
                        ConstraintSet replacement = new ConstraintSet(ConstraintOp.OR);
                        nodes.put((QueryEvaluable) ((BagConstraint) c).getQueryNode(), replacement);
                        cs.addConstraint(replacement);
                    }
                } else if (c instanceof MultipleInBagConstraint) {
                    for (QueryEvaluable qe : ((MultipleInBagConstraint) c).getEvaluables()) {
                        nodes.put(qe, cs);
                    }
                } else if (c instanceof ConstraintSet) {
                    traverseConstraint(c, nodes);
                }
            }
        }
    }

    /**
     * Return a Query modified using the connectField, className and constrainField from the
     * BagQueryCongfig.
     */
    private Query addExtraConstraint(Query queryArg, String extraFieldValue) {
        String connectFieldName = bagQueryConfig.getConnectField();
        String extraClassName = bagQueryConfig.getExtraConstraintClassName();
        String constrainFieldName = bagQueryConfig.getConstrainField();
        if (StringUtils.isEmpty(extraFieldValue) || connectFieldName == null
            || extraClassName == null || constrainFieldName == null) {
            return queryArg;
        }
        Query queryCopy = QueryCloner.cloneQuery(queryArg);
        boolean doneExtraField = false;
        for (FromElement fromElement : new HashSet<FromElement>(queryCopy.getFrom())) {
            if (fromElement instanceof QueryClass) {
                QueryClass queryClass = (QueryClass) fromElement;
                ClassDescriptor cd = model.getClassDescriptorByName(queryClass.getType().getName());
                if (cd == null) {
                    throw new RuntimeException("can't find ClassDescriptor for: "
                                               + queryClass.getType().getName());
                }

                FieldDescriptor fd = cd.getFieldDescriptorByName(connectFieldName);
                if (fd != null) {
                    // add a new QueryClass to the query and constrain the connect field of this
                    // class to be equal to the new QueryClass
                    if (fd instanceof ReferenceDescriptor) {
                        Class<?> extraClass;
                        try {
                            extraClass = Class.forName(extraClassName);
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException("can't find Class for extraClassName: "
                                                       + extraClassName);
                        }
                        QueryClass newQC = new QueryClass(extraClass);
                        QueryReference connectFieldQF =
                            new QueryObjectReference(queryClass, connectFieldName);
                        queryCopy.addFrom(newQC);
                        Constraint oldConstraint = queryCopy.getConstraint();
                        ConstraintSet newConstraint = new ConstraintSet(ConstraintOp.AND);
                        queryCopy.setConstraint(newConstraint);
                        newConstraint.addConstraint(oldConstraint);
                        QueryField constrainField = new QueryField(newQC, constrainFieldName);
                        QueryEvaluable extraFieldValueQF = new QueryValue(extraFieldValue);
                        Constraint connectFieldConstraint =
                            new ContainsConstraint(connectFieldQF, ConstraintOp.CONTAINS, newQC);
                        newConstraint.addConstraint(connectFieldConstraint);
                        Constraint extraConstraint =
                            new SimpleConstraint(constrainField, ConstraintOp.EQUALS,
                                                 extraFieldValueQF);
                        newConstraint.addConstraint(extraConstraint);
                    } else {
                        String exceptionMessage =
                            "found a FieldDescriptor for " + queryClass.getType().getName() + "."
                            + connectFieldName
                            + " but it isn't a ReferenceDescriptor";
                        throw new RuntimeException(exceptionMessage);
                    }
                    doneExtraField = true;
                }
            }
        }
        if (!doneExtraField) {
            throw new IllegalArgumentException("Class does not have required extra field");
        }
        return queryCopy;
    }

    /**
     * Return the flag passed to the constructor.
     * @return the matchesAreIssues flag
     */
    public boolean matchesAreIssues() {
        return this.matchesAreIssues;
    }

    /**
     * Return the message that was passed to the constructor.
     * @return the message
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("query=" + queryString);
        sb.append(" message=" + message);
        sb.append(" matchesAreIssues= " + matchesAreIssues);
        return sb.toString();
    }
}
