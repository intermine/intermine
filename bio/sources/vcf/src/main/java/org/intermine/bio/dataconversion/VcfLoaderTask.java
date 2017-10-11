package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.intermine.metadata.TypeUtil;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.SequenceAlteration;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.task.FileDirectDataLoaderTask;
import org.intermine.util.FormattedTextParser;

/**
 * Loader for VCF files
 *
 * @author Julie Sullivan
 */
public class VcfLoaderTask extends FileDirectDataLoaderTask
{
    private String dataSetName = null;
    private String dataSourceName = null;
    private String taxonId = null;
    private Organism org = null;
    private DataSet dataset = null;
    private DataSource datasource = null;
    private Map<String, ProxyReference> chromosomes = new HashMap<String, ProxyReference>();

    //Set this if we want to do some testing...
    private File[] files = null;
    private static final String NAMESPACE = "org.intermine.model.bio";


    /**
     * Sets the taxon ID for features in this file.
     *
     * @param vcfTaxonId a single taxon Id
     */
    public void setVcfTaxonId(String vcfTaxonId) {
        taxonId = vcfTaxonId;
    }

    /**
     * Sets the data source, e.g. Ensembl
     *
     * @param dataSourceName Name of data source (organisation)
     */
    public void setVcfDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    /**
     * Sets the data set, e.g. "Ensembl SNP data set"
     *
     * @param dataSetName name of data set being loaded
     */
    public void setVcfDataSetName(String dataSetName) {
        this.dataSetName = dataSetName;
    }

    /**
     * Process and load the SNP file.
     */
    @Override
    public void process() {
        try {
            getIntegrationWriter().beginTransaction();
            super.process();
            getIntegrationWriter().commitTransaction();
            getDirectDataLoader().close();
        } catch (ObjectStoreException e) {
            throw new BuildException("failed to store object", e);
        }
    }

    @Override
    public void execute() {
        if (files != null) {
            // setFiles() is used only for testing
            for (int i = 0; i < files.length; i++) {
                processFile(files[i]);
            }
            try {
                getDirectDataLoader().close();
            } catch (ObjectStoreException e) {
                throw new BuildException("Failed closing DirectDataLoader", e);
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

            Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
            while (lineIter.hasNext()) {
                String[] line = (String[]) lineIter.next();
                processRecord(line);
            }
        } catch (FileNotFoundException e) {
            throw new BuildException("problem reading file - file not found: " + file, e);
        } catch (IOException e) {
            throw new BuildException("error while closing FileReader for: " + file, e);
        } catch (ObjectStoreException e) {
            throw new BuildException("error while creating objects: " + file, e);
        }

    }

    private void processRecord(String[] line)
        throws ObjectStoreException {

        String chromosomeIdentifier = line[0];
        ProxyReference chromosome = getChromosome(chromosomeIdentifier);
        String start = line[1];
        String identifier = line[2];
        String referenceSeq = line[3];
        String variantSeq = line[4];
//        String qual = line[5];
//        String filter = line[6];
        String info = line[7];

        // create SNV by default?
        String type = "SequenceAlteration";

        // dbSNP_138;TSA=insertion
        String[] infoColumn = info.split(";");
        // TSA=insertion
        if (infoColumn != null && infoColumn.length > 1 && StringUtils.isNotEmpty(infoColumn[1])) {
            String[] tsaString = infoColumn[1].split("=");
            if (infoColumn != null && infoColumn.length > 1
                    && StringUtils.isNotEmpty(infoColumn[1])) {
                type = tsaString[1];
            }
        }

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

        if (identifier != null) {
            snp.setPrimaryIdentifier(identifier);
        }

        if (variantSeq != null) {
            snp.setVariantSequence(variantSeq);
        }

        if (referenceSeq != null) {
            snp.setReferenceSequence(referenceSeq);
        }

        snp.setType(type);
        snp.setLength(1);
        snp.proxyChromosome(chromosome);
        setLocation(snp, start, chromosome);
        snp.setOrganism(getOrganism());
        getDirectDataLoader().store(snp);
    }

    private ProxyReference getChromosome(String identifier) throws ObjectStoreException {
        ProxyReference chromosomeRef = chromosomes.get(identifier);
        if (chromosomeRef == null) {
            Chromosome chromosome = getDirectDataLoader().createObject(
                    org.intermine.model.bio.Chromosome.class);
            chromosome.setPrimaryIdentifier(identifier);
            chromosome.setOrganism(getOrganism());
            getDirectDataLoader().store(chromosome);

            chromosomeRef = new ProxyReference(getIntegrationWriter().getObjectStore(),
                    chromosome.getId(), Chromosome.class);
            chromosomes.put(identifier, chromosomeRef);
        }
        return chromosomeRef;
    }

    private Location setLocation(SequenceAlteration snp, String pos, ProxyReference chromosomeRef)
        throws ObjectStoreException {
        // SNPs are always size = 1
        final int length = 1;
        Location location = getDirectDataLoader().createObject(
                org.intermine.model.bio.Location.class);
        int start = new Integer(pos);
        int end = start + length;
        if (start < end) {
            location.setStart(start);
            location.setEnd(end);
        } else {
            location.setStart(end);
            location.setEnd(start);
        }
        location.setStrand("0");
        location.proxyLocatedOn(chromosomeRef);
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
            if (taxonId == null) {
                throw new RuntimeException("Taxon ID not found. Please set a valid taxon Id"
                        + " in your project XML file");
            }
            org.setTaxonId(new Integer(taxonId));
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
            dataset.setName(dataSetName);
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
            datasource.setName(dataSourceName);
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
