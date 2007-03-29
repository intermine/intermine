package org.intermine.bio.postprocess;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.*;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

import org.intermine.model.InterMineObject;
import org.flymine.model.genomic.*;
import org.apache.log4j.Logger;


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
    protected Map chrById = null;
    protected Map bandToChr = new HashMap();
    protected Map chrToBand = new HashMap();
    protected Map chrToSc = new HashMap();
    protected Map scToChr = new HashMap();
    protected Map contigToSc = new HashMap();
    protected Map contigToChr = new HashMap();

    /**
     * Create a new CalculateLocations object from an ObjectStoreWriter
     * @param osw writer on genomic ObjectStore
     */
    public CalculateLocations(ObjectStoreWriter osw) {
        this.osw = osw;
        this.os = osw.getObjectStore();
    }

    /**
     * Turn Location objects that should be partial into PartialLocations.  Locations are only
     * converted if they go to the start/end of the object BioEntity and there is at one other
     * location that refers to the same subject.
     * eg.
     *   object1  ---------++------------ object2
     *                |loc1||loc2|
     *       subject  +----------+
     * or
     *   object1  ---------++----++------------ object2
     *                |loc1||loc2||loc3|
     *       subject  +----------------+
     *
     * Locations are ignored that happen to go to the start or end but there is only one Location
     * that refers to the subject:
     *   object   ---------+
     *                |loc1|
     *   subject      +----+
     * @throws Exception if anything goes wrong
     */
    public void fixPartials() throws Exception {
        fixPartials(Contig.class, Exon.class);
    }

    /**
     * Fix the Locations that connect objectCls and subjectCls objects.
     */
    private void fixPartials(Class objectCls, Class subjectCls) throws Exception {
        Results results = PostProcessUtil.findLocationAndObjects(os, objectCls, subjectCls, true);
        results.setBatchSize(500);

        osw.beginTransaction();

        Iterator resIter = results.iterator();

        Set batch = new HashSet();

        int previousSubjectId = -1;

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            BioEntity subject = (BioEntity) rr.get(1);
            if (subject.getId().intValue() != previousSubjectId && batch.size() > 0) {
                fixPartialBatch(batch);
                batch = new HashSet();
            }

            batch.add(rr);
            previousSubjectId = subject.getId().intValue();
        }

        if (previousSubjectId != -1 && batch.size() > 0) {
            fixPartialBatch(batch);
        }

        osw.commitTransaction();
    }

    /**
     * The argument should be a Set of ResultsRow objects returned by findLocations().  All of the
     * Locations in the ResultsRow should have the same Subject.
     */
    private void fixPartialBatch(Set batch) throws Exception {
        if (batch.size() < 2) {
            // if the object doesn't have two location objects the locations can't be partial
            return;
        }

        LOG.info("processing partial batch size " + batch.size());

        Location startLocation = null;
        Location endLocation = null;
        // Locations that are partial at the start and the end
        Set veryPartialLocations = new HashSet();

        int subjectLengthSoFar = -1;

        Iterator batchIter = batch.iterator();
        while (batchIter.hasNext()) {
            ResultsRow rr = (ResultsRow) batchIter.next();
            Integer objectId = (Integer) rr.get(0);
            BioEntity object = (BioEntity) os.getObjectById(objectId);
            Location location = (Location) rr.get(2);

            try {
                Object objectLengthField = TypeUtil.getFieldValue(object, "length");
                int objectLength = -1;
                if (objectLengthField instanceof Integer) {
                    objectLength = ((Integer) objectLengthField).intValue();
               } else {
                    LOG.error("Object with ID: "
                              + object.getId() + " has no Integer length field");
                    continue;
                }

                if (location.getEnd().intValue() == objectLength
                    && location.getStart().intValue() == 1) {
                    // both ends are partial - fix last
                    veryPartialLocations.add(location);
                }

                if (location.getStart().intValue() == 1
                    && location.getEnd().intValue() != objectLength) {
                    endLocation = location;
                }

                if (location.getEnd().intValue() == objectLength
                    && location.getStart().intValue() != 1) {
                    PartialLocation pl =
                        (PartialLocation) cloneInterMineObject(location, PartialLocation.class);
                    pl.setStartIsPartial(Boolean.FALSE);
                    pl.setEndIsPartial(Boolean.TRUE);
                    pl.setSubjectStart(new Integer(1));
                    subjectLengthSoFar =
                        location.getEnd().intValue() - location.getStart().intValue() + 1;
                    pl.setSubjectEnd(new Integer(subjectLengthSoFar));
                    startLocation = pl;
                    osw.store(pl);
                }
            } catch (IllegalAccessException e) {
                LOG.error("Object with ID: " + object.getId()
                          + " has no Integer length field");
            }
        }

        Iterator veryPartialLocationsIterator = veryPartialLocations.iterator();

        while (veryPartialLocationsIterator.hasNext()) {
            Location location = (Location) veryPartialLocationsIterator.next();

            int thisObjectLength =
                location.getEnd().intValue() - location.getStart().intValue() + 1;

            PartialLocation pl =
                (PartialLocation) cloneInterMineObject(location, PartialLocation.class);
            pl.setStartIsPartial(Boolean.TRUE);
            pl.setEndIsPartial(Boolean.TRUE);
            pl.setSubjectStart(new Integer(subjectLengthSoFar + 1));
            pl.setSubjectEnd(new Integer(subjectLengthSoFar + thisObjectLength));
            osw.store(pl);
            subjectLengthSoFar += thisObjectLength;
        }

        if (endLocation == null) {
            LOG.error("endLocation is null - startLocation: " + startLocation
                      + "  veryPartialLocations: " + veryPartialLocations);
        } else {
            PartialLocation pl =
                (PartialLocation) cloneInterMineObject(endLocation, PartialLocation.class);

            pl.setStartIsPartial(Boolean.TRUE);
            pl.setEndIsPartial(Boolean.FALSE);
            pl.setSubjectStart(new Integer(subjectLengthSoFar + 1));
            int newEndPos = pl.getSubjectStart().intValue()
                + (pl.getEnd().intValue() - pl.getStart().intValue());
            pl.setSubjectEnd(new Integer(newEndPos));
            osw.store(pl);
        }

    }


    // TODO 1) support pos/neg strand
    //      2) store chromosomes in id map -> avoid getObjectById
    //      3) map from chromosome to children -> less overlap comparasons
    //      4) check if a Location already exists?
    //      5) evidence collection for locations

    /**
     * Create new Location objects where required:
     *
     *  | = Location that should exist   ( = Location to create
     *
     * Chromosome
     *   | | ( (
     *   ChromosomeBand
     *     ( ( (
     *     Supercontig
     *       | (
     *       Contig
     *         |
     *         *features*
     *
     * @throws Exception if anything goes wrong
     */
    public void createLocations() throws Exception {
        osw.beginTransaction();

        // 0. Hold Chromosomes in map by id
        chrById = makeChromosomeMap();

        // 1. Find and hold locations of ChromosomeBands on Chromsomes
        makeChromosomeBandLocations();

        // 2. Find and hold locations of Supercontigs on Chromosomes
        //    Create locations of Supercontigs on ChromosomeBands
        makeSupercontigLocations();

        // 3. hold offsets of Contigs on Supercontigs
        //    create locations Contig->ChromosomeBand, Contig->Chromosome
        makeContigLocations();

        // 4. For all BioEntities located on Contigs compute other offsets on all parents
        Results results =
            PostProcessUtil.findLocationAndObjects(os, Contig.class, BioEntity.class, false);
        results.setBatchSize(500);

        Iterator resIter = results.iterator();

        // create map ChromsomeBands to avoid calling getObjectById
        // need to keep running query after each commit transaction
        Map idBands = new HashMap();
        Iterator bandIter = PostProcessUtil.selectObjectsOfClass(os, ChromosomeBand.class);
        while (bandIter.hasNext()) {
            ChromosomeBand band = (ChromosomeBand) bandIter.next();
            idBands.put(band.getId(), band);
        }
        LOG.info("built ChromosomeBand id map, size = " + idBands.keySet().size());

        // create map of Supercontigs to avoid calling getObjectById
        // need to keep running query after each commit transaction
        Map idScs = new HashMap();
        Iterator scIter = PostProcessUtil.selectObjectsOfClass(os, Supercontig.class);
        while (scIter.hasNext()) {
            Supercontig sc = (Supercontig) scIter.next();
            idScs.put(sc.getId(), sc);
        }
        LOG.info("built Supercontig id map, size = " + idScs.keySet().size());

        // maps from BioEntity to Location
        Map partialsOnChromosomes = new HashMap();
        Map partialsOnSupercontigs = new HashMap();
        Map partialsOnChromosomeBands = new HashMap();

        int i = 0, j = 0, k = 0;
        long start = System.currentTimeMillis();
        while (resIter.hasNext()) {
            i++;
            ResultsRow rr = (ResultsRow) resIter.next();
            Location locBioOnContig = (Location) rr.get(2);
            Integer contigId = (Integer) rr.get(0);
            BioEntity bio = (BioEntity) rr.get(1);
            SimpleLoc bioOnContig = new SimpleLoc(contigId.intValue(),
                                                  bio.getId().intValue(),
                                                  locBioOnContig);

            // first create location of feature on Chromosome
            SimpleLoc contigOnChr = (SimpleLoc) contigToChr.get(contigId);

            Chromosome chr = (Chromosome)
                chrById.get(new Integer(contigOnChr.getParentId()));
            Location bioOnChrLoc =
                createTransformedLocation(contigOnChr, bioOnContig, chr, bio);

            if (locBioOnContig instanceof PartialLocation) {
                addToMapOfLists(partialsOnChromosomes, bio, bioOnChrLoc);
            } else {
                osw.store(bioOnChrLoc);
            }

            SimpleLoc bioOnChr = new SimpleLoc(chr.getId().intValue(),
                                               bio.getId().intValue(),
                                               bioOnChrLoc);

            // create location of feature on Supercontig
            Set scs = (Set) chrToSc.get(chr.getId());
            if (scs != null) {
                Iterator iter = scs.iterator();
                while (iter.hasNext()) {
                    SimpleLoc scOnChr = (SimpleLoc) iter.next();
                    if (overlap(scOnChr, bioOnChr)) {
                        Supercontig sc = (Supercontig) idScs.get(new Integer(scOnChr.getChildId()));
                        Location bioOnScLoc = createLocation(sc, scOnChr, bio, bioOnChr);

                        if (bioOnScLoc instanceof PartialLocation) {
                            addToMapOfLists(partialsOnSupercontigs, bio, bioOnScLoc);
                        } else {
                            osw.store(bioOnScLoc);
                        }
                        j++;
                    }
                }
            }

            // create location of feature on ChromosomeBand
            Set bands = (Set) chrToBand.get(chr.getId());
            if (bands != null) {
                Iterator iter = bands.iterator();
                while (iter.hasNext()) {
                    SimpleLoc bandOnChr = (SimpleLoc) iter.next();
                    if (overlap(bandOnChr, bioOnChr)) {
                        ChromosomeBand band = (ChromosomeBand)
                            idBands.get(new Integer(bandOnChr.getChildId()));
                        Location bioOnBandLoc = createLocation(band, bandOnChr, bio, bioOnChr);

                        if (bioOnBandLoc instanceof PartialLocation) {
                            addToMapOfLists(partialsOnChromosomeBands, bio, bioOnBandLoc);
                        } else {
                            osw.store(bioOnBandLoc);
                        }
                        k++;
                    }
                }
            }
            if (i % 100 == 0) {
                long now = System.currentTimeMillis();
                LOG.info("Created " + i + " Chromosome, "
                         + j + " SuperContig locations and "
                         + k + " ChromosomeBand locations (avg = "
                         + ((60000L * i) / (now - start)) + " per minute)");
            }
        }

        chrById = null;
        bandToChr = null;
        chrToBand = null;
        chrToSc = null;
        scToChr = null;
        contigToSc = null;
        contigToChr = null;

        // process partials Locations
        processPartials(partialsOnChromosomes);
        processPartials(partialsOnSupercontigs);
        processPartials(partialsOnChromosomeBands);

        osw.commitTransaction();

        LOG.info("Stored " + i + " Locations between features and Chromosome.");
        LOG.info("Stored " + j + " Locations between features and Supercontig.");
        LOG.info("Stored " + k + " Locations between features and ChromosomeBand.");
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
                 + subject.getIdentifier());

        OverlapUtil.createOverlaps(os, subject, classNamesToIgnore, ignoreSelfMatches, osw,
                summary);
    }

    /**
     * Put key and value in the given map.  The values of the map are List, to which the new value
     * is appended.  The Lists are created if missing.
     */
    private void addToMapOfLists(Map map, Object key, Object value) {
        if (map.get(key) != null) {
            ((List) map.get(key)).add(value);
        } else {
            List list = new ArrayList();
            list.add(value);
            map.put(key, list);
        }
    }

    /**
     * Process the Partial Locations in mapOfPartials and merge PartialLocations
     */
    private void processPartials(Map mapOfPartials) throws ObjectStoreException {
        Iterator mapOfPartialsIter = mapOfPartials.keySet().iterator();

        while (mapOfPartialsIter.hasNext()) {
            int minChrStart = Integer.MAX_VALUE;
            int maxChrEnd = -1;
            int newStrand = 0;
            Integer newStartPhase = null;
            Integer newEndPhase = null;
            BioEntity bioEntity = (BioEntity) mapOfPartialsIter.next();
            List partialLocList = (List) mapOfPartials.get(bioEntity);

            // check that all the PartialLocations have the same object BioEntity
            if (!checkForSameObject(partialLocList)) {
                // don't try to merge these PartialLocations
                storeAll(partialLocList);
                continue;
            }

            for (Iterator partialLocListIter = partialLocList.iterator();
                 partialLocListIter.hasNext(); ) {
                Object nextObject = partialLocListIter.next();
                PartialLocation pl = (PartialLocation) nextObject;

                // createChromosomeLocation() doesn't set subjectStart or subjectEnd yet
                //                 if (pl.getSubjectStart().intValue() < minBioStart) {
                //                     minBioStart = pl.getSubjectStart().intValue();
                //                 }

                //                 if (pl.getSubjectEnd().intValue() > maxBioEnd) {
                //                     maxBioEnd = pl.getSubjectEnd().intValue();
                //                 }

                if (pl.getStart().intValue() < minChrStart) {
                    minChrStart = pl.getStart().intValue();
                    // use the start phase of the first Location in the new Location
                    newStartPhase = pl.getPhase();
                }

                if (pl.getEnd().intValue() > maxChrEnd) {
                    maxChrEnd = pl.getEnd().intValue();
                    // use the end phase of the last Location in the new Location
                    newEndPhase = pl.getEndPhase();
                }

                if (newStrand == 0) {
                    newStrand = pl.getStrand().intValue();
                } else {
                    if (newStrand != pl.getStrand().intValue()) {
                        throw new RuntimeException("BioEntity (" + bioEntity + ") has two "
                                                   + "Locations "
                                                   + "with inconsistent strands: "
                                                   + newStrand + " != "
                                                   + pl.getStrand().intValue());
                    }
                }
            }

            BioEntity newLocationObject = ((Location) partialLocList.get(0)).getObject();

            // should check that maxChrEnd - minChrStart = maxBioEnd - minBioStart once
            // createChromosomeLocation() is fixed

            Location newLocation =
                (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
            newLocation.setStart(new Integer(minChrStart));
            newLocation.setEnd(new Integer(maxChrEnd));
            newLocation.setStartIsPartial(Boolean.FALSE);
            newLocation.setEndIsPartial(Boolean.FALSE);
            newLocation.setStrand(new Integer(newStrand));
            newLocation.setPhase(newStartPhase);
            newLocation.setEndPhase(newEndPhase);
            newLocation.setSubject(bioEntity);
            newLocation.setObject(newLocationObject);

            osw.store(newLocation);
        }
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
                         + "AS a2_, org.flymine.model.genomic.Chromosome as a3_"
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
            if (locatedOnObject instanceof Chromosome
                && locatedParents.contains(parentObject.getId())) {
                continue;
            }

            Map parentObjectMap = (Map) locatedOnObjectMap.get(locatedOnObject.getId());

            if (parentObjectMap == null) {
                parentObjectMap = new HashMap();
                locatedOnObjectMap.put(locatedOnObject.getId(), parentObjectMap);
            }

            SimpleLoc parentObjectSimpleLoc = (SimpleLoc) parentObjectMap.get(parentObject.getId());

            if (parentObjectSimpleLoc == null) {
                parentObjectSimpleLoc = new SimpleLoc(-1, -1, Integer.MAX_VALUE, -1, 0);
                parentObjectMap.put(parentObject.getId(), parentObjectSimpleLoc);
            }

            int currentParentStart = parentObjectSimpleLoc.getStart();
            int currentParentEnd = parentObjectSimpleLoc.getEnd();

            if (location.getStart().intValue() < currentParentStart) {
                parentObjectSimpleLoc.setStart(location.getStart().intValue());
            }

            if (location.getEnd().intValue() > currentParentEnd) {
                parentObjectSimpleLoc.setEnd(location.getEnd().intValue());
            }

            parentObjectSimpleLoc.setStrand(location.getStrand().intValue());

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
                newLocation.setStrand(new Integer(parentObjectSimpleLoc.getStrand()));
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
            PostProcessOperationsTask.PRECOMPUTE_CATEGORY);
        Results res = new Results(q, os, os.getSequence());

        res.setBatchSize(500);
        return res.iterator();
    }


    /**
     * Return true if and only if all of the Locations in the List have the same object reference.
     */
    private boolean checkForSameObject(List list) {
        BioEntity testObject = null;

        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
            PartialLocation pl = (PartialLocation) iter.next();

            if (testObject == null) {
                testObject = pl.getObject();
            } else {
                if (!testObject.equals(pl.getObject())) {
                    LOG.info("BioEntity (" + pl.getSubject() + ") is located "
                             + "on two different BioEntities " + pl.getObject().getId() + " and "
                             + testObject.getId());
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Store all the BioEntity objects from the List using osw.
     */
    private void storeAll(List list) throws ObjectStoreException {
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            osw.store((InterMineObject) iter.next());
        }
    }

    /**
     * Given overlapping locations of parent and child BioEntities on a Chromosme create a
     * location between the parent and child.  This may be a PartialLocation if only overlap
     * is not total.  The strand value for the child on the Chromosome is propagated.
     * @param parent BioEntity that will be object of new Location
     * @param parentOnChr location of parent on the Chromosome
     * @param child BioEntity that will be subject of new Location
     * @param childOnChr location of child on the Chromosome
     * @return the new Location
     * @throws IllegalArgumentException if the parent and child do not overlap
     */
    protected Location createLocation(BioEntity parent, SimpleLoc parentOnChr,
                                      BioEntity child, SimpleLoc childOnChr) {
        //BioEntity child, SimpleLoc childOnChr, boolean strandIsOrientation) {
        if (!overlap(childOnChr, parentOnChr)) {
            throw new IllegalArgumentException("parent (" + parentOnChr.getStart()
                                               + ", " + parentOnChr.getEnd()
                                               + ") and child (" + childOnChr.getStart()
                                               + ", " + childOnChr.getEnd()
                                               + ") do not overlap.");
        }

        boolean startIsPartial = false;
        boolean endIsPartial = false;
        // want inclusive co-ordinates
        int parentLength = (parentOnChr.getEnd() - parentOnChr.getStart()) + 1;
        int childLength = (childOnChr.getEnd() - childOnChr.getStart()) + 1;

        if (parentOnChr.getStrand() == -1) {
            if (parentOnChr.getEnd() < childOnChr.getEnd()) {
                startIsPartial = true;
            }
            if (parentOnChr.getStart() > childOnChr.getStart()) {
                endIsPartial = true;
           }
        } else {
            if (childOnChr.getStart() < parentOnChr.getStart()) {
                startIsPartial = true;
            }
            if (childOnChr.getEnd() > parentOnChr.getEnd()) {
                endIsPartial = true;
            }
        }

        Location childOnParent = null;
        if (startIsPartial || endIsPartial) {
            PartialLocation pl = (PartialLocation)
                DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
            pl.setSubjectStart(new Integer(1));
            pl.setSubjectEnd(new Integer(childLength));

            if (startIsPartial) {
                if (parentOnChr.getStrand() == -1) {
                    //  <--------+-----      parent
                    //           |
                    //           +---------  child
                    if (childOnChr.getStrand () == -1) {
                        pl.setSubjectStart(new Integer(childOnChr.getEnd()
                                                       - parentOnChr.getEnd() + 1));
                    } else {
                        pl.setSubjectEnd(new Integer(parentOnChr.getEnd()
                                                     - childOnChr.getStart() + 1));
                    }
                } else {
                    //      ------+------->  parent
                    //            |
                    //   ---------+          child
                    if (childOnChr.getStrand () == -1) {
                        pl.setSubjectEnd(new Integer(childOnChr.getEnd()
                                                     - parentOnChr.getStart() + 1));
                    } else {
                        pl.setSubjectStart(new Integer(parentOnChr.getStart()
                                                       - childOnChr.getStart() + 1));
                    }
                }
                childOnParent = pl;
            }

            if (endIsPartial) {
                if (parentOnChr.getStrand() == -1) {
                    //       <----+-------  parent
                    //            |
                    //  ----------+         child
                    if (childOnChr.getStrand() == -1) {
                        pl.setSubjectEnd(new Integer(childOnChr.getEnd()
                                                     - parentOnChr.getStart() + 1));
                    } else {
                        pl.setSubjectStart(new Integer(parentOnChr.getStart()
                                                       - childOnChr.getStart() + 1));
                    }
                } else {
                    //   -------+---->        parent
                    //          |
                    //          +----------   child
                    if (childOnChr.getStrand() == -1) {
                        pl.setSubjectStart(new Integer(childOnChr.getEnd()
                                                       - parentOnChr.getEnd() + 1));
                    } else {
                        pl.setSubjectEnd(new Integer(parentOnChr.getEnd()
                                                     - childOnChr.getStart() + 1));
                    }
                }
                childOnParent = pl;
            }
        } else {
            if (childOnChr.isPartial()) {
                // start or end of childOnParent is partial so the new location must be partial too
                childOnParent = (PartialLocation)
                    DynamicUtil.createObject(Collections.singleton(PartialLocation.class));

            } else {
                //  ----+--------+----  parent
                //      |        |
                //      +--------+      child
                childOnParent = (Location)
                    DynamicUtil.createObject(Collections.singleton(Location.class));
            }
        }

        childOnParent.setObject(parent);
        childOnParent.setSubject(child);

        int newChildOnParentStart;
        int newChildOnParentEnd;

        if (startIsPartial) {
            newChildOnParentStart = 1;
        } else {
            if (parentOnChr.getStrand() == -1) {
                newChildOnParentStart = parentOnChr.getEnd() - childOnChr.getEnd() + 1;
            } else {
                newChildOnParentStart = childOnChr.getStart() - parentOnChr.getStart() + 1;
            }
        }

        if (endIsPartial) {
            newChildOnParentEnd = parentLength;
        } else {
            if (parentOnChr.getStrand() == -1) {
                newChildOnParentEnd = parentOnChr.getEnd() - childOnChr.getStart() + 1;
            } else {
                newChildOnParentEnd = childOnChr.getEnd() - parentOnChr.getStart() + 1;
            }
        }

        childOnParent.setStart(new Integer(newChildOnParentStart));
        childOnParent.setEnd(new Integer(newChildOnParentEnd));

        // we don't just check for (childOnChr.getStrand() == parentOnChr.getStrand()) because we
        // treat strand of 0 as equal to strand 1
        if (childOnChr.getStrand() == -1 && parentOnChr.getStrand() == -1) {
            childOnParent.setStrand(new Integer(1));
        } else {
            if (childOnChr.getStrand() == -1 || parentOnChr.getStrand() == -1) {
                childOnParent.setStrand(new Integer(-1));
            } else {
                childOnParent.setStrand(new Integer(1));
            }
        }

        if (parentOnChr.getStrand() == -1) {
            if (childOnChr.endIsPartial()) {
                startIsPartial = true;
            }
            if (childOnChr.startIsPartial()) {
                endIsPartial = true;
           }
        } else {
            if (childOnChr.startIsPartial()) {
                startIsPartial = true;
            }
            if (childOnChr.endIsPartial()) {
                endIsPartial = true;
            }
        }

        childOnParent.setStartIsPartial(startIsPartial ? Boolean.TRUE : Boolean.FALSE);
        childOnParent.setEndIsPartial(endIsPartial ? Boolean.TRUE : Boolean.FALSE);

        // TODO evidence?

        //LOG.info("Created Location " + childOnParent + " for parent: " + parent + " and child: "
        //         + child);
        return childOnParent;
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

        SingletonResults sr = new SingletonResults(q, os, os.getSequence());
        Iterator chrIter = sr.iterator();
        while (chrIter.hasNext()) {
            Chromosome chr = (Chromosome) chrIter.next();
            returnMap.put(chr.getId(), chr);
        }
        return returnMap;
    }


    /**
     * Find and hold locations of ChromosomeBands on Chromsomes
     */
    private void makeChromosomeBandLocations() throws Exception {
        Results results =
            PostProcessUtil.findLocationAndObjects(os, Chromosome.class, ChromosomeBand.class,
                                                   true);
        results.setBatchSize(500);

        Iterator resIter = results.iterator();

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Integer chrId = (Integer) rr.get(0);
            ChromosomeBand band = (ChromosomeBand) rr.get(1);
            Location loc = (Location) rr.get(2);
            SimpleLoc sl = new SimpleLoc(chrId.intValue(),
                                         band.getId().intValue(),
                                         loc);
            addToMap(chrToBand, chrId, sl);
            bandToChr.put(band.getId(), sl);
        }
        LOG.info("Found " + bandToChr.size() + " ChromosomeBands located on Chromosomes");
        LOG.info("chrToBand keys " + chrToBand.keySet());
    }

    /**
     * Find and hold locations of Supercontigs on Chromosomes
     * Create locations of Supercontigs on ChromosomeBands
     */
    private void makeSupercontigLocations() throws Exception {
        Results results =
            PostProcessUtil.findLocationAndObjects(os, Chromosome.class, Supercontig.class, true);
        results.setBatchSize(500);

        Iterator resIter = results.iterator();

        // create map ChromsomeBands to avoid calling getObjectById
        // need to keep running query after each commit transaction
        Map idBands = new HashMap();
        Iterator bandIter = PostProcessUtil.selectObjectsOfClass(os, ChromosomeBand.class);
        while (bandIter.hasNext()) {
            ChromosomeBand band = (ChromosomeBand) bandIter.next();
            idBands.put(band.getId(), band);
        }
        LOG.info("built ChromosomeBand id map, size = " + idBands.keySet().size());

        int i = 0;
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Location scOnChrLoc = (Location) rr.get(2);
            Integer chrId = (Integer) rr.get(0);
            Supercontig sc = (Supercontig) rr.get(1);
            SimpleLoc scOnChr = new SimpleLoc(chrId.intValue(),
                                              sc.getId().intValue(),
                                              scOnChrLoc);
            scToChr.put(sc.getId(), scOnChr);
            addToMap(chrToSc, chrId, scOnChr);

            // find get ChromosomeBands that cover location on Chromosome
            Set bands = (Set) chrToBand.get(chrId);
            if (bands != null) {
                Iterator iter = bands.iterator();
                while (iter.hasNext()) {
                    SimpleLoc bandOnChr = (SimpleLoc) iter.next();
                    if (overlap(scOnChr, bandOnChr)) {
                        ChromosomeBand band = (ChromosomeBand)
                            idBands.get(new Integer(bandOnChr.getChildId()));
                        Location scOnBandLoc = createLocation(band, bandOnChr, sc, scOnChr);

                        osw.store(scOnBandLoc);
                        i++;
                    }
                }
            }
        }
        LOG.info("Stored " + i + " Locations between Supercontig and ChromosomeBand.");
    }

    /**
     * hold offsets of Contigs on Supercontigs
     * create locations Contig->ChromosomeBand, Contig->Chromosome
     */
    private void makeContigLocations() throws Exception {
        Results results =
            PostProcessUtil.findLocationAndObjects(os, Supercontig.class, Contig.class, true);
        results.setBatchSize(500);

        Iterator resIter = results.iterator();

        // create map ChromsomeBands to avoid calling getObjectById
        // need to keep running query after each commit transaction
        Map idBands = new HashMap();
        Iterator bandIter = PostProcessUtil.selectObjectsOfClass(os, ChromosomeBand.class);
        while (bandIter.hasNext()) {
            ChromosomeBand band = (ChromosomeBand) bandIter.next();
            idBands.put(band.getId(), band);
        }
        LOG.info("built ChromosomeBand id map, size = " + idBands.keySet().size());

        int i = 0;
        int j = 0;
        long start = System.currentTimeMillis();
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Location locContigOnSc = (Location) rr.get(2);
            Integer scId = (Integer) rr.get(0);
            Contig contig = (Contig) rr.get(1);
            SimpleLoc contigOnSc = new SimpleLoc(scId.intValue(),
                                                 contig.getId().intValue(),
                                                 locContigOnSc);

            // create location of contig on chromosome, don't expect partial locations
            SimpleLoc scOnChr = (SimpleLoc) scToChr.get(scId);
            Chromosome chr = (Chromosome) chrById.get(new Integer(scOnChr.getParentId()));
            Location contigOnChrLoc = createTransformedLocation(scOnChr, contigOnSc, chr, contig);

            SimpleLoc contigOnChr = new SimpleLoc(chr.getId().intValue(),
                                                  contig.getId().intValue(),
                                                  contigOnChrLoc);

            contigToChr.put(contig.getId(), contigOnChr);
            contigToSc.put(contig.getId(), contigOnSc);

            osw.store(contigOnChrLoc);
            i++;

            // create location of contig on ChromosomeBand
            // get ChromosomeBands that cover location on Chromosome
            Set bands = (Set) chrToBand.get(chr.getId());
            if (bands != null) {
                Iterator iter = bands.iterator();
                while (iter.hasNext()) {
                    SimpleLoc bandOnChr = (SimpleLoc) iter.next();
                    if (overlap(contigOnChr, bandOnChr)) {

                        ChromosomeBand band = (ChromosomeBand)
                            idBands.get(new Integer(bandOnChr.getChildId()));
                        Location contigOnBandLoc = createLocation(band, bandOnChr, contig,
                                                                  contigOnChr);

                        osw.store(contigOnBandLoc);
                        j++;
                    }
                }
            }
            if (i % 100 == 0) {
                long now = System.currentTimeMillis();
                LOG.info("Created " + i + " Contig/Chromosome and " + j
                         + " Contig/ChromosomeBand locations (avg = "
                         + ((60000L * i) / (now - start)) + " per minute)");
            }
        }
        LOG.info("Stored " + i + " Locations between Contig and Chromosome.");
        LOG.info("Stored " + j + " Locations between Contig and ChromosomeBand.");
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
            PostProcessUtil.findLocationsToTransform(os, moveClass, sourceClass, destClass);
        results.setBatchSize(500);

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
        if (parentOnDest.getStrand() == -1) {
            childOnDest.setStart(new Integer((parentOnDest.getEnd() - childOnParent.getEnd()) + 1));
            childOnDest.setEnd(new Integer((parentOnDest.getEnd() - childOnParent.getStart()) + 1));
        } else {
            childOnDest.setStart(new Integer((parentOnDest.getStart()
                                             + childOnParent.getStart()) - 1));
            childOnDest.setEnd(new Integer((parentOnDest.getStart() + childOnParent.getEnd()) - 1));
        }
        if (childOnParent.getStrand() == -1) {
            if (parentOnDest.getStrand() == -1) {
                childOnDest.setStrand(new Integer(1));
            } else {
                childOnDest.setStrand(new Integer(-1));
            }
        } else {
            if (parentOnDest.getStrand() == -1) {
                childOnDest.setStrand(new Integer(-1));
            } else {
                childOnDest.setStrand(new Integer(1));
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
        Results results = PostProcessUtil.findLocationAndObjects(os, Chromosome.class,
                                                        LocatedSequenceFeature.class, true);
        results.setBatchSize(2000);
        Iterator resIter = results.iterator();

        osw.beginTransaction();

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();

            LocatedSequenceFeature lsf = (LocatedSequenceFeature) rr.get(1);
            Location locOnChr = (Location) rr.get(2);

            LocatedSequenceFeature lsfClone =
                (LocatedSequenceFeature) cloneInterMineObject(lsf);

            lsfClone.setChromosomeLocation(locOnChr);
            if (locOnChr.getStart() != null && locOnChr.getEnd() != null) {
                int end = locOnChr.getEnd().intValue();
                int start = locOnChr.getStart().intValue();
                int length = Math.abs(end - start) + 1;
                lsfClone.setLength(new Integer(length));

            }

            osw.store(lsfClone);
        }

        osw.commitTransaction();
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
     * Create a clone of given InterMineObject including the id
     * @param obj object to clone
     * @return the cloned object
     * @throws Exception if problems with reflection
     */
    protected static InterMineObject cloneInterMineObject(InterMineObject obj) throws Exception {
        return cloneInterMineObject(obj, obj.getClass());
    }

    /**
     * Create a clone of given InterMineObject including the id
     * @param obj object to clone
     * @param newClass the class to create - must be the same class as obj or a sub-class
     * @return the cloned object
     * @throws Exception if problems with reflection
     */
    protected static InterMineObject cloneInterMineObject(InterMineObject obj,
                                                          Class newClass) throws Exception {
        InterMineObject newObj = (InterMineObject)
            DynamicUtil.createObject(DynamicUtil.decomposeClass(newClass));
        Map fieldInfos = new HashMap();
        Iterator clsIter = DynamicUtil.decomposeClass(obj.getClass()).iterator();
        while (clsIter.hasNext()) {
            fieldInfos.putAll(TypeUtil.getFieldInfos((Class) clsIter.next()));
        }

        Iterator fieldIter = fieldInfos.keySet().iterator();
        while (fieldIter.hasNext()) {
            String fieldName = (String) fieldIter.next();
            TypeUtil.setFieldValue(newObj, fieldName,
                                   TypeUtil.getFieldProxy(obj, fieldName));
        }
        return newObj;
    }

    private void addToMap(Map map, Integer key, SimpleLoc loc) {
        Set values = (Set) map.get(key);
        if (values == null) {
            values = new HashSet();
        }
        values.add(loc);
        map.put(key, values);
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
        private int strand;
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
        public SimpleLoc(int parentId, int childId, int start, int end, int strand) {
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
        public SimpleLoc(int parentId, int childId, int start, int end, int strand,
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
                this.strand = loc.getStrand().intValue();
            } else {
                this.strand = 0;
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
        public int getStrand() {
            return strand;
        }

        /**
         * Set strand value
         * @param strand value
         */
        public void setStrand(int strand) {
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
         */
        public String toString() {
            return "parent " + parentId + " child " + childId + " start " + start
                + " end " + end + " strand " + strand + " startIsPartial: " + startIsPartial
                + " endIsPartial: " + endIsPartial;
        }
    }
}

