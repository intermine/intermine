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
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.*;

import org.intermine.dataconversion.ItemPath;
import org.intermine.dataconversion.ItemReader;
import org.intermine.dataconversion.ObjectStoreItemReader;
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

    protected Set microArrayResults = new HashSet();
    protected Set samples = new HashSet();
    protected Map labeledExtractToMeasuredBioAssay = new HashMap();
    protected Map sampleToTreatments = new HashMap();
    protected Map sampleToLabeledExtract = new HashMap();
    // genomic:MicroArrayResult identifier to genomic:MicroArrayAssay identifier
    protected Map resultToFeature = new HashMap();
    protected Map featureToReporter = new HashMap();
    protected Map resultToBioAssayData = new HashMap();
    protected Map bioAssayDataToAssay = new HashMap();
    protected Map measuredBioAssayToMicroArrayAssay = new HashMap();
    protected Map assayToSample = new HashMap();

    /**
     * @see DataTranslator#DataTranslator
     */
    public MageDataTranslator(ItemReader srcItemReader, Properties mapping, Model srcModel,
                              Model tgtModel) {
        super(srcItemReader, mapping, srcModel, tgtModel);
        srcNs = srcModel.getNameSpace().toString();
        tgtNs = tgtModel.getNameSpace().toString();
    }



    // Set batch size to 100?
//     public Iterator getItemIterator() throws ObjectStoreException {

//         Query q = new Query();
//         QueryClass qc = new QueryClass(org.intermine.model.fulldata.Item.class);
//         q.addFrom(qc);
//         q.addToSelect(qc);
//         QueryField qf = new QueryField(qc, "className");
//         SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue(srcNs + "LabeledExtract"));
//         q.setConstraint(sc);

//         return ((ObjectStoreItemReader) srcItemReader).itemIterator(q);
//     }

    /**
     * @see DataTranslator#translate
     */
    public void translate(ItemWriter tgtItemWriter)
        throws ObjectStoreException, InterMineException {

        super.translate(tgtItemWriter);

        Iterator i;

        i = processOrganism().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        i = dbs.values().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        i = processSamples().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        i = processMicroArrayResults().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

    }

    int opCount = 0;

    /**
     * @see DataTranslator#translateItem
     */
    protected Collection translateItem(Item srcItem)
        throws ObjectStoreException, InterMineException {


        // TODO MageConverter could create BioDataTuples instead?  Seems to link
        // DesignElement with BioAssay and data

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
                } else if (className.equals("Experiment")) {
                    expItem = translateMicroArrayExperiment(srcItem, tgtItem);
                } else if (className.equals("DerivedBioAssay")) {
                    translateMicroArrayAssay(srcItem, tgtItem);
                    //storeTgtItem = false;
                } else if (className.equals("BioAssayDatum")) {
                    translateMicroArrayResult(srcItem, tgtItem);
                    storeTgtItem = false;
                } else if (className.equals("Reporter")) {
                    Item material = translateReporter(srcItem, tgtItem);
                    if (material != null) {
                        result.add(material);
                    }
                } else if (className.equals("BioSequence")) {
                    //translateBioEntity(srcItem, tgtItem);
                    storeTgtItem = false;
                } else if (className.equals("BioSource")) {
                    result.addAll(translateSample(srcItem, tgtItem));
                    storeTgtItem = false;
                } else if (className.equals("Treatment")) {
                    translateTreatment(srcItem, tgtItem);
                }

                if (storeTgtItem) {
                    result.add(tgtItem);
                }
            }

        } else if (className.equals("LabeledExtract")) {
            translateLabeledExtract(srcItem);
        } else if (className.equals("MeasuredBioAssay")) {
            setLabeledExtractToMeasuredBioAssay(srcItem);
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
     * @throws ObjectStoreException if problem occured during translating
     */
    protected Item translateMicroArrayExperiment(Item srcItem, Item tgtItem)
        throws ObjectStoreException {
        if (srcItem.hasAttribute("name")) {
            tgtItem.addAttribute(new Attribute("name", srcItem.getAttribute("name").getValue()));
        }

        // PATH Experiment.descriptions.bibliographicReferences
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
     * @param srcItem = mage: DerivedBioAssay
     * @param tgtItem = genomic:MicroArrayAssay
     * @throws ObjectStoreException if problem occured during translating
     */
     protected void translateMicroArrayAssay(Item srcItem, Item tgtItem)
         throws ObjectStoreException {

         tgtItem.setReference("experiment", getExperimentId());


         // create map from mage:MeasuredBioAssay to genomic:MicroArrayAssay (mage:DerivedBioAssay)

         // PATH DerivedBioAssay.derivedBioAssayMap.sourceBioAssays
         Item mbaItem = null;
         if (srcItem.hasCollection("derivedBioAssayMap")) {
             // TODO check is single element collection
             Iterator mapIter = getCollection(srcItem, "derivedBioAssayMap");
             while (mapIter.hasNext()) {
                 Item mapItem = (Item) mapIter.next();
                 if (mapItem.hasCollection("sourceBioAssays")) {
                     Iterator sourceIter = getCollection(mapItem, "sourceBioAssays");
                     while (sourceIter.hasNext()) {
                         mbaItem = (Item) sourceIter.next();
                         if (mbaItem.getClassName().equals(srcNs + "MeasuredBioAssay")) {
                             measuredBioAssayToMicroArrayAssay.put(mbaItem.getIdentifier(),
                                                                   tgtItem.getIdentifier());
                         }
                     }
                 }
             }
         }

         // set up map from mage:BioAssayDatum identifier to genomic:MicroArrayAssay

         if (mbaItem != null) {
             if (srcItem.hasCollection("derivedBioAssayData")) {
                 Iterator iter = srcItem.getCollection("derivedBioAssayData").getRefIds().iterator();
                 while (iter.hasNext()) {
                     bioAssayDataToAssay.put((String) iter.next(), tgtItem.getIdentifier());
                 }
             }
         }
     }


    // srcItem = mage:MeasuredBioAssay
    protected void setLabeledExtractToMeasuredBioAssay(Item srcItem) throws ObjectStoreException {

        // PATH MeasuredBioAssay.featureExtraction.physicalBioAssaySource.bioAssayCreation.sourceBioMaterialMeasurements.bioMaterial


        // set up map of MicroArrayAssay to LabeledExtract - to be used when setting
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
                         labeledExtractToMeasuredBioAssay.put(bmItem.getReference("bioMaterial").getRefId(), srcItem.getIdentifier());
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

        // TODO set isControl flag - from Reporter??


        if (srcItem.hasAttribute("value")) {
            String value = srcItem.getAttribute("value").getValue();
            if (StringUtil.allDigits(value)) {
                tgtItem.setAttribute("value", value);
                // only store if a numerical value, ignore errors
                microArrayResults.add(tgtItem);
            }
        }
        tgtItem.setReference("analysis", getExperimentId());


        // PATH BioAssayDatum.quatitationType.scale

        if (srcItem.hasReference("designElement")) {
            // map from genomic:MicroArrayResult identifier to mage:Feature identifier
            resultToFeature.put(tgtItem.getIdentifier(),
                         srcItem.getReference("designElement").getRefId());
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

        // map from mage:MeasuredBioAssayData identifier to genomic:MicroArrayResult identifier
        // duplicates collection available in MeasureBioAssayData but is much better for
        // prefetch and memory useage
        if (srcItem.hasReference("bioAssayData")) {
            String bioAssayDataId = srcItem.getReference("bioAssayData").getRefId();
            resultToBioAssayData.put(tgtItem.getIdentifier(), bioAssayDataId);
        }
    }


    protected Item translateReporter(Item srcItem, Item tgtItem) throws ObjectStoreException {

        // PATH Reporter.featureReporterMaps.featureInformationSources.feature
        if (srcItem.hasCollection("featureReporterMaps")) {
            // check is single element collection
            Iterator frmIter = getCollection(srcItem, "featureReporterMaps");
            while (frmIter.hasNext()) {
                Item frm = (Item) frmIter.next();
                if (frm.hasCollection("featureInformationSources")) {
                    Iterator fisIter = getCollection(frm, "featureInformationSources");
                    while (fisIter.hasNext()) {
                        Item fis = (Item) fisIter.next();
                        if (fis.hasReference("feature")) {
                            featureToReporter.put(fis.getReference("feature").getRefId(), srcItem.getIdentifier());
                        }
                    }
                }
            }
        }

        Item material = null;

        // PATH Reporter.controlType
        // PATH Reporter.immobilizedCharacteristics.type

        // create BioEntity with identifier as Reporter.name.  For class look in:
        if (srcItem.hasReference("controlType")) {
            Item controlType = getReference(srcItem, "controlType");
            tgtItem.setAttribute("isControl", "true");
            tgtItem.setAttribute("controlType", controlType.getAttribute("value").getValue());
        } else {
            tgtItem.setAttribute("isControl", "false");
            // Reporter.immobilizedCharacteristics.type
            // if Reporter.controlTypes exists then is a control

            if (srcItem.hasCollection("immobilizedCharacteristics")) {
                Iterator bioIter = getCollection(srcItem, "immobilizedCharacteristics");
                while (bioIter.hasNext() && material == null) {
                    Item bioSequence = (Item) bioIter.next();
                    if (bioSequence.hasReference("type")) {
                        String type = getReference(bioSequence, "type").getAttribute("value").getValue();
                        if (type.toLowerCase().equals("cdna_clone")) {
                            material = createItem(tgtNs + "CDNAClone", "");
                            material.setAttribute("identifier", srcItem.getAttribute("name").getValue());
                        } else {
                            throw new ObjectStoreException("Unknown BioSequence type: " + type);
                        }
                        tgtItem.setReference("material", material.getIdentifier());
                    }
                }
            }
        }



        // if Reporter.failTypes exists then set failure type
        if (srcItem.hasCollection("failTypes")) {
            Iterator failIter = getCollection(srcItem, "failTypes");
            while (failIter.hasNext()) {
                Item fail = (Item) failIter.next();
                tgtItem.setAttribute("failType", fail.getAttribute("value").getValue());
            }
        }

        return material;
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

        String sampleId = searchTreatments(srcItem, new ArrayList());
        LOG.error("sampleId: " + sampleId);
        // map from sample to top level LabeledExtract
        // TODO is this needed for link from MicroArrayAssay to Sample??
        if (sampleId != null) {
            sampleToLabeledExtract.put(sampleId, srcItem.getIdentifier());
        }
    }


    /**
     * For a given BioMaterial iterate through treatments applied and add to a collection.
     * Recurse into source BioMaterials and add their treatments.
     */
    protected String searchTreatments(Item bioMaterial, List treatments)  throws ObjectStoreException {
        // TODO check if BioSource (genomic:Sample) and create map from sample to top
        // level LabeledExtract.  Sample needs collection of treatments.

        // LabeledExtract.treatments.sourceBioMaterialMeasurements.bioMaterial.treatments.sourceBioMaterialMeasurements.bioMaterial.
        //               [Treatment]    [BioMaterialMeasurement]   [BioSample] [Treatment]  [BioMaterialMeasurement]    [BioSample]

        // PATH is recursive - duplicate a number of times?  Refactor easier prefetch
        // PATH LabeledExtract.treatments.sourceBioMaterialMeasurements.bioMaterial
        LOG.error("bioMaterial: " + bioMaterial.getClassName() + " - " + bioMaterial.getIdentifier());
        if (bioMaterial.getClassName().equals(srcNs + "BioSource")) {
            // if this is sample then put list of treatments in a map
            sampleToTreatments.put(bioMaterial.getIdentifier(), treatments);
            return bioMaterial.getIdentifier();
        }



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
                            return searchTreatments(getReference(sourceMaterial, "bioMaterial"), treatments);
                        }
                    }
                }
            }
        }
        return null;
    }


    /**
     * @param srcItem = mage:BioSource
     * @param tgtItem = genomic:Sample
     * extra genomic:Organism item is created and saved in  organismMap
     * @throws ObjectStoreException if problem occured during translating
     */
    protected Set translateSample(Item srcItem, Item tgtItem)
        throws ObjectStoreException {

        Set charItems = new HashSet();
        // TODO set interesting characteristic(s) to some attribute of sample - description?
        //      will need config to be passed in
        // TODO set identifier to be internal MAGE identifier?

        // PATH BioSource.characteristics
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
                        Item tgtCharItem = createItem(tgtNs + "SampleCharacteristic", "");
                        tgtCharItem.setAttribute("type", charItem.getAttribute("category").getValue());
                        tgtCharItem.setAttribute("value", charItem.getAttribute("value").getValue());
                        charItems.add(tgtCharItem);
                        list.add(tgtCharItem.getIdentifier());
                    }
                }
            }
            if (list.size() > 0) {
                ReferenceList tgtChar = new ReferenceList("characteristics", list);
                tgtItem.addCollection(tgtChar);
            }
        }

        // PATH BioSource.materialType
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

        return charItems;
    }


    /**
     * @param srcItem = mage:Treatment
     * @param tgtItem = flymine:Treatment
     * @throws ObjectStoreException if problem occured during translating
     */
    public void translateTreatment(Item srcItem, Item tgtItem)
        throws ObjectStoreException {

        // TODO protocol - either attribute of treatment or reference to object

        // PATH Treatment.action
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
    // call processSamples first to set MicroArrayResult.sample
    protected Set processMicroArrayResults() {
        Iterator resultIter = microArrayResults.iterator();
        while (resultIter.hasNext()) {
            Item maResult = (Item) resultIter.next();
            String maResultId = maResult.getIdentifier();
            if (resultToBioAssayData.containsKey(maResultId)) {
                String bioAssayDataId = (String) resultToBioAssayData.get(maResultId);
                if (bioAssayDataToAssay.containsKey(bioAssayDataId)) {
                    String assayId = (String) bioAssayDataToAssay.get(bioAssayDataId);
                    maResult.setReference("assay", assayId);
                    if (assayToSample.containsKey(assayId)) {
                        maResult.setReference("sample", (String) assayToSample.get(assayId));
                    }
                }
            }

            if (resultToFeature.containsKey(maResult.getIdentifier())) {
                String featureId = (String) resultToFeature.get(maResult.getIdentifier());
                if (featureToReporter.containsKey(featureId)) {
                    maResult.setReference("reporter", (String) featureToReporter.get(featureId));
                }
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

            LOG.error("sampleToLabeledExtract: " + sampleToLabeledExtract);
            LOG.error("labeledExtractToMeasuredBioAssay: " + labeledExtractToMeasuredBioAssay);
            LOG.error("measuredBioAssayToMicroArrayAssay: " + measuredBioAssayToMicroArrayAssay);

            if (sampleToLabeledExtract.containsKey(sampleId)) {
                String extractId = (String) sampleToLabeledExtract.get(sampleId);
                System.out.println("labeledExtract: " + extractId);
                if (labeledExtractToMeasuredBioAssay.containsKey(extractId)) {
                    String mbaId = (String) labeledExtractToMeasuredBioAssay.get(extractId);
                    System.out.println("mbaId: " + mbaId);
                    if (measuredBioAssayToMicroArrayAssay.containsKey(mbaId)) {
                        sample.setReference("assay", (String) measuredBioAssayToMicroArrayAssay.get(mbaId));
                        assayToSample.put((String) measuredBioAssayToMicroArrayAssay.get(mbaId), sampleId);
                    }
                }
            }
        }
        return samples;
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


    public static Map getPrefetchDescriptors() {
        Map paths = new HashMap();
        Set descSet;
        ItemPath path;
        String srcNs = "http://www.flymine.org/mage#";

        path = new ItemPath("Experiment.descriptions.bibliographicReferences", srcNs);
        paths.put(srcNs + "Experiment", new HashSet(Collections.singleton(path.getItemPrefetchDescriptor())));

        descSet = new HashSet();
        path = new ItemPath("MeasuredBioAssay.featureExtraction.physicalBioAssaySource.bioAssayCreation.sourceBioMaterialMeasurements.bioMaterial", srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        paths.put(srcNs + "MeasuredBioAssay", descSet);

        path = new ItemPath("DerivedBioAssay.derivedBioAssayMap.sourceBioAssays", srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        paths.put(srcNs + "DerivedBioAssay", descSet);




        descSet = new HashSet();
        path = new ItemPath("BioAssayDatum.quatitationType.scale", srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        paths.put(srcNs + "BioAssayDatum", descSet);

        descSet = new HashSet();
        path = new ItemPath("LabeledExtract.treatments.sourceBioMaterialMeasurements.bioMaterial", srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        path = new ItemPath("LabeledExtract.treatments.sourceBioMaterialMeasurements.bioMaterial.treatments.sourceBioMaterialMeasurements.bioMaterial", srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        paths.put(srcNs + "LabeledExtract", descSet);

        descSet = new HashSet();
        path = new ItemPath("BioSource.characteristics", srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        path = new ItemPath("BioSource.materialType", srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        paths.put(srcNs + "BioSource", descSet);

        path = new ItemPath("Treatment.action", srcNs);
        paths.put(srcNs + "Treatment", new HashSet(Collections.singleton(path.getItemPrefetchDescriptor())));

        descSet = new HashSet();
        path = new ItemPath("Reporter.featureReporterMaps.featureInformationSources", srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        path = new ItemPath("Reporter.controlType", srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        path = new ItemPath("Reporter.immobilizedCharacteristics.type", srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        paths.put(srcNs + "Reporter", descSet);

        return paths;
    }

}
