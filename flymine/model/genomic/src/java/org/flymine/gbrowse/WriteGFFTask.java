package org.flymine.gbrowse;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import org.biojava.bio.program.gff.SimpleGFFRecord;
import org.biojava.bio.symbol.IllegalSymbolException;

import org.intermine.objectstore.query.*;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.TypeUtil;
import org.intermine.util.DynamicUtil;
import org.intermine.objectstore.proxy.ProxyCollection;

import org.flymine.postprocess.PostProcessUtil;
import org.flymine.model.genomic.*;

import org.apache.log4j.Logger;

/**
 * A Task for creating GFF and FASTA files for use by GBrowse.  Only those features that are
 * located on a Chromosome are written.
 * @author Kim Rutherford
 */

public class WriteGFFTask extends Task
{
    protected static final Logger LOG = Logger.getLogger(WriteGFFTask.class);
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
     * @see Task#execute
     */
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
            writeGFF(os, destinationDirectory);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new BuildException(e);
        }
    }

    /**
     * Create a GFF and FASTA files for the objects in the given ObjectStore, suitable for reading
     * by GBrowse.
     * @param os the ObjectStore to read from
     * @param destinationDirectory the directory to write the GFF and FASTA files into
     * @throws IllegalSymbolException if any of the residues in a LocatedSequenceFeature can't be
     * turned into DNA symbols.
     * @throws ObjectStoreException if the is a problem with the ObjectStore
     * @throws IOException if there is a problem while writing
     */
    void writeGFF(ObjectStore os, File destinationDirectory)
        throws ObjectStoreException, IOException, IllegalSymbolException {
        Results results =
            PostProcessUtil.findLocations(os, Chromosome.class, BioEntity.class, false);

        results.setBatchSize(2000);

        Iterator resIter = results.iterator();

        PrintWriter gffWriter = null;

        // a Map of object classes to counts
        Map objectCounts = null;

        // Map from Transcript to Location (on Chromosome)
        Map seenTranscripts = new HashMap();
        // Map from exon to Location (on Chromosome)
        Map seenTranscriptParts = new HashMap();

        // the last Chromosome seen
        Integer currentChrId = null;
        Chromosome currentChr = null;

        Map synonymMap = null;

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Integer resultChrId = (Integer) rr.get(0);
            BioEntity feature = (BioEntity) rr.get(1);
            Location loc = (Location) rr.get(2);

            if (currentChrId == null || !currentChrId.equals(resultChrId)) {
                synonymMap = makeSynonymMap(os, resultChrId);

                if (currentChrId != null) {
                    writeTranscriptsAndExons(gffWriter, currentChr, seenTranscripts,
                                             seenTranscriptParts, synonymMap);
                    seenTranscripts = new HashMap();
                    seenTranscriptParts = new HashMap();
                }

                currentChr = (Chromosome) os.getObjectById(resultChrId);

                if (!currentChr.getIdentifier().endsWith("_random")
                    && !currentChr.getIdentifier().equals("M")) {
                    writeChromosomeFasta(destinationDirectory, currentChr);

                    File gffFile = chromosomeGFFFile(destinationDirectory, currentChr);
                    if (gffWriter != null) {
                        gffWriter.close();
                    }
                    gffWriter = new PrintWriter(new FileWriter(gffFile));

                    writeFeature(gffWriter, currentChr, currentChr, null,
                                 chromosomeFileNamePrefix(currentChr),
                                 new Integer(0), "chromosome",
                                 "Chromosome", null, synonymMap, feature.getId());

                    objectCounts = new HashMap();
                    currentChrId = resultChrId;
                }
            }

            if (feature instanceof Transcript && !(feature instanceof NcRNA)) {
                Transcript transcript = (Transcript) feature;

                if (transcript.getGene() != null) {
                    // process Transcripts but not tRNAs
                    seenTranscripts.put(transcript, loc);
                    continue;
                }
            }

            if (feature instanceof Exon) {
                seenTranscriptParts.put(feature, loc);
            }

            if (!currentChr.getIdentifier().endsWith("_random")
                    && !currentChr.getIdentifier().equals("M")) {
                String identifier = feature.getIdentifier();

                String featureType = getFeatureName(feature);

                if (identifier == null) {
                    identifier = featureType + "_" + objectCounts.get(feature.getClass());
                }

                writeFeature(gffWriter, currentChr, feature, loc,
                             identifier, null,
                             featureType.toLowerCase(), featureType, null,
                             synonymMap, feature.getId());
            }

            incrementCount(objectCounts, feature);
        }

        if (!currentChr.getIdentifier().endsWith("_random")
                    && !currentChr.getIdentifier().equals("M")) {
            writeTranscriptsAndExons(gffWriter, currentChr, seenTranscripts,
                                     seenTranscriptParts, synonymMap);
        }

        gffWriter.close();
    }

    private String getFeatureName(BioEntity feature) {
        Class bioEntityClass = feature.getClass();
        Set classes = DynamicUtil.decomposeClass(bioEntityClass);

        StringBuffer nameBuffer = new StringBuffer();

        Iterator iter = classes.iterator();

        while (iter.hasNext()) {
            Class thisClass = (Class) iter.next();
            if (nameBuffer.length() > 0) {
                nameBuffer.append("_");
            } else {
                nameBuffer.append(TypeUtil.unqualifiedName(thisClass.getName()));
            }
        }

        return nameBuffer.toString();
    }

    private void writeTranscriptsAndExons(PrintWriter gffWriter, Chromosome chr,
                                          Map seenTranscripts, Map seenTranscriptParts,
                                          Map synonymMap)
        throws IOException {
        Iterator transcriptIter = seenTranscripts.keySet().iterator();
        while (transcriptIter.hasNext()) {
            Transcript transcript = (Transcript) transcriptIter.next();
            Gene gene = transcript.getGene();
            Location transcriptLocation = (Location) seenTranscripts.get(transcript);

            String transcriptFeatureType = null;

            if (gene instanceof Pseudogene) {
                transcriptFeatureType = "pseudogene_mRNA";
            } else {
                transcriptFeatureType = "gene_mRNA";
            }

            Map geneNameAttributeMap = new HashMap();

            ArrayList geneNameList = new ArrayList();
            geneNameList.add(gene.getIdentifier());
            geneNameAttributeMap.put("Gene", geneNameList);

            writeFeature(gffWriter, chr, transcript, transcriptLocation, transcript.getIdentifier(),
                         null,
                         transcriptFeatureType, "mRNA", geneNameAttributeMap, synonymMap,
                         transcript.getId());

            Collection exons = transcript.getExons();

            ProxyCollection exonsResults = (ProxyCollection) exons;

            // exon collections are small enough that optimisation just slows things down
            exonsResults.setNoOptimise();
            exonsResults.setNoExplain();

            String exonFeatureType = null;
            if (gene instanceof Pseudogene) {
                exonFeatureType = "pseudogene_CDS";
            } else {
                exonFeatureType = "gene_CDS";
            }

            Iterator exonIter = exons.iterator();
            while (exonIter.hasNext()) {
                Exon exon = (Exon) exonIter.next();
                Location exonLocation = (Location) seenTranscriptParts.get(exon);

                writeFeature(gffWriter, chr, exon, exonLocation, transcript.getIdentifier(), null,
                             exonFeatureType, "mRNA", null, synonymMap, transcript.getId());
            }
        }
    }

    private void incrementCount(Map objectCounts, Object object) {
        if (objectCounts.containsKey(object.getClass())) {
            int oldCount = ((Integer) objectCounts.get(object.getClass())).intValue();
            objectCounts.put(object.getClass(), new Integer(oldCount + 1));
        } else {
            objectCounts.put(object.getClass(), new Integer(1));
        }
    }

    private static final String FLYMINE_STRING = "flymine";

    /**
     * @param bioEntity the obejct to write
     * @param location the location of the object on the chromosome
     * @param index the index of this object (first object written is 0, then 1, ...)
     * @param featureType the type (third output column) to be used when writing - null means create
     * the featureType automatically from the java class name on the object to write
     * @param idType the type tag to use when storing the ID in the attributes Map - null means use
     * the featureType
     * @param flyMineId
     */
    private void writeFeature(PrintWriter gffWriter, Chromosome chr,
                              BioEntity bioEntity, Location location, String identifier,
                              Integer index,
                              String featureType, String idType, Map extraAttributes,
                              Map synonymMap, Integer flyMineId)
        throws IOException {

        if (index == null) {
            index = new Integer(0);
        }

        StringBuffer lineBuffer = new StringBuffer();

        lineBuffer.append(chromosomeFileNamePrefix(chr)).append("\t");
        lineBuffer.append(FLYMINE_STRING).append("\t");

        lineBuffer.append(featureType).append("\t");

        if (location == null && bioEntity == chr) {
            // special case for Chromosome location
            lineBuffer.append(1).append("\t").append(chr.getLength()).append("\t");
        } else {
            lineBuffer.append(location.getStart()).append("\t");
            lineBuffer.append(location.getEnd()).append("\t");
        }

        lineBuffer.append(0).append("\t");
        int strand;

        if (location == null) {
            lineBuffer.append(".");
        } else {
            if (location.getStrand().intValue() == 1) {
                lineBuffer.append("+");
            } else {
                if (location.getStrand().intValue() == -1) {
                    lineBuffer.append("-");
                } else {
                    lineBuffer.append(".");
                }
            }
        }

        lineBuffer.append("\t");

        if (location == null) {
            lineBuffer.append(".");
        } else {
            if (location.getPhase() == null) {
                lineBuffer.append(".");
            } else {
                lineBuffer.append(location.getPhase());
            }
        }

        lineBuffer.append("\t");

        Map attributes = new LinkedHashMap();

        List identifiers = new ArrayList();
        identifiers.add(identifier);

        attributes.put(idType, identifiers);

        ArrayList flyMineIDs = new ArrayList();
        flyMineIDs.add("FlyMineInternalID_" + flyMineId);

        attributes.put("FlyMineInternalID", flyMineIDs.clone());
        List allIds = (List) flyMineIDs.clone();

//         List synonymValues = (List) synonymMap.get(bioEntity.getId());

//         if (synonymValues == null) {
//             LOG.warn("cannot find any synonyms for: " + bioEntity.getId() + " identifier: "
//                      + bioEntity.getIdentifier());
//         } else {
//             Iterator synonymIter = synonymValues.iterator();
//             while (synonymIter.hasNext()) {
//                 String thisSynonymValue = (String) synonymIter.next();
//                 if (!allIds.contains(thisSynonymValue)) {
//                     allIds.add(thisSynonymValue);
//                 }
//             }
//         }

        attributes.put("Alias", allIds);

        if (extraAttributes != null) {
            Iterator extraAttributesIter = extraAttributes.keySet().iterator();

            while (extraAttributesIter.hasNext()) {
                String key = (String) extraAttributesIter.next();
                attributes.put(key, extraAttributes.get(key));
            }
        }

        if (bioEntity instanceof ChromosomeBand) {
            ArrayList indexList = new ArrayList();
            indexList.add(index.toString());
            attributes.put("Index", indexList);
        }

        if (bioEntity instanceof PCRProduct) {
            ArrayList promoterFlagList = new ArrayList();
            promoterFlagList.add(((PCRProduct) bioEntity).getPromoter().toString());
            attributes.put("promoter", promoterFlagList);
        }

        lineBuffer.append(SimpleGFFRecord.stringifyAttributes(attributes));
        gffWriter.println(lineBuffer.toString());
    }

    /**
     * Make a Map from BioEntity ID to List of Synonym values (Strings) for BioEntity objects
     * located on the chromosome with the given ID.
     * @param os the ObjectStore to read from
     * @param chromosomeId the chromosome ID of the BioEntity objects to examine
     * @return
     */
    private Map makeSynonymMap(ObjectStore os, Integer chromosomeId) {
        Query q = new Query();
        q.setDistinct(true);
        QueryClass qcEnt = new QueryClass(BioEntity.class);
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

        Results res = new Results(q, os, os.getSequence());

        res.setBatchSize(50000);

        Iterator resIter = res.iterator();

        Map returnMap = new HashMap();

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Integer bioEntityId = (Integer) rr.get(0);
            String synonymValue = (String) rr.get(1);

            List synonymValues = (List) returnMap.get(bioEntityId);

            if (synonymValues == null) {
                synonymValues = new ArrayList();
                returnMap.put(bioEntityId, synonymValues);
            }

            synonymValues.add(synonymValue);
        }

        return returnMap;
    }

    private void writeChromosomeFasta(File destinationDirectory, Chromosome chr)
        throws IOException, IllegalArgumentException {

        Sequence chromosomeSequence = chr.getSequence();

        FileOutputStream fileStream =
            new FileOutputStream(chromosomeFastaFile(destinationDirectory, chr));

        PrintStream printStream = new PrintStream(fileStream);
        printStream.println(">" + chromosomeFileNamePrefix(chr));

        String residues = chromosomeSequence.getResidues();

        // code from BioJava's FastaFormat class:
        int length = residues.length();
        for (int pos = 0; pos < length; pos += 60) {
            int end = Math.min(pos + 60, length);
            printStream.println(residues.substring(pos, end));
        }

        printStream.close();
        fileStream.close();
    }

    private File chromosomeFastaFile(File destinationDirectory, Chromosome chr) {
        return new File(destinationDirectory, chromosomeFileNamePrefix(chr) + ".fa");
    }

    private File chromosomeGFFFile(File destinationDirectory, Chromosome chr) {
        return new File(destinationDirectory, chromosomeFileNamePrefix(chr) + ".gff");
    }

    private String chromosomeFileNamePrefix(Chromosome chr) {
        return chr.getOrganism().getGenus() + "_" + chr.getOrganism().getSpecies()
            + "_chr_" + chr.getIdentifier();

    }
}
