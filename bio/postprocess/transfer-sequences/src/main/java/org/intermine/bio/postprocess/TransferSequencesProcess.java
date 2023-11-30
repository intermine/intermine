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
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.bio.util.ClobAccessReverseComplement;
import org.intermine.bio.util.Constants;
import org.intermine.bio.util.PostProcessUtil;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Sequence;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.model.bio.Supercontig;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ClobAccess;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.PendingClob;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;
import org.intermine.postprocess.PostProcessor;

/**
 * Transfer sequences from the assembly objects to the other objects that are located on the
 * assemblies and to the objects that the assemblies are located on (eg. Chromosomes).
 *
 * @author Kim Rutherford
 * @author Sam Hokin
 */
public class TransferSequencesProcess extends PostProcessor {
    private static final Logger LOG = Logger.getLogger(TransferSequencesProcess.class);

    private Model model; // stored for various methods

    /**
     * Create a new instance
     *
     * @param osw object store writer
     */
    public TransferSequencesProcess(ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * {@inheritDoc}
     * <br/>
     * Main post-processing routine.
     * @throws ObjectStoreException if the objectstore throws an exception
     */
    public void postProcess() throws IllegalAccessException, MetaDataException, ObjectStoreException {
        model = Model.getInstanceByName("genomic");
        transferToChromosomeOrSupercontigLocatedSequenceFeatures();
        transferToTranscripts();
    }

    /**
     * Store a new Sequence along with its updated SequenceFeature.
     */
    private void storeNewSequence(SequenceFeature feature, ClobAccess sequenceString) throws ObjectStoreException {
        Sequence sequence = (Sequence) DynamicUtil.createObject(Collections.singleton(Sequence.class));
        sequence.setResidues(sequenceString);
        sequence.setLength(sequenceString.length());
        osw.store(sequence);
        feature.proxySequence(new ProxyReference(osw.getObjectStore(), sequence.getId(), Sequence.class));
        feature.setLength(new Integer(sequenceString.length()));
        osw.store(feature);
    }

    /**
     * Use the Location relations to copy the sequence from the Chromosomes and Supercontigs to every
     * SequenceFeature that is located on them and which doesn't already have a sequence.
     * Uses the ObjectStoreWriter that was passed to the constructor.
     */
    protected void transferToChromosomeOrSupercontigLocatedSequenceFeatures() throws IllegalAccessException, ObjectStoreException {
        // get the Chromosomes
        long startTime = System.currentTimeMillis();
        Query qChr = new Query();
        QueryClass qcChr = new QueryClass(Chromosome.class);
        qChr.addFrom(qcChr);
        qChr.addToSelect(qcChr);
        QueryObjectReference seqRefChr = new QueryObjectReference(qcChr, "sequence");
        ContainsConstraint ccChr = new ContainsConstraint(seqRefChr, ConstraintOp.IS_NOT_NULL);
        qChr.setConstraint(ccChr);
        Set<Chromosome> chromosomes = new HashSet<Chromosome>();
        SingletonResults resChr = osw.getObjectStore().executeSingleton(qChr);
        for (Object obj : resChr.asList()) {
            Chromosome chr = (Chromosome) obj;
            chromosomes.add(chr);
        }
        LOG.info("Found " + chromosomes.size() + " chromosomes with sequence, took " + (System.currentTimeMillis() - startTime) + " ms.");

        // get the Supercontigs
        startTime = System.currentTimeMillis();
        Query qSup = new Query();
        QueryClass qcSup = new QueryClass(Supercontig.class);
        qSup.addFrom(qcSup);
        qSup.addToSelect(qcSup);
        QueryObjectReference seqRefSup = new QueryObjectReference(qcSup, "sequence");
        ContainsConstraint ccSup = new ContainsConstraint(seqRefSup, ConstraintOp.IS_NOT_NULL);
        qSup.setConstraint(ccSup);
        Set<Supercontig> supercontigs = new HashSet<Supercontig>();
        SingletonResults resSup = osw.getObjectStore().executeSingleton(qSup);
        for (Object obj : resSup.asList()) {
            Supercontig sup = (Supercontig) obj;
            supercontigs.add(sup);
        }
        LOG.info("Found " + supercontigs.size() + " supercontigs with sequence, took " + (System.currentTimeMillis() - startTime) + " ms.");

        // do the transfer work for Chromosomes
        // CDS can be discontiguous, process them separately
        for (Chromosome chr : chromosomes) {
            int numFeatures = transferForChromosomeOrSupercontig(chr, null);
            if (numFeatures > 0) {
                transferToCDSs(chr, null);
            }
        }

        // do the transfer work for Supercontigs
        // CDS can be discontiguous, process them separately
        for (Supercontig sup : supercontigs) {
            int numFeatures = transferForChromosomeOrSupercontig(null, sup);
            if (numFeatures > 0) {
                transferToCDSs(null, sup);
            }
        }
    }

    /**
     * Transfer sequences for the given chromosome or supercontig. One or the other must be null.
     *
     * @param chr chromosome (null to process a Supercontig)
     * @param sup supercontig (null to process a Chromosome)
     * @return the number of features that have been updated
     */
    protected int transferForChromosomeOrSupercontig(Chromosome chr, Supercontig sup) throws IllegalAccessException, ObjectStoreException {
        long startTime = System.currentTimeMillis();

        boolean isChromosome = (chr != null);

        int id = 0;
        String primaryIdentifier = null;
        if (isChromosome) {
            id = chr.getId();
            primaryIdentifier = chr.getPrimaryIdentifier();
        } else {
            id = sup.getId();
            primaryIdentifier = sup.getPrimaryIdentifier();
        }

        Query q = new Query();
        q.setDistinct(false);

        QueryClass qc = null;
        if (isChromosome) {
            qc = new QueryClass(Chromosome.class);
        } else {
            qc = new QueryClass(Supercontig.class);
        }
        q.addFrom(qc);

        // Chromosome/Supercontig must have given id
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryField qfId = new QueryField(qc, "id");
        cs.addConstraint(new SimpleConstraint(qfId, ConstraintOp.EQUALS, new QueryValue(id)));
        
        // query SequenceFeature
        QueryClass qcSub = new QueryClass(SequenceFeature.class);
        q.addFrom(qcSub);
        q.addToSelect(qcSub);
        q.addToOrderBy(qcSub);

        // query Location
        QueryClass qcLoc = new QueryClass(Location.class);
        q.addFrom(qcLoc);
        q.addToSelect(qcLoc);

        // Location must be located on our Chromosome/Supercontig
        QueryObjectReference ref1 = new QueryObjectReference(qcLoc, "locatedOn");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qc);
        cs.addConstraint(cc1);
        
        // Location must be for the SequenceFeature
        QueryObjectReference ref2 = new QueryObjectReference(qcLoc, "feature");
        ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcSub);
        cs.addConstraint(cc2);

        // the feature sequence must be null
        QueryObjectReference lsfSeqRef = new QueryObjectReference(qcSub, "sequence");
        ContainsConstraint lsfSeqRefNull = new ContainsConstraint(lsfSeqRef, ConstraintOp.IS_NULL);
        cs.addConstraint(lsfSeqRefNull);

        // set the constraint
        q.setConstraint(cs);

        // create precompute indexes on our QueryClass objects to speed things up (?)
        Set<QueryNode> indexesToCreate = new HashSet<QueryNode>();
        indexesToCreate.add(qcLoc);
        indexesToCreate.add(qcSub);
        ((ObjectStoreInterMineImpl) osw.getObjectStore()).precompute(q, indexesToCreate, Constants.PRECOMPUTE_CATEGORY);

        // run the query
        Results results = osw.getObjectStore().execute(q, 1000, true, true, true);
        int numResults = results.size();
        if (numResults > 0) {
            int count = 0;
            long start = System.currentTimeMillis();
            osw.beginTransaction();
            for (Object obj : results.asList()) {
                ResultsRow rr = (ResultsRow) obj;
                SequenceFeature feature = (SequenceFeature) rr.get(0);
                Location location = (Location) rr.get(1);
                
                if (PostProcessUtil.isInstance(model, feature, "ChromosomeBand")) {
                    continue;
                }
                
                if (PostProcessUtil.isInstance(model, feature, "SNP")) {
                    continue;
                }
                
                // if we set here the transcripts, using start and end locations,
                // we won't be using the transferToTranscripts method (collating the exons)
                if (PostProcessUtil.isInstance(model, feature, "Transcript")) {
                    continue;
                }
                
                // CDSs might have multiple locations, process in transferToCDSs() instead
                if (PostProcessUtil.isInstance(model, feature, "CDS")) {
                    continue;
                }
                
                /**
                 * In human intermine, SNP is not a sequence alteration, which I think is wrong
                 * But here are the kinds of types that are alterations:
                 *
                 *      Deletion
                 *      Genetic Marker
                 *      Indel
                 *      Insertion
                 *      SNV
                 *      Substitution
                 *      Tandem Repeat
                 */
                if (PostProcessUtil.isInstance(model, feature, "SequenceAlteration")) {
                    continue;
                }
                
                if (feature instanceof Gene) {
                    Gene gene = (Gene) feature;
                    if (gene.getLength() != null && gene.getLength().intValue() > 2000000) {
                        LOG.warn("gene too long in transferToSequenceFeatures() ignoring: " + gene);
                        continue;
                    }
                }
                
                ClobAccess featureSeq = null;
                if (isChromosome) {
                    featureSeq = getSubSequence(chr.getSequence(), location);
                } else {
                    featureSeq = getSubSequence(sup.getSequence(), location);
                }
                
                if (featureSeq == null) {
                    // probably the location is out of range
                    LOG.info("Could not get feature sequence for location: " + location);
                    continue;
                }
                
                Sequence sequence = (Sequence) DynamicUtil.createObject(Collections.singleton(Sequence.class));
                sequence.setResidues(featureSeq);
                sequence.setLength(featureSeq.length());
                osw.store(sequence);
                SequenceFeature clone = PostProcessUtil.cloneInterMineObject(feature);
                clone.setSequence(sequence);
                clone.setLength(new Integer(featureSeq.length()));
                osw.store(clone);
                count++;
            }
            osw.commitTransaction();
            LOG.info("Finished setting " + count + " feature sequences for " + primaryIdentifier + ", took " + (System.currentTimeMillis() - startTime) + " ms.");
        }

        return numResults;
    }

    /**
     * Get the subsequence for a Location on a given Sequence.
     */
    private static ClobAccess getSubSequence(Sequence chromosomeSequence, Location location) {
        int charsToCopy = location.getEnd().intValue() - location.getStart().intValue() + 1;
        ClobAccess chromosomeSequenceString = chromosomeSequence.getResidues();

        if (charsToCopy > chromosomeSequenceString.length()) {
            LOG.warn("SequenceFeature too long, ignoring - Location: "
                    + location.getId() + "  LSF id: " + location.getFeature());
            return null;
        }

        int startPos = location.getStart().intValue() - 1;
        int endPos = startPos + charsToCopy;

        if (startPos < 0 || endPos < 0) {
            LOG.warn("SequenceFeature has negative coordinate, ignoring Location: "
                    + location.getId() + "  LSF id: " + location.getFeature());
            return null;
        }

        if (endPos > chromosomeSequenceString.length()) {
            LOG.warn(" has end coordinate greater than chromsome length."
                    + "ignoring Location: "
                    + location.getId() + "  LSF id: " + location.getFeature());
            return null;
        }

        ClobAccess subSeqString;

        if (startPos < endPos) {
            subSeqString = chromosomeSequenceString.subSequence(startPos, endPos);
        } else {
            subSeqString = chromosomeSequenceString.subSequence(endPos, startPos);
        }

        if ("-1".equals(location.getStrand())) {
            subSeqString = new ClobAccessReverseComplement(subSeqString);
        }

        return subSeqString;
    }


    /**
     * For each Transcript, join and transfer the sequences from the child Exons to a new Sequence
     * object for the Transcript.  Uses the ObjectStoreWriter that was passed to the constructor
     */
    protected void transferToTranscripts() throws MetaDataException, ObjectStoreException {

        String message = "Now performing TransferSequences.transferToTranscripts ";
        PostProcessUtil.checkFieldExists(model, "Transcript", "exons", message);
        PostProcessUtil.checkFieldExists(model, "Exon", null, message);

        long startTime = System.currentTimeMillis();

        Query q = new Query();
        q.setDistinct(false);

        // Transcript
        QueryClass qcTranscript = new QueryClass(model.getClassDescriptorByName("Transcript").getType());
        q.addFrom(qcTranscript);
        q.addToSelect(qcTranscript);
        q.addToOrderBy(qcTranscript);

        // Exon
        QueryClass qcExon = new QueryClass(model.getClassDescriptorByName("Exon").getType());
        q.addFrom(qcExon);
        q.addToSelect(qcExon);

        // Sequence
        QueryClass qcExonSequence = new QueryClass(Sequence.class);
        q.addFrom(qcExonSequence);
        q.addToSelect(qcExonSequence);

        // Location
        QueryClass qcExonLocation = new QueryClass(Location.class);
        q.addFrom(qcExonLocation);
        q.addToSelect(qcExonLocation);

        // Exon.location.start
        QueryField qfExonStart = new QueryField(qcExonLocation, "start");
        q.addToSelect(qfExonStart);
        q.addToOrderBy(qfExonStart);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        // Transcript.exons
        QueryCollectionReference exonsRef = new QueryCollectionReference(qcTranscript, "exons");
        ContainsConstraint cc1 = new ContainsConstraint(exonsRef, ConstraintOp.CONTAINS, qcExon);
        cs.addConstraint(cc1);

        // exon.chromosomeLocation
        QueryObjectReference locRef = new QueryObjectReference(qcExon, "chromosomeLocation");
        ContainsConstraint cc2 = new ContainsConstraint(locRef, ConstraintOp.CONTAINS, qcExonLocation);
        cs.addConstraint(cc2);

        // Exon.sequence
        QueryObjectReference sequenceRef = new QueryObjectReference(qcExon, "sequence");
        ContainsConstraint cc3 = new ContainsConstraint(sequenceRef, ConstraintOp.CONTAINS, qcExonSequence);
        cs.addConstraint(cc3);

        // Transcript.sequence IS NULL
        QueryObjectReference transcriptSeqRef = new QueryObjectReference(qcTranscript, "sequence");
        ContainsConstraint lsfSeqRefNull = new ContainsConstraint(transcriptSeqRef, ConstraintOp.IS_NULL);
        cs.addConstraint(lsfSeqRefNull);

        q.setConstraint(cs);

        ((ObjectStoreInterMineImpl) osw.getObjectStore()).precompute(q, Constants.PRECOMPUTE_CATEGORY);

        SequenceFeature currentTranscript = null;
        StringBuffer currentTranscriptBases = new StringBuffer();

        Results results = osw.getObjectStore().execute(q, 1000, true, true, true);
        if (results.size() > 0) {
            long start = System.currentTimeMillis();
            int i = 0;
            osw.beginTransaction();
            for (Object obj : results.asList()) {
                ResultsRow rr = (ResultsRow) obj;
                SequenceFeature transcript =  (SequenceFeature) rr.get(0);

                if (currentTranscript == null || !transcript.equals(currentTranscript)) {
                    if (currentTranscript != null) {
                        // copy sequence to transcript
                        storeNewSequence(currentTranscript, new PendingClob(currentTranscriptBases.toString()));
                        i++;
                        if (i % 100 == 0) {
                            LOG.info("Set sequences for " + i + " Transcripts" + " (avg = " + ((60000L * i) / (System.currentTimeMillis() - start)) + " per minute)");
                        }
                    }
                    currentTranscriptBases = new StringBuffer();
                    currentTranscript = transcript;
                }
                
                Sequence exonSequence = (Sequence) rr.get(2);
                Location  location = (Location) rr.get(3);
                
                // add exon
                if (location.getStrand() != null && "-1".equals(location.getStrand())) {
                    currentTranscriptBases.insert(0, exonSequence.getResidues().toString());
                } else {
                    currentTranscriptBases.append(exonSequence.getResidues().toString());
                }
            }
            if (currentTranscript == null) {
                LOG.info("In transferToTranscripts(): no Transcripts found");
            } else {
                storeNewSequence(currentTranscript, new PendingClob(currentTranscriptBases.toString()));
            }
            osw.commitTransaction();

            LOG.info("Finished setting " + i + " Transcript sequences - took " + (System.currentTimeMillis() - startTime) + " ms.");
        }
    }

    /**
     * For each CDS, join and transfer the sequences from the each CDS location to a new Sequence
     * object for the CDS.  Uses the ObjectStoreWriter that was passed to the constructor
     *
     * CDS.sequence length is a sum of all locations. CDS.sequence residues should be the
     * combined sequence of all the locations.
     */
    private void transferToCDSs(Chromosome chr, Supercontig sup) throws ObjectStoreException {
        long startTime = System.currentTimeMillis();

        boolean isChromosome = (chr != null);

        Sequence sequence = null;
        if (isChromosome) {
            sequence = chr.getSequence();
        } else {
            sequence = sup.getSequence();
        }

        // get all CDSs for this chromosome/supercontig
        Query q = getCDSQuery(chr, sup);
        ((ObjectStoreInterMineImpl) osw.getObjectStore()).precompute(q, Constants.PRECOMPUTE_CATEGORY);

        SequenceFeature currentCDS = null;
        StringBuffer currentCDSBases = new StringBuffer();

        Results res = osw.getObjectStore().execute(q, 1000, true, true, true);
        if (res.size() > 0) {
            long start = System.currentTimeMillis();
            int i = 0;
            osw.beginTransaction();
            for (Object obj : res.asList()) {
                ResultsRow rr = (ResultsRow) obj;
                SequenceFeature cds =  (SequenceFeature) rr.get(0);
                
                // if this is a new CDS, store the sequence for the just-processed previous CDS
                if (currentCDS == null || !cds.equals(currentCDS)) {
                    if (currentCDS != null) {
                        // copy sequence to CDS
                        storeNewSequence(currentCDS, new PendingClob(currentCDSBases.toString()));
                        i++;
                        if (i % 100 == 0) {
                            LOG.info("Set sequences for " + i + " CDSs" + " (avg = " + ((60000L * i) / (System.currentTimeMillis() - start)) + " per minute)");
                        }
                    }
                    // reset for current CDS
                    currentCDSBases = new StringBuffer();
                    currentCDS = cds;
                }
                
                Location  location = (Location) rr.get(1);
                
                // add CDS
                ClobAccess clob = getSubSequence(sequence, location);
                if (location.getStrand() != null && "-1".equals(location.getStrand())) {
                    currentCDSBases.insert(0, clob.toString());
                } else {
                    currentCDSBases.append(clob.toString());
                }
            }
            if (currentCDS == null) {
                LOG.info("In transferToCDSs(): no CDSs found");
            } else {
                storeNewSequence(currentCDS, new PendingClob(currentCDSBases.toString()));
            }
            osw.commitTransaction();

            LOG.info("Finished setting " + i + " CDS sequences - took " + (System.currentTimeMillis() - startTime) + " ms.");
        }
    }

    /**
     * Return a Query for CDSes on a Chromosome or Supercontig.
     */
    private Query getCDSQuery(Chromosome chr, Supercontig sup) {
        boolean isChromosome = (chr != null);

        int id = 0;
        if (isChromosome) {
            id = chr.getId();
        } else {
            id = sup.getId();
        }
        
        Query q = new Query();
        q.setDistinct(false);

        // Chromosome / Supercontig
        QueryClass qc = null;
        if (isChromosome) {
            qc = new QueryClass(Chromosome.class);
        } else {
            qc = new QueryClass(Supercontig.class);
        }
        q.addFrom(qc);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        // get all CDSs for this chromosome/supercontig only
        QueryField qfId = new QueryField(qc, "id");
        cs.addConstraint(new SimpleConstraint(qfId, ConstraintOp.EQUALS, new QueryValue(id)));
        
        // CDS
        QueryClass qcCDS = new QueryClass(model.getClassDescriptorByName("CDS").getType());
        q.addFrom(qcCDS);
        q.addToSelect(qcCDS);
        q.addToOrderBy(qcCDS);

        // CDS.Location
        QueryClass qcCDSLocation = new QueryClass(Location.class);
        q.addFrom(qcCDSLocation);
        q.addToSelect(qcCDSLocation);

        // CDS.location.start
        QueryField qfCDSStart = new QueryField(qcCDSLocation, "start");
        q.addToSelect(qfCDSStart);
        q.addToOrderBy(qfCDSStart);

        // Location.locatedOn == chromosome of interest
        QueryObjectReference ref1 = new QueryObjectReference(qcCDSLocation, "locatedOn");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qc);
        cs.addConstraint(cc1);

        // CDS.Locations
        QueryCollectionReference locationsRefs = new QueryCollectionReference(qcCDS, "locations");
        ContainsConstraint cc2 = new ContainsConstraint(locationsRefs, ConstraintOp.CONTAINS, qcCDSLocation);
        cs.addConstraint(cc2);

        // CDS.sequence IS NULL
        QueryObjectReference transcriptSeqRef = new QueryObjectReference(qcCDS, "sequence");
        ContainsConstraint lsfSeqRefNull = new ContainsConstraint(transcriptSeqRef, ConstraintOp.IS_NULL);
        cs.addConstraint(lsfSeqRefNull);

        q.setConstraint(cs);

        return q;
    }
}
