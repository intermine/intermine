package org.flymine.postprocess;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;

import org.intermine.objectstore.query.*;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreException;
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
 */
public class CalculateLocations
{
    private static final Logger LOG = Logger.getLogger(CalculateLocations.class);

    protected ObjectStoreWriter osw;
    protected Map chrById = new HashMap();
    protected Map bandToChr = new HashMap();
    protected Map chrToBand = new HashMap();
    protected Map chrToSc = new HashMap();
    protected Map scToChr = new HashMap();
    protected Map contigToSc = new HashMap();
    protected Map contigToChr = new HashMap();
    private int i, j, k;

    /**
     * Consctruct with an ObjectStoreWriter
     * @param osw writer on genomic ObjectStore
     */
    public CalculateLocations(ObjectStoreWriter osw) {
        this.osw = osw;
    }


    // TODO 1) support pos/neg strand
    //      2) store chromosomes in id map -> avaoid getObjectById
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
    public void createLocations() throws Exception  {
        ObjectStore os = osw.getObjectStore();

        // 0. Hold Chromosomes in map by id
        Query q = new Query();
        QueryClass qc = new QueryClass(Chromosome.class);
        q.addToSelect(qc);
        q.addFrom(qc);
        SingletonResults sr = new SingletonResults(q, os, os.getSequence());
        Iterator chrIter = sr.iterator();
        while (chrIter.hasNext()) {
            Chromosome chr = (Chromosome) chrIter.next();
            chrById.put(chr.getId(), chr);
        }

        // 1. Find and hold locations of ChromosomeBands on Chromsomes
        Iterator resIter = findLocations(Chromosome.class, ChromosomeBand.class);
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Chromosome chr = (Chromosome) rr.get(0);
            ChromosomeBand band = (ChromosomeBand) rr.get(1);
            Location loc = (Location) rr.get(2);
            SimpleLoc sl = new SimpleLoc(chr.getId().intValue(),
                                         band.getId().intValue(),
                                         loc);
            addToMap(chrToBand, chr.getId(), sl);
            bandToChr.put(band.getId(), sl);
        }
        LOG.info("Found " + bandToChr.size() + " ChromosomeBands located on Chromosomes");
        LOG.info("chrToBand keys " + chrToBand.keySet());

        // 2. Find and hold locations of Supercontigs on Chromosomes
        //    Create locations of Supercontigs on ChromosomeBands
        resIter = findLocations(Chromosome.class, Supercontig.class);

        // create map ChromsomeBands to avoid calling getObjectById
        // need to keep running query after each commit transaction
        Map idBands = new HashMap();
        Iterator bandIter = PostProcessUtil.selectObjectsOfClass(os, ChromosomeBand.class);
        while (bandIter.hasNext()) {
            ChromosomeBand band = (ChromosomeBand) bandIter.next();
            idBands.put(band.getId(), band);
        }
        LOG.info("built ChromosomeBand id map, size = " + idBands.keySet().size());

        osw.beginTransaction();
        i = 0;
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Location scOnChrLoc = (Location) rr.get(2);
            Chromosome chr = (Chromosome) rr.get(0);
            Supercontig sc = (Supercontig) rr.get(1);
            SimpleLoc scOnChr = new SimpleLoc(chr.getId().intValue(),
                                              sc.getId().intValue(),
                                              scOnChrLoc);
            scToChr.put(sc.getId(), scOnChr);
            addToMap(chrToSc, chr.getId(), scOnChr);

            // find get ChromosomeBands that cover location on Chromosome
            Set bands = (Set) chrToBand.get(chr.getId());
            if (bands != null) {
                Iterator iter = bands.iterator();
                while (iter.hasNext()) {
                    SimpleLoc bandOnChr = (SimpleLoc) iter.next();
                    if (overlap(scOnChr, bandOnChr)) {
                        ChromosomeBand band = (ChromosomeBand)
                            idBands.get(new Integer(bandOnChr.getChildId()));
                        Location scOnBandLoc = createLocation(band, bandOnChr, sc, scOnChr);

                        Iterator storeIter = updateCollections(band, sc, scOnBandLoc).iterator();
                        while (storeIter.hasNext()) {
                            osw.store((InterMineObject) storeIter.next());
                        }
                        i++;
                    }
                }
            }
        }
        osw.commitTransaction();
        LOG.info("Stored " + i + " Locations between Supercontig and ChromosomeBand.");

        // 3. hold offsets of Contigs on Supercontigs
        //    create locations Contig->ChromosomeBand, Contig->Chromosome
        resIter = findLocations(Supercontig.class, Contig.class);

        // create map ChromsomeBands to avoid calling getObjectById
        // need to keep running query after each commit transaction
        idBands = new HashMap();
        bandIter = PostProcessUtil.selectObjectsOfClass(os, ChromosomeBand.class);
        while (bandIter.hasNext()) {
            ChromosomeBand band = (ChromosomeBand) bandIter.next();
            idBands.put(band.getId(), band);
        }
        LOG.info("built ChromosomeBand id map, size = " + idBands.keySet().size());

        osw.beginTransaction();
        i = 0;
        j = 0;
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Location locContigOnSc = (Location) rr.get(2);
            Supercontig sc = (Supercontig) rr.get(0);
            Contig contig = (Contig) rr.get(1);
            SimpleLoc contigOnSc = new SimpleLoc(sc.getId().intValue(),
                                                 contig.getId().intValue(),
                                                 locContigOnSc);

            // create location of contig on chromosome, don't expect partial locations
            SimpleLoc scOnChr = (SimpleLoc) scToChr.get(sc.getId());
            Chromosome chr = (Chromosome) chrById.get(new Integer(scOnChr.getParentId()));
            Location contigOnChrLoc = createChromosomeLocation(scOnChr, contigOnSc, chr, contig);

            SimpleLoc contigOnChr = new SimpleLoc(chr.getId().intValue(),
                                                  contig.getId().intValue(),
                                                  contigOnChrLoc);

            contigToChr.put(contig.getId(), contigOnChr);
            contigToSc.put(contig.getId(), contigOnSc);

            Iterator storeIter = updateCollections(chr, contig, contigOnChrLoc).iterator();
            while (storeIter.hasNext()) {
                osw.store((InterMineObject) storeIter.next());
            }
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

                        storeIter = updateCollections(band, contig, contigOnBandLoc).iterator();
                        while (storeIter.hasNext()) {
                            osw.store((InterMineObject) storeIter.next());
                        }
                        j++;
                    }
                }
            }
        }
        osw.commitTransaction();
        LOG.info("Stored " + i + " Locations between Contig and Chromosome.");
        LOG.info("Stored " + j + " Locations between Contig and ChromosomeBand.");

        // 4. For all BioEntities located on Contigs compute other offsets on all parents
        resIter = findLocations(Contig.class, BioEntity.class);

        // create map ChromsomeBands to avoid calling getObjectById
        // need to keep running query after each commit transaction
        idBands = new HashMap();
        bandIter = PostProcessUtil.selectObjectsOfClass(os, ChromosomeBand.class);
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

        i = 0;
        j = 0;
        k = 0;
        osw.beginTransaction();
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Location locBioOnContig = (Location) rr.get(2);
            Contig contig = (Contig) rr.get(0);
            BioEntity bio = (BioEntity) rr.get(1);
            SimpleLoc bioOnContig = new SimpleLoc(contig.getId().intValue(),
                                                  bio.getId().intValue(),
                                                  locBioOnContig);

            // first create location of feature on Chromosome
            SimpleLoc contigOnChr = (SimpleLoc) contigToChr.get(contig.getId());
            Chromosome chr = (Chromosome)
                chrById.get(new Integer(contigOnChr.getParentId()));
            Location bioOnChrLoc =
                createChromosomeLocation(contigOnChr, bioOnContig, chr, bio);

            Iterator storeIter = updateCollections(chr, bio, bioOnChrLoc).iterator();
            while (storeIter.hasNext()) {
                osw.store((InterMineObject) storeIter.next());
            }
            i++;

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
                        LOG.error("scOnChr (" + sc.getId() + ", " + sc.getName() + "): "
                                  + scOnChr.toString());
                        LOG.error("bioOnChr (" + bio.getId() + ", " + bio.getName() + ", "
                                  + DynamicUtil.decomposeClass(bio.getClass())
                                  + ") : " + bioOnChr.toString());
                        storeIter = updateCollections(sc, bio, bioOnScLoc).iterator();
                        while (storeIter.hasNext()) {
                            osw.store((InterMineObject) storeIter.next());
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
                        LOG.error("bandOnChr (" + band.getId() + ", " + band.getName() + "): "
                                  + bandOnChr.toString());
                        LOG.error("bioOnChr (" + bio.getId() + ", " + bio.getName() + ", "
                                  + DynamicUtil.decomposeClass(bio.getClass())
                                  + ") : " + bioOnChr.toString());

                        storeIter = updateCollections(band, bio, bioOnBandLoc).iterator();
                        while (storeIter.hasNext()) {
                            osw.store((InterMineObject) storeIter.next());
                        }
                        k++;
                    }
                }
            }
            LOG.info("feature: to Chromosome " + i + " to Supercontig " + j
                      + " ChromsomeBand " + k);
        }
        osw.commitTransaction();
        LOG.info("Stored " + i + " Locations between features and Chromosome.");
        LOG.info("Stored " + j + " Locations between features and Supercontig.");
        LOG.info("Stored " + k + " Locations between features and ChromosomeBand.");
    }


    /**
     * Given overlapping locations of parent and child BioEntities on a Chromosme create a
     * location between the parent and child.  This may be a PartialLocation if only overlap
     * is not total.  The strand value for the child on the Chromosome is propogated.  All
     * co-ordinates are on fwd strand regardless of actual strand value.
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
                    //  <--------------      parent
                    //           |
                    //           ----------  child
                    if (childOnChr.getStrand () == -1) {
                        pl.setSubjectStart(new Integer(childOnChr.getEnd()
                                                       - parentOnChr.getEnd() + 1));
                    } else {
                        pl.setSubjectEnd(new Integer(parentOnChr.getEnd()
                                                     - childOnChr.getStart() + 1));
                    }
                } else {
                    //      -------------->  parent
                    //            |
                    //   ----------          child
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
                    //       <------------  parent
                    //            |
                    //  -----------         child
                    if (childOnChr.getStrand() == -1) {
                        pl.setSubjectEnd(new Integer(childOnChr.getEnd()
                                                     - parentOnChr.getStart() + 1));
                    } else {
                        pl.setSubjectStart(new Integer(parentOnChr.getStart()
                                                       - childOnChr.getStart() + 1));
                    }
                } else {
                    //   ------------>        parent
                    //          |
                    //          -----------   child
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
            //  ------------------  parent
            //      |        |
            //      ----------      child
            childOnParent = (Location)
                DynamicUtil.createObject(Collections.singleton(Location.class));
        }
        childOnParent.setObject(parent);
        childOnParent.setSubject(child);
        childOnParent.setStartIsPartial(startIsPartial ? Boolean.TRUE : Boolean.FALSE);
        childOnParent.setEndIsPartial(endIsPartial ? Boolean.TRUE : Boolean.FALSE);

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

        if (childOnChr.getStrand() == parentOnChr.getStrand()) {
            childOnParent.setStrand(new Integer(1));
        } else {
            childOnParent.setStrand(new Integer(-1));
        }
        // TODO evidence?

        LOG.info("Created Location " + childOnParent + " for parent: " + parent + " and child: "
                 + child);
        return childOnParent;
    }


    /**
     * Given the location of a child BioEntity on a parent and the location of
     * the parent on a Chromsome, create a Location for the child on the Chromosome.
     * @param parentOnChr location of parent object on Chromosome
     * @param childOnParent location of child on parent
     * @param chr the Chromosome
     * @param child the child BioEntity
     * @return location of Chromosome
     */
    protected Location createChromosomeLocation(SimpleLoc parentOnChr, SimpleLoc childOnParent,
                                                Chromosome chr, BioEntity child) {
        Location childOnChr =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        if (parentOnChr.getStrand() == -1) {
            childOnChr.setStart(new Integer((parentOnChr.getEnd() - childOnParent.getEnd()) + 1));
            childOnChr.setEnd(new Integer((parentOnChr.getEnd() - childOnParent.getStart()) + 1));
        } else {
            childOnChr.setStart(new Integer((parentOnChr.getStart()
                                             + childOnParent.getStart()) - 1));
            childOnChr.setEnd(new Integer((parentOnChr.getStart() + childOnParent.getEnd()) - 1));
        }
        childOnChr.setStrand(new Integer(childOnParent.getStrand()));
        childOnChr.setStartIsPartial(Boolean.FALSE);
        childOnChr.setEndIsPartial(Boolean.FALSE);
        childOnChr.setObject(chr);
        childOnChr.setSubject(child);
        return childOnChr;
    }


    /**
     * Given a Location and its subject and object add the location to the objects
     * and subjects collections, put all three in a set and return.  Subject and
     * object are actually cloned and new objects returned.
     * @param object the object of the Location
     * @param subject the subject of the Location
     * @param loc the location
     * @return a set of InterMineObjects to store
     * @throws Exception if problem cloning InterMineObject
     */
    protected Set updateCollections(BioEntity object, BioEntity subject, Location loc)
        throws Exception {
        BioEntity newObj = (BioEntity) cloneInterMineObject(object);
        List subjects = new ArrayList(newObj.getSubjects());
        subjects.add(loc);
        newObj.setSubjects(subjects);
        BioEntity newSub = (BioEntity) cloneInterMineObject(subject);
        List objects = new ArrayList(newSub.getObjects());
        objects.add(loc);
        newSub.setObjects(objects);
        return new HashSet(Arrays.asList(new Object[] {newObj, newSub, loc}));
    }

    /**
     * Return true if locations of two objects on some parent object
     * have any overlap.
     * @param sl1 first location
     * @param sl2 second location
     * @return true if the two locations have any overlap
     */
    protected boolean overlap(SimpleLoc sl1, SimpleLoc sl2) {
        if ((sl1.getStart() >= sl2.getStart() && sl1.getStart() <= sl2.getEnd())
            || (sl1.getEnd() >= sl2.getStart() && sl1.getEnd() <= sl2.getEnd())
            || (sl1.getStart() >= sl2.getStart() && sl1.getEnd() <= sl2.getEnd())
            || (sl2.getStart() >= sl1.getStart() && sl2.getEnd() <= sl1.getEnd())) {
            return true;
        }
        return false;
    }


    /**
     * Query ObjectStore for all Location object between given object and
     * subject classes.  Return an iterator.
     * @param objectCls object type of the Location
     * @param subjectCls subject type of the Location
     * @return an iterator over the results
     * @throws ObjectStoreException if problem reading ObjectStore
     */
    protected Iterator findLocations(Class objectCls, Class subjectCls)
        throws ObjectStoreException {
        // TODO check objectCls and subjectCls assignable to BioEntity

        Query q = new Query();
        q.setDistinct(false);
        QueryClass qcObj = new QueryClass(objectCls);
        q.addFrom(qcObj);
        q.addToSelect(qcObj);
        QueryClass qcSub = new QueryClass(subjectCls);
        q.addFrom(qcSub);
        q.addToSelect(qcSub);
        QueryClass qcLoc = new QueryClass(Location.class);
        q.addFrom(qcLoc);
        q.addToSelect(qcLoc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryObjectReference ref1 = new QueryObjectReference(qcLoc, "object");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcObj);
        cs.addConstraint(cc1);
        QueryObjectReference ref2 = new QueryObjectReference(qcLoc, "subject");
        ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcSub);
        cs.addConstraint(cc2);
        q.setConstraint(cs);

        ObjectStore os = osw.getObjectStore();
        Results res = new Results(q, os, os.getSequence());
        return res.iterator();
    }


    /**
     * Create a clone of given InterMineObject including the id
     * @param obj object to clone
     * @return the cloned object
     * @throws Exception if problems with reflection
     */
    protected InterMineObject cloneInterMineObject(InterMineObject obj) throws Exception {
        InterMineObject newObj = (InterMineObject)
            DynamicUtil.createObject(DynamicUtil.decomposeClass(obj.getClass()));
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
        int start;
        int parentId;
        int childId;
        int strand;
        int end;

        /**
         * Construct with integer values
         * @param parentId id of object
         * @param childId id of subject
         * @param start start value
         * @param end end value
         * @param strand strand value
         */
        public SimpleLoc(int parentId, int childId, int start, int end, int strand) {
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
         * Get stand value
         * @return stand value
         */
        public int getStrand() {
            return strand;
        }

        /**
         * @see Object#toString()
         */
        public String toString() {
            return "parent " + parentId + " child " + childId + " start " + start + " end " + end;
        }
    }
}

