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

import java.io.Reader;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.ArrayList;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;


/**
 * DataConverter to parse an Ensembl Disease data file downloaded from EnsMart
 * into Items
 * @author Wenyan Ji
 */
public class EnsemblDiseaseConverter extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";

    protected Map genes = new HashMap();
    protected Map diseases = new HashMap();

    protected Map annotations = new HashMap();
    protected Map annotationEvidence = new HashMap();
    protected Map geneAnnotation = new HashMap();
    protected Map diseaseAnnotation = new HashMap();

    protected Item ensemblDb;
    protected Item omimDb;
    protected int id = 0;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public EnsemblDiseaseConverter(ItemWriter writer) throws ObjectStoreException {
        super(writer);

        ensemblDb = getEnsemblDb();
        omimDb = getOmimDb();
        writer.store(ItemHelper.convert(ensemblDb));
        writer.store(ItemHelper.convert(omimDb));
    }

    /**
     * @see DataConverter#process
     */
    public void process(Reader reader) throws Exception {

        BufferedReader br = new BufferedReader(reader);
        //intentionally throw away first line
        String line = br.readLine();

        while ((line = br.readLine()) != null) {
            String[] array = line.split("\t", -1); //keep trailing empty Strings
            Item disease = getDiseaseItem(array[1], array[2]);
            Item gene = getGeneItem(array[0], disease.getIdentifier());
            newAnnotation(gene, disease);
        }
    }

    /**
     * @see FileConverter#close
     */
    public void close() throws ObjectStoreException {
        store(genes.values());
        store(annotations.values());
        store(diseases.values());
    }

    /**
     * Convenience method to create a gene Item
     * @param ensemblGeneId the ensembl gene id used as organismDbId in ensembl-human translator
     * @param diseaseItemId the disease item id used to create omimDiseases collection
     * @return a new gene Item/or updated geneItem
     * @throws ObjectStoreException if an error occurs when storing the Item
     */
     protected Item getGeneItem(String ensemblGeneId, String diseaseItemId)
        throws ObjectStoreException {
        Item item = (Item) genes.get(ensemblGeneId);
        if (item != null) {
            ReferenceList omimList = (ReferenceList) item.getCollection("omimDiseases");
            if (!omimList.getRefIds().contains(diseaseItemId)) {
                omimList.addRefId(diseaseItemId);
                item.addCollection(omimList);
            }
        } else {
            item = createItem("Gene");
            item.addAttribute(new Attribute("organismDbId", ensemblGeneId));
            ReferenceList annotationCollection = new ReferenceList("annotations");
            item.addCollection(annotationCollection);
            geneAnnotation.put(item.getIdentifier(), new HashSet());

            ReferenceList omimDiseases = new ReferenceList("omimDiseases");
            omimDiseases.addRefId(diseaseItemId);
            item.addCollection(omimDiseases);

            genes.put(ensemblGeneId, item);
        }
        return item;
     }

    /**
     * Convenience method to create a disease Item
     * disease extends BioProperty in core model
     * @param omimId the disease item attribute
     * @param omimDescription the disease item attribute
     * one omimId may have many descriptions. put all descriptions in the attribute separated by ;
     * @return a new disease Item/or updated diseaseItem
     * @throws ObjectStoreException if an error occurs when storing the Item
     */
    protected Item getDiseaseItem(String omimId, String omimDescription)
        throws ObjectStoreException {
        Item item = (Item) diseases.get(omimId);
        if (item != null) {
            String des = item.getAttribute("description").getValue();
            String addedDes = des.concat(";").concat(omimDescription);
            item.addAttribute(new Attribute("description", addedDes));
        } else {
            item = createItem("Disease");
            item.addAttribute(new Attribute("omimId", omimId));
            item.addAttribute(new Attribute("description", omimDescription));
            ReferenceList evidence = new ReferenceList("evidence",
                          new ArrayList(Arrays.asList(new Object[]{getOmimDb().getIdentifier()})));
            item.addCollection(evidence);
            ReferenceList annotationCollection = new ReferenceList("annotations");
            item.addCollection(annotationCollection);
            diseaseAnnotation.put(item.getIdentifier(), new HashSet());
            diseases.put(omimId, item);
        }

        return item;

     }


     /**
     * Creates an Annotation, and puts it into a map for future reference.
     * This map must be written out at the end.
     *
     * @param gene the gene to be attached
     * @param disease the disease to be attached
     */
    protected void newAnnotation(Item gene, Item disease) {
        String annotationKey = gene.getIdentifier() + "-" + disease.getIdentifier();
        Item annotation = (Item) annotations.get(annotationKey);
        if (annotation == null) {
            annotation = createItem("Annotation");
            annotation.addReference(new Reference("subject", gene.getIdentifier()));
            annotation.addReference(new Reference("property", disease.getIdentifier()));
            ReferenceList evidence = new ReferenceList("evidence",
                 new ArrayList(Arrays.asList(new Object[]{getEnsemblDb().getIdentifier()})));
            annotation.addCollection(evidence);
            annotations.put(annotationKey, annotation);
        }

        Set geneAnnotations = (Set) geneAnnotation.get(gene.getIdentifier());
        if (!geneAnnotations.contains(annotationKey)) {
            ReferenceList annotationCollection = gene.getCollection("annotations");
            annotationCollection.addRefId(annotation.getIdentifier());
            geneAnnotations.add(annotationKey);
        }
        Set diseaseAnnotations = (Set) diseaseAnnotation.get(disease.getIdentifier());
        if (!diseaseAnnotations.contains(annotationKey)) {
            ReferenceList annotationCollection = disease.getCollection("annotations");
            annotationCollection.addRefId(annotation.getIdentifier());
            diseaseAnnotations.add(annotationKey);
        }
    }


    /**
     * set database object
     * @return db item
     */
    private Item getEnsemblDb() {
        if (ensemblDb == null) {
            ensemblDb = createItem("DataSource");
            Attribute title = new Attribute("title", "ensembl");
            Attribute url = new Attribute("url", "http://www.ensembl.org");
            ensemblDb.addAttribute(title);
            ensemblDb.addAttribute(url);
        }
        return ensemblDb;
    }


    /**
     * set database object
     * @return db item
     */
    private Item getOmimDb() {
        if (omimDb == null) {
            omimDb = createItem("DataSource");
            Attribute title = new Attribute("title", "OMIM");
            Attribute url = new Attribute("url",
                      "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=OMIM");
            omimDb.addAttribute(title);
            omimDb.addAttribute(url);
        }
        return omimDb;
    }

    /**
     * Convenience method for creating a new Item
     * @param className the name of the class
     * @return a new Item
     */
    protected Item createItem(String className) {
        Item item = new Item();
        item.setIdentifier(alias(className) + "_" + (id++));
        item.setClassName(GENOMIC_NS + className);
        item.setImplementations("");
        return item;
    }
}
