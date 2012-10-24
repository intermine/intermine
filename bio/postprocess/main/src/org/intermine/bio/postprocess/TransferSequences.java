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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.bio.util.ClobAccessReverseComplement;
import org.intermine.bio.util.Constants;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Sequence;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ClobAccess;
import org.intermine.objectstore.query.ConstraintOp;
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

/**
 * Transfer sequences from the Assembly objects to the other objects that are located on the
 * Assemblys and to the objects that the Assemblys are located on (eg. Chromosomes).
 *
 * @author Kim Rutherford
 */

public class TransferSequences
{
    protected ObjectStoreWriter osw;
    private Model model;
    private static final Logger LOG = Logger.getLogger(TransferSequences.class);

    /**
     * Create a new TransferSequences object from the given ObjectStoreWriter
     * @param osw writer on genomic ObjectStore
     */
    public TransferSequences (ObjectStoreWriter osw) {
        this.osw = osw;
        this.model = osw.getModel();
    }

    private void storeNewSequence(SequenceFeature feature, ClobAccess sequenceString)
        throws ObjectStoreException {
        Sequence sequence =
            (Sequence) DynamicUtil.createObject(Collections.singleton(Sequence.class));
        sequence.setResidues(sequenceString);
        sequence.setLength(sequenceString.length());
        osw.store(sequence);
        feature.proxySequence(new ProxyReference(osw.getObjectStore(),
                                                 sequence.getId(), Sequence.class));
        feature.setLength(new Integer(sequenceString.length()));
        osw.store(feature);
    }

    /**
     * Use the Location relations to copy the sequence from the Chromosomes to every
     * SequenceFeature that is located on a Chromosome and which doesn't already have a
     * sequence (ie. don't copy to Assembly).  Uses the ObjectStoreWriter that was passed to the
     * constructor.
     *
     * @throws Exception if there are problems with the transfer
     */
    public void transferToLocatedSequenceFeatures()
        throws Exception {
        long startTime = System.currentTimeMillis();

        ObjectStore os = osw.getObjectStore();
        Query q = new Query();
        QueryClass qcChr = new QueryClass(Chromosome.class);
        q.addFrom(qcChr);
        q.addToSelect(qcChr);
        QueryObjectReference seqRef = new QueryObjectReference(qcChr, "sequence");
        ContainsConstraint cc = new ContainsConstraint(seqRef, ConstraintOp.IS_NOT_NULL);
        q.setConstraint(cc);

        SingletonResults res = os.executeSingleton(q);
        Iterator<?> chrIter = res.iterator();

        Set<Chromosome> chromosomes = new HashSet<Chromosome>();
        while (chrIter.hasNext()) {
            Chromosome chr = (Chromosome) chrIter.next();
            chromosomes.add(chr);
        }

        LOG.info("Found " + chromosomes.size() + " chromosomes with sequence, took "
                + (System.currentTimeMillis() - startTime) + " ms.");

        for (Chromosome chr : chromosomes) {
            String organism = "";
            if (chr.getOrganism() != null) {
                organism = chr.getOrganism().getShortName();
            }
            LOG.info("Starting transfer for " + organism + " chromosome "
                    + chr.getPrimaryIdentifier());
            transferForChromosome(chr);
        }
    }


    private void transferForChromosome(Chromosome chr) throws Exception {

        long startTime = System.currentTimeMillis();

        ObjectStore os = osw.getObjectStore();
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qcChr = new QueryClass(Chromosome.class);
        QueryField qfChrId = new QueryField(qcChr, "id");
        q.addFrom(qcChr);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        SimpleConstraint sc = new SimpleConstraint(qfChrId, ConstraintOp.EQUALS,
                new QueryValue(chr.getId()));
        cs.addConstraint(sc);

        QueryClass qcSub = new QueryClass(SequenceFeature.class);
        q.addFrom(qcSub);
        q.addToSelect(qcSub);

        q.addToOrderBy(qcSub);

        QueryClass qcLoc = new QueryClass(Location.class);
        q.addFrom(qcLoc);
        q.addToSelect(qcLoc);
        QueryObjectReference ref1 = new QueryObjectReference(qcLoc, "locatedOn");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcChr);
        cs.addConstraint(cc1);
        QueryObjectReference ref2 = new QueryObjectReference(qcLoc, "feature");
        ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcSub);
        cs.addConstraint(cc2);

        QueryObjectReference lsfSeqRef = new QueryObjectReference(qcSub, "sequence");
        ContainsConstraint lsfSeqRefNull = new ContainsConstraint(lsfSeqRef, ConstraintOp.IS_NULL);

        cs.addConstraint(lsfSeqRefNull);

        q.setConstraint(cs);

        osw.beginTransaction();

        Set<QueryNode> indexesToCreate = new HashSet<QueryNode>();
        indexesToCreate.add(qcLoc);
        indexesToCreate.add(qcSub);
        ((ObjectStoreInterMineImpl) os).precompute(q, indexesToCreate,
            Constants.PRECOMPUTE_CATEGORY);
        Results results = os.execute(q, 1000, true, true, true);

        @SuppressWarnings("unchecked") Iterator<ResultsRow> resIter = (Iterator) results.iterator();

        long start = System.currentTimeMillis();
        int i = 0;
        while (resIter.hasNext()) {
            ResultsRow<?> rr = resIter.next();

            SequenceFeature feature = (SequenceFeature) rr.get(0);
            Location locationOnChr = (Location) rr.get(1);

            try {

                if (PostProcessUtil.isInstance(model, feature, "ChromosomeBand")) {
                    continue;
                }

                if (PostProcessUtil.isInstance(model, feature, "Transcript")) {
                    continue;
                }

                if (PostProcessUtil.isInstance(model, feature, "CDS")) {
                    continue;
                }

                if (PostProcessUtil.isInstance(model, feature, "SNP")) {
                    continue;
                }

                if (feature instanceof Gene) {
                    Gene gene = (Gene) feature;
                    if (gene.getLength() != null && gene.getLength().intValue() > 2000000) {
                        LOG.warn("gene too long in transferToSequenceFeatures() ignoring: "
                                  + gene);
                        continue;
                    }
                }

                ClobAccess featureSeq = getSubSequence(chr.getSequence(), locationOnChr);

                if (featureSeq == null) {
                    // probably the locationOnChr is out of range
                    continue;
                }

                Sequence sequence =
                    (Sequence) DynamicUtil.createObject(Collections.singleton(Sequence.class));
                sequence.setResidues(featureSeq);
                sequence.setLength(featureSeq.length());
                osw.store(sequence);
                SequenceFeature cloneLsf = PostProcessUtil.cloneInterMineObject(feature);
                cloneLsf.setSequence(sequence);
                cloneLsf.setLength(new Integer(featureSeq.length()));
                osw.store(cloneLsf);
                i++;
                if (i % 1000 == 0) {
                    long now = System.currentTimeMillis();
                    LOG.info("Set sequences for " + i + " features"
                             + " (avg = " + ((60000L * i) / (now - start)) + " per minute)");
                }
            } catch (Exception e) {
                Exception e2 = new Exception("Exception while processing SequenceFeature "
                        + feature);
                e2.initCause(e);
                throw e2;
            }
        }

        osw.commitTransaction();

        String organism = "";
        if (chr.getOrganism() != null) {
            organism = chr.getOrganism().getShortName();
        }
        LOG.info("Finished setting " + i + " feature sequences for " + organism + " chromosome "
                + chr.getPrimaryIdentifier() + " - took "
                + (System.currentTimeMillis() - startTime) + " ms.");
    }

    private ClobAccess getSubSequence(Sequence chromosomeSequence, Location locationOnChr) {
        int charsToCopy =
            locationOnChr.getEnd().intValue() - locationOnChr.getStart().intValue() + 1;
        ClobAccess chromosomeSequenceString = chromosomeSequence.getResidues();

        if (charsToCopy > chromosomeSequenceString.length()) {
            LOG.warn("SequenceFeature too long, ignoring - Location: "
                      + locationOnChr.getId() + "  LSF id: " + locationOnChr.getFeature());
            return null;
        }

        int startPos = locationOnChr.getStart().intValue() - 1;
        int endPos = startPos + charsToCopy;

        if (startPos < 0 || endPos < 0) {
            LOG.warn("SequenceFeature has negative coordinate, ignoring Location: "
                      + locationOnChr.getId() + "  LSF id: " + locationOnChr.getFeature());
            return null;
        }

        if (endPos > chromosomeSequenceString.length()) {
            LOG.warn(" has end coordinate greater than chromsome length."
                      + "ignoring Location: "
                      + locationOnChr.getId() + "  LSF id: " + locationOnChr.getFeature());
            return null;
        }

        ClobAccess subSeqString;

        if (startPos < endPos) {
            subSeqString = chromosomeSequenceString.subSequence(startPos, endPos);
        } else {
            subSeqString = chromosomeSequenceString.subSequence(endPos, startPos);
        }

        if ("-1".equals(locationOnChr.getStrand())) {
            subSeqString = new ClobAccessReverseComplement(subSeqString);
        }

        return subSeqString;
    }


    /**
     * For each Transcript, join and transfer the sequences from the child Exons to a new Sequence
     * object for the Transcript.  Uses the ObjectStoreWriter that was passed to the constructor
     * @throws Exception if there are problems with the transfer
     */
    public void transferToTranscripts()
        throws Exception {

        try {
            String message = "Not performing TransferSequences.transferToTranscripts ";
            PostProcessUtil.checkFieldExists(model, "Transcript", "exons", message);
            PostProcessUtil.checkFieldExists(model, "Exon", null, message);
        } catch (MetaDataException e) {
            return;
        }

        long startTime = System.currentTimeMillis();

        osw.beginTransaction();

        ObjectStore os = osw.getObjectStore();
        Query q = new Query();
        q.setDistinct(false);

        QueryClass qcTranscript =
            new QueryClass(model.getClassDescriptorByName("Transcript").getType());
        q.addFrom(qcTranscript);
        q.addToSelect(qcTranscript);
        q.addToOrderBy(qcTranscript);

        QueryClass qcExon = new QueryClass(model.getClassDescriptorByName("Exon").getType());
        q.addFrom(qcExon);
        q.addToSelect(qcExon);


        QueryClass qcExonSequence = new QueryClass(Sequence.class);
        q.addFrom(qcExonSequence);
        q.addToSelect(qcExonSequence);

        QueryClass qcExonLocation = new QueryClass(Location.class);
        q.addFrom(qcExonLocation);
        q.addToSelect(qcExonLocation);

        QueryField qfExonStart = new QueryField(qcExonLocation, "start");
        q.addToSelect(qfExonStart);
        q.addToOrderBy(qfExonStart);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference exonsRef =
            new QueryCollectionReference(qcTranscript, "exons");
        ContainsConstraint cc1 =
            new ContainsConstraint(exonsRef, ConstraintOp.CONTAINS, qcExon);
        cs.addConstraint(cc1);

        QueryObjectReference locRef =
            new QueryObjectReference(qcExon, "chromosomeLocation");
        ContainsConstraint cc2 =
            new ContainsConstraint(locRef, ConstraintOp.CONTAINS, qcExonLocation);
        cs.addConstraint(cc2);

        QueryObjectReference sequenceRef = new QueryObjectReference(qcExon, "sequence");
        ContainsConstraint cc3 =
            new ContainsConstraint(sequenceRef, ConstraintOp.CONTAINS, qcExonSequence);
        cs.addConstraint(cc3);

        QueryObjectReference transcriptSeqRef = new QueryObjectReference(qcTranscript, "sequence");
        ContainsConstraint lsfSeqRefNull =
            new ContainsConstraint(transcriptSeqRef, ConstraintOp.IS_NULL);

        cs.addConstraint(lsfSeqRefNull);

        q.setConstraint(cs);

        ((ObjectStoreInterMineImpl) os).precompute(q, Constants
                                                   .PRECOMPUTE_CATEGORY);
        Results res = os.execute(q, 1000, true, true, true);

        Iterator<?> resIter = res.iterator();

        SequenceFeature currentTranscript = null;
        StringBuffer currentTranscriptBases = new StringBuffer();

        long start = System.currentTimeMillis();
        int i = 0;
        while (resIter.hasNext()) {
            ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
            SequenceFeature transcript =  (SequenceFeature) rr.get(0);

            if (currentTranscript == null || !transcript.equals(currentTranscript)) {
                if (currentTranscript != null) {
                    storeNewSequence(currentTranscript,
                            new PendingClob(currentTranscriptBases.toString()));
                    i++;
                    if (i % 100 == 0) {
                        long now = System.currentTimeMillis();
                        LOG.info("Set sequences for " + i + " Transcripts"
                                + " (avg = " + ((60000L * i) / (now - start)) + " per minute)");
                    }
                }
                currentTranscriptBases = new StringBuffer();
                currentTranscript = transcript;
            }

            Sequence exonSequence = (Sequence) rr.get(2);
            Location  location = (Location) rr.get(3);
            if (location.getStrand() != null && "-1".equals(location.getStrand())) {
                currentTranscriptBases.insert(0, exonSequence.getResidues().toString());
            } else {
                currentTranscriptBases.append(exonSequence.getResidues().toString());
            }
        }
        if (currentTranscript == null) {
            LOG.error("in transferToTranscripts(): no Transcripts found");
        } else {
            storeNewSequence(currentTranscript, new PendingClob(currentTranscriptBases.toString()));
        }

        LOG.info("Finished setting " + i + " Trascript sequences - took "
                 + (System.currentTimeMillis() - startTime) + " ms.");

        osw.commitTransaction();
    }
}
