package org.intermine.bio.gbrowse;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.bio.util.BioQueries;
import org.intermine.bio.util.Constants;
import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Sequence;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.model.bio.Synonym;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.objectstore.query.ClobAccess;
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
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

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
    public void execute() {
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
            throw new BuildException(e);
        }
    }

    private static final Class<SequenceFeature> LOCATED_SEQUENCE_FEATURE_CLASS =
        SequenceFeature.class;

    /**
     * Create a GFF and FASTA files for the objects in the given ObjectStore, suitable for reading
     * by GBrowse.
     * @param os the ObjectStore to read from
     * @throws ObjectStoreException if the is a problem with the ObjectStore
     * @throws IOException if there is a problem while writing
     */
    void writeGFF(ObjectStore os)
        throws ObjectStoreException, IOException {

        Model model = os.getModel();

        Results results =
            BioQueries.findLocationAndObjects(os, Chromosome.class,
                    LOCATED_SEQUENCE_FEATURE_CLASS, false, true, false, 2000);

        @SuppressWarnings({ "unchecked", "rawtypes" })
        Iterator<ResultsRow> resIter = (Iterator) results.iterator();

        PrintWriter gffWriter = null;

        // a Map of object classes to counts
        Map<String, Integer> objectCounts = null;

        // Map from Transcript to Location (on Chromosome)
        Map<SequenceFeature, Location> seenTranscripts = new HashMap<SequenceFeature, Location>();
        // Map from exon primary identifier to Location (on Chromosome)
        Map<String, Location> seenTranscriptParts = new HashMap<String, Location>();

        // the last Chromosome seen
        Integer currentChrId = null;
        Chromosome currentChr = null;

        Map<Integer, List<String>> synonymMap = null;

        while (resIter.hasNext()) {
            ResultsRow<?> rr = resIter.next();
            Integer resultChrId = (Integer) rr.get(0);
            SequenceFeature feature = (SequenceFeature) rr.get(1);
            Location loc = (Location) rr.get(2);

            // TODO XXX FIXME - see #628
            if (isInstance(model, feature, "ChromosomeBand")) {
                continue;
            }

            try {
                if (TypeUtil.isInstanceOf(feature,
                                          "org.intermine.model.bio.ChromosomalDeletion")) {
                    try {
                        if (feature.getFieldValue("available") != Boolean.TRUE) {
                            // write only the available deletions because there are too many
                            // ChromosomalDeletions for GBrowse to work well
                            continue;
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("can't access 'available' field in: " + feature);
                    }
                }
            } catch (ClassNotFoundException e) {
               // ignore - ChromosomalDeletion is not in the model
            }

            if (isInstance(model, feature, "CDS")) {
                // ignore for now as it interferes with the CDS GFF records created by
                // writeTranscriptsAndExons() for use by the processed_transcript
                // aggregator
                continue;
            }

            if (currentChrId == null || !currentChrId.equals(resultChrId)) {
                if (currentChrId != null) {
                    writeTranscriptsAndExons(model, gffWriter, currentChr, seenTranscripts,
                                             seenTranscriptParts, synonymMap);
                    seenTranscripts = new HashMap<SequenceFeature, Location>();
                    seenTranscriptParts = new HashMap<String, Location>();
                }

                synonymMap = makeSynonymMap(os, resultChrId);
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

                if (!currentChr.getPrimaryIdentifier().endsWith("_random")) {

                    writeChromosomeFasta(currentChr);

                    File gffFile = chromosomeGFFFile(currentChr);
                    if (gffWriter != null) {
                        gffWriter.close();
                    }
                    gffWriter = new PrintWriter(new FileWriter(gffFile));

                    List<String> synonymList = synonymMap.get(currentChr.getId());

                    writeFeature(gffWriter, currentChr, currentChr, null,
                                 chromosomeFileNamePrefix(currentChr),
                                 "chromosome",
                                 "Chromosome", null, synonymList, currentChr.getId());

                    objectCounts = new HashMap<String, Integer>();
                    currentChrId = resultChrId;
                }
            }

            if (currentChr == null || synonymMap == null || objectCounts == null) {
                throw new RuntimeException("Internal error - failed to set maps");
            }

            // process Transcripts but not tRNAs
            // we can't just check for MRNA because the Transcripts of Pseudogenes aren't MRNAs
            if (isInstance(model, feature, "Transcript")) {
                if (!isInstance(model, feature, "NcRNA")) {
                    seenTranscripts.put(feature, loc);
                }
            }

            String primaryIdentifier = feature.getPrimaryIdentifier();
            if (isInstance(model, feature, "Exon")) {
                seenTranscriptParts.put(primaryIdentifier, loc);
            }

            String identifier = primaryIdentifier;

            String featureType = getFeatureName(feature);

            if (identifier == null) {
                identifier = featureType + "_" + objectCounts.get(feature.getClass());
            }

            List<String> synonymList = synonymMap.get(feature.getId());
            Map<String, List<String>> extraAttributes = new HashMap<String, List<String>>();

            writeFeature(gffWriter, currentChr, feature, loc, identifier,
                    featureType.toLowerCase(), featureType, extraAttributes,
                    synonymList, feature.getId());

            incrementCount(objectCounts, feature);
        }

        if (currentChr == null) { // case of returning no results
            throw new RuntimeException("no chromosomes found");
        }

        writeTranscriptsAndExons(model, gffWriter, currentChr, seenTranscripts, seenTranscriptParts,
                synonymMap);

        if (gffWriter != null) {
            gffWriter.close();
        }
    }

    private String getFeatureName(SequenceFeature feature) {
        Class<?> bioEntityClass = feature.getClass();
        Set<Class<?>> classes = DynamicUtil.decomposeClass(bioEntityClass);

        StringBuffer nameBuffer = new StringBuffer();

        for (Class<?> thisClass : classes) {
            if (nameBuffer.length() > 0) {
                nameBuffer.append("_");
            }
            nameBuffer.append(TypeUtil.unqualifiedName(thisClass.getName()));
        }

        return nameBuffer.toString();
    }

    private void writeTranscriptsAndExons(Model model, PrintWriter gffWriter, Chromosome chr,
                                          Map<SequenceFeature, Location> seenTranscripts,
                                          Map<String, Location> seenTranscriptParts,
                                          Map<Integer, List<String>> synonymMap) {
        Iterator<SequenceFeature> transcriptIter = seenTranscripts.keySet().iterator();
        while (transcriptIter.hasNext()) {
            // we can't just use MRNA here because the Transcripts of a pseudogene are Transcripts,
            // but aren't MRNAs
            SequenceFeature transcript = transcriptIter.next();

            Gene gene = null;
            try {
                gene = (Gene) transcript.getFieldValue("gene");
            } catch (IllegalAccessException e) {
                // there is not gene
            }
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

            if (isInstance(model, transcript, "MRNA")) {
                // special case for CDS objects - display them as MRNA as GBrowse uses the CDS class
                // for displaying MRNAs
                try {
                    @SuppressWarnings("unchecked") Set<InterMineObject> cdss =
                        (Set<InterMineObject>) transcript.getFieldValue("CDSs");
                    for (InterMineObject cds : cdss) {
                        synonymList.add(makeIdString(cds.getId()));
                    }
                } catch (IllegalAccessException e) {
                    // no nothing, there was no collection of CDSs
                }
            }

            writeFeature(gffWriter, chr, transcript, transcriptLocation,
                         transcript.getPrimaryIdentifier(),
                         transcriptFeatureType, "mRNA", geneNameAttributeMap, synonymList,
                         transcript.getId());

            try {
                Collection<SequenceFeature> exons =
                    (Collection<SequenceFeature>) transcript.getFieldValue("exons");

                ProxyCollection<SequenceFeature> exonsResults =
                    (ProxyCollection<SequenceFeature>) exons;

                // exon collections are small enough that optimisation just slows things down
                exonsResults.setNoOptimise();
                exonsResults.setNoExplain();

                Iterator<SequenceFeature> exonIter = exons.iterator();
                while (exonIter.hasNext()) {
                    SequenceFeature exon = exonIter.next();
                    Location exonLocation = seenTranscriptParts.get(exon.getPrimaryIdentifier());

                    List<String> exonSynonymValues = synonymMap.get(exon.getId());

                    writeFeature(gffWriter, chr, exon, exonLocation,
                            transcript.getPrimaryIdentifier(), "CDS", "mRNA", null,
                            exonSynonymValues, transcript.getId());
                }
            } catch (IllegalAccessException e) {
                // there was no exons collection
            }
        }
    }

    private void incrementCount(Map<String, Integer> objectCounts, Object object) {
        if (objectCounts.containsKey(object.getClass())) {
            int oldCount = objectCounts.get(object.getClass()).intValue();
            objectCounts.put(object.getClass().toString(), new Integer(oldCount + 1));
        } else {
            objectCounts.put(object.getClass().toString(), new Integer(1));
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
    private void writeFeature(PrintWriter gffWriter, Chromosome chr, SequenceFeature bioEntity,
            Location chromosomeLocation, String identifier, String featureType, String idType,
            Map<String, List<String>> extraAttributes, List<String> synonymValues,
            Integer flyMineId) {

        StringBuffer lineBuffer = new StringBuffer();

        lineBuffer.append(chromosomeFileNamePrefix(chr)).append("\t");

        String source = ".";

        lineBuffer.append(source).append("\t");
        lineBuffer.append(featureType).append("\t");

        if (chromosomeLocation == null) {
            if (bioEntity == chr) {
                // special case for Chromosome location
                lineBuffer.append(1).append("\t").append(chr.getLength()).append("\t");
            } else {
                LOG.error("No valid chromomsome location found, not writing to GFF: " + bioEntity);
            }
        } else {
            lineBuffer.append(chromosomeLocation.getStart()).append("\t");
            lineBuffer.append(chromosomeLocation.getEnd()).append("\t");
        }

        lineBuffer.append(0).append("\t");

        if (chromosomeLocation == null) {
            lineBuffer.append(".");
        } else {
            if ("1".equals(chromosomeLocation.getStrand())) {
                lineBuffer.append("+");
            } else {
                if ("-1".equals(chromosomeLocation.getStrand())) {
                    lineBuffer.append("-");
                } else {
                    lineBuffer.append(".");
                }
            }
        }

        lineBuffer.append("\t");
        lineBuffer.append(".");
        lineBuffer.append("\t");

        Map<String, List<String>> attributes = new LinkedHashMap<String, List<String>>();

        List<String> identifiers = new ArrayList<String>();
        identifiers.add(identifier);

        attributes.put(idType, identifiers);

        String secondaryIdentifier = bioEntity.getSecondaryIdentifier();
        if (secondaryIdentifier != null) {
            List<String> notes = new ArrayList<String>();
            notes.add(secondaryIdentifier);
            attributes.put("Note", notes);
        }

        List<String> allIds = new ArrayList<String>();

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
            if (TypeUtil.isInstanceOf(bioEntity, "org.intermine.model.bio.PCRProduct")) {
                Boolean fieldValue;
                try {
                    fieldValue = (Boolean) bioEntity.getFieldValue("promoter");
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
     * Make a Map from SequenceFeature ID to List of Synonym values (Strings) for
     * SequenceFeature objects located on the chromosome with the given ID.
     * @param os the ObjectStore to read from
     * @param chromosomeId the chromosome ID of the SequenceFeature objects to examine
     * @return a Map from id to synonym List
     * @throws ObjectStoreException
     */
    private Map<Integer, List<String>> makeSynonymMap(ObjectStore os, Integer chromosomeId)
        throws ObjectStoreException {
        Query q = new Query();
        q.setDistinct(true);
        QueryClass qcEnt = new QueryClass(SequenceFeature.class);
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

        QueryObjectReference ref1 = new QueryObjectReference(qcLoc, "feature");
        ContainsConstraint cc2 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcEnt);
        cs.addConstraint(cc2);

        QueryObjectReference ref2 = new QueryObjectReference(qcLoc, "locatedOn");
        ContainsConstraint cc3 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qcChr);
        cs.addConstraint(cc3);

        q.setConstraint(cs);


        Set<QueryNode> indexesToCreate = new HashSet<QueryNode>();
        indexesToCreate.add(qfEnt);
        indexesToCreate.add(qfSyn);
        ((ObjectStoreInterMineImpl) os).precompute(q, indexesToCreate,
                                                   Constants.PRECOMPUTE_CATEGORY);
        Results res = os.execute(q, 50000, true, true, true);

        @SuppressWarnings({ "unchecked", "rawtypes" })
        Iterator<ResultsRow> resIter = (Iterator) res.iterator();

        Map<Integer, List<String>> returnMap = new HashMap<Integer, List<String>>();

        while (resIter.hasNext()) {
            ResultsRow<?> rr = resIter.next();
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

    private void writeChromosomeFasta(Chromosome chr) throws IOException {
        Sequence chromosomeSequence = chr.getSequence();

        if (chromosomeSequence == null) {
            LOG.warn("cannot find any sequence for chromosome " + chr.getPrimaryIdentifier());
        } else {
            ClobAccess residues = chromosomeSequence.getResidues();

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
                    printStream.println(residues.subSequence(pos, end).toString());
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

    private boolean isInstance(Model model, InterMineObject obj, String clsName) {
        if (model.hasClassDescriptor(clsName)) {
            Class<? extends FastPathObject> cls = model.getClassDescriptorByName(clsName).getType();
            if (DynamicUtil.isInstance(obj, cls)) {
                return true;
            }
        }
        return false;
    }
}
