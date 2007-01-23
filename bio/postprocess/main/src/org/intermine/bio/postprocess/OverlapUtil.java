package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.util.DynamicUtil;

import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Location;
import org.flymine.model.genomic.OverlapRelation;

import org.apache.log4j.Logger;

/**
 * Utility methods for finding overlaps.
 *
 * @author Kim Rutherford
 * @author Matthew Wakeling
 */
public abstract class OverlapUtil
{
    private static final Logger LOG = Logger.getLogger(OverlapUtil.class);

    /**
     * Creates OverlapRelations for overlapping LocatedSequenceFeature objects that are located
     * on the given subject (generally a Chromosome).
     * 
     * @param os the ObjectStore to query
     * @param subject the LocatedSequenceFeature (eg. a Chromosome) where the LSFs are located
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
    public static void createOverlaps(final ObjectStore os, LocatedSequenceFeature subject,
            List classNamesToIgnore, boolean ignoreSelfMatches, ObjectStoreWriter osw, Map summary)
        throws ObjectStoreException, ClassNotFoundException {
        Model model = os.getModel();

        Map classesToIgnore = new HashMap();

        Iterator classNamesToIgnoreIter = classNamesToIgnore.iterator();

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

            Class thisClass = Class.forName(className);
            Class targetClass = Class.forName(targetClassName);

            Set targetClasses = (Set) classesToIgnore.get(thisClass);
            if (targetClasses == null) {
                targetClasses = new HashSet();
                classesToIgnore.put(thisClass, targetClasses);
            }
            targetClasses.add(targetClass);
        }

        Query q = new Query();
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        q.setConstraint(cs);

        QueryClass qcLoc = new QueryClass(Location.class);
        q.addFrom(qcLoc);
        q.addToSelect(qcLoc);

        q.setDistinct(false);
        QueryClass qcObj = new QueryClass(LocatedSequenceFeature.class);
        q.addFrom(qcObj);
        q.addToSelect(qcObj);

        QueryObjectReference ref1 = new QueryObjectReference(qcLoc, "subject");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcObj);
        cs.addConstraint(cc1);

        QueryObjectReference ref2 = new QueryObjectReference(qcLoc, "object");
        ContainsConstraint subjectIdConstraint = new ContainsConstraint(ref2, ConstraintOp.CONTAINS,
                subject);
        cs.addConstraint(subjectIdConstraint);

        /*if (subject instanceof Chromosome) {
            // improve the speed by adding an extra contain for locations on chromosomes
            QueryObjectReference objChromosomeRef = new QueryObjectReference(qcObj, "chromosome");
            ContainsConstraint chromosomeConstraint =
                new ContainsConstraint(objChromosomeRef, ConstraintOp.CONTAINS, subject);

            cs.addConstraint(chromosomeConstraint);
        }*/

        q.addToOrderBy(new QueryField(qcLoc, "start"));

        try {
            ((ObjectStoreInterMineImpl) os).goFaster(q);

            Results results = os.execute(q);

            // A Map from Location to the corresponding LocatedSequenceFeature
            Map currentLocations = new HashMap();

            int count = 0;
            Iterator resIter = results.iterator();

            while (resIter.hasNext()) {
                ResultsRow rr = (ResultsRow) resIter.next();

                Location location = (Location) rr.get(0);

                if (location.getStart() == null || location.getEnd() == null) {
                    continue;
                }

                LocatedSequenceFeature lsf = (LocatedSequenceFeature) rr.get(1);

                if (isAClassToIgnore(classesToIgnore, lsf.getClass())) {
                    continue;
                }

                int start = location.getStart().intValue();

                // Okay, first we compare this location to all the currentLocations.
                Iterator currIter = currentLocations.entrySet().iterator();
                while (currIter.hasNext()) {
                    Map.Entry currEntry = (Map.Entry) currIter.next();
                    Location currLoc = (Location) currEntry.getKey();
                    LocatedSequenceFeature currLsf = (LocatedSequenceFeature) currEntry
                        .getValue();
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
                                OverlapRelation overlapRelation = (OverlapRelation)
                                    DynamicUtil.createObject(Collections.singleton(
                                                OverlapRelation.class));
                                Set bioEntityCollection = overlapRelation.getBioEntities();
                                bioEntityCollection.add(lsf);
                                bioEntityCollection.add(currLsf);

                                ++count;

                                osw.store(overlapRelation);
                                osw.addToCollection(lsf.getId(), LocatedSequenceFeature.class,
                                        "overlappingFeatures", currLsf.getId());
                                osw.addToCollection(currLsf.getId(),
                                        LocatedSequenceFeature.class,
                                        "overlappingFeatures", lsf.getId());

                                // Log it, for the summary.
                                String classname1 = DynamicUtil.getFriendlyName(lsf.getClass());
                                String classname2 = DynamicUtil.getFriendlyName(currLsf
                                        .getClass());

                                String summaryLine = classname1.compareTo(classname2) > 0
                                    ? classname2 + " - " + classname1 : classname1 + " - "
                                    + classname2;
                                Integer summaryCount = (Integer) summary.get(summaryLine);
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
            LOG.info("Stored " + count + " overlaps for id " + subject.getId() + ", identifier: "
                     + subject.getIdentifier());
            Integer summaryCount = (Integer) summary.get("total");
            if (summaryCount == null) {
                summaryCount = new Integer(0);
            }
            summary.put("total", new Integer(summaryCount.intValue() + count));
        } finally {
            ((ObjectStoreInterMineImpl) os).releaseGoFaster(q);
        }
    }

    /**
     * Return true if and only if the given LocatedSequenceFeature should be ignored when looking
     * for overlaps.
     */
    private static boolean isAClassToIgnore(Map classesToIgnore, Class clazz) {
        return ignoreCombination(classesToIgnore, clazz, InterMineObject.class);
    }

    /**
     * Return true if the given class should be ignored when overlapping with the other given class.
     */
    private static boolean ignoreCombination(Map classesToIgnore, Class class1, Class class2) {
        Iterator iter = classesToIgnore.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Class thisClass = (Class) entry.getKey();
            Set targetClasses = (Set) entry.getValue();

            if (thisClass.isAssignableFrom(class1)) {
                Iterator iter2 = targetClasses.iterator();
                while (iter2.hasNext()) {
                    Class targetClass = (Class) iter2.next();
                    if (targetClass.isAssignableFrom(class2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
