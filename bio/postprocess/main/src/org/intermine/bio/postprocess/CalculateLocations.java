package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.flymine.model.genomic.AssemblyComponent;
import org.flymine.model.genomic.BioEntity;
import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Location;
import org.flymine.model.genomic.PartialLocation;
import org.intermine.bio.util.BioQueries;
import org.intermine.bio.util.Constants;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.util.DynamicUtil;


/**
 * Calculate additional mappings between annotation after loading into genomic ObjectStore.
 * Currently designed to cope with situation after loading ensembl, may need to change
 * as other annotation is loaded.  New Locations (and updated BioEntities) are stored
 * back in originating ObjectStore.
 *
 * @author Richard Smith
 * @author Kim Rutherford
 */
public class CalculateLocations
{
    private static final Logger LOG = Logger.getLogger(CalculateLocations.class);

    protected ObjectStoreWriter osw;
    protected ObjectStore os;



    /**
     * Create a new CalculateLocations object from an ObjectStoreWriter
     * @param osw writer on genomic ObjectStore
     */
    public CalculateLocations(ObjectStoreWriter osw) {
        this.osw = osw;
        this.os = osw.getObjectStore();
    }

 
    /**
     * Create OverlapRelation objects for all overlapping LocatedSequenceFeatures by querying
     * objects that are located on chromosomes and overlap.
     * @param classNamesToIgnore a List of the names of those classes that should be ignored when
     * searching for overlaps.  Sub classes to these classes are ignored too. In addition, an
     * entry can be of the form class=class, which specifies that the particular combination should
     * be ignored. Hence an entry of the form class is equivalent to class=InterMineObject
     * @param ignoreSelfMatches if true, don't create OverlapRelations between two objects of the
     * same class
     * @throws Exception if anything goes wrong
     */
    public void createOverlapRelations(List classNamesToIgnore, boolean ignoreSelfMatches)
         throws Exception {
        osw.beginTransaction();
        Map summary = new HashMap();
        Map chromosomeMap = makeChromosomeMap();
        Iterator chromosomeIdIter = chromosomeMap.keySet().iterator();
        while (chromosomeIdIter.hasNext()) {
            Integer id = (Integer) chromosomeIdIter.next();
            createSubjectOverlapRelations((Chromosome) chromosomeMap.get(id), classNamesToIgnore,
                    ignoreSelfMatches, summary);
        }
        osw.commitTransaction();
        LOG.info("Stored a total of " + summary.remove("total") + " overlaps");
        List sortList = new ArrayList();
        Iterator summaryIter = summary.entrySet().iterator();
        while (summaryIter.hasNext()) {
            Map.Entry summaryEntry = (Map.Entry) summaryIter.next();
            sortList.add(new SortElement((String) summaryEntry.getKey(),
                        ((Integer) summaryEntry.getValue()).intValue()));
        }
        Collections.sort(sortList);
        summaryIter = sortList.iterator();
        while (summaryIter.hasNext()) {
            LOG.info(((SortElement) summaryIter.next()).toString());
        }
    }

    private class SortElement implements Comparable
    {
        String text;
        int number;

        public SortElement(String text, int number) {
            this.text = text;
            this.number = number;
        }

        public int compareTo(Object o) {
            int retval = ((SortElement) o).number - number;
            if (retval == 0) {
                retval = ((SortElement) o).text.compareTo(text);
            }
            return retval;
        }

        public String toString() {
            return number + " overlap" + (number == 1 ? "" : "s") + " for " + text;
        }
    }

    /**
     * Create OverlapRelation objects for locations that have the given subject.
     *
     * @param subject a Chromosome object on which to create the overlaps
     * @param classNamesToIgnore a List of the names of those classes that should be ignored when
     * searching for overlaps.  Sub classes to these classes are ignored too. In addition, an
     * entry can be of the form class=class, which specifies that the particular combination should
     * be ignored. Hence an entry of the form class is equivalent to class=InterMineObject
     * @param ignoreSelfMatches if true, don't create OverlapRelations between two objects of the
     * same class
     * @param summary a Map to which summary data will be added
     */
    private void createSubjectOverlapRelations(Chromosome subject, List classNamesToIgnore,
            boolean ignoreSelfMatches, Map summary) throws Exception {
        LOG.info("Creating overlaps for id " + subject.getId() + ", identifier: "
                 + subject.getPrimaryIdentifier());

        OverlapUtil.createOverlaps(os, subject, classNamesToIgnore, ignoreSelfMatches, osw,
                summary);
    }


    /**
     * Create a Location that spans the locations of some child objects.  eg. create a location for
     * Transcript that is as big as all the exons in it's exons collection.  One new location will
     * be created for each possible Location.object - Transcript->Chromosome, Transcript->Contig
     * etc.
     * @param parentClass the parent, eg. Transcript
     * @param childClass the child, eg. Exon
     * @param refField the linking field eg. "exons"
     * @throws ObjectStoreException if the is a problem with the ObjectStore
     */
    public void createSpanningLocations(Class parentClass, Class childClass, String refField)
        throws ObjectStoreException {

        Query parentIdQuery =
            new IqlQuery("SELECT DISTINCT a1_.id as id FROM "
                         + parentClass.getName() + " AS a1_, org.flymine.model.genomic.Location "
                         + "AS a2_, org.flymine.model.genomic.BioEntity as a3_"
                         + " WHERE (a1_.objects CONTAINS a2_ and a3_.subjects CONTAINS a2_)",
                         null).toQuery();

        Results parentIdResults = os.execute(parentIdQuery);
        Set locatedParents = new HashSet();
        Iterator parentIdIter = parentIdResults.iterator();

        while (parentIdIter.hasNext()) {
            Object parentId = ((ResultsRow) parentIdIter.next()).get(0);
            locatedParents.add(parentId);
        }

        Iterator resIter = findCollections(os, parentClass, childClass, refField);

        // Map of location.objects to Maps from parent objects to a to their (new) start and end
        // positions.  eg.  Chromosome10 -> Exon1 -> SimpleLoc {start -> 2111, end -> 2999}
        //                  Contig23 ->     Exon1 -> SimpleLoc {start -> 1111, end -> 1999}
        Map locatedOnObjectMap = new HashMap();

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();

            BioEntity parentObject = (BioEntity) rr.get(0);
            Location location = (Location) rr.get(2);

            // the object that childObject is located on
            BioEntity locatedOnObject = (BioEntity) rr.get(3);

            // ignore objects that already have locations on Chromosomes
            Integer parentObjectId = parentObject.getId();
            if ((locatedOnObject instanceof Chromosome
                 || locatedOnObject instanceof AssemblyComponent)
                && locatedParents.contains(parentObjectId)) {
                continue;
            }

            Map parentObjectMap = (Map) locatedOnObjectMap.get(locatedOnObject.getId());

            if (parentObjectMap == null) {
                parentObjectMap = new HashMap();
                locatedOnObjectMap.put(locatedOnObject.getId(), parentObjectMap);
            }

            SimpleLoc parentObjectSimpleLoc = (SimpleLoc) parentObjectMap.get(parentObjectId);

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
        Iterator locatedOnObjectIterator = locatedOnObjectMap.keySet().iterator();
        while (locatedOnObjectIterator.hasNext()) {
            Integer locatedOnObjectId = (Integer) locatedOnObjectIterator.next();
            BioEntity locatedOnObject = (BioEntity) os.getObjectById(locatedOnObjectId);
            Map parentObjectMap = (Map) locatedOnObjectMap.get(locatedOnObjectId);
            Iterator parentObjectMapIterator = parentObjectMap.keySet().iterator();

            while (parentObjectMapIterator.hasNext()) {
                Integer parentObjectId = (Integer) parentObjectMapIterator.next();
                BioEntity parentObject = (BioEntity) os.getObjectById(parentObjectId);
                SimpleLoc parentObjectSimpleLoc = (SimpleLoc) parentObjectMap.get(parentObjectId);
                Location newLocation =
                    (Location) DynamicUtil.createObject(Collections.singleton(Location.class));


                newLocation.setStart(new Integer(parentObjectSimpleLoc.getStart()));
                newLocation.setEnd(new Integer(parentObjectSimpleLoc.getEnd()));
                newLocation.setStartIsPartial(Boolean.FALSE);
                newLocation.setEndIsPartial(Boolean.FALSE);
                newLocation.setStrand(parentObjectSimpleLoc.getStrand());
                newLocation.setSubject(parentObject);
                newLocation.setObject(locatedOnObject);

                osw.store(newLocation);
            }
        }
        osw.commitTransaction();
    }

    /**
     * Query a class like Transcript that refers to a collection of located classes (like Exon) and
     * return an Results object containing Transcript, Exon, Exon location and location.object
     */
    private static Iterator findCollections(ObjectStore os, Class parentClass, Class childClass,
                                            String refField)
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

        QueryObjectReference ref1 = new QueryObjectReference(qcLoc, "object");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcLocObject);
        cs.addConstraint(cc1);
        QueryObjectReference ref2 = new QueryObjectReference(qcLoc, "subject");
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
     * Hold Chromosomes in map by id
     */
    private Map makeChromosomeMap() throws Exception {
        Map returnMap = new HashMap();
        Query q = new Query();
        QueryClass qc = new QueryClass(Chromosome.class);
        q.addToSelect(qc);
        q.addFrom(qc);

        SingletonResults sr = os.executeSingleton(q);
        Iterator chrIter = sr.iterator();
        while (chrIter.hasNext()) {
            Chromosome chr = (Chromosome) chrIter.next();
            returnMap.put(chr.getId(), chr);
        }
        return returnMap;
    }


    /**
     * Make a Location on a destClass (eg. Chromosome) for each moveClass (eg. Contig).
     * The moveClass object must be located on a sourceClass object (eg. Supercontig) and the
     * sourceClass object must be located on the destClass.
     * Example: Create Chromosome Locations for Contig objects that are currently Located
     * on Supercontigs.
     * @param sourceClass the moveClass objects are originally located on sourceClass objects
     * @param destClass after createTransformedLocations(), extra Locations will exist between
     * moveClass objects and destClass
     * @param moveClass the class of objects to create new Locations for
     * @throws ObjectStoreException if anything goes wrong
     */
    protected void createTransformedLocations(Class sourceClass, Class destClass, Class moveClass)
         throws ObjectStoreException {

        Results results =
            PostProcessUtil.findLocationsToTransform(os, moveClass, sourceClass, destClass, 500);

        Iterator resIter = results.iterator();

        int i = 0;
        long start = System.currentTimeMillis();
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            BioEntity moveObject = (BioEntity) rr.get(0);
            Location locMoveObjectOnSource = (Location) rr.get(1);
            BioEntity sourceObject = (BioEntity) rr.get(2);
            Location locSourceOnDest = (Location) rr.get(3);
            BioEntity destObject = (BioEntity) rr.get(4);

            SimpleLoc moveObjectOnSource = new SimpleLoc(sourceObject.getId().intValue(),
                                                         moveObject.getId().intValue(),
                                                         locMoveObjectOnSource);

            SimpleLoc sourceOnDest = new SimpleLoc(destObject.getId().intValue(),
                                                   sourceObject.getId().intValue(),
                                                   locSourceOnDest);

            // create location of moveClass on destClass (eg. Contig on Supercontig)
            Location contigOnDestLoc =
                createTransformedLocation(sourceOnDest, moveObjectOnSource, destObject, moveObject);

            osw.store(contigOnDestLoc);
            i++;

            if (i % 100 == 0) {
                long now = System.currentTimeMillis();
                LOG.info("Created " + i + " BioEntity->BioEntity locations (avg = "
                         + ((60000L * i) / (now - start)) + " per minute)");
            }
        }
        LOG.info("Stored " + i + " Locations between Contig and Chromosome.");
    }


    /**
     * Given the location of a child BioEntity on a parent and the location of
     * the parent on a destination BioEntity (eg. Chromosome), create a Location for the child on
     * the destination.
     * @param parentOnDest location of parent (eg. Supercontig) object on dest (eg. Chromosome)
     * @param childOnParent location of child (eg. Contig) on parent (eg. Supercontig)
     * @param dest the object that will be the object of the new Location (eg. Chromosome)
     * @param child the child BioEntity - ie. the subject of the new Location
     * @return the new Location
     */
    protected Location createTransformedLocation(SimpleLoc parentOnDest, SimpleLoc childOnParent,
                                                 BioEntity dest, BioEntity child) {
        Location childOnDest;
        if (childOnParent.startIsPartial() || childOnParent.endIsPartial()) {
            childOnDest = (PartialLocation)
                DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        } else {
            childOnDest = (Location)
                DynamicUtil.createObject(Collections.singleton(Location.class));
        }
        if (parentOnDest.getStrand().equals("-1")) {
            childOnDest.setStart(new Integer((parentOnDest.getEnd() - childOnParent.getEnd()) + 1));
            childOnDest.setEnd(new Integer((parentOnDest.getEnd() - childOnParent.getStart()) + 1));
        } else {
            childOnDest.setStart(new Integer((parentOnDest.getStart()
                                             + childOnParent.getStart()) - 1));
            childOnDest.setEnd(new Integer((parentOnDest.getStart() + childOnParent.getEnd()) - 1));
        }
        if (childOnParent.getStrand().equals("-1")) {
            if (parentOnDest.getStrand().equals("-1")) {
                childOnDest.setStrand("1");
            } else {
                childOnDest.setStrand("-1");
            }
        } else {
            if (parentOnDest.getStrand().equals("-1")) {
                childOnDest.setStrand("-1");
            } else {
                childOnDest.setStrand("1");
            }
        }

        childOnDest.setStartIsPartial(Boolean.FALSE);
        childOnDest.setEndIsPartial(Boolean.FALSE);

        if (childOnParent.startIsPartial()) {
            childOnDest.setStartIsPartial(Boolean.TRUE);
        }
        if (childOnParent.endIsPartial()) {
            childOnDest.setEndIsPartial(Boolean.TRUE);
        }
        childOnDest.setObject(dest);
        childOnDest.setSubject(child);
        return childOnDest;
    }

    /**
     * For each LocatedSequenceFeature, if it has a Location on a Chromosome, set the
     * LocatedSequenceFeature.chromosomeLocation reference to be that Location and set the length
     * field of the LocatedSequenceFeature to chromosomeLocation.end - chromosomeLocation.start + 1
     * @throws Exception if anything goes wrong
     */
    public void setChromosomeLocationsAndLengths() throws Exception {
        Results results = BioQueries.findLocationAndObjects(os, Chromosome.class,
                LocatedSequenceFeature.class, true, false, 10000);
        Iterator resIter = results.iterator();

        osw.beginTransaction();

        // we need to check that there is only one location before setting chromosome[Location]
        // references.  If there are duplicates do nothing - this has happened for some affy
        // probes in FlyMine.
        Integer lastChrId = null;
        LocatedSequenceFeature lastFeature = null;
        boolean storeLastFeature = true;  // will get set to false if duplicate locations seen
        Location lastLoc = null;
        
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();

            Integer chrId = (Integer) rr.get(0);
            LocatedSequenceFeature lsf = (LocatedSequenceFeature) rr.get(1);
            Location locOnChr = (Location) rr.get(2);

            if (lastFeature != null && !lsf.getId().equals(lastFeature.getId())) {
                // not a duplicated so we can set references for last feature
                if (storeLastFeature) {
                    setChromosomeReferencesAndStore(lastFeature, lastLoc, lastChrId);
                }
                storeLastFeature = true;
            } else if (lastFeature != null) {
                storeLastFeature = false;
            }

            lastFeature = lsf;
            lastChrId = chrId;
            lastLoc = locOnChr;
        }
        
        // make sure final feature gets stored
        if (storeLastFeature) {
            setChromosomeReferencesAndStore(lastFeature, lastLoc, lastChrId);
        }
        
        osw.commitTransaction();
    }

    private void setChromosomeReferencesAndStore(LocatedSequenceFeature lsf, Location loc, 
                                                 Integer chrId) throws Exception {
        LocatedSequenceFeature lsfClone =
            (LocatedSequenceFeature) PostProcessUtil.cloneInterMineObject(lsf);

        lsfClone.setChromosomeLocation(loc);
        if (loc.getStart() != null && loc.getEnd() != null) {
            int end = loc.getEnd().intValue();
            int start = loc.getStart().intValue();
            // only set length if it isn't already set to stop eg. mRNA lengths getting broken.
            // an alternative is to set according to type of feature.
            if (lsfClone.getLength() == null) {
                int length = Math.abs(end - start) + 1;
                lsfClone.setLength(new Integer(length));
            }
        }
        lsfClone.proxyChromosome(new ProxyReference(os, chrId, Chromosome.class));

        osw.store(lsfClone);
    }
    
    
    /**
     * Return true if locations of two objects on some parent object
     * have any overlap.
     * @param sl1 first location
     * @param sl2 second location
     * @return true if the two locations have any overlap
     */
    protected static boolean overlap(SimpleLoc sl1, SimpleLoc sl2) {
        if ((sl1.getStart() >= sl2.getStart() && sl1.getStart() <= sl2.getEnd())
            || (sl1.getEnd() >= sl2.getStart() && sl1.getEnd() <= sl2.getEnd())
            || (sl1.getStart() >= sl2.getStart() && sl1.getEnd() <= sl2.getEnd())
            || (sl2.getStart() >= sl1.getStart() && sl2.getEnd() <= sl1.getEnd())) {
            return true;
        }
        return false;
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
        private boolean startIsPartial;
        private boolean endIsPartial;

        /**
         * Construct with integer values
         * @param parentId id of object
         * @param childId id of subject
         * @param start start value
         * @param end end value
         * @param strand strand value
         */
        public SimpleLoc(int parentId, int childId, int start, int end, String strand) {
            this(parentId, childId, start, end, strand, false, false);
        }

        /**
         * Construct with integer values
         * @param parentId id of object
         * @param childId id of subject
         * @param start start value
         * @param end end value
         * @param strand strand value
         * @param startIsPartial start is partial flag
         * @param endIsPartial end is partial flag
         */
        public SimpleLoc(int parentId, int childId, int start, int end, String strand,
                         boolean startIsPartial, boolean endIsPartial) {
            this.parentId = parentId;
            this.childId = childId;
            this.start = start;
            this.end = end;
            this.strand = strand;
            this.startIsPartial = startIsPartial;
            this.endIsPartial = endIsPartial;
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
            if (loc.getStartIsPartial() == null) {
                this.startIsPartial = false;
            } else {
                this.startIsPartial = loc.getStartIsPartial().booleanValue();
            }
            if (loc.getEndIsPartial() == null) {
                this.endIsPartial = false;
            } else {
                this.endIsPartial = loc.getEndIsPartial().booleanValue();
            }
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
         * Return true if and only if the start is partial.
         * @return true if and only if the start is partial.
         */
        public boolean startIsPartial() {
            return startIsPartial;
        }

        /**
         * Set the start-is-partial flag
         * @param startIsPartial new start-is-partial flag
         */
        public void setStartIsPartial(boolean startIsPartial) {
            this.startIsPartial = startIsPartial;
        }

        /**
         * Return true if and only if the end is partial.
         * @return true if and only if the end is partial.
         */
        public boolean endIsPartial() {
            return endIsPartial;
        }

        /**
         * Set the end-is-partial flag
         * @param endIsPartial the new end-is-partial
         */
        public void setEndIsPartial(boolean endIsPartial) {
            this.endIsPartial = endIsPartial;
        }

        /**
         * Return true if the start or end of this SimpleLoc are partial.
         * @return true if the start or end of this SimpleLoc are partial.
         */
        public boolean isPartial() {
            return (startIsPartial() || endIsPartial());
        }

        /**
         * @see Object#toString()
         * {@inheritDoc}
         */
        public String toString() {
            return "parent " + parentId + " child " + childId + " start " + start
                + " end " + end + " strand " + strand + " startIsPartial: " + startIsPartial
                + " endIsPartial: " + endIsPartial;
        }
    }
}

