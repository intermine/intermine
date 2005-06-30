package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.DataConverter;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

/**
 * DataConverter to parse homophila data file into Items.
 * 
 * @author Thomas Riley
 */
public class HomophilaConverter extends FileConverter
{
    private static final Logger LOG = Logger.getLogger(HomophilaConverter.class);
    
    private static final int TRANSLATION_ID = 0;
    private static final int OMIM_ID = 1;
    private static final int PROTEIN_ID = 2;
    private static final int E_VALUE = 3;
        
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";

    protected Map diseaseDescriptions = new HashMap();
    protected Map proteinToGene = new HashMap();
    
    protected Map translations = new HashMap();
    protected Map proteins = new HashMap();
    protected Map diseases = new HashMap();
    protected Map genes = new HashMap();

    protected Item orgHuman;
    protected Item orgDrosophila;
    protected int id = 0;
    protected int matchCount = 0;

    protected File diseaseFile;
    protected File proteinGeneFile;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public HomophilaConverter(ItemWriter writer) throws ObjectStoreException {
        super(writer);

        orgHuman = newItem("Organism");
        orgHuman.addAttribute(new Attribute("abbreviation", "HS"));
        orgHuman.addAttribute(new Attribute("taxonId", "9606"));
        store(orgHuman);
        
        orgDrosophila = newItem("Organism");
        orgDrosophila.addAttribute(new Attribute("abbreviation", "DM"));
        orgDrosophila.addAttribute(new Attribute("taxonId", "7227"));
        store(orgDrosophila);
    }
    
    /**
     * Read omim ids and descriptions from reader. Input is two columns, tab seperated, first
     * column is OMIM id and second column is a line of description. There may be several lines
     * of description (with the same OMIM id) on adjacent lines.
     * 
     * @param reader reader
     */
    protected void readDiseaseDescriptions(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        String line;
        
        LOG.info("Reading disease descriptions...");
        
        String desc = "";
        String omim = "";
        while ((line = br.readLine()) != null) {
            String fields[] = StringUtils.split(line, '\t');
            if (omim.equals(fields[0])) {
                desc += "; " + fields[1];
                continue;
            }
            if (!StringUtils.isBlank(omim)) {
                // New entry
                diseaseDescriptions.put(omim, desc);
            }
            omim = fields[0];
            desc = fields[1];
        }
        diseaseDescriptions.put(omim, desc); // last line
        
        LOG.info("" + diseaseDescriptions.size() + " descriptions read.");
    }

    /**
     * Read mappings from protein id to gene id.
     * 
     * @param reader reader
     */
    protected void readProteinGeneFile(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        String line;
        int done = 1;
        
        LOG.info("Reading protein_gene mapping...");
        
        while ((line = br.readLine()) != null) {
            String fields[] = StringUtils.split(line, '\t');
            if (fields.length == 2) {
                proteinToGene.put(fields[0], fields[1]);
            } else {
                LOG.warn("line " + done + " in protein_gene.txt contained " + fields.length + " columns");
            }
            done++;
        }
        
        LOG.info("" + proteinToGene.size() + " proteins read.");
    }
    
    /**
     * Set the disease description input file. This file just contains OMIM ids and
     * several lines of description for each id.
     * 
     * @param diseaseFile disease description input file
     */
    public void setDiseasefile(File diseaseFile) {
        this.diseaseFile = diseaseFile;
    }
    
    /**
     * Set the protein to gene input file. This file contains two columns, the NP_ protein
     * id followed by the gene id.
     * 
     * @param proteinGeneFile protein to gene input file
     */
    public void setProteingenefile(File proteinGeneFile) {
        this.proteinGeneFile = proteinGeneFile;
    }
    
    /**
     * Reads disease description file first, then reads homophila matches file.
     * 
     * @see DataConverter#process
     */
    public void process(Reader reader) throws Exception {
       
        if (diseaseFile == null) {
            throw new NullPointerException("diseaseFile property not set");
        } else if (proteinGeneFile == null) {
            throw new NullPointerException("proteinGeneFile property not set");
        }
        
        try {
            readDiseaseDescriptions(new FileReader(diseaseFile));
        } catch (IOException err) {
            throw new RuntimeException("error reading disease descriptions", err);
        }
        
        try {
            readProteinGeneFile(new FileReader(proteinGeneFile));
        } catch (IOException err) {
            throw new RuntimeException("error reading protein to gene file", err);
        }
        
        BufferedReader br = new BufferedReader(reader);
        br.readLine();
        String line;
        int done = 0;
        
        while ((line = br.readLine()) != null) {
            String array[] = StringUtils.split(line, '\t');
            
            newBlastMatch(array);
            
            if (++done % 10000 == 0) {
                LOG.info("Processed " + done + " homophila matches");
            }
        }
    }

    /**
     * Create new BlastMatch item. Fills in all attributes and references.
     * 
     * @param array line of homophila matches file
     * @return BlastMatch Item
     * @throws ObjectStoreException if something goes wrong
     */
    protected Item newBlastMatch(String array[]) throws ObjectStoreException {
        Item item = newItem("BlastMatch");
        item.addReference(new Reference("subject", findProtein(array).getIdentifier()));
        item.addReference(new Reference("object", findTranslation(array).getIdentifier()));
        item.addAttribute(new Attribute("eValue", array[E_VALUE]));
        store(item);
        matchCount++;
        return item;
    }
    
    /**
     * Create new Translation item. Fills in all attributes and references.
     * 
     * @param array line of homophila matches file
     * @return Translation Item
     * @throws ObjectStoreException if something goes wrong
     */
    protected Item findTranslation(String array[]) throws ObjectStoreException {
        Item translation = (Item) translations.get(array[TRANSLATION_ID]);
        if (translation == null) {
            translation = newItem("Translation");
            translation.addAttribute(new Attribute("identifier", array[TRANSLATION_ID]));
            translation.addReference(new Reference("organism", orgDrosophila.getIdentifier()));
            translations.put(array[TRANSLATION_ID], translation);
            store(translation);
        }
        return translation;
    }
    
    /**
     * Create new Protein item. Fills in all attributes and references.
     * 
     * @param array line of homophila matches file
     * @return Protein Item
     * @throws ObjectStoreException if something goes wrong
     */
    protected Item findProtein(String array[]) throws ObjectStoreException {
        Item protein = (Item) proteins.get(array[PROTEIN_ID]);
        if (protein == null) {
            protein = newItem("Protein");
            protein.addAttribute(new Attribute("identifier", array[PROTEIN_ID]));
            protein.addReference(new Reference("organism", orgHuman.getIdentifier()));
            addToCollection(protein, "genes", findGene(array));
            proteins.put(array[PROTEIN_ID], protein);
            store(protein);
        }
        return protein;
    }
    
    /**
     * Create new Gene item. Fills in all attributes and references.
     * 
     * @param array line of homophila matches file
     * @return Gene Item
     * @throws ObjectStoreException if something goes wrong
     */
    protected Item findGene(String array[]) throws ObjectStoreException {
        String geneId = (String) proteinToGene.get(array[PROTEIN_ID]);
        if (geneId == null) {
            throw new IllegalStateException("protein id " + array[PROTEIN_ID]
                    + " doesn't map to a gene id");
        }
        Item gene = (Item) genes.get(geneId);
        if (gene == null) {
            gene = newItem("Gene");
            genes.put(geneId, gene);
            gene.addAttribute(new Attribute("identifier", geneId));
            gene.addReference(new Reference("organism", orgHuman.getIdentifier()));
            addToCollection(gene, "omimDiseases", findDisease(array));
            store(gene);
        }
        return gene;
    }
    
    /**
     * Create new Disease item. Fills in all attributes and references.
     * 
     * @param array line of homophila matches file
     * @return Disease Item
     * @throws ObjectStoreException if something goes wrong
     */
    protected Item findDisease(String array[]) throws ObjectStoreException {
        Item disease = (Item) diseases.get(array[OMIM_ID]);
        if (disease == null) {
            disease = newItem("Disease");
            diseases.put(array[OMIM_ID], disease);
            disease.addAttribute(new Attribute("omimId", array[OMIM_ID]));
            String desc = (String) diseaseDescriptions.get(array[OMIM_ID]);
            if (desc == null) {
                LOG.error("no disease description for OMIM " + array[OMIM_ID]);
                desc = "";
            }
            disease.addAttribute(new Attribute("description", desc));
            store(disease);
        }
        return disease;
    }
    
    /**
     * @see FileConverter#close()
     */
    public void close() throws Exception {
        LOG.info("translations.size() == " + translations.size());
        LOG.info("genes.size() == " + genes.size());
        LOG.info("proteins.size() == " + proteins.size());
        LOG.info("diseases.size() == " + diseases.size());
        LOG.info("matches.size() == " + matchCount);
        super.close();
    }

    /**
     * Convenience method for creating a new Item.
     * 
     * @param className the name of the class
     * @return a new Item
     */
    protected Item newItem(String className) {
        Item item = new Item();
        item.setIdentifier(alias(className) + "_" + (id++));
        item.setClassName(GENOMIC_NS + className);
        item.setImplementations("");
        return item;
    }
    
    /**
     * Add an Item to a named collection on another Item. If the collection does not exist
     * if will be created.
     * 
     * @param item item with collection
     * @param collection collection name
     * @param addition item to add to collection
     * @throws ObjectStoreException if something goes wrong
     */
    protected void addToCollection(Item item, String collection, Item addition)
        throws ObjectStoreException {
        ReferenceList coll = item.getCollection(collection);
        if (coll == null) {
            coll = new ReferenceList(collection);
            item.addCollection(coll);
        }
        coll.addRefId(addition.getIdentifier());
    }
}

