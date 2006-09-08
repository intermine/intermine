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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;

import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Location;

/**
 * Utility methods for finding overlaps.
 *
 * @author Kim Rutherford
 */

public abstract class OverlapUtil
{
    /**
     * Return an Iterator for pairs of overlapping LocatedSequenceFeature objects that are located
     * on the given subject (generally a Chromosome).
     * @param os the ObjectStore to query
     * @param subject the LocatedSequenceFeature (eg. a Chromosome) where the LSFs are located
     * @param classNamesToIgnore a comma separated list of the names of those classes that should be
     * ignored when searching for overlaps.  Sub classes to these classes are ignored too
     * @param ignoreSelfMatches if true, don't create OverlapRelations between two objects of the
     * same class.
     * @return an Iterator over the overlapping features
     * @throws ObjectStoreException if an error occurs while writing
     * @throws ClassNotFoundException if there is an ObjectStore problem
     */
    public static Iterator findOverlaps(final ObjectStore os, LocatedSequenceFeature subject,
                                        List classNamesToIgnore, boolean ignoreSelfMatches)
        throws ObjectStoreException, ClassNotFoundException {
        Model model = os.getModel();

        Set classesToIgnore = new HashSet();

        Iterator classNamesToIgnoreIter = classNamesToIgnore.iterator();

        while (classNamesToIgnoreIter.hasNext()) {
            String className = (String) classNamesToIgnoreIter.next();

            String fullClassName;

            if (className.indexOf(".") == -1) {
                fullClassName = model.getPackageName() + "." + className;
            } else {
                fullClassName = className;
            }

            Class thisClass = Class.forName(fullClassName);

            classesToIgnore.add(thisClass);
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

        QueryClass qcSub;

        if (subject instanceof Chromosome) {
            // Special case that will hopefully make things faster
            qcSub = new QueryClass(Chromosome.class);
        } else {
            qcSub = new QueryClass(LocatedSequenceFeature.class);
        }

        q.addFrom(qcSub);

        QueryObjectReference ref1 = new QueryObjectReference(qcLoc, "subject");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcObj);
        cs.addConstraint(cc1);

        QueryObjectReference ref2 = new QueryObjectReference(qcLoc, "object");
        ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcSub);
        cs.addConstraint(cc2);

        QueryField subIdQF = new QueryField(qcSub, "id");
        SimpleConstraint subjectIdConstraint =
            new SimpleConstraint(subIdQF, ConstraintOp.EQUALS, new QueryValue(subject.getId()));

        cs.addConstraint(subjectIdConstraint);

        if (subject instanceof Chromosome) {
            // improve the speed by adding an extra contain for locations on chromosomes
            QueryObjectReference objChromosomeRef = new QueryObjectReference(qcObj, "chromosome");
            ContainsConstraint chromosomeConstraint =
                new ContainsConstraint(objChromosomeRef, ConstraintOp.CONTAINS, qcSub);

            cs.addConstraint(chromosomeConstraint);
        }

        ((ObjectStoreInterMineImpl) os).precompute(q, PostProcessTask.PRECOMPUTE_CATEGORY);

        Results results = os.execute(q);

        // ordered by length - largest first
        final TreeMap locationsByLength = new TreeMap(new SimpleLocLengthComparator());

        // ordered by start position - smallest first
        final TreeMap locationsByStartPos = new TreeMap(new SimpleLocStartComparator());

        // a Map from Location ID to subject LocatedSequenceFeature
        final Map locationSubjectMap = new HashMap();

        Iterator resIter = results.iterator();

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();

            Location location = (Location) rr.get(0);

            if (location.getStart() == null || location.getEnd() == null) {
                continue;
            }

            LocatedSequenceFeature lsf = (LocatedSequenceFeature) rr.get(1);

            if (isAClassToIgnore(classesToIgnore, lsf)) {
                continue;
            }

            locationsByLength.put(location, location);
            locationsByStartPos.put(location, location);

            locationSubjectMap.put(location.getId(), lsf);
        }

        return new OverlappingFeaturesIterator(locationsByLength, locationsByStartPos,
                                               locationSubjectMap, ignoreSelfMatches);
    }

    /**
     * Return true if and only if the given LocatedSequenceFeature should be ignored when looking
     * for overlaps.
     */
    private static boolean isAClassToIgnore(Set classesToIgnore, LocatedSequenceFeature lsf) {
        Iterator classesToIgnoreIter = classesToIgnore.iterator();

        while (classesToIgnoreIter.hasNext()) {
            Class thisClass = (Class) classesToIgnoreIter.next();

            if (thisClass.isAssignableFrom(lsf.getClass())) {
                return true;
            }
        }

        return false;
    }

    /**
     * The Iterator returned by findOverlaps().
     */
    private static final class OverlappingFeaturesIterator implements Iterator
    {
        private final TreeMap locationsByStartPos;
        private final boolean ignoreSelfMatches;
        private Iterator locationsByLengthIter = null;
        private Iterator otherLocationIter = null;
        private Location currentLocation = null;
        private Location[] nextReturnValue = null;
        private final Map locationSubjectMap;

        /**
         * Create a new OverlappingFeaturesIterator.
         * @param locationsByLengthMap A Map from LocatedSequenceFeature lengths to Location
         * @param locationsByStartPos A Map from Location start position to Location
         * @param locationSubjectMap
         * @param ignoreSelfMatches if true, don't create OverlapRelations between two objects of
         * the same class.
         */
        private OverlappingFeaturesIterator(TreeMap locationsByLengthMap,
                                            TreeMap locationsByStartPos,
                                            Map locationSubjectMap, boolean ignoreSelfMatches) {
            super();
            this.locationsByStartPos = locationsByStartPos;
            this.locationSubjectMap  = locationSubjectMap;
            this.ignoreSelfMatches = ignoreSelfMatches;
            locationsByLengthIter = locationsByLengthMap.entrySet().iterator();
        }

        /**
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            nextReturnValue = getNextReturnValue();

            return nextReturnValue != null;
        }


        /**
         * Return next pair of overlapping Locations, or null when we run out.
         * @return the next pair
         */
        private Location[] getNextReturnValue() {
            while (otherLocationIter != null && otherLocationIter.hasNext()) {
                Location nextOtherLocation = (Location) otherLocationIter.next();

                Location[] returnArray = new Location[2];
                returnArray[0] = currentLocation;
                returnArray[1] = nextOtherLocation;
                return returnArray;
            }

            while (locationsByLengthIter.hasNext()) {
                Map.Entry entry = (Entry) locationsByLengthIter.next();

                Location location = (Location) entry.getValue();
                int length = getLocationLength(location);

                currentLocation = location;

                List overlappingLocations = getOverlappingLocations(location, length);

                // remove from the locationsByStartPos Map so that we find each overlapping
                // pair once only
                removeFromLocationsFromStartPos(location);

                otherLocationIter = overlappingLocations.iterator();

                if (otherLocationIter.hasNext()) {
                    return getNextReturnValue();
                }
            }

            return null;
        }

        /**
         * Return the next pair of overlapping Locations as a Location[].
         * @see java.util.Iterator#next()
         */
        public Object next() throws NoSuchElementException {
            if (nextReturnValue == null) {
                throw new NoSuchElementException("next() called twice");
            }

            Location[] returnValue = nextReturnValue;

            nextReturnValue = null;

            return returnValue;
        }

        /**
         * remove() is unsupported
         * @see java.util.Iterator#remove()
         */
        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        private void removeFromLocationsFromStartPos(Location location) {
            Map subMap = locationsByStartPos.subMap(location.getStart(),
                                                    new Integer(location.getEnd().intValue() + 1));
            Iterator subMapIter = subMap.entrySet().iterator();

            while (subMapIter.hasNext()) {
                Map.Entry entry = (Map.Entry) subMapIter.next();
                Location thisLocation = (Location) entry.getValue();

                if (location == thisLocation) {
                    subMapIter.remove();
                }
            }
        }

        // look at a range between start-length .. end
        // we know that we don't need to look further back because we are examining the
        // locations in reverse length order
        private List getOverlappingLocations(Location thisLocation, int length) {
            List overlappingLocations = new ArrayList();

            Map subRange =
                locationsByStartPos.subMap(new Integer(thisLocation.getStart().intValue() - length),
                                           new Integer(thisLocation.getEnd().intValue() + 1));

            Iterator subRangeIter = subRange.values().iterator();

            while (subRangeIter.hasNext()) {
                Location otherLocation = (Location) subRangeIter.next();
                LocatedSequenceFeature otherLsf =
                    (LocatedSequenceFeature) locationSubjectMap.get(thisLocation.getId());
                Class thisLocationClass = otherLsf.getClass();
                LocatedSequenceFeature thisLsf =
                    (LocatedSequenceFeature) locationSubjectMap.get(otherLocation.getId());
                Class otherLocationClass = thisLsf.getClass();
                if (thisLocation != otherLocation
                    && (!ignoreSelfMatches || !thisLocationClass.equals(otherLocationClass))
                    && (thisLocation.getStart().intValue() <= otherLocation.getStart().intValue()
                        && thisLocation.getEnd().intValue() >= otherLocation.getStart().intValue()
                        || thisLocation.getStart().intValue() <= otherLocation.getEnd().intValue()
                        && thisLocation.getEnd().intValue() >= otherLocation.getEnd().intValue())) {
                    Location originalLocation = otherLocation;
                    overlappingLocations.add(originalLocation);
                }
            }

            return overlappingLocations;
        }
    }

    /**
     * A Comparator that compares Location objects by start position, or by id for pairs of
     * Location objects that have the same start position.
     */
    private static class SimpleLocStartComparator implements Comparator
    {
        /**
         * Return -1 if the first Location (o1) starts before the second (o2) or if the start
         * positions are the same but o1.id is less than o2.id, return 0 if the starts and ids
         * are the same, and 1 otherwise.
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object o1, Object o2) {
            int loc1Start = -1;
            int loc2Start = -1;

            Location loc1 = null;
            Location loc2 = null;

            if (o1 instanceof Location) {
                loc1 = (Location) o1;
                loc1Start = loc1.getStart().intValue();
            } else {
                // get() is being called with an Integer
                loc1Start = ((Integer) o1).intValue();
            }

            if (o2 instanceof Location) {
                loc2 = (Location) o2;
                loc2Start = loc2.getStart().intValue();
            } else {
                // get() is being called with an Integer
                loc2Start = ((Integer) o2).intValue();
            }

            if (loc1Start < loc2Start) {
                return -1;
            } else {
                if (loc1Start > loc2Start) {
                    return 1;
                } else {
                    if (loc1 == null) {
                        return -1;
                    }
                    if (loc2 == null) {
                        return 1;
                    }

                    if (loc1.getId().intValue() < loc2.getId().intValue()) {
                        return -1;
                    } else {
                        if (loc1.getId().intValue() > loc2.getId().intValue()) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                }
            }
        }
    }

    /**
     * A Comparator that compares SimpleLoc objects by length, or by id for pairs of
     * SimpleLoc objects that have the same length position.  Longer locations are ordered first.
     */
    private static class SimpleLocLengthComparator implements Comparator
    {
        /**
         * Return -1 if the length of the first Location (o1) is less than the length of the second
         * (o2) or if the lengths are the same, but o1.id is less than o2.id, return 0 if the
         * lengths and ids are the same, and 1 otherwise.
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object o1, Object o2) {
            if (o1 instanceof Location && o2 instanceof Location) {
                Location loc1 = (Location) o1;
                Location loc2 = (Location) o2;
                if (getLocationLength(loc1) > getLocationLength(loc2)) {
                    return -1;
                } else {
                    if (getLocationLength(loc1) < getLocationLength(loc2)) {
                        return 1;
                    } else {
                        if (loc1.getId().intValue() < loc2.getId().intValue()) {
                            return -1;
                        } else {
                            if (loc1.getId().intValue() > loc2.getId().intValue()) {
                                return 1;
                            } else {
                                return 0;
                            }
                        }
                    }
                }

            } else {
                throw new RuntimeException("argument to compare() is not a Location");
            }
        }
    }

    /**
     * Return the length of the given Location (ie. end - start + 1).
     * @param location the Location
     * @return the length
     */
    static int getLocationLength(Location location) {
        return location.getEnd().intValue() - location.getStart().intValue() + 1;
    }
}
