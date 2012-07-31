package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2012 FlyMine
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

import org.apache.log4j.Logger;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.DynamicUtil;

/**
 * Utility methods for finding overlaps.
 *
 * @author Kim Rutherford
 * @author Matthew Wakeling
 */
public abstract class OverlapUtil
{
    private static final Logger LOG = Logger.getLogger(OverlapUtil.class);

    private OverlapUtil() {
      //disable external instantiation
    }

    /**
     * Creates OverlapRelations for overlapping SequenceFeature objects that are located
     * on the given subject (generally a Chromosome).
     *
     * @param os the ObjectStore to query
     * @param subject the SequenceFeature (eg. a Chromosome) where the LSFs are located
     * @param classNamesToIgnore a List of the names of those classes that should be ignored when
     * searching for overlaps.  Sub classes to these classes are ignored too. In addition, an
     * entry can be of the form class=class, which specifies that the particular combination should
     * be ignored. Hence an entry of the form class is equivalent to class=InterMineObject
     * @param ignoreSelfMatches if true, don't create OverlapRelations between two objects of the
     * same class
     * @param osw the ObjectStoreWriter to use to write to the database
     * @param summary a Map, to which summary data will be added
     * @throws ObjectStoreException if an error occurs while writing
     * @throws ClassNotFoundException if there is an ObjectStore problem
     */
    public static void createOverlaps(final ObjectStore os, SequenceFeature subject,
            List<?> classNamesToIgnore, boolean ignoreSelfMatches, ObjectStoreWriter osw,
            Map<String, Integer> summary)
        throws ObjectStoreException, ClassNotFoundException {
        Model model = os.getModel();

        Map<Class<?>, Set<Class<?>>> classesToIgnore = new HashMap<Class<?>, Set<Class<?>>>();

        Iterator<?> classNamesToIgnoreIter = classNamesToIgnore.iterator();

        while (classNamesToIgnoreIter.hasNext()) {
            String className = (String) classNamesToIgnoreIter.next();

            int eq = className.indexOf('=');
            String targetClassName = (eq == -1 ? "org.intermine.model.InterMineObject"
                    : className.substring(eq + 1));
            className = (eq == -1 ? className : className.substring(0, eq));

            className = (className.indexOf('.') == -1 ? model.getPackageName() + "." + className
                    : className);
            targetClassName = (targetClassName.indexOf('.') == -1 ? model.getPackageName() + "."
                    + targetClassName : targetClassName);

            try {
                Class<?> thisClass = Class.forName(className);
                Class<?> targetClass = Class.forName(targetClassName);

                Set<Class<?>> targetClasses = classesToIgnore.get(thisClass);
                if (targetClasses == null) {
                    targetClasses = new HashSet<Class<?>>();
                    classesToIgnore.put(thisClass, targetClasses);
                }
                targetClasses.add(targetClass);
            } catch (java.lang.ClassNotFoundException e) {
                // ignore
            }
        }

        Query q = new Query();
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        q.setConstraint(cs);

        QueryClass qcLoc = new QueryClass(Location.class);
        q.addFrom(qcLoc);
        q.addToSelect(qcLoc);

        q.setDistinct(false);
        QueryClass qcObj = new QueryClass(SequenceFeature.class);
        q.addFrom(qcObj);
        q.addToSelect(qcObj);

        QueryObjectReference ref1 = new QueryObjectReference(qcLoc, "feature");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcObj);
        cs.addConstraint(cc1);

        QueryObjectReference ref2 = new QueryObjectReference(qcLoc, "locatedOn");
        ContainsConstraint subjectIdConstraint = new ContainsConstraint(ref2, ConstraintOp.CONTAINS,
                subject);
        cs.addConstraint(subjectIdConstraint);

        q.addToOrderBy(new QueryField(qcLoc, "start"));

        try {
            ((ObjectStoreInterMineImpl) os).goFaster(q);
            Results results = os.execute(q);
            Map<Location, SequenceFeature> currentLocations
                = new HashMap<Location, SequenceFeature>();
            int count = 0;
            Iterator<?> resIter = results.iterator();

            while (resIter.hasNext()) {
                ResultsRow<?> rr = (ResultsRow<?>) resIter.next();

                Location location = (Location) rr.get(0);

                if (location.getStart() == null || location.getEnd() == null) {
                    continue;
                }

                SequenceFeature lsf = (SequenceFeature) rr.get(1);

                if (isAClassToIgnore(classesToIgnore, lsf.getClass())) {
                    continue;
                }

                int start = location.getStart().intValue();

                // Okay, first we compare this location to all the currentLocations.
                Iterator<?> currIter = currentLocations.entrySet().iterator();
                while (currIter.hasNext()) {
                    Map.Entry<Location, SequenceFeature> currEntry = (Map.Entry<Location,
                            SequenceFeature>) currIter.next();
                    Location currLoc = currEntry.getKey();
                    SequenceFeature currLsf = currEntry.getValue();
                    if (currLoc.getEnd().intValue() < start) {
                        currIter.remove();
                    } else {
                        // They overlap, so check to see if we have configured them out.
                        if ((!ignoreSelfMatches)
                                || (!lsf.getClass().equals(currLsf.getClass()))) {
                            if (!(ignoreCombination(classesToIgnore, lsf.getClass(),
                                            currLsf.getClass())
                                    || ignoreCombination(classesToIgnore, currLsf.getClass(),
                                        lsf.getClass()))) {
                                ++count;
                                osw.addToCollection(lsf.getId(), SequenceFeature.class,
                                        "overlappingFeatures", currLsf.getId());
                                osw.addToCollection(currLsf.getId(), SequenceFeature.class,
                                        "overlappingFeatures", lsf.getId());

                                // Log it, for the summary.
                                String classname1 = DynamicUtil.getFriendlyName(lsf.getClass());
                                String classname2 = DynamicUtil.getFriendlyName(currLsf
                                        .getClass());

                                String summaryLine = classname1.compareTo(classname2) > 0
                                    ? classname2 + " - " + classname1 : classname1 + " - "
                                    + classname2;
                                Integer summaryCount = summary.get(summaryLine);
                                if (summaryCount == null) {
                                    summaryCount = new Integer(0);
                                }
                                summary.put(summaryLine, new Integer(summaryCount.intValue()
                                            + 1));
                            }
                        }
                    }
                }
                currentLocations.put(location, lsf);
            }
            LOG.info("Stored " + count + " overlaps for " + results.size()
                    + " features on feature id " + subject.getId() + ", identifier: "
                     + subject.getSecondaryIdentifier());
            Integer summaryCount = summary.get("total");
            if (summaryCount == null) {
                summaryCount = new Integer(0);
            }
            summary.put("total", new Integer(summaryCount.intValue() + count));
        } finally {
            ((ObjectStoreInterMineImpl) os).releaseGoFaster(q);
        }
    }

    /**
     * Return true if and only if the given SequenceFeature should be ignored when looking
     * for overlaps.
     */
    private static boolean isAClassToIgnore(Map<Class<?>, Set<Class<?>>> classesToIgnore,
            Class<?> clazz) {
        return ignoreCombination(classesToIgnore, clazz, InterMineObject.class);
    }

    /**
     * Return true if the given class should be ignored when overlapping with the other given class.
     */
    private static boolean ignoreCombination(Map<Class<?>, Set<Class<?>>> classesToIgnore,
            Class<?> class1, Class<?> class2) {
        Iterator<?> iter = classesToIgnore.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<Class<?>, Set<Class<?>>> entry = (Map.Entry<Class<?>, Set<Class<?>>>)
                iter.next();
            Class<?> thisClass = entry.getKey();
            Set<Class<?>> targetClasses = entry.getValue();

            if (thisClass.isAssignableFrom(class1)) {
                Iterator<?> iter2 = targetClasses.iterator();
                while (iter2.hasNext()) {
                    Class<?> targetClass = (Class<?>) iter2.next();
                    if (targetClass.isAssignableFrom(class2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
