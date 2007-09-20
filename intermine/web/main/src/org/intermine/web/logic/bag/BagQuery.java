package org.intermine.web.logic.bag;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.FromElement;
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
    private static final Logger LOG = Logger.getLogger(BagQuery.class);
    private Query query;
    private String message, queryString, packageName;
    private boolean matchesAreIssues;
    private final BagQueryConfig bagQueryConfig;
    private final Model model;

    /**
     * Create a new BagQuery object.
     * @param bagQueryConfig the configuration to use
     * @param model the Model for the query
     * @param queryString the query IQL
     * @param message the message from the bag-queries.xml describing this query
     * @param packageName the package name
     * @param matchesAreIssues true if and and if matches for this bag query should be treated as
     * issues (aka low quality matches)
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
        this.query = null;
    }

    /**
     * Create a new BagQuery object.
     * @param bagQueryConfig the configuration to use
     * @param model the Model for the query
     * @param query the Query
     * @param message the message from the bag-queries.xml describing this query
     * @param matchesAreIssues true if and and if matches for this bag query should be treated as
     * issues (aka low quality matches)
     */
    public BagQuery(BagQueryConfig bagQueryConfig, Model model, Query query,
                    String message, boolean matchesAreIssues) {
        if (bagQueryConfig == null) {
            throw new IllegalArgumentException("bagQueryConfig argument cannot be null");
        }
        this.bagQueryConfig = bagQueryConfig;
        this.model = model;
        this.queryString = "";
        this.query = query;
        this.message = message;
        this.matchesAreIssues = matchesAreIssues;
    }

    /**
     * Return the Query that was passed to the constructor.
     * @param bag the collection to use to constrain the query
     * @param extraFieldValue the value used if any extra constraint is configured
     * @return the Query
     */
    public Query getQuery(Collection bag, String extraFieldValue) {
        List lowerCaseBag = new ArrayList();
        for (Object o : bag) {
            if (o instanceof String) {
                o = ((String) o).toLowerCase();
                lowerCaseBag.add(o);
                try {
                    lowerCaseBag.add(new Integer((String) o));
                } catch (NumberFormatException e) {
                    //LOG.info("Couldn't parse string \"" + o + "\" into integer");
                    // Wasn't a number
                }
            } else {
                lowerCaseBag.add(o);
            }
        }
        
        if (query == null) {
            IqlQuery q = new IqlQuery(queryString, packageName,
                                      new ArrayList(Collections.singleton(lowerCaseBag)));
            return addExtraContraint(q.toQuery(), extraFieldValue);
        }
        return addExtraContraint(query, extraFieldValue);
    }

    /**
     * Return a Query to fetch bag contents for wildcards.
     *
     * @param bag the Collection of strings to use to constrain the query
     * @param extraFieldValue the value used if any extra constraint is configured
     * @return the Query
     */
    public Query getQueryForWildcards(Collection<String> bag, String extraFieldValue) {
        Query q = QueryCloner.cloneQuery(getQuery(Collections.EMPTY_SET, extraFieldValue));
        Map<QueryEvaluable, ConstraintSet> nodes = new HashMap<QueryEvaluable, ConstraintSet>();
        if (q.getConstraint() instanceof BagConstraint) {
            ConstraintSet cs = new ConstraintSet(ConstraintOp.OR);
            nodes.put((QueryEvaluable) ((BagConstraint) q.getConstraint()).getQueryNode(), cs);
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
            Set<Constraint> constraints = new HashSet(cs.getConstraints());
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
    private Query addExtraContraint(Query queryArg, String extraFieldValue) {
        String connectFieldName = bagQueryConfig.getConnectField();
        String extraClassName = bagQueryConfig.getExtraConstraintClassName();
        String constrainFieldName = bagQueryConfig.getConstrainField();
        if (StringUtils.isEmpty(extraFieldValue) || connectFieldName == null
            || extraClassName == null || constrainFieldName == null) {
            return queryArg;
        }
        Query queryCopy = QueryCloner.cloneQuery(queryArg);
        Set fromSet = new HashSet(queryCopy.getFrom());
        Iterator fromIter = fromSet.iterator();
        while (fromIter.hasNext()) {
            FromElement fromElement = (FromElement) fromIter.next();
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
                        Class extraClass;
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
                }
            }
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
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("query=" + queryString);
        sb.append(" message=" + message);
        sb.append(" matchesAreIssues= " + matchesAreIssues);
        return sb.toString();
    }
}
