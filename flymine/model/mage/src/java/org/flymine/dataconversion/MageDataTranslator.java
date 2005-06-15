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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

import org.intermine.InterMineException;
import org.intermine.util.XmlUtil;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.objectstore.ObjectStoreException;

import org.intermine.dataconversion.ItemPath;
import org.intermine.dataconversion.ItemReader;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ObjectStoreItemPathFollowingImpl;
import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.ItemPrefetchDescriptor;
import org.intermine.dataconversion.ItemPrefetchConstraintDynamic;
import org.intermine.dataconversion.FieldNameAndValue;
import org.intermine.metadata.Model;

import org.apache.log4j.Logger;

/**
 * Convert MAGE data in fulldata Item format conforming to a source OWL definition
 * to fulldata Item format conforming to InterMine OWL definition.
 *
 * @author Wenyan Ji
 * @author Richard Smith
 */
public class MageDataTranslator extends DataTranslator
{
    protected static final Logger LOG = Logger.getLogger(MageDataTranslator.class);
    // flymine:ReporterLocation id -> flymine:Feature id
    protected Map rlToFeature = new HashMap();
    // mage:Feature id -> flymine:MicroArraySlideDesign id
    protected Map featureToDesign = new HashMap();
    // hold on to ReporterLocation items until end
    protected Set reporterLocs = new HashSet();
    protected Map bioEntity2Gene = new HashMap();

    //mage: Feature id -> flymine:MicroArrayExperimentResult id when processing BioAssayDatum
    protected Map maer2Feature = new HashMap();
    protected Set maerSet = new HashSet(); //maerItem
    //flymine: BioEntity id -> mage:Feature id when processing Reporter
    protected Map bioEntity2Feature = new HashMap();
    protected Map bioEntity2IdentifierMap = new HashMap();
    protected Set bioEntitySet = new HashSet();

    protected Map synonymMap = new HashMap(); //key:itemId value:synonymItem
    protected Map synonymAccessionMap = new HashMap();//key:accession value:synonymItem

    // reporterSet:Flymine reporter. reporter:material -> bioEntity may probably merged when
    //it has same identifier. need to reprocess reporterMaterial
    protected Set reporterSet = new HashSet();
    protected Map bioEntityRefMap = new HashMap();

    protected Map identifier2BioEntity = new HashMap();

    protected Map treatment2BioSourceMap = new HashMap();
    protected Set bioSource = new HashSet();

    protected Map organismMap = new HashMap();

    private Map dbs = new HashMap();
    private Map dbRefs = new HashMap();
    private String srcNs;
    private String tgtNs;

    private Item expItem = new Item();//assume only one experiment item presented

    protected Map reporter2FeatureMap = new HashMap();
    protected Map assay2Maer = new HashMap();
    protected Map maer2Reporter = new HashMap();
    protected Map maer2Assay =  new HashMap();
    protected Map maer2Tissue =  new HashMap();
    protected Map maer2Material =  new HashMap();
    protected Map maer2Gene =  new HashMap();
    protected Set cdnaSet = new HashSet();
    protected Map geneMap =  new HashMap(); //organismDbId, geneItem
    protected Set assaySet = new HashSet();
    protected Map sample2LabeledExtract = new HashMap();
    protected Set labeledExractSet = new HashSet();
    protected Map reporter2rl = new HashMap();
    protected Map assayToLabeledExtract = new HashMap();


    protected Set microArrayResults = new HashSet();
    protected Set samples = new HashSet();
    protected Map labeledExtractToAssay = new HashMap();
    protected Map sampleToTreatments = new HashMap();
    protected Map sampleToLabeledExtract = new HashMap();
    // genomic:MicroArrayResult identifier to genomic:MicroArrayAssay identifier
    protected Map resultToAssay = new HashMap();


    /**
     * @see DataTranslator#DataTranslator
     */
    public MageDataTranslator(ItemReader srcItemReader, Properties mapping, Model srcModel,
                              Model tgtModel) {
        super(srcItemReader, mapping, srcModel, tgtModel);
        srcNs = srcModel.getNameSpace().toString();
        tgtNs = tgtModel.getNameSpace().toString();
    }

    /**
     * @see DataTranslator#translate
     */
    public void translate(ItemWriter tgtItemWriter)
        throws ObjectStoreException, InterMineException {

        super.translate(tgtItemWriter);

        Iterator i;

//         i = processReporterLocs().iterator();
//         while (i.hasNext()) {
//             tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
//         }

//         i = processBioEntity().iterator();
//         while (i.hasNext()) {
//             tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
//         }

//         i = processGene().iterator();
//         while (i.hasNext()) {
//             tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
//         }

//         i = processReporterMaterial().iterator();
//         while (i.hasNext()) {
//             tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
//         }

//         i = processBioSourceTreatment().iterator();
//         while (i.hasNext()) {
//             tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
//         }

        i = processOrganism().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        i = dbs.values().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

//         i = processMaer().iterator();
//         while (i.hasNext()) {
//             tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
//         }

        i = processMicroArrayResults().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        i = processSamples().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

//         i = processLabeledExtract2Assay().iterator();
//         while (i.hasNext()) {
//             tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
//         }

//         i = processAssay2Experiment().iterator();
//         while (i.hasNext()) {
//             tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
//         }
    }

    /**
     * @see DataTranslator#translateItem
     */
    protected Collection translateItem(Item srcItem)
        throws ObjectStoreException, InterMineException {

        Collection result = new HashSet();
        String className = XmlUtil.getFragmentFromURI(srcItem.getClassName());

        Collection translated = super.translateItem(srcItem);
        Item gene = new Item();
        Item organism = new Item();

        if (translated != null) {
            for (Iterator i = translated.iterator(); i.hasNext();) {
                boolean storeTgtItem = true;
                Item tgtItem = (Item) i.next();
                // mage: BibliographicReference flymine:Publication
                if (className.equals("BibliographicReference")) {
                    Set authors = createAuthors(srcItem);
                    List authorIds = new ArrayList();
                    Iterator j = authors.iterator();
                    while (j.hasNext()) {
                        Item author = (Item) j.next();
                        authorIds.add(author.getIdentifier());
                        result.add(author);
                    }
                    ReferenceList authorsRef = new ReferenceList("authors", authorIds);
                    tgtItem.addCollection(authorsRef);
                } else if (className.equals("Database")) {
                    Attribute attr = srcItem.getAttribute("name");
                    if (attr != null) {
                        getDb(attr.getValue());
                    }
                    storeTgtItem = false;
                } else if (className.equals("FeatureReporterMap")) {
                    //setReporterLocationCoords(srcItem, tgtItem);
                     storeTgtItem = false;
                } else if (className.equals("Experiment")) {
                    expItem = translateMicroArrayExperiment(srcItem, tgtItem);
                } else if (className.equals("MeasuredBioAssay")) {
                    translateMicroArrayAssay(srcItem, tgtItem);
                    //storeTgtItem = false;
                } else if (className.equals("BioAssayDatum")) {
                    translateMicroArrayResult(srcItem, tgtItem);
                    storeTgtItem = false;
                } else if (className.equals("Reporter")) {
                    //setBioEntityMap(srcItem, tgtItem);
                    storeTgtItem = false;
                } else if (className.equals("BioSequence")) {
                    //translateBioEntity(srcItem, tgtItem);
                    storeTgtItem = false;
                } else if (className.equals("LabeledExtract")) {
                    translateLabeledExtract(srcItem);
                    storeTgtItem = false;
                } else if (className.equals("BioSource")) {
                    translateSample(srcItem, tgtItem);
                    storeTgtItem = false;
                } else if (className.equals("Treatment")) {
                    translateTreatment(srcItem, tgtItem);
                }

                if (storeTgtItem) {
                    result.add(tgtItem);
                }
            }

        }
        return result;
    }

    /**
     * @param srcItem = mage:BibliographicReference
     * @return author set
     */
    protected Set createAuthors(Item srcItem) {
        Set result = new HashSet();
        if (srcItem.hasAttribute("authors")) {
            Attribute authorsAttr = srcItem.getAttribute("authors");
            if (authorsAttr != null) {
                String authorStr = authorsAttr.getValue();
                StringTokenizer st = new StringTokenizer(authorStr, ";");
                while (st.hasMoreTokens()) {
                    String name = st.nextToken().trim();
                    Item author = createItem(tgtNs + "Author", "");
                    author.addAttribute(new Attribute("name", name));
                    result.add(author);
                }
            }
        }
        return result;
    }


    /**
     * @param srcItem = mage:Experiment
     * @param tgtItem = flymine: MicroArrayExperiment
     * @param srcNs = mage: src namespace
     * @return experiment Item
     * also created a HashMap labeledExtractToAssay
     * @throws ObjectStoreException if problem occured during translating
     */
    protected Item translateMicroArrayExperiment(Item srcItem, Item tgtItem)
        throws ObjectStoreException {
        if (srcItem.hasAttribute("name")) {
            tgtItem.addAttribute(new Attribute("name", srcItem.getAttribute("name").getValue()));
        }
        // prefetch done
        if (srcItem.hasCollection("descriptions")) {
            boolean desFlag = false;
            boolean pubFlag = false;
            Iterator desIter = getCollection(srcItem, "descriptions");
            while (desIter.hasNext()) {
                Item desItem = (Item) desIter.next();
                if (desItem.hasAttribute("text")) {
                    if (desFlag) {
                        LOG.error("Already set description for MicroArrayExperiment, "
                                  + " srcItem = " + srcItem.getIdentifier());
                    } else {
                        tgtItem.setAttribute("description", desItem.getAttribute("text").getValue());
                        desFlag = true;
                    }
                }
                if (desItem.hasCollection("bibliographicReferences")) {
                    ReferenceList publication = desItem.getCollection(
                                                  "bibliographicReferences");
                    if (publication != null) {
                        if (!isSingleElementCollection(publication)) {
                            throw new IllegalArgumentException("Experiment description collection ("
                                + desItem.getIdentifier()
                                + ") has more than one bibliographicReference");
                        } else {
                            if (pubFlag) {
                                LOG.error("Already set publication for MicroArrayExperiment, "
                                      + " srcItem = " + srcItem.getIdentifier());
                            } else {
                                tgtItem.setReference("publication", getFirstId(publication));
                                pubFlag = true;
                            }
                        }
                    }
                }
            }
        }

        // may have already created references to experiment
        if (expItem.getIdentifier() != "") {
            tgtItem.setIdentifier(expItem.getIdentifier());
        }
        return tgtItem;
    }


    /**
     * @param srcItem = mage: MeasuredBioAssay
     * @param tgtItem = genomic:MicroArrayAssay
     * @throws ObjectStoreException if problem occured during translating
     */
     protected void translateMicroArrayAssay(Item srcItem, Item tgtItem)
         throws ObjectStoreException {

         tgtItem.setReference("experiment", getExperimentId());

         // set up map from mage:BioAssayDatum identifier to genomic:MicroArrayAssay

         // MeasuredBioAssay.measuredBioAssayData.bioDataValues.bioAssayTupleData
         if (srcItem.hasCollection("measuredBioAssayData")) {
             Iterator iter = getCollection(srcItem, "measuredBioAssayData");

             List datumList = new ArrayList();

             while (iter.hasNext()) {
                 Item mbadItem = (Item) iter.next();
                 if (mbadItem.hasReference("bioDataValues")) {
                     Item bioDataTuples = getReference(mbadItem, "bioDataValues");
                     if (bioDataTuples.hasCollection("bioAssayTupleData")) {
                         ReferenceList rl = bioDataTuples.getCollection("bioAssayTupleData");
                         for (Iterator i = rl.getRefIds().iterator(); i.hasNext(); ) {
                             // datumId is identifier of a BioAssayDatum object
                             String datumId = (String) i.next();
                             if (!datumList.contains(datumId)) {
                                 resultToAssay.put(datumId, srcItem.getIdentifier());
                             }
                         }
                     }
                 }
             }

             if (datumList.size() > 0) {
                 //assay2Maer.put(srcItem.getIdentifier(), datumList);
                 // TODO only store MicroArrayAssay object if found some data??
                 assaySet.add(tgtItem);
             }
         }

         // set up map of MicroArrayAssay to LableedExtract - to be used when setting
         // link between MicroArrayAssay and Sample

         // MeasuredBioAssay.featureExtraction.physicalBioAssaySource -> PhysicalBioAssay
         // Item pbaItem = getItemByPath(new ItemPath("MeasuredBioAssay.featureExtraction.physicalBioAssaySource",
         //                                      srcNs), srcItem);

         Item pbaItem = null;
         if (srcItem.hasReference("featureExtraction")) {
             Item feItem = getReference(srcItem, "featureExtraction");
             if (feItem.hasReference("physicalBioAssaySource")) {
                 pbaItem = getReference(feItem, "physicalBioAssaySource");;
             }
         }


         List labeledExtracts = new ArrayList();
         if (pbaItem != null && pbaItem.hasReference("bioAssayCreation")) {
             Item hybri = getReference(pbaItem, "bioAssayCreation");
             if (hybri.hasCollection("sourceBioMaterialMeasurements")) {
                 Iterator iter = getCollection(hybri, "sourceBioMaterialMeasurements");
                 while (iter.hasNext()) {
                     Item bmItem = (Item) iter.next();
                     if (bmItem.hasReference("bioMaterial")) {
                         labeledExtracts.add(bmItem.getReference("bioMaterial").getRefId());
                         // map from mage:LabeledExtract identifier to genomic:MicroArrayAssay identifier
                         labeledExtractToAssay.put(bmItem.getReference("bioMaterial").getRefId(), tgtItem.getIdentifier());
                     }
                 }
             }
         }

         // map from genomic:MicroArrayAssay identifier to list of mage:LabeledExtract identifiers
             //         assayToLabeledExtract.put(tgtItem.getIdentifier(), labeledExtracts);
     }



    /**
     * @param srcItem = mage:BioAssayDatum
     * @param tgtItem = flymine:MicroArrayResult
     * @throws ObjectStoreException if problem occured during translating
     */
    public void translateMicroArrayResult(Item srcItem, Item tgtItem)
        throws ObjectStoreException {

        // TODO set isControl flag

        if (srcItem.hasAttribute("value")) {
            tgtItem.setAttribute("value", srcItem.getAttribute("value").getValue());
        }
        tgtItem.setReference("analysis", getExperimentId());

        // TODO do we still need maer2FeatureMap?

        //create maer2Feature map, and maer set
        if (srcItem.hasReference("designElement")) {
            maer2Feature.put(tgtItem.getIdentifier(),
                         srcItem.getReference("designElement").getRefId());
            //maerSet.add(tgtItem.getIdentifier());
        }
        // TODO mapping: MicroArrayResult.type = BioAssayDatum.quantitationType.name
        if (srcItem.hasReference("quantitationType")) {
            Item qtItem = getReference(srcItem, "quantitationType");

            if (qtItem.hasAttribute("name")) {
                tgtItem.setAttribute("type", qtItem.getAttribute("name").getValue());
            } else {
                LOG.error("srcItem ( " + qtItem.getIdentifier()
                          + " ) does not have name attribute");
            }
            if (qtItem.getClassName().endsWith("MeasuredSignal")
                || qtItem.getClassName().endsWith("Ratio")
                || qtItem.getClassName().endsWith("SpecializedQuantitationType")) {
                if (qtItem.hasReference("scale")) {
                    Item oeItem = getReference(qtItem, "scale");

                    tgtItem.setAttribute("scale", oeItem.getAttribute("value").getValue());
                } else {
                    LOG.error("srcItem (" + qtItem.getIdentifier()
                              + "( does not have scale attribute ");
                }
            } else if (qtItem.getClassName().endsWith("Error")) {
                if (qtItem.hasReference("targetQuantitationType")) {
                    // TODO if an Error does this mean flag should be set to fail??
                    Item msItem = getReference(qtItem, "targetQuantitationType");
                    if (msItem.hasReference("scale")) {
                        Item oeItem = getReference(msItem, "scale");

                        tgtItem.setAttribute("scale", oeItem.getAttribute("value").getValue());
                    } else {
                        LOG.error("srcItem (" + msItem.getIdentifier()
                                  + "( does not have scale attribute ");
                    }
                }
            }
        }

        microArrayResults.add(tgtItem);
    }


    /**
     * @param srcItem = databaseEntry item refed in BioSequence
     * @param sourceRef  ref to sourceId = database id
     * @param subjectId = bioEntity identifier will probably be changed
     * when reprocessing bioEntitySet
     * @return synonym item
     */
    protected Item createSynonym(Item srcItem, Reference sourceRef, String subjectId) {
        Item synonym = new Item();
        synonym.setClassName(tgtNs + "Synonym");
        synonym.setIdentifier(srcItem.getIdentifier());
        synonym.setImplementations("");
        synonym.addAttribute(new Attribute("type", "accession"));
        synonym.addReference(sourceRef);
        synonym.addAttribute(new Attribute("value",
                                 srcItem.getAttribute("accession").getValue()));
        synonym.addReference(new Reference("subject", subjectId));

        return synonym;
    }


    /**
     * @param srcItem = mage:LabeledExtract
     * @param tgtItem = flymine:LabeledExtract
     * @throws ObjectStoreException if problem occured during translating
     * LabeledExtract -> {treatments} -> {sourceBioMaterialMeasurements} ->
     *(BioSample)extract -> {treatments} -> {sourceBioMaterialMeasurements} ->
     *(BioSample)not-extract -> {treatments} -> {sourceBioMaterialMeasurements} ->
     *(BioSource)
     */
    public void translateLabeledExtract(Item srcItem)
        throws ObjectStoreException {

        String sampleId = processTreatments(srcItem, new ArrayList());
        // map from sample to top level LabeledExtract
        // TODO is this needed for link from MicroArrayAssay to Sample??
        sampleToLabeledExtract.put(sampleId, srcItem.getIdentifier());
    }


    /**
     * For a given BioMaterial iterate through treatments applied and add to a collection.
     * Recurse into source BioMaterials and add their treatments.
     */
    protected String processTreatments(Item bioMaterial, List treatments)  throws ObjectStoreException {
        // TODO check if BioSource (genomic:Sample) and create map from sample to top
        // level LabeledExtract.  Sample needs collection of treatments.


        if (bioMaterial.hasCollection("treatments")) {
            Iterator treatmentIter = getCollection(bioMaterial, "treatments");
            while (treatmentIter.hasNext()) {
                Item treatment = (Item) treatmentIter.next();

                // put id on collection
                treatments.add(treatment.getIdentifier());

                // search for source bio material and nested treatments
                if (treatment.hasCollection("sourceBioMaterialMeasurements")) {
                    Iterator sourceIter = getCollection(treatment, "sourceBioMaterialMeasurements");
                    while (sourceIter.hasNext()) {
                        Item sourceMaterial = (Item) sourceIter.next();
                        if (sourceMaterial.hasReference("bioMaterial")) {
                            // recurse into next BioMaterial
                            processTreatments(getReference(sourceMaterial, "bioMaterial"), treatments);
                        }
                    }
                }
            }
        }

        if (bioMaterial.getClassName().equals(srcNs + "BioSource")) {
            // if this is sample then put list of treatments in a map
            sampleToTreatments.put(bioMaterial.getIdentifier(), treatments);
            return bioMaterial.getIdentifier();
        }
        return null;
    }


    /**
     * @param srcItem = mage:BioSource
     * @param tgtItem = genomic:Sample
     * extra genomic:Organism item is created and saved in  organismMap
     * @throws ObjectStoreException if problem occured during translating
     */
    protected void translateSample(Item srcItem, Item tgtItem)
        throws ObjectStoreException {

        // TODO set interesting characteristic(s) to some attribute of sample - description?
        //      will need config to be passed in
        // TODO set identifier to be internal MAGE identifier?

        List list = new ArrayList();
        Item organism = new Item();
        if (srcItem.hasCollection("characteristics")) {
            Iterator charIter = getCollection(srcItem, "characteristics");
            while (charIter.hasNext()) {
                Item charItem = (Item) charIter.next();
                if (charItem.hasAttribute("category")) {
                    String category = charItem.getAttribute("category").getValue();
                    if (category.equals("Organism")) {
                        if (charItem.hasAttribute("value")) {
                            String organismName = charItem.getAttribute("value").getValue();
                            organism = createOrganism("Organism", "", organismName);
                            tgtItem.setReference("organism", organism.getIdentifier());
                        }
                    } else {
                        list.add(charItem.getIdentifier());
                    }
                }
            }
            if (list.size() > 0) {
                ReferenceList tgtChar = new ReferenceList("characteristics", list);
                tgtItem.addCollection(tgtChar);
            }
        }

        if (srcItem.hasReference("materialType")) {
            Item type = ItemHelper.convert(srcItemReader.getItemById(
                 (String) srcItem.getReference("materialType").getRefId()));
            tgtItem.addAttribute(new Attribute("materialType",
                  type.getAttribute("value").getValue()));
        }

        if (srcItem.hasAttribute("name")) {
            tgtItem.addAttribute(new Attribute("name", srcItem.getAttribute("name").getValue()));
        }

        samples.add(tgtItem);
    }


    /**
     * @param srcItem = mage:Treatment
     * @param tgtItem = flymine:Treatment
     * @throws ObjectStoreException if problem occured during translating
     */
    public void translateTreatment(Item srcItem, Item tgtItem)
        throws ObjectStoreException {

        // TODO protocol - either attribute of treatment or reference to object

        if (srcItem.hasReference("action")) {
            Item action = ItemHelper.convert(srcItemReader.getItemById(
                              (String) srcItem.getReference("action").getRefId()));
            if (action.hasAttribute("value")) {
                tgtItem.addAttribute(new Attribute("action",
                               action.getAttribute("value").getValue()));
            }
        }
    }


    /**
     * bioSourceItem = flymine:Sample item without treatment collection
     * treatment collection is from mage:BioSample<type: not-Extract> treatment collection
     * treatment2BioSourceMap is created when tranlating LabeledExtract
     * @return resultSet
     */
    protected Set processBioSourceTreatment() {
        Set results = new HashSet();
        Iterator i = bioSource.iterator();
        while (i.hasNext()) {
            Item bioSourceItem = (Item) i.next();
            String sampleId = (String) bioSourceItem.getIdentifier();
            List treatList = new ArrayList();
            String s = (String) treatment2BioSourceMap.get(sampleId);
            LOG.debug("treatmentList " + s + " for " + sampleId);
            if (s != null) {
                StringTokenizer st = new StringTokenizer(s);
                while (st.hasMoreTokens()) {
                    treatList.add(st.nextToken());
                }
            }

            if (treatList.size() > 0) {
                ReferenceList treatments = new ReferenceList("treatments", treatList);
                bioSourceItem.addCollection(treatments);
            }

            //add labeledExtract reference to Sample
            String le = null;
            if (sample2LabeledExtract.containsKey(sampleId)) {
                le = (String) sample2LabeledExtract.get(sampleId);
                if (le != null) {
                    bioSourceItem.addReference(new Reference("labeledExtract", le));
                }
            }

            results.add(bioSourceItem);
        }
        return results;
    }

    /**
     * get le2Assay hashmap from assayToLabeledExtract map
     * add assay reference to LabeledExtract
     * @return LabeledExtract
     */
    protected Set processLabeledExtract2Assay() {
        Set results = new HashSet();
        Map le2Assay = new HashMap();
        for (Iterator i = assaySet.iterator(); i.hasNext(); ) {
            Item assayItem = (Item) i.next();
            String id = (String) assayItem.getIdentifier();
            if (assayToLabeledExtract.containsKey(id)) {
                List tissue = (ArrayList) assayToLabeledExtract.get(id);
                if (tissue != null) {
                    for (Iterator j = tissue.iterator(); j.hasNext();) {
                        le2Assay.put((String) j.next(), id);
                    }
                }
            }
        }

        for (Iterator k = labeledExractSet.iterator(); k.hasNext();) {
            Item leItem = (Item) k.next();
            String key = (String) leItem.getIdentifier();
            if (le2Assay.containsKey(key)) {
                String assay = (String) le2Assay.get(key);
                if (assay != null) {
                    leItem.addReference(new Reference("assay", assay));
                    results.add(leItem);
                }
            }
        }

        return results;
    }

    /**
     * set ReporterLocation.design reference, don't need to set
     * MicroArraySlideDesign.locations explicitly
     * @return results set
     */
    protected Set processReporterLocs() {
        Set results = new HashSet();
        if (reporterLocs != null && featureToDesign != null && rlToFeature != null) {
            for (Iterator i = reporterLocs.iterator(); i.hasNext();) {
                Item rl = (Item) i.next();
                String designId = (String) featureToDesign.get(rlToFeature.get(rl.getIdentifier()));
                Reference designRef = new Reference();
                if (designId != null) {
                    designRef.setName("design");
                    designRef.setRefId(designId);
                    rl.addReference(designRef);
                    results.add(rl);
                }
            }
        }
        return results;
    }

    /**
     * BioAssayDatum  mage:FeatureId -> flymine:MAEResultId (MicroArrayResults)
     * Reporter flymine: BioEntityId -> mage:FeatureId
     * BioSequece  BioEntityId -> BioEntity Item
     *                         -> extra Gene Item
     * @return add microResults collection to BioEntity item
     * and add identifier attribute to BioEntity
     */
    protected Set processBioEntity() {
        Set results = new HashSet();
        Set entitySet = new HashSet();
        String s = null;
        String itemId = null;
        String identifierAttribute = null;
        String bioEntityId = null;
        String anotherId = null;
        Set identifierSet = new HashSet();

        for (Iterator i = bioEntitySet.iterator(); i.hasNext();) {
            Item bioEntity = (Item) i.next();
            bioEntityId = bioEntity.getIdentifier();
            //add attribute identifier for bioEntity
            identifierAttribute = (String) bioEntity2IdentifierMap.get(bioEntityId);

            if (identifier2BioEntity.containsKey(identifierAttribute)) {
                Item anotherEntity = (Item) identifier2BioEntity.get(identifierAttribute);
                anotherId = anotherEntity.getIdentifier();
                List synonymList = new ArrayList();
                if (anotherEntity.hasCollection("synonyms")) {
                    ReferenceList synonymList1 = anotherEntity.getCollection("synonyms");
                    for (Iterator k = synonymList1.getRefIds().iterator(); k.hasNext();) {
                        String refId = (String) k.next();
                        synonymList.add(refId);
                    }
                }
                if (bioEntity.hasCollection("synonyms")) {
                    ReferenceList synonymList2 = bioEntity.getCollection("synonyms");
                    String subject = anotherEntity.getIdentifier();
                    for  (Iterator k = synonymList2.getRefIds().iterator(); k.hasNext();) {
                        String refId = (String) k.next();
                        if (!synonymList.contains(refId)) {
                            synonymList.add(refId);
                        }
                        //change subjectId for sysnonym when its bioentity is merged
                        Item synonym = (Item) synonymMap.get(refId);
                        String accession = synonym.getAttribute("value").getValue();
                        synonym.addReference(new Reference("subject", subject));
                        synonymAccessionMap.put(accession, synonym);
                    }
                }
                if (synonymList != null) {
                    anotherEntity.addCollection(new ReferenceList("synonyms", synonymList));
                }

                identifier2BioEntity.put(identifierAttribute, anotherEntity);
                bioEntityRefMap.put(bioEntityId, anotherId);
                //modify bioEntity2Feature
                String mergedFeatures = (String) bioEntity2Feature.get(bioEntityId);
                String features = (String) bioEntity2Feature.get(anotherId);
                String newFeatures = null;
                if (mergedFeatures != null && features != null) {
                    newFeatures = features.concat(" " + mergedFeatures);
                } else if (mergedFeatures != null && features == null) {
                    newFeatures = mergedFeatures;
                } else if (mergedFeatures == null && features != null) {
                    newFeatures = features;
                }
                if (newFeatures != null) {
                    bioEntity2Feature.remove(bioEntityId);
                    bioEntity2Feature.remove(anotherId);
                    bioEntity2Feature.put(anotherId, newFeatures);
                }
                //modify bioEntity2Gene
                String mergedGene = (String) bioEntity2Gene.get(bioEntityId);
                String gene = (String) bioEntity2Gene.get(anotherId);
                String newGenes = null;
                if (gene != null && mergedGene != null) {
                    newGenes = gene.concat(" " + mergedGene);
                } else if (gene == null && mergedGene != null) {
                    newGenes = mergedGene;
                } else if (gene != null && mergedGene == null) {
                    newGenes = gene;
                }
                if (newGenes != null) {
                    bioEntity2Gene.remove(bioEntityId);
                    bioEntity2Gene.remove(anotherId);
                    bioEntity2Gene.put(anotherId, newGenes);
                }

            } else {
                bioEntity.addAttribute(new Attribute("identifier", identifierAttribute));
                identifier2BioEntity.put(identifierAttribute, bioEntity);
                identifierSet.add(identifierAttribute);
            }

        }

        for (Iterator i = identifierSet.iterator(); i.hasNext();) {
            Item bioEntity = (Item) identifier2BioEntity.get((String) i.next());
            results.add(bioEntity);
            if (bioEntity.getClassName().equals(tgtNs + "CDNAClone")) {
                 cdnaSet.add(bioEntity.getIdentifier());
            }

        }

        for (Iterator i = synonymAccessionMap.keySet().iterator(); i.hasNext();) {
            Item synonym = (Item) synonymAccessionMap.get((String) i.next());
            results.add(synonym);

        }
        return results;
    }

    /**
     * BioAssayDatum  mage:FeatureId -> flymine:MAEResultId (MicroArrayResult)
     * Reporter flymine: BioEntityId -> mage:FeatureId
     * BioSequece  BioEntityId -> BioEntity Item
     *                         -> extra Gene Item
     * @return add microArrayResult collection to Gene item
     */
    protected Set processGene() {
        Set results = new HashSet();

        for (Iterator i = geneMap.keySet().iterator(); i.hasNext();) {
            String organismDbId = (String) i.next();
            Item gene = (Item) geneMap.get(organismDbId);
            results.add(gene);
        }
        return results;
    }

    /**
     * got reporterSet from setBioEntityMap()
     * refererence material may be changed after processBioEntity2MAEResult()
     * bioEntity is merged if it has the same identifierAttribute
     * @return resutls with right material refid
     */
    protected Set processReporterMaterial() {
        Set results = new HashSet();
        for (Iterator i = reporterSet.iterator(); i.hasNext();) {
            Item reporter = (Item) i.next();
            String rl = (String) reporter2rl.get(reporter.getIdentifier());
            reporter.addReference(new Reference("location", rl));
            if (reporter.hasReference("material")) {
                String bioEntityId = (String) reporter.getReference("material").getRefId();
                if (bioEntityRefMap.containsKey(bioEntityId)) {
                    String newBioEntityId = (String) bioEntityRefMap.get(bioEntityId);
                    reporter.addReference(new Reference("material", newBioEntityId));
                }
            }
            results.add(reporter);
        }
        return results;
    }

    /**
     * got organismMap from createOrganism()
     * @return organism only once for the same item
     */
    protected Set processOrganism() {
        Set results = new HashSet();
        for (Iterator i = organismMap.keySet().iterator(); i.hasNext();) {
            String organismValue = (String) i.next();
            Item organism = (Item) organismMap.get(organismValue);
            results.add(organism);
        }
        return results;
    }


    // set MicroArrayResult.assay
    // TODO set MicroArrayResult.sample
    protected Set processMicroArrayResults() {
        Iterator resultIter = microArrayResults.iterator();
        while (resultIter.hasNext()) {
            Item maResult = (Item) resultIter.next();
            if (resultToAssay.containsKey(maResult.getIdentifier())) {
                maResult.setReference("assay", (String) resultToAssay.get(maResult.getIdentifier()));
            }
        }
        return microArrayResults;
    }


    // set Sample.assay
    // set Sample.treatments
    protected Set processSamples() {
        Iterator sampleIter = samples.iterator();
        while (sampleIter.hasNext()) {
            Item sample = (Item) sampleIter.next();
            String sampleId = sample.getIdentifier();
            if (sampleToTreatments.containsKey(sampleId)) {
                sample.addCollection(new ReferenceList("treatments", (List) sampleToTreatments.get(sampleId)));
            }

            if (sampleToLabeledExtract.containsKey(sampleId)) {
                String extractId = (String) sampleToLabeledExtract.get(sampleId);
                if (labeledExtractToAssay.containsKey(extractId)) {
                    sample.setReference("assay", (String) labeledExtractToAssay.get(extractId));
                }
            }
        }
        return samples;
    }


    /**
     * maer add reference of reporter, material(CDNAClone only), assay(MicroArrayAssay)
     * add collection of genes and tissues(LabeledExtract)
     * @return maerItem collections
     */
    protected Set processMaer() {
        Set results = new HashSet();
        maer2Reporter = createMaer2ReporterMap(maer2Feature, reporter2FeatureMap, maerSet);
        maer2Assay =  createMaer2AssayMap();
        maer2Tissue = createMaer2TissueMap(maer2Assay, assayToLabeledExtract, maerSet);
        maer2Material = createMaer2MaterialMap(maer2Feature, bioEntity2Feature, maerSet, cdnaSet);
        maer2Gene = createMaer2GeneMap(maer2Feature, bioEntity2Feature, bioEntity2Gene, maerSet);

        for (Iterator i = maerSet.iterator(); i.hasNext(); ) {
            Item maerItem = (Item) i.next();
            String id = maerItem.getIdentifier();
            if (maer2Reporter.containsKey(id)) {
                String reporter =  (String) maer2Reporter.get(id);
                if (reporter != null) {
                    maerItem.addReference(new Reference("reporter", reporter));
                }
            }
            // set MicroArrayResult.assay
            if (maer2Assay.containsKey(id)) {
                String assay = (String) maer2Assay.get(id);
                if (assay != null) {
                    maerItem.addReference(new Reference("assay", assay));
                }
            }
            if (maer2Tissue.containsKey(id)) {
                List tissue = (List) maer2Tissue.get(id);
                if (tissue != null && !tissue.isEmpty()) {
                    maerItem.addCollection(new ReferenceList("tissues", tissue));
                }
            }
            if (maer2Material.containsKey(id)) {
                String material = (String) maer2Material.get(id);
                if (material != null) {
                    maerItem.addReference(new Reference("material", material));
                }
            }

            if (maer2Gene.containsKey(id)) {
                String genes = (String) maer2Gene.get(id);
                if (genes != null) {
                    StringTokenizer st = new StringTokenizer(genes);
                    List l = new ArrayList();
                    while (st.hasMoreTokens()) {
                        String gene = st.nextToken();
                        l.add(gene);
                    }
                    maerItem.addCollection(new ReferenceList("genes", l));
                }
            }

            results.add(maerItem);
        }

        return results;
    }

    /**
     * @param maer2Feature = HashMap
     * @param reporter2FeatureMap = HashMap
     * @param maerSet = HashSet
     * @return maer2Reporter
     */
    protected Map createMaer2ReporterMap(Map maer2Feature, Map reporter2FeatureMap, Set maerSet) {
        Map feature2Reporter = new HashMap();
        String features = null;
        String feature = null;
        String reporter = null;

        for (Iterator i = reporter2FeatureMap.keySet().iterator(); i.hasNext(); ) {
            reporter = (String) i.next();
            features = null;
            feature = null;
            if (reporter2FeatureMap.containsKey(reporter)) {
                features = (String) reporter2FeatureMap.get(reporter);
                if (features != null) {
                    StringTokenizer st = new StringTokenizer(features);
                    while (st.hasMoreTokens()) {
                        feature = (String) st.nextToken();
                        feature2Reporter.put(feature, reporter);
                    }
                }
            }
        }

        for (Iterator i = maerSet.iterator(); i.hasNext(); ) {
            Item maerItem = (Item) i.next();
            String maer = maerItem.getIdentifier();
            feature = null;
            reporter = null;

            if (maer2Feature.containsKey(maer)) {
                feature = (String) maer2Feature.get(maer);
                if (feature != null && feature2Reporter.containsKey(feature)) {
                    reporter = (String) feature2Reporter.get(feature);
                }
                if (reporter != null) {
                    maer2Reporter.put(maer, reporter);
                }
            }
        }
        return maer2Reporter;
    }

    /**
     * @return maer2Assay
     */
    protected Map createMaer2AssayMap() {
        // Identifiers preserved when translating BioAssayDatum -> MicroArrayResilt
        // so just use same map
        return resultToAssay;
    }

    /**
     * @param maer2Assay = HashMap
     * @param assayToLabeledExtract = HashMap
     * @param maerSet = HashSet
     * @return maer2Tissue
     */
    protected Map createMaer2TissueMap(Map maer2Assay, Map assayToLabeledExtract, Set maerSet) {

        for (Iterator i = maerSet.iterator(); i.hasNext(); ) {
            List le = new ArrayList();
            String assay = null;

            Item maerItem = (Item) i.next();
            String maer = maerItem.getIdentifier();
            if (maer2Assay.containsKey(maer)) {
                assay = (String) maer2Assay.get(maer);
            }
            if (assay != null) {
                le = (ArrayList) assayToLabeledExtract.get(assay);
            }
            if (le != null) {
                maer2Tissue.put(maer, le);
            }
        }
        return maer2Tissue;
    }

    /**
     * @param maer2Feature = HashMap
     * @param bioEntity2Feature = HashMap
     * @param maerSet = HashSet
     * @param cdnaSet = HashSet
     * @return maer2Material
     */
    protected Map createMaer2MaterialMap(Map maer2Feature, Map bioEntity2Feature, Set maerSet,
                         Set cdnaSet) {
        Map feature2Cdna = new HashMap();
        for (Iterator i = cdnaSet.iterator(); i.hasNext(); ) {
            String cdnaId = (String) i.next();

            String feature = null;
            String features = null;

            if (bioEntity2Feature.containsKey(cdnaId)) {
                features = (String) bioEntity2Feature.get(cdnaId);
            }
            if (features != null) {
                StringTokenizer st = new StringTokenizer(features);
                while (st.hasMoreTokens()) {
                    feature = st.nextToken();
                    feature2Cdna.put(feature, cdnaId);
                }
            }
        }
        for (Iterator j = maerSet.iterator(); j.hasNext();) {
            Item maerItem = (Item) j.next();
            String maer = maerItem.getIdentifier();
            String feature = null;
            String material = null;

            if (maer2Feature.containsKey(maer)) {
                feature = (String) maer2Feature.get(maer);
                if (feature != null && feature2Cdna.containsKey(feature)) {
                    material = (String) feature2Cdna.get(feature);
                }
                if (material != null) {
                    maer2Material.put(maer, material);
                }
            }
        }
        return maer2Material;
    }

    /**
     * @param maer2Feature = HashMap
     * @param bioEntity2Feature = HashMap
     * @param bioEntity2Gene = HashMap
     * @param maerSet = HashSet
     * @return maer2Gene
     */
    protected Map createMaer2GeneMap(Map maer2Feature, Map bioEntity2Feature, Map bioEntity2Gene,
                                     Set maerSet) {
        Map feature2BioEntity = new HashMap();
        String bioEntity = null;
        String genes = null;
        String feature = null;
        String features = null;

        for (Iterator i = bioEntity2Feature.keySet().iterator(); i.hasNext(); ) {
            String bioEntityId = (String) i.next();
            feature = null;
            features = null;

            features = (String) bioEntity2Feature.get(bioEntityId);
            if (features != null) {
                StringTokenizer st = new StringTokenizer(features);
                while (st.hasMoreTokens()) {
                    feature = st.nextToken();
                    feature2BioEntity.put(feature, bioEntityId);
                }
            }
         }

        for (Iterator j = maerSet.iterator(); j.hasNext(); ) {
            Item maerItem = (Item) j.next();
            String maer = maerItem.getIdentifier();
            feature = null;
            genes = null;
            bioEntity = null;
            if (maer2Feature.containsKey(maer)) {
                feature = (String) maer2Feature.get(maer);
            }
            if (feature != null && feature2BioEntity.containsKey(feature)) {
                bioEntity = (String) feature2BioEntity.get(feature);
            }
            if (bioEntity != null && bioEntity2Gene.containsKey(bioEntity)) {
                genes = (String) bioEntity2Gene.get(bioEntity);
            }
            if (genes != null) {
                maer2Gene.put(maer, genes);
            }
        }
        return maer2Gene;
    }

    /**
     * add experiment reference to MicroArrayAssay
     * @return assay set
     */
    protected Set processAssay2Experiment() {
        Set results = new HashSet();
        for (Iterator i = assaySet.iterator(); i.hasNext();) {
            Item assayItem = (Item) i.next();
            assayItem.addReference(new Reference("experiment", expItem.getIdentifier()));
            results.add(assayItem);
        }
        return results;
    }


    /**
     * @param className = tgtClassName
     * @param implementation = tgtClass implementation
     * @param identifier = gene item identifier from database item identifier
     * @param organismDbId = attribute for gene organismDbId
     * @return gene item
     */
    private Item createGene(String className, String implementation, String identifier,
            String organismDbId, String bioEntityId) {
        Item gene = new Item();
        if (!geneMap.containsKey(organismDbId)) {
            gene = createItem(className, implementation);
            gene.setIdentifier(identifier);
            gene.addAttribute(new Attribute("organismDbId", organismDbId));
            geneMap.put(organismDbId, gene);
            bioEntity2Gene.put(bioEntityId, identifier);
        } else {
            gene = (Item) geneMap.get(organismDbId);
        }
        return gene;
    }

    /**
     * @param className = tgtClassName
     * @param implementation = tgtClass implementation
     * @param value = attribute for organism name
     * @return organism item
     */
     private Item createOrganism(String className, String implementation, String value) {
         Item organism = new Item();
         if (!organismMap.containsKey(value)) {
             organism = createItem(tgtNs + className, implementation);
             organism.setAttribute("name", value);
             organismMap.put(value, organism);
         } else {
             organism = (Item) organismMap.get(value);
         }
        return organism;
    }

    /**
     * @param dbName = databaseName
     * @return databaseItem
     */
    private Item getDb(String dbName) {
        Item db = (Item) dbs.get(dbName);
        if (db == null) {
            db = createItem(tgtNs + "Database", "");
            Attribute title = new Attribute("title", dbName);
            db.addAttribute(title);
            dbs.put(dbName, db);
        }
        return db;
    }

    /**
     * @param dbName = databaseName
     * @return databaseReference
     */
    private Reference getSourceRef(String dbName) {
        Reference sourceRef = (Reference) dbRefs.get(dbName);
        if (sourceRef == null) {
             sourceRef = new Reference("source", getDb(dbName).getIdentifier());
             dbRefs.put(dbName, sourceRef);
        }
        return sourceRef;
    }

    /**
     * @return identifier for experimentItem assume only one experiment item presented
     */
    private String getExperimentId() {
        if (expItem.getIdentifier() == "") {
            expItem = createItem(tgtNs + "MicroArrayExperiment", "");
        }
        return expItem.getIdentifier();
    }

    // get an item by path and deal with conversion to/from fulldata items
    private Item getItemByPath(ItemPath path, Item startItem) throws ObjectStoreException {
        return ItemHelper.convert(srcItemReader.getItemByPath(path, ItemHelper.convert(startItem)));
    }

    /**
     * @see DataTranslatorTask#execute
     */
    public static Map getPrefetchDescriptors() {
        Map paths = new HashMap();
        HashSet descSet = new HashSet();

        //setReporterLocationCoords
        ItemPrefetchDescriptor desc1 = new ItemPrefetchDescriptor(
                "(FeatureGroup <- Feature.featureGroup)");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic(
                                      ObjectStoreItemPathFollowingImpl.IDENTIFIER,
                                      "featureGroup"));
        desc1.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    "http://www.flymine.org/model/mage#Feature", false));
        paths.put("http://www.flymine.org/model/mage#FeatureGroup", Collections.singleton(desc1));


        // BioAssayDatum.quantitationType.scale
        desc1 = new ItemPrefetchDescriptor("BioAssayDatum.quantitationType");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("quantitationType",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));

        ItemPrefetchDescriptor desc2 = new ItemPrefetchDescriptor(
                 "(BioAssayDatum.quantitationType).scale");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("scale",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc1.addPath(desc2);
        descSet.add(desc1);

        // BioAssayDatum.quantitationType.targetQuantitationType.scale
        ItemPrefetchDescriptor desc3 = new ItemPrefetchDescriptor(
                  "(BioAssayDatum.quantitationType).targetQuantitationType");
        desc3.addConstraint(new ItemPrefetchConstraintDynamic(
                  "targetQuantitationType", ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        ItemPrefetchDescriptor desc4 = new ItemPrefetchDescriptor(
                   "(BioAssayDatum.quantitationType.targetQuantitationType).scale");
        desc4.addConstraint(new ItemPrefetchConstraintDynamic("scale",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc3.addPath(desc4);
        desc1.addPath(desc3);
        descSet.add(desc1);
        //paths.put("http://www.flymine.org/model/mage#BioAssayDatum",Collections.singleton(desc1));
        paths.put("http://www.flymine.org/model/mage#BioAssayDatum", descSet);


        // BioSequence...
        descSet = new HashSet();

        // BioSequence.type
        desc1 = new ItemPrefetchDescriptor("BioSequence.type");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("type",
                                     ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descSet.add(desc1);

        // BioSequence.sequenceDatabases.databaseEntry.database
        desc1 = new ItemPrefetchDescriptor("BioSequence.sequenceDatabases");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("sequenceDatabases",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2 = new ItemPrefetchDescriptor("(BioSequence.sequenceDatabases).databaseEntry");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("databaseEntry",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));

        desc3 = new ItemPrefetchDescriptor(
                      "((BioSequence.sequenceDatabases).databaseEntry).database");
        desc3.addConstraint(new ItemPrefetchConstraintDynamic("database",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2.addPath(desc3);
        desc1.addPath(desc2);
        descSet.add(desc1);

        paths.put("http://www.flymine.org/model/mage#BioSequence", descSet);


        // LabeledExtract...
        descSet = new HashSet();

        // LabeledExtract.materialType
        desc1 = new ItemPrefetchDescriptor("LabeledExtract.materialType");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("materialType",
                                     ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descSet.add(desc1);

        // LabeledExtract.labels
        desc1 = new ItemPrefetchDescriptor("LabeledExtract.labels");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("labels",
                                     ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descSet.add(desc1);

        // code descends through treatments in a loop - prefetch can't handle
        // that so need to define cutoff (??)
        // LabeledExtract.treatments.sourceBioMaterialMeasurements.bioMaterial.treatments
        //       .sourceBioMaterialMeasurements.bioMaterial.treatments
        desc1 = new ItemPrefetchDescriptor("LabeledExtract.treatments");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("treatments",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2 = new ItemPrefetchDescriptor(
                 "(LabeledExtract.treatments).sourceBioMaterialMeasurements");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("bioMaterialMeasurements",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc3 = new ItemPrefetchDescriptor(
                 "((LabeledExtract.treatments).sourceBioMaterialMeasurements).bioMaterial");
        desc3.addConstraint(new ItemPrefetchConstraintDynamic("bioMaterial",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc4 = new ItemPrefetchDescriptor(
          "(((LabeledExtract.treatments).sourceBioMaterialMeasurements).bioMaterial).treatments");
        desc4.addConstraint(new ItemPrefetchConstraintDynamic("treatments",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));

        ItemPrefetchDescriptor desc5 = new ItemPrefetchDescriptor(
            "(...).sourceBioMaterialMeasurements");
        desc5.addConstraint(new ItemPrefetchConstraintDynamic("sourceBioMaterialMeasurements",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        ItemPrefetchDescriptor desc6 = new ItemPrefetchDescriptor(
            "((...).sourceBioMaterialMeasurements).bioMaterial");
        desc6.addConstraint(new ItemPrefetchConstraintDynamic("bioMaterial",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        ItemPrefetchDescriptor desc7 = new ItemPrefetchDescriptor(
                 "(((...).sourceBioMaterialMeasurements).bioMaterial).treatments");
        desc7.addConstraint(new ItemPrefetchConstraintDynamic("treatments",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc6.addPath(desc7);
        desc5.addPath(desc6);
        desc4.addPath(desc5);
        desc3.addPath(desc4);
        desc2.addPath(desc3);
        desc1.addPath(desc2);
        descSet.add(desc1);

        paths.put("http://www.flymine.org/model/mage#LabeledExtract", descSet);


        // BioSource...
        descSet = new HashSet();

        // BioSource.materialType
        desc1 = new ItemPrefetchDescriptor("BioSource.materialType");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("materialType",
                                     ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descSet.add(desc1);

        // BioSource.characteristics
        desc1 = new ItemPrefetchDescriptor("BioSource.characteristics");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("characteristics",
                                     ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descSet.add(desc1);

        paths.put("http://www.flymine.org/model/mage#BioSource", descSet);


        // Treatment.action
        desc1 = new ItemPrefetchDescriptor("Treatment.action");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic(
                                     ObjectStoreItemPathFollowingImpl.IDENTIFIER, "action"));
        paths.put("http://www.flymine.org/model/mage#Treatment",
                   Collections.singleton(desc1));


        // new prefetch with collections

        // FeatureReporterMap->featureInformationSources->feature->featureLocation
        descSet = new HashSet();
        desc1 = new ItemPrefetchDescriptor("FeatureReporterMap.featureInformaionSources");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("featureInformationSources",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2 = new ItemPrefetchDescriptor(
                 "(FeatureReporterMap.featureInformaionSources).feature");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("feature",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc3 = new ItemPrefetchDescriptor(
                 "((FeatureReporterMap.featureInformaionSources).feature).featureLocation");
        desc3.addConstraint(new ItemPrefetchConstraintDynamic("featureLocation",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));

        desc1.addPath(desc2);
        desc2.addPath(desc3);
        //descSet.add(desc1); required?

        // FeatureReporterMap->featureInformationSources->feature->zone
        desc4 = new ItemPrefetchDescriptor(
                 "((FeatureReporterMap.featureInformaionSources).feature).zone");
        desc4.addConstraint(new ItemPrefetchConstraintDynamic("zone",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2.addPath(desc4);
        descSet.add(desc1);
        paths.put("http://www.flymine.org/model/mage#FeatureReporterMap", descSet);


        // PhysicalArrayDesign...
        descSet = new HashSet();

        // PhysicalArrayDesign->featureGroups
        desc1 = new ItemPrefetchDescriptor("PhysicalArrayDesign.featureGroups");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("featureGroups",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descSet.add(desc1);

        // PhysicalArrayDesign->surfaceType
        desc1 = new ItemPrefetchDescriptor("PhysicalArrayDesign.surfaceType");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("surfaceType",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descSet.add(desc1);

        // PhysicalArrayDesign->descriptions
        desc1 = new ItemPrefetchDescriptor("PhysicalArrayDesign.descriptions");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("descripions",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descSet.add(desc1);

        paths.put("http://www.flymine.org/model/mage#PhysicalArrayDesign", descSet);


        // Reporter...
        descSet = new HashSet();

        // Reporter <- FeatureReporterMap.reporter
        desc1 = new ItemPrefetchDescriptor("(Reporter <- FeatureReporterMap.reporter)");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic(
                                       ObjectStoreItemPathFollowingImpl.IDENTIFIER, "reporter"));
        desc1.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    "http://www.flymine.org/model/mage#FeatureReporterMap", false));
        descSet.add(desc1);

        // Reporter->failTypes
        desc1 = new ItemPrefetchDescriptor("Reporter.failTypes");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("failTypes",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descSet.add(desc1);
        // Reporter->controlType
        desc1 = new ItemPrefetchDescriptor("Reporter.controlType");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("controlType",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descSet.add(desc1);

        // Reporter->immobilizedCharacteristics->type
        desc1 = new ItemPrefetchDescriptor("Reporter.immobilizedCharacteristics");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("immobilizedCharacteristics",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2 = new ItemPrefetchDescriptor(
                 "(Reporter.immobilizedCharacteristics).type");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("type",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc1.addPath(desc2);
        descSet.add(desc1);

        // Reporter->descriptions
        desc1 = new ItemPrefetchDescriptor("Reporter.descriptions");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("descriptions",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descSet.add(desc1);

        // Reporter->featureReporterMaps->featureInformationSources
        desc1 = new ItemPrefetchDescriptor("Reporter.featureReporterMaps");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("featureReporterMaps",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2 = new ItemPrefetchDescriptor(
                 "(Reporter.featureReporterMaps).featureInformationSources");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("featureInformationSources",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc1.addPath(desc2);
        descSet.add(desc1);

        paths.put("http://www.flymine.org/model/mage#Reporter", descSet);


        // Experiment...
        descSet = new HashSet();

        // Experiment->bioAssays
        desc1 = new ItemPrefetchDescriptor("Experiment.bioAssays");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("bioAssays",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descSet.add(desc1);


        // Experiment->bioAssays.physicalBioAssay->Hybridization
        desc2 = new ItemPrefetchDescriptor(
                    "(Experiment.bioAssays:physicalBioAssay).bioAssayCreation");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("bioAssayCreation",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc1.addPath(desc2);

        // Experiment->bioAssays.physicalBioAssay->Hybridization.sourceBioMaterialMeasurements
        desc3 = new ItemPrefetchDescriptor(
          "(Experiment.bioAssays:physicalBioAssay.bioAssayCreation).sourceBioMaterialMeasurements");
        desc3.addConstraint(new ItemPrefetchConstraintDynamic("sourceBioMaterialMeasurements",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));

        desc2.addPath(desc3);

        descSet.add(desc1);

        // Experiment.descriptions
        desc1 = new ItemPrefetchDescriptor("Experiment.descriptions");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("descriptions",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        descSet.add(desc1);

        paths.put("http://www.flymine.org/model/mage#Experiment", descSet);

        // DerivedBioAssay.derivedBioAssayData.bioDataValues.
        desc1 = new ItemPrefetchDescriptor("DerivedBioAssay.derivedBioAssayData");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("derivedBioAssayData",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2 = new ItemPrefetchDescriptor(
                 "(DerivedBioAssay.derivedBioAssayData).bioDataValues");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("bioDataValues",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc1.addPath(desc2);
        descSet.add(desc1);

        paths.put("http://www.flymine.org/model/mage#DerivedBioAssay",
                  Collections.singleton(desc1));

        return paths;
    }
}

