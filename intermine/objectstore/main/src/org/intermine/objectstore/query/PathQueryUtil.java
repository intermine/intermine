package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.util.TypeUtil;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.tools.ant.BuildException;

/**
 * Utility methods for paths.
 * @author Kim Rutherford
 */
public class PathQueryUtil
{
    /**
     * Given a class return a set with the unqualified class name in and if preceded by
     * a '+' also the unqualified names of all subclasses.
     * @param clsName an unqullified class name
     * @param model the Model use to find meta data   
     * @return a set of class names
     */
    protected static Set getClassNames(Model model, String clsName) {
        boolean useSubClasses = false;
        if (clsName.startsWith("+")) {
            clsName = clsName.substring(1);
            useSubClasses = true;
        }
    
        ClassDescriptor cld = model.getClassDescriptorByName(model.getPackageName()
                                                             + "." + clsName);
        if (cld == null) {
            throw new IllegalArgumentException("cannot find ClassDescriptor for " + clsName);
        }
    
        Set clsNames = new LinkedHashSet();
        clsNames.add(clsName);
    
        if (useSubClasses) {
            Set clds = model.getAllSubs(cld);
            Iterator cldIter = clds.iterator();
            while (cldIter.hasNext()) {
                clsNames.add(TypeUtil.unqualifiedName(((ClassDescriptor)
                                                       cldIter.next()).getName()));
            }
        }
        return clsNames;
    }

    /**
     * Path should be of the form: Class1 ref1 Class2 ref2 Class3
     * Where the number of elements is greater than one and an odd number.  Check
     * that all classes anf references are valid in the model.
     * @param path the path string
     * @param model the Model use to find meta data
     * @throws IllegalArgumentException if path not valid
     */
    protected static void validatePath(String path, Model model) {
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
            ReferenceDescriptor rd = startCld.getReferenceDescriptorByName(refName);
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
     * @param model the Model use to find meta data
     * @param path the path to expand
     * @return a Set of paths
     */
    public static Set expandPath(Model model, String path) {
        Set paths = new LinkedHashSet();

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
    
        Set subs;
        try {
            subs = getClassNames(model, clsName);
        } catch (IllegalArgumentException e) {
            throw new BuildException("Cannot find class names", e);
        }
        Iterator subIter = subs.iterator();
        while (subIter.hasNext()) {
            String subName = (String) subIter.next();
            Set nextPaths = new LinkedHashSet();
            if (refName != "") {
                nextPaths.addAll(expandPath(model, path.substring(refEnd + 1).trim()));
            } else {
                nextPaths.addAll(subs);
                return nextPaths;
            }
            Iterator pathIter = nextPaths.iterator();
            while (pathIter.hasNext()) {
                String nextPath = (String) pathIter.next();
                paths.add((subName + refName + " " + nextPath).trim());
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
    public static Query constructQuery(Model model, String path)
        throws ClassNotFoundException, IllegalArgumentException {
        String[] queryBits = path.split("[ \t]");
    
        // validate path against model
        validatePath(path, model);

        Query q = new Query();
        QueryClass qcLast = null;
        for (int i = 0; i + 2 < queryBits.length; i += 2) {
            QueryClass qcStart = new QueryClass(Class.forName(model.getPackageName()
                                                              + "." + queryBits[i]));
            String refName = queryBits[i + 1];
            QueryClass qcEnd = new QueryClass(Class.forName(model.getPackageName()
                                                            + "." + queryBits[i + 2]));
            if (qcLast != null) {
                qcStart = qcLast;
            }
            qcLast = addReferenceConstraint(model, q, qcStart, refName, qcEnd, (i == 0));
        }
    
        return q;
    }

    /**
     * Add a contains constraint to Query (q) from qcStart from qcEnd via reference refName.
     * Return qcEnd as it may need to be passed into mehod again as qcStart.
     * @param model the Model use to find meta data
     * @param q the query
     * @param qcStart the QueryClass that contains the reference
     * @param refName name of reference to qcEnd
     * @param qcEnd the target QueryClass of refName
     * @param first true if this is the first constraint added - qcStart needs to be added
     * to the query
     * @return QueryClass return qcEnd
     */
    protected static QueryClass addReferenceConstraint(Model model,
                                                Query q, QueryClass qcStart, String refName,
                                                QueryClass qcEnd, boolean first) {
        if (first) {
            q.addToSelect(qcStart);
            q.addFrom(qcStart);
            q.addToOrderBy(qcStart);
        }
        q.addToSelect(qcEnd);
        q.addFrom(qcEnd);
        q.addToOrderBy(qcEnd);
    
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
        QueryHelper.addConstraint(q, cc);
    
        return qcEnd;
    }

}
