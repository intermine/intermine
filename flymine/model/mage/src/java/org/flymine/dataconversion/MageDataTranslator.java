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

import java.io.InputStream;
import java.io.IOException;
import java.util.Collection;
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
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.ItemPrefetchDescriptor;
import org.intermine.dataconversion.ItemPrefetchConstraintDynamic;
import org.intermine.dataconversion.ObjectStoreItemPathFollowingImpl;
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

    protected Map config = new HashMap();
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

    private Map experiments = new HashMap();

    protected Set microArrayResults = new HashSet();
    protected Set samples = new HashSet();
    protected Map samplesById = new HashMap();
    // keep track of Reporter identifiers that are controls to set MicroArrayResult.isContol
    protected Set controls = new HashSet();
    protected Map labeledExtractToMeasuredBioAssay = new HashMap();
    protected Map sampleToTreatments = new HashMap();
    protected Map sampleToLabeledExtracts = new HashMap();
    // genomic:MicroArrayResult identifier to genomic:MicroArrayAssay identifier
    protected Map resultToFeature = new HashMap();
    protected Map resultToReporter = new HashMap();
    protected Map featureToReporter = new HashMap();
    protected Map resultToBioAssay = new HashMap();
    protected Map assayToSamples = new HashMap();

    // geneomic:MicroArrayAssay -> experiment name
    protected Map assayToExpName = new HashMap();

    // geneomic:MicroArrayAssay -> genomic:MicroArrayExperiment
    protected Map assayToExperiment = new HashMap();

    // genomic:Sample -> genomic:SampleCharacteristics
    protected Map sampleToChars = new HashMap();
    protected Set assays = new HashSet();

    protected Map clones = new HashMap(); //cloneItem identifier, cloneItem
    protected Map cloneMap = new HashMap();//cloneIdentifier, cloneItem
    protected Map reporterToMaterial = new HashMap();
    protected Map cloneIds = new HashMap();//cloneItem identifier, alternative identifier
    protected Set materialIdTypes = new HashSet();
    protected Map expIdNames = new HashMap();

    /**
     * @see DataTranslator#DataTranslator
     */
    public MageDataTranslator(ItemReader srcItemReader, Properties mapping, Model srcModel,
                              Model tgtModel) throws Exception {
        super(srcItemReader, mapping, srcModel, tgtModel);

        srcNs = srcModel.getNameSpace().toString();
        tgtNs = tgtModel.getNameSpace().toString();

        readConfig();
        LOG.info(config);
        LOG.info("materialIdTypes: " + materialIdTypes);
    }


    /**
     * Read in a properties file with additional information about experiments.  Key is
     * the MAGE:Experiment.name, values are for e.g. a longer name and primary characteristic
     * type of samples.
     * @throws IOException if file not found
     */
    protected void readConfig() throws IOException {
        // create a map from experiment name to a map of config values
        String propertiesFileName = "mage_config.properties";
        InputStream is =
            MageDataTranslator.class.getClassLoader().getResourceAsStream(propertiesFileName);

        if (is == null) {
            throw new IllegalArgumentException("Cannot find " + propertiesFileName
                                               + " in the class path");
        }

        Properties properties = new Properties();
        properties.load(is);

        Iterator iter = properties.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            String exptName = key.substring(0, key.indexOf("."));
            String propName = key.substring(key.indexOf(".") + 1);

            addToMap(config, exptName, propName, value);

            // also set up a map of any expt.materialIdType config values found,
            // these need to be kept as possible alternative material ids
            if (propName.equals("materialIdType")) {
                materialIdTypes.add(value);
            }
        }
    }

    private void addToMap(Map config, String group, String key, String value) {
        Map exptConfig = (Map) config.get(group);
        if (exptConfig == null) {
            exptConfig = new HashMap();
            config.put(group, exptConfig);
        }
        exptConfig.put(key, value);
    }


    private String  getConfig(String exptName, String propName) {
        String value = null;
        Map exptConfig = (Map) config.get(exptName);
        if (exptConfig != null) {
            value = (String) exptConfig.get(propName);
        } else {
            LOG.warn("No config details found for experiment: " + exptName);
        }
        return value;
    }


    /**
     * @see DataTranslator#translate
     */
    public void translate(ItemWriter tgtItemWriter)
        throws ObjectStoreException, InterMineException {

        super.translate(tgtItemWriter);

        //LOG.error("materialToIdTypes: " + materialIdTypes);
        //LOG.error("expIdNames: " + expIdNames);
        //LOG.error("cloneIds: " + cloneIds);

        Iterator i;

        i = processOrganism().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        i = dbs.values().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        // needs to be called before other processXX methods
        i = processSamples().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        i = processMicroArrayResults().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        i = processMicroArrayAssays().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        i = clones.values().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }


    }


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
                // TODO publication data in file from FlyChip not correct, add pubmed id from
                // config?
                // TODO add the lab which ran the experiment
                // mage: BibliographicReference flymine:Publication
//                 if (className.equals("BibliographicReference")) {
//                     Set authors = createAuthors(srcItem);
//                     List authorIds = new ArrayList();
//                     Iterator j = authors.iterator();
//                     while (j.hasNext()) {
//                         Item author = (Item) j.next();
//                         authorIds.add(author.getIdentifier());
//                         result.add(author);
//                     }
//                     ReferenceList authorsRef = new ReferenceList("authors", authorIds);
//                     tgtItem.addCollection(authorsRef);
//                 } else
                if (className.equals("DataSource")) {
                    Attribute attr = srcItem.getAttribute("name");
                    if (attr != null) {
                        getDb(attr.getValue());
                    }
                    storeTgtItem = false;
                } else if (className.equals("Experiment")) {
                    translateMicroArrayExperiment(srcItem, tgtItem);
                } else if (className.equals("MeasuredBioAssay")) {
                    setLabeledExtractToMeasuredBioAssay(srcItem);
                    translateMicroArrayAssay(srcItem, tgtItem);
                    storeTgtItem = false;
                } else if (className.equals("BioAssayDatum")) {
                    translateMicroArrayResult(srcItem, tgtItem);
                    storeTgtItem = false;
                } else if (className.equals("Reporter")) {
                    translateReporter(srcItem, tgtItem);
                } else if (className.equals("BioSequence")) {
                    //translateBioEntity(srcItem, tgtItem);
                    storeTgtItem = false;
                } else if (className.equals("BioSource")) {
                    result.addAll(translateSample(srcItem, tgtItem));
                    storeTgtItem = false;
                } else if (className.equals("Treatment")) {
                    result.addAll(translateTreatment(srcItem, tgtItem));
                }

                if (storeTgtItem) {
                    result.add(tgtItem);
                }
            }
        } else if (className.equals("LabeledExtract")) {
            translateLabeledExtract(srcItem);
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
     * @return experiment Item
     * @throws ObjectStoreException if problem occured during translating
     */
    protected Item translateMicroArrayExperiment(Item srcItem, Item tgtItem)
        throws ObjectStoreException {

        String exptName = null;
        if (srcItem.hasAttribute("name")) {
            exptName = srcItem.getAttribute("name").getValue();
            String propName = getConfig(exptName, "experimentName");
            if (propName != null) {
                tgtItem.setAttribute("name", propName);
            }
        }

        // may have already created references to experiment
        tgtItem.setIdentifier(getExperimentId(exptName));

        expIdNames.put(tgtItem.getIdentifier(), exptName);


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
                        tgtItem.setAttribute("description", desItem.getAttribute("text")
                                             .getValue());
                        desFlag = true;
                    }
                }
// TODO publication data in mage files not very good - get pubmed id from config?
//                 if (desItem.hasCollection("bibliographicReferences")) {
//                     ReferenceList publication = desItem.getCollection(
//                                                   "bibliographicReferences");
//                     if (publication != null) {
//                         if (!isSingleElementCollection(publication)) {
//                             throw new IllegalArgumentException("Experiment description
//                                           collection ("
//                                 + desItem.getIdentifier()
//                                 + ") has more than one bibliographicReference");
//                         } else {
//                             if (pubFlag) {
//                                 LOG.error("Already set publication for MicroArrayExperiment, "
//                                       + " srcItem = " + srcItem.getIdentifier());
//                             } else {
//                                 tgtItem.setReference("publication", getFirstId(publication));
//                                 pubFlag = true;
//                             }
//                         }
//                     }
//                 }
            }
        }

        // PATH Experiment.bioAssays

        // create map from mage:DerivedBioAssay to experiment name (String)
        if (srcItem.hasCollection("bioAssays")) {
            Iterator assayIter = getCollection(srcItem, "bioAssays");
            while (assayIter.hasNext()) {
                Item bioAssayItem = (Item) assayIter.next();
                if (bioAssayItem.getClassName().equals(srcNs + "MeasuredBioAssay")) {
                    assayToExperiment.put(bioAssayItem.getIdentifier(), tgtItem.getIdentifier());
                    assayToExpName.put(bioAssayItem.getIdentifier(), exptName);
                }
            }
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

         if (srcItem.hasAttribute("identifier")) {
             tgtItem.addAttribute(new Attribute("name",
                     srcItem.getAttribute("identifier").getValue()));
         }
         assays.add(tgtItem);
     }


    /**
     * @param srcItem = mage:MeasuredBioAssay
     * @throws ObjectStoreException if anything goes wrong
     */
    protected void setLabeledExtractToMeasuredBioAssay(Item srcItem)
        throws ObjectStoreException {

        // PATH MeasuredBioAssay.featureExtraction.physicalBioAssaySource
        // .bioAssayCreation.sourceBioMaterialMeasurements.bioMaterial


        // set up map of MicroArrayAssay to LabeledExtract - to be used when setting
        // link between MicroArrayAssay and Sample

        // MeasuredBioAssay.featureExtraction.physicalBioAssaySource -> PhysicalBioAssay

        Item pbaItem = null;
        if (srcItem.hasReference("featureExtraction")) {
            Item feItem = getReference(srcItem, "featureExtraction");
            if (feItem.hasReference("physicalBioAssaySource")) {
                pbaItem = getReference(feItem, "physicalBioAssaySource");
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
                         // map from mage:LabeledExtract identifier to
                         // genomic:MicroArrayAssay identifier
                         labeledExtractToMeasuredBioAssay.put(
                                 bmItem.getReference("bioMaterial").getRefId(),
                                 srcItem.getIdentifier());
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

        if (srcItem.hasAttribute("value")) {
            String value = srcItem.getAttribute("value").getValue();
            if (StringUtil.allDigits(value)) {
                tgtItem.setAttribute("value", value);
                // only store if a numerical value, ignore errors
                microArrayResults.add(tgtItem);
            }
        }

        // TODO need to set this at the end when we can relate to correct Experiment
        //tgtItem.setReference("analysis", getExperimentId());


        // PATH BioAssayDatum.quatitationType.scale
        if (srcItem.hasReference("bioAssay")) {
            tgtItem.setReference("assay", srcItem.getReference("bioAssay").getRefId());
        }

        if (srcItem.hasReference("designElement")) {
            // map from genomic:MicroArrayResult identifier to mage:Feature identifier
            resultToFeature.put(tgtItem.getIdentifier(),
                         srcItem.getReference("designElement").getRefId());
        }

        if (srcItem.hasReference("reporter")) {
            // map from genomic:MicroArrayResult identifier to mage:Reporter identifier
            //LOG.error("resultToReporter.put(" + tgtItem.getIdentifier() + ", "
            //             + srcItem.getReference("reporter").getRefId() + ")");
            resultToReporter.put(tgtItem.getIdentifier(),
                         srcItem.getReference("reporter").getRefId());
        }

        if (srcItem.hasReference("quantitationType")) {
            Item qtItem = getReference(srcItem, "quantitationType");

            if (qtItem.hasAttribute("name")) {
                tgtItem.setAttribute("type", "(Normalised) "
                                  + qtItem.getAttribute("name").getValue());
            } else {
                LOG.warn("QuantitationType ( " + qtItem.getIdentifier()
                          + " ) does not have name attribute");
            }
            if (qtItem.getClassName().endsWith("MeasuredSignal")
                || qtItem.getClassName().endsWith("DerivedSignal")
                || qtItem.getClassName().endsWith("Ratio")
                || qtItem.getClassName().endsWith("SpecializedQuantitationType")) {
                if (qtItem.hasReference("scale")) {
                    Item oeItem = getReference(qtItem, "scale");

                    tgtItem.setAttribute("scale", oeItem.getAttribute("value").getValue());
                } else {
                    LOG.warn("QuantitationType (" + qtItem.getIdentifier()
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
                        LOG.warn("QuantitationType (" + msItem.getIdentifier()
                                  + "( does not have scale attribute ");
                    }
                }
            }
        }

        // map from mage:MeasuredBioAssayData identifier to genomic:MicroArrayResult identifier
        // duplicates collection available in MeasureBioAssayData but is much better for
        // prefetch and memory useage
        if (srcItem.hasReference("bioAssay")) {
            String bioAssayId = srcItem.getReference("bioAssay").getRefId();
            resultToBioAssay.put(tgtItem.getIdentifier(), bioAssayId);
        }
    }

    /**
     * @param srcItem = mage:Reporter
     * @param tgtItem = flymine:Reporter
     * @throws ObjectStoreException if problem occured during translating
     */
    protected void translateReporter(Item srcItem, Item tgtItem) throws ObjectStoreException {

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
                            featureToReporter.put(fis.getReference("feature").getRefId(),
                                                  srcItem.getIdentifier());
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
            // will be used to set MicroArrayResult.isControl
            controls.add(tgtItem.getIdentifier());
        } else {
            tgtItem.setAttribute("isControl", "false");
            // Reporter.immobilizedCharacteristics.type
            // if Reporter.controlTypes exists then is a control

            if (srcItem.hasCollection("immobilizedCharacteristics")) {
                Iterator bioIter = getCollection(srcItem, "immobilizedCharacteristics");
                while (bioIter.hasNext() && material == null) {
                    Item bioSequence = (Item) bioIter.next();
                    if (bioSequence.hasReference("type")) {
                        String type = getReference(bioSequence, "type").getAttribute("value")
                                      .getValue();
                        if (type.toLowerCase().equals("cdna_clone")) {
                            String cloneId = srcItem.getAttribute("name").getValue();
                            material = (Item) cloneMap.get(cloneId);
                            if (material == null) {
                                material = createItem(tgtNs + "CDNAClone", "");
                                material.setAttribute("identifier", cloneId);
                                cloneMap.put(cloneId, material);
                                clones.put(material.getIdentifier(), material);
                            }
                            tgtItem.setReference("material", material.getIdentifier());
                            reporterToMaterial.put(tgtItem.getIdentifier(),
                                               material.getIdentifier());
                        } else {
                            throw new ObjectStoreException("Unknown BioSequence type: " + type);
                        }
                    }
                    if (!materialIdTypes.isEmpty()
                        && bioSequence.hasCollection("sequenceDatabases")) {
                        Iterator dbIter = getCollection(bioSequence, "sequenceDatabases");
                        while (dbIter.hasNext()) {
                            Item dbRef = (Item) dbIter.next();
                            if (dbRef.hasReference("database")) {
                                Item db = getReference(dbRef, "database");
                                String dbName = db.getAttribute("name").getValue();
                                if (materialIdTypes.contains(dbName)) {
                                    Map altIds = (Map) cloneIds.get(material.getIdentifier());
                                    if (altIds == null) {
                                        altIds = new HashMap();
                                        cloneIds.put(material.getIdentifier(), altIds);
                                    }
                                    altIds.put(dbName, dbRef.getAttribute("accession").getValue());
                                }
                            }
                        }
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
     * @throws ObjectStoreException if problem occured during translating
     * LabeledExtract -> {treatments} -> {sourceBioMaterialMeasurements} ->
     *(BioSample)extract -> {treatments} -> {sourceBioMaterialMeasurements} ->
     *(BioSample)not-extract -> {treatments} -> {sourceBioMaterialMeasurements} ->
     *(BioSource)
     */
    public void translateLabeledExtract(Item srcItem)
        throws ObjectStoreException {

        String sampleId = searchTreatments(srcItem, new ArrayList());
        // map from sample to top level LabeledExtract
        if (sampleId != null) {
            Set extracts = (Set) sampleToLabeledExtracts.get(sampleId);
            if (extracts == null) {
                extracts = new HashSet();
                sampleToLabeledExtracts.put(sampleId, extracts);
            }
            extracts.add(srcItem.getIdentifier());
        }
    }


    /**
     * For a given BioMaterial iterate through treatments applied and add to a collection.
     * Recurse into source BioMaterials and add their treatments.
     * @param bioMaterial = item bioMaterial
     * @param treatments = list treatments
     * @return string of treatments
     * @throws ObjectStoreException if anything goes wrong
     */
    protected String searchTreatments(Item bioMaterial, List treatments)
        throws ObjectStoreException {
        // TODO check if BioSource (genomic:Sample) and create map from sample to top
        // level LabeledExtract.  Sample needs collection of treatments.

        // LabeledExtract.treatments.sourceBioMaterialMeasurements.bioMaterial
        //.treatments.sourceBioMaterialMeasurements.bioMaterial.
        //               [Treatment]    [BioMaterialMeasurement]   [BioSample]
        //[Treatment]  [BioMaterialMeasurement]    [BioSample]

        // PATH is recursive - duplicate a number of times?  Refactor easier prefetch
        // PATH LabeledExtract.treatments.sourceBioMaterialMeasurements.bioMaterial
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
                            return searchTreatments(getReference(sourceMaterial, "bioMaterial"),
                                                    treatments);
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
     * @return set of SampleCharacteristic
     * extra genomic:Organism item is created and saved in  organismMap
     * @throws ObjectStoreException if problem occured during translating
     */
    protected Set translateSample(Item srcItem, Item tgtItem)
        throws ObjectStoreException {

        Set charItems = new HashSet();
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
                    String value = charItem.getAttribute("value").getValue();
                    if (category.equals("Organism")) {
                        if (charItem.hasAttribute("value")) {
                            organism = createOrganism("Organism", "", value);
                            tgtItem.setReference("organism", organism.getIdentifier());
                        }
                    } else {
                        Item tgtCharItem = createItem(tgtNs + "SampleCharacteristic", "");
                        tgtCharItem.setAttribute("type", charItem.getAttribute("category")
                                                 .getValue());
                        tgtCharItem.setAttribute("value", value);
                        charItems.add(tgtCharItem);
                        list.add(tgtCharItem.getIdentifier());
                    }
                    HashMap charMap = new HashMap();
                    addToMap(sampleToChars, tgtItem.getIdentifier(), category, value);
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

        samplesById.put(tgtItem.getIdentifier(), tgtItem);
        samples.add(tgtItem);

        return charItems;
    }


    /**
     * @param srcItem = mage:Treatment
     * @param tgtItem = flymine:Treatment
     * @return set of target TreatmentParameter
     * @throws ObjectStoreException if problem occured during translating
     */
    public Set translateTreatment(Item srcItem, Item tgtItem)
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

        Set params = new HashSet();
        List paramIds = new ArrayList();

        // Protocol:  Treatment.protocolApplications.protocol
        if (srcItem.hasCollection("protocolApplications")) {
            Iterator protIter = getCollection(srcItem, "protocolApplications");
            while (protIter.hasNext()) {
                Item appItem = (Item) protIter.next();
                if (tgtItem.hasReference("protocol")) {
                    throw new IllegalArgumentException(
                          "Treatment has more than one ProtocolApplication: "
                          + srcItem.getAttribute("name").getValue()
                          + ", " + srcItem.getIdentifier());
                }
                if (appItem.hasReference("protocol")) {
                    tgtItem.setReference("protocol",
                            appItem.getReference("protocol").getRefId());
                }

                if (appItem.hasCollection("parameterValues")) {
                    Iterator paramIter = getCollection(appItem, "parameterValues");
                    while (paramIter.hasNext()) {
                        Item valueItem = (Item) paramIter.next();
                        Item tgtParam = createItem(tgtNs + "TreatmentParameter", "");
                        tgtParam.setReference("treatment", tgtItem.getIdentifier());
                        if (valueItem.hasAttribute("value")) {
                            tgtParam.setAttribute("value",
                                     valueItem.getAttribute("value").getValue());
                        }

                        Item srcParam = getReference(valueItem, "parameterType");
                        if (srcParam.hasAttribute("name")) {
                            tgtParam.setAttribute("type",
                                     srcParam.getAttribute("name").getValue());
                        }


                        // set units
                        if (srcParam.hasReference("defaultValue")) {
                            Item defaultItem = getReference(srcParam, "defaultValue");
                            if (defaultItem.hasReference("measurement")) {
                                Item measItem = getReference(defaultItem, "measurement");
                                if (measItem.hasReference("unit")) {
                                    Item unitItem = getReference(measItem, "unit");
                                    if (unitItem.hasAttribute("unitNameCV")) {
                                        tgtParam.setAttribute("unit",
                                                 unitItem.getAttribute("unitNameCV").getValue());
                                    }
                                }
                            }
                        }
                        paramIds.add(tgtParam.getIdentifier());
                        params.add(tgtParam);
                    }
                }
            }
        }
        return params;
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

   /**
     * got map fo assays
     * add experiment reference and sample1, sample2 attribute
     * @return assay only once for the same item
     */
    protected Set processMicroArrayAssays() {
        //        LOG.error ("assayToSamples " + assayToSamples);
        Iterator assayIter = assays.iterator();
        while (assayIter.hasNext()) {
            Item assay = (Item) assayIter.next();
            String assayId = assay.getIdentifier();
            if (assayToExperiment.containsKey(assayId)) {
                assay.setReference("experiment", (String) assayToExperiment.get(assayId));
            }

            if (assayToSamples.containsKey(assayId)) {
                List sampleIds = (List) assayToSamples.get(assayId);
                assay.addCollection(new ReferenceList("samples", sampleIds));

                if (sampleIds.size() > 0) {
                    String summary =  getSampleSummary((String) sampleIds.get(0));
                    if (summary != null) {
                        assay.setAttribute("sample1", summary);
                    }
                }
                if (sampleIds.size() > 1) {
                    String summary =  getSampleSummary((String) sampleIds.get(1));
                    if (summary != null) {
                        assay.setAttribute("sample2", summary);
                    }
                }
            }
        }
        return assays;
    }

    /**
     * @param id sample id
     * @return sample attributes as summary
     */
    private String getSampleSummary(String id) {
        Item sample = (Item) samplesById.get(id);
        String summary = "";
        if (sample != null
            && sample.getAttribute("primaryCharacteristicType") != null
            && sample.getAttribute("primaryCharacteristic") != null) {
            return sample.getAttribute("primaryCharacteristicType").getValue()
            + ": " + sample.getAttribute("primaryCharacteristic").getValue();
        }
        return null;
    }

    /**
     * set MicroArrayResult.assay
     * call processSamples first to set MicroArrayResult.sample
     * @return micorArrayResult
     */
    protected Set processMicroArrayResults() {
        Iterator resultIter = microArrayResults.iterator();
        while (resultIter.hasNext()) {
            Item maResult = (Item) resultIter.next();
            String maResultId = maResult.getIdentifier();
            String experimentId = null;


            // MicroArrayResult.assay
            // MicroArrayResult.samples

            //should be result2bioassay
            if (resultToBioAssay.containsKey(maResultId)) {
                String assayId = (String) resultToBioAssay.get(maResultId);
                // assay reference should already be set
                // maResult.setReference("assay", assayId);
                if (assayToSamples.containsKey(assayId)) {
                    maResult.addCollection(new ReferenceList("samples",
                            (List) assayToSamples.get(assayId)));
                }
                if (assayToExperiment.containsKey(assayId)) {
                    experimentId = (String) assayToExperiment.get(assayId);
                    //LOG.error("experimentId: " + experimentId);
                    maResult.setReference("experiment", experimentId);
                }
            }

            // MicroArrayResult.isControl
            String reporterId = null;
            if (resultToReporter.containsKey(maResult.getIdentifier())) {
                reporterId = (String) resultToReporter.get(maResult.getIdentifier());
            } else if (resultToFeature.containsKey(maResult.getIdentifier())) {
                String featureId = (String) resultToFeature.get(maResult.getIdentifier());
                if (featureToReporter.containsKey(featureId)) {
                    reporterId = (String)  featureToReporter.get(featureId);
                }
            }

            //LOG.error("reportedId: " + reporterId);
            if (reporterId != null) {
                maResult.setReference("reporter", reporterId);
                if (controls.contains(reporterId)) {
                    maResult.setAttribute("isControl", "true");
                } else {
                    maResult.setAttribute("isControl", "false");
                }
                // MicroArrayResult.material
                if (reporterToMaterial.containsKey(reporterId)) {
                    String materialId = (String) reporterToMaterial.get(reporterId);
                    maResult.setReference("material", materialId);


                    // for some experiments we want to change the material identifier for
                    // an alternative database reference defined in the config.  Alternatives
                    // are in cloneIds map - material->alternative id
                    String expName = (String) expIdNames.get(experimentId);
                    String materialIdType = getConfig(expName, "materialIdType");
                    //LOG.info("materialId, expName: " + materialId + ", " + expName);

                    if (materialIdType != null && cloneIds.containsKey(materialId)) {
                        Map typeMap = (Map) cloneIds.get(materialId);
                        if (typeMap != null) {
                            if (typeMap.containsKey(materialIdType)) {
                                Item clone = (Item) clones.get(materialId);
                                if (clone != null) {
                                    clone.setAttribute("identifier",
                                          (String) typeMap.get(materialIdType));
                                }
                            }
                        }
                    }
                }
            }
        }
        return microArrayResults;
    }

    /**
     * set Sample.assay
     * set Sample.treatments
     * @return sample
     */
    protected Set processSamples() {
        //LOG.error("sampleToLabeledExtracts " + sampleToLabeledExtracts);
        //LOG.error("labeledExtractToMeasuredBioAssay " + labeledExtractToMeasuredBioAssay);
        Iterator sampleIter = samples.iterator();
        while (sampleIter.hasNext()) {
            Item sample = (Item) sampleIter.next();
            String sampleId = sample.getIdentifier();

            if (sampleToTreatments.containsKey(sampleId)) {
                sample.addCollection(new ReferenceList("treatments",
                       (List) sampleToTreatments.get(sampleId)));
            }

            if (sampleToLabeledExtracts.containsKey(sampleId)) {
                Iterator extractIter = ((Set) sampleToLabeledExtracts.get(sampleId)).iterator();
                while (extractIter.hasNext()) {
                    String extractId = (String) extractIter.next();
                    if (labeledExtractToMeasuredBioAssay.containsKey(extractId)) {
                        String mbaId = (String) labeledExtractToMeasuredBioAssay.get(extractId);
                        List sampleIds = (List) assayToSamples.get(mbaId);
                        if (sampleIds == null) {
                            sampleIds = new ArrayList();
                            assayToSamples.put(mbaId, sampleIds);
                        }
                        sampleIds.add(sampleId);

                        String expName = (String) assayToExpName.get(mbaId);
                        String primaryCharacteristic = getConfig(expName, "primaryCharacteristic");

                        Map chars = (Map) sampleToChars.get(sampleId);
                        if (chars != null) {
                            Iterator charIter = chars.entrySet().iterator();
                            while (charIter.hasNext()) {
                                Map.Entry entry = (Map.Entry) charIter.next();
                                if (entry.getKey().equals(primaryCharacteristic)) {
                                    sample.setAttribute("primaryCharacteristicType",
                                           primaryCharacteristic);
                                    sample.setAttribute("primaryCharacteristic",
                                           (String) entry.getValue());
                                }
                            }
                        }
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
            db = createItem(tgtNs + "DataSource", "");
            Attribute title = new Attribute("name", dbName);
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
    private String getExperimentId(String expName) {
        Item exp = (Item) experiments.get(expName);
        if (exp == null) {
            exp = createItem(tgtNs + "MicroArrayExperiment", "");
            experiments.put(expName, exp);
        }
        return exp.getIdentifier();
    }

    /**
     * get an item by path and deal with conversion to/from fulldata items
     * @param path = ItemPath
     * @param startItem = Item
     * @return item
     * @throws ObjectStoreException if anything goes wrong with finding item
     */
    private Item getItemByPath(ItemPath path, Item startItem)
        throws ObjectStoreException {
        return ItemHelper.convert(srcItemReader.getItemByPath(path,
               ItemHelper.convert(startItem)));
    }

    /**
     * get an item by path and deal with conversion to/from fulldata items
     * @param path = ItemPath
     * @param start = Item
     * @return item
     * @throws ObjectStoreException if anything goes wrong with finding item
     */
    public Iterator getItemsByPath(ItemPath path, Item start)
        throws ObjectStoreException {
        List items = new ArrayList();
        Iterator iter = srcItemReader.getItemsByPath(path,
                   ItemHelper.convert(start)).iterator();
        while (iter.hasNext()) {
            items.add(ItemHelper.convert(
                  (org.intermine.model.fulldata.Item) iter.next()));
        }
        return items.iterator();
    }

    /**
     * static method
     * @return map of prefetchDescriptors
     */
    public static Map getPrefetchDescriptors() {
        Map paths = new HashMap();
        Set descSet;
        ItemPath path;
        String srcNs = "http://www.flymine.org/model/mage#";
        LOG.info("srcNs: " + srcNs);

        descSet = new HashSet();
        path = new ItemPath("Experiment.descriptions", srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        path = new ItemPath("Experiment.bioAssays", srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        paths.put(srcNs + "Experiment", descSet);

        descSet = new HashSet();
        path = new ItemPath("MeasuredBioAssay.featureExtraction.physicalBioAssaySource."
                            + "bioAssayCreation.sourceBioMaterialMeasurements.bioMaterial",
                            srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        paths.put(srcNs + "MeasuredBioAssay", descSet);


        path = new ItemPath("DerivedBioAssay.derivedBioAssayMap.sourceBioAssays", srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        paths.put(srcNs + "DerivedBioAssay", descSet);


        descSet = new HashSet();
        path = new ItemPath("BioAssayDatum.quantitationType.scale", srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        path = new ItemPath("BioAssayDatum.quantitationType.targetQuantitationType.scale",
                            srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        paths.put(srcNs + "BioAssayDatum", descSet);
        //prefetch cache miss?
        descSet = new HashSet();
        path = new ItemPath("LabeledExtract.treatments.sourceBioMaterialMeasurements.bioMaterial"
                            , srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        path = new ItemPath("LabeledExtract.treatments.sourceBioMaterialMeasurements.bioMaterial."
                            + "treatments.sourceBioMaterialMeasurements.bioMaterial", srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        paths.put(srcNs + "LabeledExtract", descSet);

        descSet = new HashSet();
        path = new ItemPath("BioSource.characteristics", srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        path = new ItemPath("BioSource.materialType", srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        paths.put(srcNs + "BioSource", descSet);

        descSet = new HashSet();
        path = new ItemPath("Treatment.action", srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        path = new ItemPath("Treatment.protocolApplications.protocol", srcNs);
        descSet.add(path.getItemPrefetchDescriptor());
        path = new ItemPath(
               "Treatment.protocolApplications.parameterValues.parameterType.defaultValue"
               + ".measurement.unit",
               srcNs);
        descSet.add(path.getItemPrefetchDescriptor());

        paths.put(srcNs + "Treatment", descSet);


        ItemPrefetchDescriptor desc, desc1, desc2, desc3, desc4;

        descSet = new HashSet();

        desc = new ItemPrefetchDescriptor("Reporter.featureReporterMaps");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("featureReporterMaps",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2 = new ItemPrefetchDescriptor("Reporter.featureReporterMaps.feature");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("feature",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        descSet.add(desc);

        desc = new ItemPrefetchDescriptor("Reporter.immobilizedCharacteristics");
        desc.addConstraint(new ItemPrefetchConstraintDynamic("immobilizedCharacteristics",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2 = new ItemPrefetchDescriptor("Reporter.immobilizedCharacteristics.type");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("type",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc.addPath(desc2);
        desc3 = new ItemPrefetchDescriptor(
                    "Reporter.immobilizedCharacteristics.type.sequenceDatabases");
        desc3.addConstraint(new ItemPrefetchConstraintDynamic("sequenceDatabases",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc2.addPath(desc3);
        desc4 = new ItemPrefetchDescriptor(
                "Reporter.immobilizedCharacteristics.type.sequenceDatabases.database");
        desc4.addConstraint(new ItemPrefetchConstraintDynamic("database",
                    ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc3.addPath(desc4);
        descSet.add(desc);

        paths.put(srcNs + "Reporter", descSet);

        //path = new ItemPath("Reporter.featureReporterMaps.featureInformationSources", srcNs);
//         path = new ItemPath(
//               "Reporter.featureReporterMaps.featureInformationSources.feature", srcNs);
//         descSet.add(path.getItemPrefetchDescriptor());
//         path = new ItemPath("Reporter.controlType", srcNs);
//         descSet.add(path.getItemPrefetchDescriptor());
//         path = new ItemPath("Reporter.immobilizedCharacteristics.type", srcNs);
//         descSet.add(path.getItemPrefetchDescriptor());
//         path = new ItemPath(
//               "Reporter.immobilizedCharacteristics.type.sequenceDatabases.database", srcNs);
//         descSet.add(path.getItemPrefetchDescriptor());
//         paths.put(srcNs + "Reporter", descSet);

        return paths;
    }

}
