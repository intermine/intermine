package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.bio.io.gff3.GFF3Parser;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.TypeUtil;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Consequence;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.SequenceAlteration;
import org.intermine.model.bio.Transcript;

import org.intermine.model.bio.SNV;
import org.intermine.model.bio.Deletion;
import org.intermine.model.bio.TandemRepeat;
import org.intermine.model.bio.Insertion;
import org.intermine.model.bio.Substitution;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.task.FileDirectDataLoaderTask;

/**
 * Reworked version of GFF converter for GVF files
 *
 * @author Julie Sullivan
 */
public class EnsembSnpLoaderTask extends FileDirectDataLoaderTask
{
    private int storeCount = 0;

    private static final String DATASET_TITLE = "Ensembl SNP";
    private static final String DATA_SOURCE_NAME = "Ensembl";
    private static final String TAXON_ID = "9606";
    private Organism org = null;
    private DataSet dataset = null;
    private DataSource datasource = null;
    private Map<String, Transcript> transcripts = new HashMap<String, Transcript>();
    private Map<String, Chromosome> chromosomes = new HashMap<String, Chromosome>();
    private static final Logger LOG = Logger.getLogger(EnsembSnpLoaderTask.class);
    //Set this if we want to do some testing...
    private File[] files = null;
    private static final String NAMESPACE = "org.intermine.model.bio";
    /**
     * Process and load the SNP file.
     */
    @Override
    public void process() {
        long start = System.currentTimeMillis();
        try {
            storeCount++;
            super.process();
            getIntegrationWriter().commitTransaction();
            getIntegrationWriter().beginTransaction();
        } catch (ObjectStoreException e) {
            throw new BuildException("failed to store object", e);
        }
        long now = System.currentTimeMillis();
        LOG.info("Finished dataloading " + storeCount + " objects at " + ((60000L * storeCount)
                    / (now - start)) + " objects per minute (" + (now - start)
                + " ms total) for source " + sourceName);
    }

    /**
     * @throws BuildException if an ObjectStore method fails
     */
    @Override
    public void execute() {
        if (files != null) {
            // setFiles() is used only for testing
            for (int i = 0; i < files.length; i++) {
                processFile(files[i]);
            }
        } else {
            // this will call processFile() for each file
            super.execute();
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    public void processFile(File file) {

        FileReader fileReader;
        try {
            fileReader = new FileReader(file);
            BufferedReader reader = new BufferedReader(fileReader);

            for (Iterator<?> i = GFF3Parser.parse(reader); i.hasNext();) {
                GFF3Record record = (GFF3Record) i.next();
                processRecord(record);
            }
        } catch (FileNotFoundException e) {
            throw new BuildException("problem reading file - file not found: " + file, e);
        } catch (IOException e) {
            throw new BuildException("error while closing FileReader for: " + file, e);
        } catch (ObjectStoreException e) {
            throw new BuildException("error while storing SNPs", e);
        }

    }

    private void processRecord(GFF3Record record)
        throws ObjectStoreException {
        String type = record.getType();
        String className =  TypeUtil.generateClassName(NAMESPACE, type);

        Class<? extends InterMineObject> imClass;
        Class<?> c;
        try {
            c = Class.forName(className);
            if (InterMineObject.class.isAssignableFrom(c)) {
                imClass = (Class<? extends InterMineObject>) c;
            } else {
                throw new RuntimeException("Feature className must be a valid class in the "
                        + "model that inherits from InterMineObject, but was: " + className);
            }
        } catch (ClassNotFoundException e1) {
            throw new BuildException("unknown class: " + className
                    + " while creating new SequenceAlteration object");
        }
        SequenceAlteration snp
            = (SequenceAlteration) getDirectDataLoader().createObject(imClass);

        List<String> dbxrefs = record.getAttributes().get("Dbxref");
        String identifier = getIdentifier(dbxrefs);
        if (identifier != null) {
            snp.setPrimaryIdentifier(identifier);
        }

        List<String> variantSeqs = record.getAttributes().get("Variant_seq");
        if (variantSeqs != null && variantSeqs.size() > 0) {
            snp.setVariantSequence(variantSeqs.get(0));
        }
        List<String> referenceSeqs = record.getAttributes().get("Reference_seq");
        if (referenceSeqs != null && referenceSeqs.size() > 0) {
            snp.setReferenceSequence(referenceSeqs.get(0));
        }

        List<String> variantEffects = record.getAttributes().get("Variant_effect");
        if (variantEffects != null && !variantEffects.isEmpty()) {
            for (String effect : variantEffects) {
                // Variant_effect=upstream_gene_variant 0 transcript ENST00000519787
                String transcriptIdentifier = getTranscriptIdentifier(effect);

                Consequence consequence = getDirectDataLoader().createObject(
                        org.intermine.model.bio.Consequence.class);
                consequence.setDescription(effect);
                if (StringUtils.isNotEmpty(transcriptIdentifier)) {
                    Transcript transcript;
                    try {
                        transcript = getTranscript(transcriptIdentifier);
                        consequence.setTranscript(transcript);
                    } catch (ObjectStoreException e) {
                        throw new RuntimeException("Can't store transcript", e);
                    }
                }
                snp.setConsequences(Collections.singleton(consequence));
                try {
                    getDirectDataLoader().store(consequence);
                } catch (ObjectStoreException e) {
                    throw new RuntimeException("Can't store consequence", e);
                }
            }
        }
        snp.setLength(getLength(record));
        String chromosomeIdentifier = record.getSequenceID();
        Chromosome chromosome = getChromosome(chromosomeIdentifier);
        snp.setChromosome(chromosome);
        setLocation(record, snp, chromosome);
        snp.setOrganism(getOrganism());
        getDirectDataLoader().store(snp);
    }

    private Transcript getTranscript(String identifier) throws ObjectStoreException {
        Transcript transcript = transcripts.get(identifier);
        if (transcript == null) {
            transcript = getDirectDataLoader().createObject(
                    org.intermine.model.bio.Transcript.class);
            transcript.setPrimaryIdentifier(identifier);
            transcript.setOrganism(getOrganism());
            transcripts.put(identifier, transcript);
            getDirectDataLoader().store(transcript);
        }
        return transcript;
    }

    private Chromosome getChromosome(String identifier) throws ObjectStoreException {
        Chromosome chromosome = chromosomes.get(identifier);
        if (chromosome == null) {
            chromosome = getDirectDataLoader().createObject(
                    org.intermine.model.bio.Chromosome.class);
            chromosome.setPrimaryIdentifier(identifier);
            chromosome.setOrganism(getOrganism());
            chromosomes.put(identifier, chromosome);
            getDirectDataLoader().store(chromosome);
        }
        return chromosome;
    }


    private String getIdentifier(List<String> xrefs) {
        if (xrefs == null) {
            return null;
        }
        for (String xref : xrefs) {
            String[] bits = xref.split(":");
            if (bits.length == 2) {
                String identifier = bits[1];
                if (identifier != null && identifier.startsWith("rs")) {
                    return identifier;
                }
            }
        }
        return null;
    }

    private String getTranscriptIdentifier(String description) {
        if (description == null) {
            return null;
        }
        String[] bits = description.split(" ");
        for (String word : bits) {
            if (word != null && word.startsWith("ENST")) {
                return word;
            }
        }
        return null;
    }

    private int getLength(GFF3Record record) {
        int start = record.getStart();
        int end = record.getEnd();
        int length = Math.abs(end - start) + 1;
        return length;
    }

    private Location setLocation(GFF3Record record, SequenceAlteration snp, Chromosome chromosome)
        throws ObjectStoreException {
        Location location = getDirectDataLoader().createObject(
                org.intermine.model.bio.Location.class);
        int start = record.getStart();
        int end = record.getEnd();
        if (record.getStart() < record.getEnd()) {
            location.setStart(start);
            location.setEnd(end);
        } else {
            location.setStart(end);
            location.setEnd(start);
        }
        if (record.getStrand() != null && "+".equals(record.getStrand())) {
            location.setStrand("1");
        } else if (record.getStrand() != null && "-".equals(record.getStrand())) {
            location.setStrand("-1");
        } else {
            location.setStrand("0");
        }
        location.setLocatedOn(chromosome);
        location.setFeature(snp);
        getDirectDataLoader().store(location);
        return location;
    }

    /**
     * Get and store() the Organism object to reference when creating new objects.
     * @throws ObjectStoreException if there is a problem
     * @return the new Organism
     */
    protected Organism getOrganism() throws ObjectStoreException {
        if (org == null) {
            org = getDirectDataLoader().createObject(Organism.class);
            org.setTaxonId(new Integer(TAXON_ID));
            getDirectDataLoader().store(org);
        }
        return org;
    }

    /**
     * Get and store() the DataSet object to reference when creating new objects.
     * @throws ObjectStoreException if there is a problem
     * @return the new DataSet
     */
    protected DataSet getDataSet() throws ObjectStoreException {
        if (dataset == null) {
            dataset = getDirectDataLoader().createObject(DataSet.class);
            dataset.setName(DATASET_TITLE);
            dataset.setDataSource(getDataSource());
            getDirectDataLoader().store(dataset);
        }
        return dataset;
    }

    /**
     * Get and store() the DataSource object to reference when creating new objects.
     * @throws ObjectStoreException if there is a problem
     * @return the new DataSource
     */
    protected DataSource getDataSource() throws ObjectStoreException {
        if (datasource == null) {
            datasource = getDirectDataLoader().createObject(DataSource.class);
            datasource.setName(DATA_SOURCE_NAME);
            getDirectDataLoader().store(datasource);
        }
        return datasource;
    }

    /**
     * Directly set the array of files to read from.  Use this for testing with junit.
     * @param files the File objects
     */
    protected void setFileArray(File[] files) {
        this.files = files;
    }
}
