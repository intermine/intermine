package org.intermine.objectstore.querygen;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.TypeUtil;
import org.intermine.metadata.Util;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryHelper;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryReference;

/**
 * Utility methods for paths.
 * @author Kim Rutherford
 */
public final class QueryGenUtil
{
    private QueryGenUtil() {
    }

    /**
     * Given a String, perform a one of a set of expansions and return a Set of unqualified class
     * names. Firstly, if the argument contains commas, it will be split up in the obvious way, and
     * each element will be expanded in one of the following ways:
     *
     * <UL><LI>Plain unqualified class names will be returned unchanged</LI>
     *     <LI>An unqualified class name preceded by a "+" will be expanded to include all the
     *         subclasses</LI>
     *     <LI>A String with dots will be treated as a path to build into a query, and the results
     *         taken as a list of classes to include - the path must end with ".class"</LI>
     * </UL>
     *
     * @param os the ObjectStore that the data to be queried is in
     * @param clsName an unqualified class name
     * @return a set of class names
     * @throws ObjectStoreException if an error occurs running a query
     */
    protected static Set<String> getClassNames(ObjectStore os,
            String clsName) throws ObjectStoreException {
        boolean useSubClasses = false;
        Set<String> clsNames = new LinkedHashSet<String>();
        for (String part : clsName.split(",")) {
            if (part.indexOf('.') != -1) {
                QueryAndClass qac = createClassFindingQuery(os.getModel(), part);
                for (Object cls : os.executeSingleton(qac.getQuery(), 1000, false, false, false)) {
                    Class<?> clazz = (Class<?>) cls;
                    for (Class<?> classPart : Util.decomposeClass(clazz)) {
                        if (qac.getClazz().isAssignableFrom(classPart)) {
                            clsNames.add(TypeUtil.unqualifiedName(classPart.getName()));
                        }
                    }
                }
            } else {
                if (part.startsWith("+")) {
                    part = part.substring(1);
                    useSubClasses = true;
                }

                ClassDescriptor cld = os.getModel().getClassDescriptorByName(os.getModel()
                        .getPackageName() + "." + part);
                if (cld == null) {
                    throw new IllegalArgumentException("cannot find ClassDescriptor for " + part
                            + " in argument \"" + clsName + "\"");
                }

                clsNames.add(part);

                if (useSubClasses) {
                    for (ClassDescriptor nextCld : os.getModel().getAllSubs(cld)) {
                        clsNames.add(TypeUtil.unqualifiedName(nextCld.getName()));
                    }
                }
            }
        }
        return clsNames;
    }

    /**
     * Takes a String representation of a query (a path), and creates a query. For example, the
     * String "Department.class" will produce the query
     * "SELECT DISTINCT Department.class FROM Department".
     *
     * @param model the Model that the query uses
     * @param part the String describing the query
     * @return a QueryAndClass - the results of the given Query is a list of classes, which should
     * all be decomposed and the constituents filtered to allow only subclasses of the given Class
     */
    protected static QueryAndClass createClassFindingQuery(Model model, String part) {
        try {
            String[] paths = part.split("\\.");
            Query q = new Query();
            QueryClass qc = new QueryClass(Class.forName(model.getPackageName() + "."
                        + paths[0]));
            q.addFrom(qc);
            ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
            for (int i = 1; i < paths.length; i++) {
                if (i == paths.length - 1) {
                    QueryField qf = new QueryField(qc, paths[i]);
                    q.addToSelect(qf);
                } else {
                    try {
                        QueryObjectReference qor = new QueryObjectReference(qc, paths[i]);
                        qc = new QueryClass(qor.getType());
                        q.addFrom(qc);
                        cs.addConstraint(new ContainsConstraint(qor, ConstraintOp.CONTAINS,
                                    qc));
                    } catch (IllegalArgumentException e) {
                        // Not a reference - try collection instead
                        QueryCollectionReference qcr = new QueryCollectionReference(qc,
                                paths[i]);
                        qc = new QueryClass(TypeUtil.getElementType(qc.getType(), paths[i]));
                        q.addFrom(qc);
                        cs.addConstraint(new ContainsConstraint(qcr, ConstraintOp.CONTAINS,
                                    qc));
                    }
                }
            }
            q.setDistinct(true);
            int csSize = cs.getConstraints().size();
            if (csSize == 1) {
                q.setConstraint(cs.getConstraints().iterator().next());
            } else if (csSize > 1) {
                q.setConstraint(cs);
            }
            return new QueryAndClass(q, qc.getType());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Class not found while processing " + part, e);
        }
    }

    /**
     * Path should be of the form: Class1 ref1 Class2 ref2 Class3
     * Where the number of elements is greater than one and an odd number.  Check
     * that all classes anf references are valid in the model.
     * @param path the path string
     * @param model the Model use to find meta data
     * @throws IllegalArgumentException if path not valid
     */
    public static void validatePath(String path, Model model) {
        int posOfOrder = path.indexOf(" ORDER BY ");
        if (posOfOrder != -1) {
            path = path.substring(0, posOfOrder);
        }
        // must be more than one element and odd number
        String[] queryBits = path.split("[ \t]");
        if (!(queryBits.length > 1) || (queryBits.length % 2 == 0)) {
            throw new IllegalArgumentException("Construct query path does not have valid "
                                               + " number of elements: " + path);
        }

        for (int i = 0; i + 2 < queryBits.length; i += 2) {
            String start = model.getPackageName() + "." + queryBits[i];
            String refName = queryBits[i + 1];
            String end = model.getPackageName() + "." + queryBits[i + 2];

            if (!model.hasClassDescriptor(start)) {
                throw new IllegalArgumentException("Class not found in model: " + start);
            } else if (!model.hasClassDescriptor(end)) {
                throw new IllegalArgumentException("Class not found in model: " + end);
            }

            ClassDescriptor startCld = model.getClassDescriptorByName(start);
            ClassDescriptor endCld = model.getClassDescriptorByName(end);
            int dotPos = refName.indexOf('.');
            if (dotPos != -1) {
                String prefix = refName.substring(0, dotPos);
                if (!("reverse".equals(prefix))) {
                    int replacementStartNo;
                    try {
                        replacementStartNo = Integer.parseInt(prefix) - 1;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Prefix " + prefix + " is not a number "
                                + "or \"reverse\"", e);
                    }
                    if (replacementStartNo > (i / 2) - 1) {
                        throw new IllegalArgumentException("Prefix " + prefix
                                + " refers to future");
                    }
                    startCld = model.getClassDescriptorByName(model.getPackageName() + "."
                            + queryBits[replacementStartNo * 2]);
                    refName = refName.substring(dotPos + 1);
                }
            }
            if (refName.startsWith("reverse.")) {
                ClassDescriptor tempCld = startCld;
                startCld = endCld;
                endCld = tempCld;
                refName = refName.substring(8);
            }

            if ((startCld.getReferenceDescriptorByName(refName, true) == null)
                && (startCld.getCollectionDescriptorByName(refName, true) == null)) {
                throw new IllegalArgumentException("Cannot find descriptor for " + refName
                                         + " in " + startCld.getName());
            }
            // TODO check type of end vs. referenced type
        }
    }

    /**
     * Given a path return a set of paths replacing a path with a '+' preceding a class
     * name with an additional path for every subclass of that class.
     *
     * @param os the ObjectStore that the data is stored in
     * @param path the path to expand
     * @return a Set of paths
     * @throws ObjectStoreException if an error occurs while running a query
     */
    public static Set<String> expandPath(ObjectStore os, String path) throws ObjectStoreException {
        int posOfOrder = path.indexOf(" ORDER BY ");
        String order = "";
        if (posOfOrder != -1) {
            order = path.substring(posOfOrder);
            path = path.substring(0, posOfOrder);
        }
        Set<String> paths = new LinkedHashSet<String>();

        String clsName;
        String refName = "";
        int refEnd = 0;
        if (path.indexOf(' ') != -1) {
            int clsEnd = path.indexOf(' ');
            clsName = path.substring(0, clsEnd);
            refEnd = path.indexOf(' ', clsEnd + 1);
            refName = path.substring(clsEnd, refEnd);
        } else {
            // at end, this is last clsName
            clsName = path;
        }

        Set<String> subs = getClassNames(os, clsName);

        for (String subName : subs) {
            Set<String> nextPaths = new LinkedHashSet<String>();
            if (!("".equals(refName))) {
                nextPaths.addAll(expandPath(os, path.substring(refEnd + 1).trim()));
            } else {
                nextPaths.addAll(subs);
                return nextPaths;
            }
            for (String nextPath : nextPaths) {
                paths.add((subName + refName + " " + nextPath).trim() + order);
            }
        }
        return paths;
    }

    /**
     * Construct an objectstore query represented by the given path.
     * @param model the Model use to find meta data
     * @param path path to construct query for
     * @return the constructed query
     * @throws ClassNotFoundException if problem processing path
     * @throws IllegalArgumentException if problem processing path
     */
    public static Query constructQuery(Model model, String path) throws ClassNotFoundException {
        // validate path against model
        validatePath(path, model);

        int posOfOrder = path.indexOf(" ORDER BY ");
        String order = null;
        if (posOfOrder != -1) {
            order = path.substring(posOfOrder + 10);
            path = path.substring(0, posOfOrder);
        }
        String[] queryBits = path.split("[ \t]");

        Query q = new Query();
        q.setDistinct(false);
        QueryClass qcStart = new QueryClass(Class.forName(model.getPackageName()
                                                          + "." + queryBits[0]));
        List<QueryClass> qcs = new ArrayList<QueryClass>();
        qcs.add(qcStart);
        for (int i = 0; i + 2 < queryBits.length; i += 2) {
            String refName = queryBits[i + 1];
            int dotPos = refName.indexOf('.');
            if (dotPos != -1) {
                String prefix = refName.substring(0, dotPos);
                if (!("reverse".equals(prefix))) {
                    int replacementStartNo;
                    try {
                        replacementStartNo = Integer.parseInt(prefix) - 1;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Prefix " + prefix + " is not a number "
                                + "or \"reverse\"", e);
                    }
                    qcStart = qcs.get(replacementStartNo);
                    refName = refName.substring(dotPos + 1);
                }
            }
            QueryClass qcEnd = new QueryClass(Class.forName(model.getPackageName()
                        + "." + queryBits[i + 2]));
            addReferenceConstraint(model, q, qcStart, refName, qcEnd, (i == 0));
            qcs.add(qcEnd);
            qcStart = qcEnd;
        }

        if (order != null) {
            String[] orderBits = order.split("[ \t]");
            for (String orderBit : orderBits) {
                int posOfDot = orderBit.indexOf('.');
                int classNo = Integer.parseInt(orderBit.substring(0, posOfDot)) - 1;
                String fieldName = orderBit.substring(posOfDot + 1);
                QueryField qf = new QueryField(qcs.get(classNo), fieldName);
                q.addToSelect(qf);
                q.addToOrderBy(qf);
            }
        }
        return q;
    }

    /**
     * Add a contains constraint to Query (q) from qcStart from qcEnd via reference refName.
     *
     * @param model the Model use to find meta data
     * @param q the query
     * @param qcStart the QueryClass that contains the reference
     * @param refName name of reference to qcEnd
     * @param qcEnd the target QueryClass of refName
     * @param first true if this is the first constraint added - qcStart needs to be added
     * to the query
     */
    protected static void addReferenceConstraint(Model model, Query q, QueryClass qcStart,
            String refName, QueryClass qcEnd, boolean first) {
        if (first) {
            q.addFrom(qcStart);
            q.addToSelect(qcStart);
        }
        q.addFrom(qcEnd);
        q.addToSelect(qcEnd);

        if (refName.startsWith("reverse.")) {
            refName = refName.substring(8);
            // Already validated against model
            ClassDescriptor endCld = model.getClassDescriptorByName(qcEnd.getType().getName());
            FieldDescriptor fd = endCld.getFieldDescriptorByName(refName);

            QueryReference qRef;
            if (fd.isReference()) {
                qRef = new QueryObjectReference(qcEnd, refName);
            } else {
                qRef = new QueryCollectionReference(qcEnd, refName);
            }
            ContainsConstraint cc = new ContainsConstraint(qRef, ConstraintOp.CONTAINS, qcStart);
            QueryHelper.addAndConstraint(q, cc);
        } else {
            // already validated against model
            ClassDescriptor startCld = model.getClassDescriptorByName(qcStart.getType().getName());
            FieldDescriptor fd = startCld.getFieldDescriptorByName(refName);

            QueryReference qRef;
            if (fd.isReference()) {
                qRef = new QueryObjectReference(qcStart, refName);
            } else {
                qRef = new QueryCollectionReference(qcStart, refName);
            }
            ContainsConstraint cc = new ContainsConstraint(qRef, ConstraintOp.CONTAINS, qcEnd);
            QueryHelper.addAndConstraint(q, cc);
        }
    }

    /**
     * Class to allow returning of two arguments from a method. It sucks, but that's how it is.
     *
     * @author Matthew Wakeling
     */
    protected static class QueryAndClass
    {
        private Query query;
        private Class<?> clazz;

        /**
         * Constructor.
         *
         * @param query a Query
         * @param clazz a Class
         */
        public QueryAndClass(Query query, Class<?> clazz) {
            this.query = query;
            this.clazz = clazz;
        }

        /**
         * Returns the query.
         *
         * @return a Query
         */
        public Query getQuery() {
            return query;
        }

        /**
         * Returns the class.
         *
         * @return a Class
         */
        public Class<?> getClazz() {
            return clazz;
        }
    }
}
