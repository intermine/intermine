package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2022 FlyMine
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
import java.util.Map;
import java.util.Set;
import org.intermine.bio.util.Constants;
import org.intermine.bio.util.PostProcessUtil;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.metadata.ConstraintOp;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.Location;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.postprocess.PostProcessor;

/**
 * Create a Location that spans the locations of some child objects.  eg. create a location for
 * Transcript that is as big as all the exons in it's exons collection.  One new location will
 * be created for each possible Location.object - Transcript->Chromosome, Transcript->Contig
 * etc.
 * @author Richard Smith
 * @author Kim Rutherford
 */
public class MakeSpanningLocationsProcess extends PostProcessor
{
    Model model = Model.getInstanceByName("genomic");
    protected ObjectStore os;

    /**
     * Create a new instance
     *
     * @param osw object store writer
     */
    public MakeSpanningLocationsProcess(ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * {@inheritDoc}
     * <br/>
     * Main post-processing routine.
     *
     * @throws ObjectStoreException if the objectstore throws an exception
     */
    public void postProcess()
            throws ObjectStoreException {
        this.os = osw.getObjectStore();
        createSpanningLocations("Transcript", "Exon", "exons");
        createSpanningLocations("Gene", "Transcript", "transcripts");
    }

    /**
     * Create a Location that spans the locations of some child objects.  eg. create a location for
     * Transcript that is as big as all the exons in it's exons collection.  One new location will
     * be created for each possible Location.object - Transcript->Chromosome, Transcript->Contig
     * etc.
     * @param parentClsName the parent, eg. Transcript
     * @param childClsName the child, eg. Exon
     * @param refField the linking field eg. "exons"
     * @throws ObjectStoreException if the is a problem with the ObjectStore
     */
    protected void createSpanningLocations(String parentClsName, String childClsName,
        String refField) throws ObjectStoreException {

        try {
            String message = "Not performing CalculateLocations.createSpanningLocations("
                    + parentClsName + ", " + childClsName + ", " + refField + ") ";
            PostProcessUtil.checkFieldExists(model, parentClsName, refField, message);
            PostProcessUtil.checkFieldExists(model, childClsName, null, message);
        } catch (MetaDataException e) {
            return;
        }

        Class<?> parentClass = model.getClassDescriptorByName(parentClsName).getType();
        Class<?> childClass = model.getClassDescriptorByName(childClsName).getType();

        Query parentIdQuery =
                new IqlQuery("SELECT DISTINCT a1_.id as id FROM "
                        + parentClass.getName() + " AS a1_, org.intermine.model.bio.Location "
                        + "AS a2_, org.intermine.model.bio.BioEntity as a3_ "
                        + "WHERE (a1_.locations CONTAINS a2_ "
                        + "and a3_.locatedFeatures CONTAINS a2_)", null).toQuery();
        if (os == null) {
            os = osw.getObjectStore();
        }
        Results parentIdResults = os.execute(parentIdQuery);
        Set<Object> locatedParents = new HashSet<Object>();
        Iterator<?> parentIdIter = parentIdResults.iterator();

        while (parentIdIter.hasNext()) {
            Object parentId = ((ResultsRow<?>) parentIdIter.next()).get(0);
            locatedParents.add(parentId);
        }

        Iterator<?> resIter = findCollections(os, parentClass, childClass, refField);

        // Map of location.objects to Maps from parent objects to a to their (new) start and end
        // positions.  eg.  Chromosome10 -> Exon1 -> SimpleLoc {start -> 2111, end -> 2999}
        //                  Contig23 ->     Exon1 -> SimpleLoc {start -> 1111, end -> 1999}
        Map<Integer, Map<Integer, SimpleLoc>> locatedOnObjectMap
                = new HashMap<Integer, Map<Integer, SimpleLoc>>();

        while (resIter.hasNext()) {
            ResultsRow<?> rr = (ResultsRow<?>) resIter.next();

            BioEntity parentObject = (BioEntity) rr.get(0);
            Location location = (Location) rr.get(2);

            // the object that childObject is located on
            BioEntity locatedOnObject = (BioEntity) rr.get(3);

            // ignore objects that already have locations
            Integer parentObjectId = parentObject.getId();
            if (locatedParents.contains(parentObjectId)) {
                continue;
            }

            Map<Integer, SimpleLoc> parentObjectMap
                    = locatedOnObjectMap.get(locatedOnObject.getId());

            if (parentObjectMap == null) {
                parentObjectMap = new HashMap<Integer, SimpleLoc>();
                locatedOnObjectMap.put(locatedOnObject.getId(), parentObjectMap);
            }

            SimpleLoc parentObjectSimpleLoc = parentObjectMap.get(parentObjectId);

            if (parentObjectSimpleLoc == null) {
                parentObjectSimpleLoc = new SimpleLoc(-1, -1, Integer.MAX_VALUE, -1, "0");
                parentObjectMap.put(parentObjectId, parentObjectSimpleLoc);
            }

            int currentParentStart = parentObjectSimpleLoc.getStart();
            int currentParentEnd = parentObjectSimpleLoc.getEnd();

            if (location.getStart().intValue() < currentParentStart) {
                parentObjectSimpleLoc.setStart(location.getStart().intValue());
            }

            if (location.getEnd().intValue() > currentParentEnd) {
                parentObjectSimpleLoc.setEnd(location.getEnd().intValue());
            }

            parentObjectSimpleLoc.setStrand(location.getStrand());

            // TODO XXX FIXME: deal with partial locations and do consistency checks (eg. make
            // sure all exons are on the same strand)
        }

        osw.beginTransaction();
        // make new locations and store them
        Iterator<?> locatedOnObjectIterator = locatedOnObjectMap.keySet().iterator();
        while (locatedOnObjectIterator.hasNext()) {
            Integer locatedOnObjectId = (Integer) locatedOnObjectIterator.next();
            BioEntity locatedOnObject = (BioEntity) os.getObjectById(locatedOnObjectId);
            Map<Integer, SimpleLoc> parentObjectMap
                    = locatedOnObjectMap.get(locatedOnObjectId);
            Iterator<?> parentObjectMapIterator = parentObjectMap.keySet().iterator();

            while (parentObjectMapIterator.hasNext()) {
                Integer parentObjectId = (Integer) parentObjectMapIterator.next();
                BioEntity parentObject = (BioEntity) os.getObjectById(parentObjectId);
                SimpleLoc parentObjectSimpleLoc = parentObjectMap.get(parentObjectId);
                Location newLocation =
                        (Location) DynamicUtil.createObject(Collections.singleton(Location.class));

                newLocation.setStart(new Integer(parentObjectSimpleLoc.getStart()));
                newLocation.setEnd(new Integer(parentObjectSimpleLoc.getEnd()));
                newLocation.setStrand(parentObjectSimpleLoc.getStrand());
                newLocation.setFeature(parentObject);
                newLocation.setLocatedOn(locatedOnObject);

                osw.store(newLocation);
            }
        }
        osw.commitTransaction();
    }

    /**
     * Query a class like Transcript that refers to a collection of located classes (like Exon) and
     * return an Results object containing Transcript, Exon, Exon location and location.object
     */
    private static Iterator<?> findCollections(ObjectStore os, Class<?> parentClass,
                                               Class<?> childClass, String refField)
            throws ObjectStoreException {

        Query q = new Query();
        q.setDistinct(false);
        QueryClass qcParent = new QueryClass(parentClass);
        q.addFrom(qcParent);
        q.addToSelect(qcParent);
        q.addToOrderBy(qcParent);
        QueryClass qcChild = new QueryClass(childClass);
        q.addFrom(qcChild);
        q.addToSelect(qcChild);

        QueryClass qcLoc = new QueryClass(Location.class);
        q.addFrom(qcLoc);
        q.addToSelect(qcLoc);

        QueryClass qcLocObject = new QueryClass(BioEntity.class);
        q.addFrom(qcLocObject);
        q.addToSelect(qcLocObject);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryObjectReference ref1 = new QueryObjectReference(qcLoc, "locatedOn");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcLocObject);
        cs.addConstraint(cc1);
        QueryObjectReference ref2 = new QueryObjectReference(qcLoc, "feature");
        ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcChild);
        cs.addConstraint(cc2);

        QueryCollectionReference ref3 = new QueryCollectionReference(qcParent, refField);
        ContainsConstraint cc3 = new ContainsConstraint(ref3, ConstraintOp.CONTAINS, qcChild);
        cs.addConstraint(cc3);

        q.setConstraint(cs);

        ((ObjectStoreInterMineImpl) os).precompute(q,
                Constants.PRECOMPUTE_CATEGORY);
        Results res = os.execute(q, 1000, true, true, true);
        return res.iterator();
    }


    /**
     * Lightweight representation of a Location for easier manipulation and
     * storing in maps.
     */
    protected class SimpleLoc
    {
        private int start;
        private int parentId;
        private int childId;
        private String strand;
        private int end;

        /**
         * Construct with integer values
         * @param parentId id of object
         * @param childId id of subject
         * @param start start value
         * @param end end value
         * @param strand strand value
         */
        public SimpleLoc(int parentId, int childId, int start, int end, String strand) {
            this.parentId = parentId;
            this.childId = childId;
            this.start = start;
            this.end = end;
            this.strand = strand;
        }

        /**
         * Construct with integer values for object and subject and a Location object
         * @param parentId id of object
         * @param childId id of subject
         * @param loc description of location
         */
        public SimpleLoc(int parentId, int childId, Location loc) {
            this.parentId = parentId;
            this.childId = childId;
            this.start = loc.getStart().intValue();
            this.end = loc.getEnd().intValue();
            if (loc.getStrand() != null) {
                this.strand = loc.getStrand();
            } else {
                this.strand = "0";
            }
        }

        /**
         * Get start value
         * @return start value
         */
        public int getStart() {
            return start;
        }

        /**
         * Set start value
         * @param start value
         */
        public void setStart(int start) {
            this.start = start;
        }

        /**
         * Get parentId value
         * @return parentId value
         */
        public int getParentId() {
            return parentId;
        }

        /**
         * Get childId value
         * @return childId value
         */
        public int getChildId() {
            return childId;
        }

        /**
         * Get start value
         * @return start value
         */
        public int getEnd() {
            return end;
        }

        /**
         * Set end value
         * @param end value
         */
        public void setEnd(int end) {
            this.end = end;
        }

        /**
         * Get strand value
         * @return strand value
         */
        public String getStrand() {
            return strand;
        }

        /**
         * Set strand value
         * @param strand value
         */
        public void setStrand(String strand) {
            this.strand = strand;
        }

        /**
         * @see Object#toString()
         * @return String representation of location object
         */
        @Override
        public String toString() {
            return "parent " + parentId + " child " + childId + " start " + start
                    + " end " + end + " strand " + strand;
        }
    }
}


