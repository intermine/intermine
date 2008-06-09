package org.intermine.bio.gbrowse;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
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

import org.intermine.bio.postprocess.PostProcessOperationsTask;
import org.intermine.bio.postprocess.PostProcessUtil;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

import org.flymine.model.genomic.Analysis;
import org.flymine.model.genomic.CDS;
import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.ChromosomeBand;
import org.flymine.model.genomic.ComputationalAnalysis;
import org.flymine.model.genomic.ComputationalResult;
import org.flymine.model.genomic.Evidence;
import org.flymine.model.genomic.Exon;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Location;
import org.flymine.model.genomic.MRNA;
import org.flymine.model.genomic.NcRNA;
import org.flymine.model.genomic.Sequence;
import org.flymine.model.genomic.Synonym;
import org.flymine.model.genomic.Transcript;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * A Task for creating GFF and FASTA files for use by GBrowse.  Only those features that are
 * located on a Chromosome are written.
 * @author Kim Rutherford
 */

public class WriteGFFTask extends Task
{
    private static final Logger LOG = Logger.getLogger(WriteGFFTask.class);

    private String alias;

    private File destinationDirectory;

    /**
     * Set the ObjectStore alias to read from
     * @param alias name of the ObjectStore
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Set the name of the directory where the GFF and FASTA files should be created.
     * @param destinationDirectory the directory for creating new files in.
     */
    public void setDest(File destinationDirectory) {
        this.destinationDirectory = destinationDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws BuildException {
        if (destinationDirectory == null) {
            throw new BuildException("dest attribute is not set");
        }
        if (alias == null) {
            throw new BuildException("alias attribute is not set");
        }

        ObjectStore os = null;
        try {
            os = ObjectStoreFactory.getObjectStore(alias);
            writeGFF(os);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new BuildException(e);
        }
    }

    final private static Class<LocatedSequenceFeature> LOCATED_SEQUENCE_FEATURE_CLASS =
        LocatedSequenceFeature.class;

    /**
     * Create a GFF and FASTA files for the objects in the given ObjectStore, suitable for reading
     * by GBrowse.
     * @param os the ObjectStore to read from
     * @throws ObjectStoreException if the is a problem with the ObjectStore
     * @throws IOException if there is a problem while writing
     */
    void writeGFF(ObjectStore os)
        throws ObjectStoreException, IOException {

        Results results =
            PostProcessUtil.findLocationAndObjects(os, Chromosome.class,
                                                   LOCATED_SEQUENCE_FEATURE_CLASS, false);

        results.setBatchSize(2000);

        Iterator<ResultsRow> resIter = results.iterator();

        PrintWriter gffWriter = null;

        // a Map of object classes to counts
        Map<String, Integer> objectCounts = null;

        // Map from Transcript to Location (on Chromosome)
        Map<Transcript, Location> seenTranscripts = new HashMap<Transcript, Location>();
        // Map from exon primary identifier to Location (on Chromosome)
        Map<String, Location> seenTranscriptParts = new HashMap<String, Location>();

        // the last Chromosome seen
        Integer currentChrId = null;
        Chromosome currentChr = null;

        Map<Integer, List<String>> synonymMap = null;
        Map<Integer, List<Evidence>> evidenceMap = null;

        while (resIter.hasNext()) {
            ResultsRow rr = resIter.next();
            Integer resultChrId = (Integer) rr.get(0);
            LocatedSequenceFeature feature = (LocatedSequenceFeature) rr.get(1);
            Location loc = (Location) rr.get(2);

            // TODO XXX FIXME - see #628
            if (feature instanceof ChromosomeBand) {
                continue;
            }

            try {
                if (TypeUtil.isInstanceOf(feature,
                                          "org.flymine.model.genomic.ArtificialDeletion")) {
                    try {
                        if (TypeUtil.getFieldValue(feature, "available") != Boolean.TRUE) {
                            // write only the available deletions because there are too many
                            // ArtificialDeletions for GBrowse to work well
                            continue;
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("can't access 'available' field in: " + feature);
                    }
                }
            } catch (ClassNotFoundException e) {
               // ignore - ArtificialDeletion is not in the model
            }

            if (feature instanceof CDS) {
                // ignore for now as it interferes with the CDS GFF records created by
                // writeTranscriptsAndExons() for use by the processed_transcript
                // aggregator
                continue;
            }

            // special case for stemcellmine
            if (feature.getChromosome() == null) {
                continue;
            }

            if (feature.getChromosome().getPrimaryIdentifier() == null) {
                continue;
            }

            if (currentChrId == null || !currentChrId.equals(resultChrId)) {
                if (currentChrId != null) {
                    writeTranscriptsAndExons(gffWriter, currentChr, seenTranscripts,
                                             seenTranscriptParts, synonymMap, evidenceMap);
                    seenTranscripts = new HashMap<Transcript, Location>();
                    seenTranscriptParts = new HashMap<String, Location>();
                }

                synonymMap = makeSynonymMap(os, resultChrId);
                evidenceMap = makeEvidenceMap(os, resultChrId);

                currentChr = (Chromosome) os.getObjectById(resultChrId);

                if (currentChr == null) {
                    throw new RuntimeException("get null from getObjectById()");
                }

                if (currentChr.getPrimaryIdentifier() == null) {
                    LOG.error("chromosome has no identifier: " + currentChr);
                    continue;
                }

                if (currentChr.getOrganism() == null) {
                    LOG.error("chromosome has no organism: " + currentChr);
                    continue;
                }

                if (currentChr.getOrganism().getAbbreviation() == null
                    || !currentChr.getPrimaryIdentifier().endsWith("_random")
                    && !currentChr.getPrimaryIdentifier().equals("M")
                    && !currentChr.getOrganism().getAbbreviation().equals("MM")
                    && !currentChr.getOrganism().getAbbreviation().equals("MD")
                    && !currentChr.getOrganism().getAbbreviation().equals("CF")
                    && !currentChr.getOrganism().getAbbreviation().equals("RN")) {

                    writeChromosomeFasta(currentChr);

                    File gffFile = chromosomeGFFFile(currentChr);
                    if (gffWriter != null) {
                        gffWriter.close();
                    }
                    gffWriter = new PrintWriter(new FileWriter(gffFile));

                    List<String> synonymList = synonymMap.get(currentChr.getId());
                    List<Evidence> evidenceList = evidenceMap.get(currentChr.getId());

                    writeFeature(gffWriter, currentChr, currentChr, null,
                                 chromosomeFileNamePrefix(currentChr),
                                 "chromosome",
                                 "Chromosome", null, synonymList, evidenceList, currentChr.getId());

                    objectCounts = new HashMap<String, Integer>();
                    currentChrId = resultChrId;
                }
            }

            if (currentChr == null || synonymMap == null || evidenceMap == null
                || objectCounts == null) {
                throw new RuntimeException("Internal error - failed to set maps");
            }

            // process Transcripts but not tRNAs
            // we can't just check for MRNA because the Transcripts of Pseudogenes aren't MRNAs
            if (feature instanceof Transcript && !(feature instanceof NcRNA)) {
                seenTranscripts.put((Transcript) feature, loc);
            }

            String primaryIdentifier = feature.getPrimaryIdentifier();
            if (feature instanceof Exon
                // || feature instanceof FivePrimeUTR || feature instanceof ThreePrimeUTR
                ) {
                seenTranscriptParts.put(primaryIdentifier, loc);
            }

            if (currentChr.getOrganism().getAbbreviation() == null
                || !currentChr.getPrimaryIdentifier().endsWith("_random")
                && !currentChr.getPrimaryIdentifier().equals("M")
                && !currentChr.getOrganism().getAbbreviation().equals("MM")
                && !currentChr.getOrganism().getAbbreviation().equals("CF")
                && !currentChr.getOrganism().getAbbreviation().equals("MD")
                && !currentChr.getOrganism().getAbbreviation().equals("RN")) {

                String identifier = primaryIdentifier;

                String featureType = getFeatureName(feature);

                if (identifier == null) {
                    identifier = featureType + "_" + objectCounts.get(feature.getClass());
                }

                List<String> synonymList = synonymMap.get(feature.getId());
                List<Evidence> evidenceList = evidenceMap.get(feature.getId());

                Map<String, List<String>> extraAttributes = new HashMap<String, List<String>>();

                if (feature instanceof ChromosomeBand) {
                    ArrayList<String> indexList = new ArrayList<String>();
                    indexList.add(objectCounts.get(feature.getClass()).toString());
                    extraAttributes.put("Index", indexList);
                }

                writeFeature(gffWriter, currentChr, feature, loc,
                             identifier,
                             featureType.toLowerCase(), featureType, extraAttributes,
                             synonymList, evidenceList, feature.getId());
            }

            incrementCount(objectCounts, feature);
        }

        if (currentChr == null) {
            throw new RuntimeException("no chromosomes found");
        }

        // special case for t1dmine/stemcellmine
        if (!currentChr.getPrimaryIdentifier().endsWith("_random")
            && !currentChr.getPrimaryIdentifier().equals("M")
            && currentChr.getOrganism().getAbbreviation() != null
            && !currentChr.getOrganism().getAbbreviation().equals("MM")
            && !currentChr.getOrganism().getAbbreviation().equals("MD")
            && !currentChr.getOrganism().getAbbreviation().equals("RN")) {
            writeTranscriptsAndExons(gffWriter, currentChr, seenTranscripts,
                                     seenTranscriptParts, synonymMap, evidenceMap);
        }

        if (gffWriter != null) {
            gffWriter.close();
        }
    }

    private String getFeatureName(LocatedSequenceFeature feature) {
        Class<?> bioEntityClass = feature.getClass();
        Set<Class> classes = DynamicUtil.decomposeClass(bioEntityClass);

        StringBuffer nameBuffer = new StringBuffer();

        Iterator<Class> iter = classes.iterator();

        while (iter.hasNext()) {
            Class thisClass = iter.next();
            if (nameBuffer.length() > 0) {
                nameBuffer.append("_");
            } else {
                nameBuffer.append(TypeUtil.unqualifiedName(thisClass.getName()));
            }
        }

        return nameBuffer.toString();
    }

    private void writeTranscriptsAndExons(PrintWriter gffWriter, Chromosome chr,
                                          Map<Transcript, Location> seenTranscripts,
                                          Map<String, Location> seenTranscriptParts,
                                          Map<Integer, List<String>> synonymMap,
                                          Map<Integer, List<Evidence>> evidenceMap) {
        Iterator<Transcript> transcriptIter = seenTranscripts.keySet().iterator();
        while (transcriptIter.hasNext()) {
            // we can't just use MRNA here because the Transcripts of a pseudogene are Transcripts,
            // but aren't MRNAs
            Transcript transcript = transcriptIter.next();
            Gene gene = transcript.getGene();
            if (gene == null) {
                continue;
            }
            Location transcriptLocation = seenTranscripts.get(transcript);

            String transcriptFeatureType = "mRNA";

            Map<String, List<String>> geneNameAttributeMap = new HashMap<String, List<String>>();

            List<String> geneNameList = new ArrayList<String>();
            geneNameList.add(gene.getSecondaryIdentifier());
            geneNameAttributeMap.put("Gene", geneNameList);

            List<String> synonymList = synonymMap.get(transcript.getId());

            if (synonymList == null) {
                synonymList = new ArrayList<String>();
            }

            if (transcript instanceof MRNA) {
                // special case for CDS objects - display them as MRNA as GBrowse uses the CDS class
                // for displaying MRNAs
                Iterator<CDS> cdsIter = ((MRNA) transcript).getcDSs().iterator();
                while (cdsIter.hasNext()) {
                    CDS cds = cdsIter.next();
                    synonymList.add(makeIdString(cds.getId()));
                }
            }
            List<Evidence> evidenceList = evidenceMap.get(transcript.getId());

            writeFeature(gffWriter, chr, transcript, transcriptLocation,
                         transcript.getPrimaryIdentifier(),
                         transcriptFeatureType, "mRNA", geneNameAttributeMap, synonymList,
                         evidenceList, transcript.getId());

            Collection<Exon> exons = transcript.getExons();

            ProxyCollection exonsResults = (ProxyCollection) exons;

            // exon collections are small enough that optimisation just slows things down
            exonsResults.setNoOptimise();
            exonsResults.setNoExplain();

            Iterator<Exon> exonIter = exons.iterator();
            while (exonIter.hasNext()) {
                Exon exon = exonIter.next();
                Location exonLocation = seenTranscriptParts.get(exon.getPrimaryIdentifier());

                List<String> exonSynonymValues = synonymMap.get(exon.getId());
                List<Evidence> exonEvidence = evidenceMap.get(exon.getId());

                writeFeature(gffWriter, chr, exon, exonLocation, transcript.getPrimaryIdentifier(),
                             "CDS", "mRNA", null, exonSynonymValues, exonEvidence,
                             transcript.getId());
            }

            /*

            --- we need correct CDS locations before this can work properly
            --- curently we write out the Exons as CDSs so the exons overlap the CDS feature

            if (transcript instanceof MRNA) {
            MRNA mRNA = (MRNA) transcript;
            FivePrimeUTR fivePrimeUTR = mRNA.getFivePrimeUTR();
            if (fivePrimeUTR != null) {
                Location fivePrimeUTRLocation = (Location) seenTranscriptParts.get(fivePrimeUTR);

                List fivePrimeUTRSynonymValues = (List) synonymMap.get(fivePrimeUTR.getId());
                List fivePrimeUTREvidence = (List) evidenceMap.get(fivePrimeUTR.getId());

                writeFeature(gffWriter, chr, fivePrimeUTR, fivePrimeUTRLocation,
                             transcript.getIdentifier(), "5'-UTR", "mRNA", null,
                             fivePrimeUTRSynonymValues, fivePrimeUTREvidence, transcript.getId());
            }

            ThreePrimeUTR threePrimeUTR = mRNA.getThreePrimeUTR();
            if (threePrimeUTR != null) {
                Location threePrimeUTRLocation = (Location) seenTranscriptParts.get(threePrimeUTR);

                List threePrimeUTRSynonymValues = (List) synonymMap.get(threePrimeUTR.getId());
                List threePrimeUTREvidence = (List) evidenceMap.get(threePrimeUTR.getId());

                writeFeature(gffWriter, chr, threePrimeUTR, threePrimeUTRLocation,
                             transcript.getIdentifier(), "3'-UTR", "mRNA", null,
                             threePrimeUTRSynonymValues, threePrimeUTREvidence, transcript.getId());
            }
            }
            */
        }
    }

    private void incrementCount(Map<String, Integer> objectCounts, Object object) {
        if (objectCounts.containsKey(object.getClass())) {
            int oldCount = objectCounts.get(object.getClass()).intValue();
            objectCounts.put(object.getClass().toString(), oldCount + 1);
        } else {
            objectCounts.put(object.getClass().toString(), 1);
        }
    }

    /**
     * @param bioEntity the object to write
     * @param chromosomeLocation the location of the object on the chromosome
     * @param featureType the type (third output column) to be used when writing - null means create
     * the featureType automatically from the java class name on the object to write
     * @param idType the type tag to use when storing the ID in the attributes Map - null means use
     * the featureType
     * @param flyMineId
     * @param synonymValues a List of synonyms for this feature
     * @param evidenceList a List of evidence objects for this feature
     */
    private void writeFeature(PrintWriter gffWriter, Chromosome chr,
                              LocatedSequenceFeature bioEntity, Location chromosomeLocation,
                              String identifier,
                              String featureType, String idType,
                              Map<String, List<String>> extraAttributes,
                              List<String> synonymValues,
                              List<Evidence> evidenceList, Integer flyMineId) {

        StringBuffer lineBuffer = new StringBuffer();

        lineBuffer.append(chromosomeFileNamePrefix(chr)).append("\t");

        String source = ".";

        if (evidenceList != null) {
            Iterator<Evidence> evidenceIter = evidenceList.iterator();

            while (evidenceIter.hasNext()) {
                Evidence evidence = evidenceIter.next();
                if (evidence instanceof ComputationalResult) {
                    Analysis analysis = ((ComputationalResult) evidence).getAnalysis();
                    if (analysis instanceof ComputationalAnalysis) {
                        source = ((ComputationalAnalysis) analysis).getAlgorithm();
                    }
                }
            }
        }

        lineBuffer.append(source).append("\t");
        lineBuffer.append(featureType).append("\t");

        if (chromosomeLocation == null) {
            if (bioEntity == chr) {
                // special case for Chromosome location
                lineBuffer.append(1).append("\t").append(chr.getLength()).append("\t");
            } else {
                throw new RuntimeException("no chromomsome location for: " + bioEntity);
            }
        } else {
            lineBuffer.append(chromosomeLocation.getStart()).append("\t");
            lineBuffer.append(chromosomeLocation.getEnd()).append("\t");
        }

        lineBuffer.append(0).append("\t");

        if (chromosomeLocation == null) {
            lineBuffer.append(".");
        } else {
            if (chromosomeLocation.getStrand().equals("1")) {
                lineBuffer.append("+");
            } else {
                if (chromosomeLocation.getStrand().equals("-1")) {
                    lineBuffer.append("-");
                } else {
                    lineBuffer.append(".");
                }
            }
        }

        lineBuffer.append("\t");

        if (chromosomeLocation == null) {
            lineBuffer.append(".");
        } else {
            if (chromosomeLocation.getPhase() == null) {
                lineBuffer.append(".");
            } else {
                lineBuffer.append(chromosomeLocation.getPhase());
            }
        }

        lineBuffer.append("\t");

        Map<String, List<String>> attributes = new LinkedHashMap<String, List<String>>();

        List<String> identifiers = new ArrayList<String>();
        identifiers.add(identifier);

        attributes.put(idType, identifiers);

        List<String> flyMineIDs = new ArrayList<String>();
        flyMineIDs.add(makeIdString(flyMineId));

        attributes.put("FlyMineInternalID", new ArrayList<String>(flyMineIDs));
        List<String> allIds = new ArrayList<String>(flyMineIDs);

        if (synonymValues != null) {
            Iterator<String> synonymIter = synonymValues.iterator();
            while (synonymIter.hasNext()) {
                String thisSynonymValue = synonymIter.next();
                if (!allIds.contains(thisSynonymValue)) {
                    allIds.add(thisSynonymValue);
                }
            }
        }

        attributes.put("Alias", allIds);

        if (extraAttributes != null) {
            Iterator<String> extraAttributesIter = extraAttributes.keySet().iterator();

            while (extraAttributesIter.hasNext()) {
                String key = extraAttributesIter.next();
                attributes.put(key, extraAttributes.get(key));
            }
        }

        try {
            if (TypeUtil.isInstanceOf(bioEntity, "org.flymine.model.genomic.PCRProduct")) {
                Boolean fieldValue;
                try {
                    fieldValue = (Boolean) TypeUtil.getFieldValue(bioEntity, "promoter");
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("can't access 'promoter' field in: " + bioEntity);
                }
                List<String> promoterFlagList = new ArrayList<String>();
                promoterFlagList.add(fieldValue.toString());
                attributes.put("promoter", promoterFlagList);
            }
        } catch (ClassNotFoundException e) {
            // ignore - PCRProduct is not in the model
        }

        lineBuffer.append(stringifyAttributes(attributes));
        gffWriter.println(lineBuffer.toString());
    }

    private String makeIdString(Integer id) {
        return "FlyMineInternalID_" + id;
    }

    /**
     * Return a String representation of the attributes Map. Taken from BioJava's
     * SimpleGFFRecord.java
     * @param attMap the Map of attributes
     * @return a String representation of the attributes
     */
    static String stringifyAttributes(Map<String, List<String>> attMap) {
        StringBuffer sBuff = new StringBuffer();
        Iterator<String> ki = attMap.keySet().iterator();
        while (ki.hasNext()) {
            String key = ki.next();

            List<String> values = attMap.get(key);
            if (values.size() == 0) {
                sBuff.append(key);
                sBuff.append(";");
            } else {
                for (Iterator<String> vi = values.iterator(); vi.hasNext();) {
                    sBuff.append(key);
                    String value = vi.next();
                    sBuff.append(" \"" + value + "\"");
                    if (ki.hasNext() || vi.hasNext()) {
                        sBuff.append(";");

                    }
                    if (vi.hasNext()) {
                        sBuff.append(" ");
                    }
                }
            }
            if (ki.hasNext()) {
                sBuff.append(" ");
            }
        }
        return sBuff.toString();
    }

    /**
     * Make a Map from LocatedSequenceFeature ID to List of Synonym values (Strings) for
     * LocatedSequenceFeature objects located on the chromosome with the given ID.
     * @param os the ObjectStore to read from
     * @param chromosomeId the chromosome ID of the LocatedSequenceFeature objects to examine
     * @return a Map from id to synonym List
     * @throws ObjectStoreException
     */
    private Map<Integer, List<String>> makeSynonymMap(ObjectStore os,
                                                      Integer chromosomeId)
    throws ObjectStoreException {
        Query q = new Query();
        q.setDistinct(true);
        QueryClass qcEnt = new QueryClass(LocatedSequenceFeature.class);
        QueryField qfEnt = new QueryField(qcEnt, "id");
        q.addFrom(qcEnt);
        q.addToSelect(qfEnt);

        QueryClass qcSyn = new QueryClass(Synonym.class);
        QueryField qfSyn = new QueryField(qcSyn, "value");
        q.addFrom(qcSyn);
        q.addToSelect(qfSyn);

        QueryClass qcLoc = new QueryClass(Location.class);
        q.addFrom(qcLoc);

        QueryClass qcChr = new QueryClass(Chromosome.class);
        QueryField qfChr = new QueryField(qcChr, "id");
        q.addFrom(qcChr);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference col = new QueryCollectionReference(qcEnt, "synonyms");
        ContainsConstraint cc1 = new ContainsConstraint(col, ConstraintOp.CONTAINS, qcSyn);
        cs.addConstraint(cc1);

        QueryValue chrIdQueryValue = new QueryValue(chromosomeId);
        SimpleConstraint sc = new SimpleConstraint(qfChr, ConstraintOp.EQUALS, chrIdQueryValue);
        cs.addConstraint(sc);

        QueryObjectReference ref1 = new QueryObjectReference(qcLoc, "subject");
        ContainsConstraint cc2 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcEnt);
        cs.addConstraint(cc2);

        QueryObjectReference ref2 = new QueryObjectReference(qcLoc, "object");
        ContainsConstraint cc3 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcChr);
        cs.addConstraint(cc3);

        q.setConstraint(cs);


        Set<QueryNode> indexesToCreate = new HashSet<QueryNode>();
        indexesToCreate.add(qfEnt);
        indexesToCreate.add(qfSyn);
        ((ObjectStoreInterMineImpl) os).precompute(q, indexesToCreate,
                                                   PostProcessOperationsTask.PRECOMPUTE_CATEGORY);
        Results res = os.execute(q);
        res.setBatchSize(50000);

        Iterator<ResultsRow> resIter = res.iterator();

        Map<Integer, List<String>> returnMap = new HashMap<Integer, List<String>>();

        while (resIter.hasNext()) {
            ResultsRow rr = resIter.next();
            Integer bioEntityId = (Integer) rr.get(0);
            String synonymValue = (String) rr.get(1);

            List<String> synonymValues = returnMap.get(bioEntityId);

            if (synonymValues == null) {
                synonymValues = new ArrayList<String>();
                returnMap.put(bioEntityId, synonymValues);
            }

            synonymValues.add(synonymValue);
        }

        return returnMap;
    }

    /**
     * Make a Map from LocatedSequenceFeature ID to List of Evidence objects for
     * LocatedSequenceFeature objects located on the chromosome with the given ID.
     * @param os the ObjectStore to read from
     * @param chromosomeId the chromosome ID of the LocatedSequenceFeature objects to examine
     * @return a Map from id to Evidence List
     * @throws ObjectStoreException
     */
    private Map<Integer, List<Evidence>> makeEvidenceMap(ObjectStore os, Integer chromosomeId)
        throws ObjectStoreException {
        Query q = new Query();
        q.setDistinct(true);
        QueryClass qcEnt = new QueryClass(LocatedSequenceFeature.class);
        QueryField qfEnt = new QueryField(qcEnt, "id");
        q.addFrom(qcEnt);
        q.addToSelect(qfEnt);

        QueryClass qcEvidence = new QueryClass(Evidence.class);
        q.addFrom(qcEvidence);
        q.addToSelect(qcEvidence);

        QueryClass qcLoc = new QueryClass(Location.class);
        q.addFrom(qcLoc);

        QueryClass qcChr = new QueryClass(Chromosome.class);
        QueryField qfChr = new QueryField(qcChr, "id");
        q.addFrom(qcChr);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference col = new QueryCollectionReference(qcEnt, "evidence");
        ContainsConstraint cc1 = new ContainsConstraint(col, ConstraintOp.CONTAINS, qcEvidence);
        cs.addConstraint(cc1);

        QueryValue chrIdQueryValue = new QueryValue(chromosomeId);
        SimpleConstraint sc = new SimpleConstraint(qfChr, ConstraintOp.EQUALS, chrIdQueryValue);
        cs.addConstraint(sc);

        QueryObjectReference ref1 = new QueryObjectReference(qcLoc, "subject");
        ContainsConstraint cc2 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcEnt);
        cs.addConstraint(cc2);

        QueryObjectReference ref2 = new QueryObjectReference(qcLoc, "object");
        ContainsConstraint cc3 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcChr);
        cs.addConstraint(cc3);

        q.setConstraint(cs);
        Set<QueryNode> indexesToCreate = new HashSet<QueryNode>();
        indexesToCreate.add(qfEnt);
        ((ObjectStoreInterMineImpl) os).precompute(q, indexesToCreate,
                                                   PostProcessOperationsTask.PRECOMPUTE_CATEGORY);

        Results res = os.execute(q);

        res.setBatchSize(50000);

        Iterator<ResultsRow> resIter = res.iterator();

        Map<Integer, List<Evidence>> returnMap = new HashMap<Integer, List<Evidence>>();

        while (resIter.hasNext()) {
            ResultsRow rr = resIter.next();
            Integer bioEntityId = (Integer) rr.get(0);
            Evidence evidence = (Evidence) rr.get(1);

            List<Evidence> idEvidence = returnMap.get(bioEntityId);

            if (idEvidence == null) {
                idEvidence = new ArrayList<Evidence>();
                returnMap.put(bioEntityId, idEvidence);
            }

            idEvidence.add(evidence);
        }

        return returnMap;
    }

    private void writeChromosomeFasta(Chromosome chr)
        throws IOException, IllegalArgumentException {

        Sequence chromosomeSequence = chr.getSequence();

        if (chromosomeSequence == null) {
            LOG.warn("cannot find any sequence for chromosome " + chr.getPrimaryIdentifier());
        } else {
            String residues = chromosomeSequence.getResidues();

            if (residues == null) {
                LOG.warn("cannot find any sequence residues for chromosome "
                         + chr.getPrimaryIdentifier());
            } else {
                FileOutputStream fileStream =
                    new FileOutputStream(chromosomeFastaFile(chr));

                PrintStream printStream = new PrintStream(fileStream);
                printStream.println(">" + chromosomeFileNamePrefix(chr));

                // code from BioJava's FastaFormat class:
                int length = residues.length();
                for (int pos = 0; pos < length; pos += 60) {
                    int end = Math.min(pos + 60, length);
                    printStream.println(residues.substring(pos, end));
                }

                printStream.close();
                fileStream.close();
            }
        }
    }

    private File chromosomeFastaFile(Chromosome chr) {
        return new File(destinationDirectory, chromosomeFileNamePrefix(chr) + ".fa");
    }

    private File chromosomeGFFFile(Chromosome chr) {
        return new File(destinationDirectory, chromosomeFileNamePrefix(chr) + ".gff");
    }

    private String chromosomeFileNamePrefix(Chromosome chr) {
        String orgPrefix;
        if (chr.getOrganism().getGenus() == null) {
            orgPrefix = "Unknown_organism";
        } else {
            orgPrefix = chr.getOrganism().getGenus() + "_"
            + chr.getOrganism().getSpecies().replaceAll(" ", "_");
        }
        return orgPrefix + "_chr_" + chr.getPrimaryIdentifier();
    }
}
