package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2004 FlyMine
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
import java.util.StringTokenizer;
import java.io.File;
import java.io.FileReader;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import org.intermine.InterMineException;
import org.intermine.util.XmlUtil;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;

import org.intermine.dataconversion.ItemReader;
import org.intermine.dataconversion.ObjectStoreItemReader;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ObjectStoreItemWriter;
import org.intermine.dataconversion.ObjectStoreItemPathFollowingImpl;
import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.ItemPrefetchDescriptor;
import org.intermine.dataconversion.ItemPrefetchConstraintDynamic;
import org.intermine.dataconversion.FieldNameAndValue;

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
    protected Map gene2BioEntity = new HashMap();

    //mage: Feature id -> flymine:MicroArrayExperimentResult id when processing BioAssayDatum
    protected Map maer2Feature = new HashMap();
    protected Map feature2Maer = new HashMap();
    protected Set maerSet = new HashSet();
    //flymine: BioEntity id -> mage:Feature id when processing Reporter
    protected Map bioEntity2Feature = new HashMap();
    protected Map bioEntity2IdentifierMap = new HashMap();
    protected Set bioEntitySet = new HashSet();
    protected Set geneSet = new HashSet();
    protected Map synonymMap = new HashMap(); //key:itemId value:synonymItem
    protected Map synonymAccessionMap = new HashMap();//key:accession value:synonymItem
    protected Set synonymAccession = new HashSet();

    protected Map identifier2BioEntity = new HashMap();
    protected Map organismDbId2Gene = new HashMap();

    protected Map treatment2BioSourceMap = new HashMap();
    protected Set bioSource = new HashSet();

    /**
     * @see DataTranslator#DataTranslator
     */
    public MageDataTranslator(ItemReader srcItemReader, OntModel model, String ns) {
        super(srcItemReader, model, ns);
    }

    /**
     * @see DataTranslator#translate
     */
    public void translate(ItemWriter tgtItemWriter)
        throws ObjectStoreException, InterMineException {
        super.translate(tgtItemWriter);

        Iterator i = processReporterLocs().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        i = processBioEntity2MAEResult().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        i = processGene2MAEResult().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        i = processBioSourceTreatment().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

    }

    /**
     * @see DataTranslator#translateItem
     */
    protected Collection translateItem(Item srcItem)
        throws ObjectStoreException, InterMineException {

        Collection result = new HashSet();
        String normalised = null;
        String srcNs = XmlUtil.getNamespaceFromURI(srcItem.getClassName());
        String className = XmlUtil.getFragmentFromURI(srcItem.getClassName());
        if (className.equals("BioAssayDatum")) {
            normalised = srcItem.getAttribute("normalised").getValue();
            srcItem = removeNormalisedAttribute(srcItem);
        }

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
                } else if (className.equals("FeatureReporterMap")) {
                     setReporterLocationCoords(srcItem, tgtItem);
                     storeTgtItem = false;
                } else if (className.equals("PhysicalArrayDesign")) {
                    createFeatureMap(srcItem);
                    translateMicroArraySlideDesign(srcItem, tgtItem);
                } else if (className.equals("Experiment")) {
                    // collection bioassays includes MeasuredBioAssay, PhysicalBioAssay
                    // and DerivedBioAssay, only keep DerivedBioAssay
                    keepDBA(srcItem, tgtItem, srcNs);
                    translateMicroArrayExperiment(srcItem, tgtItem);
                } else if (className.equals("DerivedBioAssay")) {
                    translateMicroArrayAssay(srcItem, tgtItem);
                } else if (className.equals("BioAssayDatum")) {
                    translateMicroArrayExperimentalResult(srcItem, tgtItem, normalised);
                } else if (className.equals("Reporter")) {
                    setBioEntityMap(srcItem, tgtItem);
                } else if (className.equals("BioSequence")) {
                    translateBioEntity(srcItem, tgtItem);
                    storeTgtItem = false;
                } else if (className.equals("LabeledExtract")) {
                    translateLabeledExtract(srcItem, tgtItem);
                } else if (className.equals("BioSource")) {
                    organism = translateSample(srcItem, tgtItem);
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
     * @param srcItem = mage: FeatureReporterMap
     * @param tgtItem = flymine: ReporterLocation
     * @throws ObjectStoreException if problem occured during translating
     */
    protected void setReporterLocationCoords(Item srcItem, Item tgtItem)
        throws ObjectStoreException {
        if (srcItem.hasCollection("featureInformationSources")) {
            ReferenceList featureInfos = srcItem.getCollection("featureInformationSources");

            if (!isSingleElementCollection(featureInfos)) {
                LOG.error("FeatureReporterMap (" + srcItem.getIdentifier()
                        + ") does not have single element collection"
                        + featureInfos.getRefIds().toString());
               // throw new IllegalArgumentException("FeatureReporterMap ("
               //            + srcItem.getIdentifier()
              //             + " has more than one featureInformationSource");
            }

            //FeatureInformationSources->FeatureInformation->Feature->FeatureLocation
            //can't do prefetch from FeatureReporterMap to featureInformationSources
            Item featureInfo = ItemHelper.convert(srcItemReader
                        .getItemById((String) featureInfos.getRefIds().get(0)));

            if (featureInfo.hasReference("feature")) {
                Item feature = ItemHelper.convert(srcItemReader
                          .getItemById(featureInfo.getReference("feature").getRefId()));
                if (feature.hasReference("featureLocation")) {
                    Item featureLoc = ItemHelper.convert(srcItemReader
                          .getItemById(feature.getReference("featureLocation").getRefId()));
                    if (featureLoc != null) {
                        tgtItem.addAttribute(new Attribute("localX",
                                   featureLoc.getAttribute("column").getValue()));
                        tgtItem.addAttribute(new Attribute("localY",
                                   featureLoc.getAttribute("row").getValue()));
                    }
                }
                if (feature.hasReference("zone")) {
                    Item zone = ItemHelper.convert(srcItemReader
                                   .getItemById(feature.getReference("zone").getRefId()));

                    if (zone != null) {
                        tgtItem.addAttribute(new Attribute("zoneX",
                                   zone.getAttribute("column").getValue()));
                        tgtItem.addAttribute(new Attribute("zoneY",
                                   zone.getAttribute("row").getValue()));
                    }
                }
                // to set MicroArraySlideDesign <-> ReporterLocation reference
                // need to hold on to ReporterLocations and their feature ids
                // until end of processing
                reporterLocs.add(tgtItem);
                rlToFeature.put(tgtItem.getIdentifier(), feature.getIdentifier());
            }
        } else {

            LOG.error("FeatureReporterMap (" + srcItem.getIdentifier()
                        + ") does not have featureInformationSource");
            // throw new IllegalArgumentException("FeatureReporterMap ("
            //            + srcItem.getIdentifier()
            //            + " does not have featureInformationSource");

        }
    }

    /**
     * @param srcItem = mage:PhysicalArrayDesign
     * @throws ObjectStoreException if problem occured during translating
     */
    protected void createFeatureMap(Item srcItem)
        throws ObjectStoreException {
        if (srcItem.hasCollection("featureGroups")) {
            ReferenceList featureGroups = srcItem.getCollection("featureGroups");
            if (featureGroups == null || !isSingleElementCollection(featureGroups)) {
                throw new IllegalArgumentException("PhysicalArrayDesign (" + srcItem.getIdentifier()
                        + ") does not have exactly one featureGroup");
            }
            //ItemPrefetchDescriptor desc1
            Item featureGroup = ItemHelper.convert(srcItemReader
                  .getItemById((String) featureGroups.getRefIds().get(0)));
            Iterator featureIter = featureGroup.getCollection("features").getRefIds().iterator();
            while (featureIter.hasNext()) {
                featureToDesign.put((String) featureIter.next(), srcItem.getIdentifier());
            }
        }
    }

    /**
     * @param maer2FeatureMap and
     * @param maerSet
     * both created during translating BioAssayDatum to MicroArrayExperimentalResult
     * iterator through maerSet to create feature2Maer
     * @return feature2Maer map
     */
    protected Map createFeature2MaerMap(Map maer2FeatureMap, Set maerSet) {

        for (Iterator i = maerSet.iterator(); i.hasNext(); ) {
            String maer = (String) i.next();
            String feature = (String) maer2FeatureMap.get(maer);
            if (feature2Maer.containsKey(feature)) {
                String multiMaer = ((String) feature2Maer.get(feature)).concat(" " + maer);
                feature2Maer.put(feature, multiMaer);
            } else {
                feature2Maer.put(feature, maer);
            }
        }
        LOG.debug("feature2maer " + feature2Maer.toString());
        return feature2Maer;

    }

   /**
     * @param srcItem = mage:Reporter
     * @param tgtItem = flymine:Reporter
     * set BioEntity2FeatureMap when translating Reporter
     * set BioEntity2IdentifierMap when translating Reporter
     * @throws ObjectStoreException if errors occured during translating
     */
    protected void setBioEntityMap(Item srcItem, Item tgtItem)
        throws ObjectStoreException {

        StringBuffer sb = new StringBuffer();
        boolean controlFlg = false;
        ReferenceList failTypes = new ReferenceList();
        if (srcItem.hasCollection("failTypes")) {
            failTypes = srcItem.getCollection("failTypes");
            if (failTypes != null) {
                for (Iterator l = srcItem.getCollection("failTypes").getRefIds().iterator();
                     l.hasNext();) {
                    Item ontoItem = ItemHelper.convert(srcItemReader.getItemById(
                                                       (String) l.next()));
                    sb.append(ontoItem.getAttribute("value").getValue() + " ");
                }
                tgtItem.addAttribute(new Attribute("failType", sb.toString()));
            }

        }

        if (srcItem.hasReference("controlType")) {
            Reference controlType = srcItem.getReference("controlType");
            if (controlType != null) {
                Item controlItem = ItemHelper.convert(srcItemReader.getItemById(
                                     (String) srcItem.getReference("controlType").getRefId()));
                if (controlItem.getAttribute("value").getValue().equals("control_buffer")) {
                    controlFlg = true;
                }
            }
        }


        ReferenceList featureReporterMaps = srcItem.getCollection("featureReporterMaps");
        ReferenceList immobilizedChar = srcItem.getCollection("immobilizedCharacteristics");

        sb = new StringBuffer();
        String identifier = null;

        if (immobilizedChar == null) {
            if (failTypes != null) {
                //throw new IllegalArgumentException(
                LOG.info("Reporter ("
                        + srcItem.getIdentifier()
                        + ") does not have immobilizedCharacteristics because it fails");
            } else if (controlFlg) {
                LOG.info("Reporter ("
                        + srcItem.getIdentifier()
                        + ") does not have immobilizedCharacteristics because"
                        + " it is a control_buffer");
            } else {
                throw new IllegalArgumentException("Reporter ("
                        + srcItem.getIdentifier()
                        + ") does not have immobilizedCharacteristics");
            }

        } else if (!isSingleElementCollection(immobilizedChar)) {
            throw new IllegalArgumentException("Reporter ("
                        + srcItem.getIdentifier()
                        + ") have more than one immobilizedCharacteristics");

        } else {
            //create bioEntity2IdentifierMap
            //identifier = reporter: name for CDNAClone, Vector
            //identifier = reporter: controlType;name;descriptions for genomic_dna

            identifier = (String) immobilizedChar.getRefIds().get(0);
            String identifierAttribute = null;
            Item bioSequence = ItemHelper.convert(srcItemReader.getItemById(identifier));
            if (bioSequence.hasReference("type")) {
                Item oeItem = ItemHelper.convert(srcItemReader.getItemById(
                                    (String) bioSequence.getReference("type").getRefId()));
                if (oeItem.getAttribute("value").getValue().equals("genomic_DNA")) {
                    sb = new StringBuffer();
                    if (srcItem.hasReference("controlType")) {
                        Item controlItem = ItemHelper.convert(srcItemReader.getItemById((String)
                                           srcItem.getReference("controlType").getRefId()));
                        if (controlItem.hasAttribute("value")) {
                            sb.append(controlItem.getAttribute("value").getValue() + ";");
                        }
                    }
                    if (srcItem.hasAttribute("name")) {
                        sb.append(srcItem.getAttribute("name").getValue() + ";");
                    }
                    if (srcItem.hasCollection("descriptions")) {
                        for (Iterator k = srcItem.getCollection(
                                          "descriptions").getRefIds().iterator(); k.hasNext();) {
                            Item description = ItemHelper.convert(srcItemReader.getItemById(
                                                (String) k.next()));
                            if (description.hasAttribute("text")) {
                                sb.append(description.getAttribute("text").getValue() + ";");
                            }
                        }
                    }

                    if (sb.length() > 1) {
                        identifierAttribute = sb.substring(0, sb.length() - 1);
                        bioEntity2IdentifierMap.put(identifier, identifierAttribute);
                    }
                } else {
                    sb = new StringBuffer();
                    if (srcItem.hasAttribute("name")) {
                        sb.append(srcItem.getAttribute("name").getValue());
                        identifierAttribute = sb.toString();
                        bioEntity2IdentifierMap.put(identifier, identifierAttribute);

                    }
                }
            }
            //create bioEntity2FeatureMap
            sb = new StringBuffer();
            if (featureReporterMaps != null) {
                for (Iterator i = featureReporterMaps.getRefIds().iterator(); i.hasNext(); ) {
                    //FeatureReporterMap //desc2
                    String s = (String) i.next();
                    Item frm = ItemHelper.convert(srcItemReader.getItemById(s));
                    if (frm.hasCollection("featureInformationSources")) {
                        Iterator j = frm.getCollection("featureInformationSources").
                                   getRefIds().iterator();
                        //can't prefetch from FeatureReporterMap get featureInformationSources
                        while (j.hasNext()) {
                            Item fis = ItemHelper.convert(srcItemReader.getItemById(
                                       (String) j.next()));
                            if (fis.hasReference("feature")) {
                                sb.append(fis.getReference("feature").getRefId() + " ");
                            }
                        }
                    }
                }

                if (sb.length() > 1) {
                    bioEntity2Feature.put(identifier, sb.substring(0, sb.length() - 1));
                }
                LOG.debug("bioEntity2Feature" + bioEntity2Feature.toString());

                //identifier =  (String) immobilizedChar.getRefIds().get(0);
                tgtItem.addReference(new Reference("material", identifier));

            }
        }

    }


    /**
     * @param srcItem = mage:PhysicalArrayDesign
     * @param tgtItem = flymine:MicroArraySlideDesign
     * @throws ObjectStoreException if problem occured during translating
     */
    protected void translateMicroArraySlideDesign(Item srcItem, Item tgtItem)
        throws ObjectStoreException {
        // move descriptions reference list
        if (srcItem.hasCollection("descriptions")) {
            ReferenceList des =  srcItem.getCollection("descriptions");
            if (des != null) {
                for (Iterator i = des.getRefIds().iterator(); i.hasNext(); ) {
                    Item anno = ItemHelper.convert(srcItemReader.getItemById((String) i.next()));
                    if (anno != null) {
                        promoteCollection(srcItem, "descriptions", "annotations",
                                          tgtItem, "descriptions");
                    }
                }
            }
        }

        // change substrateType reference to attribute
        if (srcItem.hasReference("surfaceType")) {
            Item surfaceType = ItemHelper.convert(srcItemReader
                                .getItemById(srcItem.getReference("surfaceType").getRefId()));
            if (surfaceType.hasAttribute("value")) {
                tgtItem.addAttribute(new Attribute("surfaceType",
                                               surfaceType.getAttribute("value").getValue()));

            }
        }

        if (srcItem.hasAttribute("version")) {
            tgtItem.addAttribute(new Attribute("version",
                                               srcItem.getAttribute("version").getValue()));

         }

        if (srcItem.hasAttribute("name")) {
            tgtItem.addAttribute(new Attribute("name",
                                               srcItem.getAttribute("name").getValue()));

         }

    }

    /**
     * @param srcItem = mage:Experiment
     * @param tgtItem = flymine: MicroArrayExperiment
     * @param srcNs = mage: src namespace
     * @throws ObjectStoreException if problem occured during translating
     */
    protected void keepDBA(Item srcItem, Item tgtItem, String srcNs)
        throws ObjectStoreException {
        ReferenceList rl = srcItem.getCollection("bioAssays");
        ReferenceList newRl = new ReferenceList();
        newRl.setName("assays");

        //can't prefetch from Experiment to bioAssays
        if (rl != null) {
            for (Iterator i = rl.getRefIds().iterator(); i.hasNext(); ) {
                Item baItem = ItemHelper.convert(srcItemReader.getItemById((String) i.next()));
                if (baItem.getClassName().equals(srcNs + "DerivedBioAssay")) {
                    newRl.addRefId(baItem.getIdentifier());
                }
            }
            tgtItem.addCollection(newRl);
        }
    }

    /**
     * @param srcItem = mage:Experiment
     * @param tgtItem = flymine: MicroArrayExperiment
     * @throws ObjectStoreException if problem occured during translating
     */
    protected void translateMicroArrayExperiment(Item srcItem, Item tgtItem)
        throws ObjectStoreException {
        if (srcItem.hasAttribute("name")) {
            tgtItem.addAttribute(new Attribute("name", srcItem.getAttribute("name").getValue()));
        }
        //can't prefetch from Experiment get descriptions
        ReferenceList desRl = srcItem.getCollection("descriptions");
        boolean desFlag = false;
        boolean pubFlag = false;
        for (Iterator i = desRl.getRefIds().iterator(); i.hasNext(); ) {
            Item desItem = ItemHelper.convert(srcItemReader.getItemById((String) i.next()));
            if (desItem != null) {
                if (desItem.hasAttribute("text")) {
                    if (desFlag) {
                        LOG.error("Already set description for MicroArrayExperiment, "
                                  + " srcItem = " + srcItem.getIdentifier());
                    } else {
                        tgtItem.addAttribute(new Attribute("description",
                                  desItem.getAttribute("text").getValue()));
                        desFlag = true;
                    }
                }

                ReferenceList publication = desItem.getCollection("bibliographicReferences");
                if (publication != null) {
                    if (!isSingleElementCollection(publication)) {
                        throw new IllegalArgumentException("Experiment description collection ("
                                + desItem.getIdentifier()
                                + ") has more than one bibliographicReferences");
                    } else {
                        if (pubFlag) {
                            LOG.error("Already set publication for MicroArrayExperiment, "
                                      + " srcItem = " + srcItem.getIdentifier());
                        } else {
                            tgtItem.addReference(new Reference("publication",
                                      (String) publication.getRefIds().get(0)));
                            pubFlag = true;
                        }
                    }
                }
            }
        }
    }

    /**
     * @param srcItem = mage: DerivedBioAssay
     * @param tgtItem = flymine:MicroArrayAssay
     * @throws ObjectStoreException if problem occured during translating
     */
     protected void translateMicroArrayAssay(Item srcItem, Item tgtItem)
         throws ObjectStoreException {
         ReferenceList dbad = srcItem.getCollection("derivedBioAssayData");
         if (dbad != null) {
             for (Iterator j = dbad.getRefIds().iterator(); j.hasNext(); ) {
                 //can't prefetch from DerivedBioAssay get derivedBioAssayData
                 Item dbadItem = ItemHelper.convert(srcItemReader.getItemById((String) j.next()));
                 if (dbadItem.hasReference("bioDataValues")) {
                     Item bioDataTuples = ItemHelper.convert(srcItemReader.getItemById(
                              dbadItem.getReference("bioDataValues").getRefId()));
                     if (bioDataTuples.hasCollection("bioAssayTupleData")) {
                         ReferenceList rl = bioDataTuples.getCollection("bioAssayTupleData");
                         ReferenceList resultsRl = new ReferenceList();
                         resultsRl.setName("results");
                         for (Iterator i = rl.getRefIds().iterator(); i.hasNext(); ) {
                             resultsRl.addRefId((String) i.next());
                         }
                         tgtItem.addCollection(resultsRl);
                     }
                 }
             }
         }
     }

    /**
     * @param srcItem = mage:BioAssayDatum
     * @param tgtItem = flymine:MicroArrayExperimentalResult
     * @param normalised is defined in translateItem
     * @throws ObjectStoreException if problem occured during translating
     */
    public void translateMicroArrayExperimentalResult(Item srcItem, Item tgtItem, String normalised)
        throws ObjectStoreException {
        tgtItem.addAttribute(new Attribute("normalised", normalised));

        //create maer2Feature map, and maer set
        if (srcItem.hasReference("designElement")) {
            maer2Feature.put(tgtItem.getIdentifier(),
                         srcItem.getReference("designElement").getRefId());
            maerSet.add(tgtItem.getIdentifier());
        }

        if (srcItem.hasReference("quantitationType")) {
            Item qtItem = ItemHelper.convert(srcItemReader.getItemById(
                            srcItem.getReference("quantitationType").getRefId()));

            if (qtItem.getClassName().endsWith("MeasuredSignal")
                || qtItem.getClassName().endsWith("Ratio")) {
                if (qtItem.hasAttribute("name")) {
                    tgtItem.addAttribute(new Attribute("type",
                                                   qtItem.getAttribute("name").getValue()));
                } else {
                    LOG.error("srcItem ( " + qtItem.getIdentifier()
                          + " ) does not have name attribute");
                }
                if (qtItem.hasReference("scale")) {
                    Item oeItem = ItemHelper.convert(srcItemReader.getItemById(
                                  qtItem.getReference("scale").getRefId()));

                    tgtItem.addAttribute(new Attribute("scale",
                                  oeItem.getAttribute("value").getValue()));
                } else {
                    LOG.error("srcItem (" + qtItem.getIdentifier()
                              + "( does not have scale attribute ");
                }
                if (qtItem.hasAttribute("isBackground")) {
                    tgtItem.addAttribute(new Attribute("isBackground",
                               qtItem.getAttribute("isBackground").getValue()));
                } else {
                    LOG.error("srcItem (" + qtItem.getIdentifier()
                              + "( does not have scale reference ");
                }
            } else if (qtItem.getClassName().endsWith("Error")) {
                 if (qtItem.hasAttribute("name")) {
                     tgtItem.addAttribute(new Attribute("type",
                                                   qtItem.getAttribute("name").getValue()));
                 } else {
                     LOG.error("srcItem ( " + qtItem.getIdentifier()
                          + " ) does not have name attribute");
                 }
                if (qtItem.hasReference("targetQuantitationType")) {
                    Item msItem = ItemHelper.convert(srcItemReader.getItemById(
                                qtItem.getReference("targetQuantitationType").getRefId()));
                    if (msItem.hasReference("scale")) {
                        Item oeItem = ItemHelper.convert(srcItemReader.getItemById(
                                  msItem.getReference("scale").getRefId()));

                        tgtItem.addAttribute(new Attribute("scale",
                                  oeItem.getAttribute("value").getValue()));
                    } else {
                        LOG.error("srcItem (" + msItem.getIdentifier()
                                  + "( does not have scale attribute ");
                    }
                    if (msItem.hasAttribute("isBackground")) {
                        tgtItem.addAttribute(new Attribute("isBackground",
                                        msItem.getAttribute("isBackground").getValue()));
                    } else {
                        LOG.error("srcItem (" + msItem.getIdentifier()
                                  + "( does not have scale reference ");
                    }
                }
            }
        }

    }

    /**
     * @param srcItem = mage:BioSequence
     * @param tgtItem = flymine:BioEntity(genomic_DNA =>NuclearDNA cDNA_clone=>CDNAClone,
     *                  vector=>Vector)
     * extra will create for Gene(FBgn), Vector(FBmc) and Synonym(embl)
     * synonymMap(itemid, item), synonymAccessionMap(accession, item) and
     * synonymAccession (HashSet) created for flymine:Synonym,
     * geneList include Gene and Vector to reprocess to add mAER collection
     * bioEntityList include CDNAClone and NuclearDNA to add identifier attribute
     * and mAER collection
     * @throws ObjectStoreException if problem occured during translating
     */
    protected void translateBioEntity(Item srcItem, Item tgtItem)
        throws ObjectStoreException {
        Item gene = new Item();
        Item vector = new Item();
        Item synonym = new Item();
        String s = null;
        String identifier = null;

        if (srcItem.hasReference("type")) {
            Item item = ItemHelper.convert(srcItemReader.getItemById(
                               srcItem.getReference("type").getRefId()));
            if (item.hasAttribute("value")) {
                s = item.getAttribute("value").getValue();
                if (s.equals("genomic_DNA")) {
                   tgtItem.setClassName(tgtNs + "NuclearDNA");
                } else if (s.equals("cDNA_clone")) {
                    tgtItem.setClassName(tgtNs + "CDNAClone");
                } else if (s.equals("vector")) {
                     tgtItem.setClassName(tgtNs + "Vector");
                } else {
                    tgtItem = null;
                }
            }
        }

        //can't prefetch From BioSequence get sequenceDatabases
        if (srcItem.hasCollection("sequenceDatabases")) {
            ReferenceList rl = srcItem.getCollection("sequenceDatabases");
            identifier = null;
            List geneList = new ArrayList();
            List emblList = new ArrayList();
            for (Iterator i = rl.getRefIds().iterator(); i.hasNext(); ) {
                Item dbEntryItem = ItemHelper.convert(srcItemReader.
                                       getItemById((String) i.next()));
                if (dbEntryItem.hasReference("database")) {
                    Item dbItem =  ItemHelper.convert(srcItemReader.getItemById(
                                (String) dbEntryItem.getReference("database").getRefId()));
                    String synonymSourceId = dbItem.getIdentifier();
                    if (dbItem.hasAttribute("name")) {
                        String dbName = dbItem.getAttribute("name").getValue();
                        String organismDbId = dbEntryItem.getAttribute("accession").getValue();
                        if (dbName.equals("flybase") && organismDbId.startsWith("FBgn")) {
                            gene = createGene(tgtNs + "Gene", "", dbEntryItem.getIdentifier(),
                                      organismDbId);
                            geneList.add(dbItem.getIdentifier());
                            // } else if (dbName.equals("flybase")
                            // && organismDbId.startsWith("FBmc")) {
                            // tgtItem.addAttribute(new Attribute("organismDbId", organismDbId));

                        } else if (dbName.equals("embl") && dbEntryItem.hasAttribute("accession")) {
                            String accession = dbEntryItem.getAttribute("accession").getValue();
                            //make sure synonym only create once for same accession
                            if (!synonymAccession.contains(accession)) {
                                emblList.add(dbEntryItem.getIdentifier());
                                synonym = createSynonym(dbEntryItem, synonymSourceId,
                                                             srcItem.getIdentifier());
                                synonymAccession.add(accession); //Set
                                synonymMap.put(dbEntryItem.getIdentifier(), synonym);
                                synonymAccessionMap.put(accession, synonym);
                            }
                            // result.add(synonym);
                        } else {
                            //getDatabaseRef();

                        }
                    }
                }
            }
            if (!geneList.isEmpty()) {
                geneSet.add(gene);
                gene2BioEntity.put(gene.getIdentifier(), srcItem.getIdentifier());
            }

            if (!emblList.isEmpty()) {
                ReferenceList synonymEmblRl = new ReferenceList("synonyms", emblList);
                tgtItem.addCollection(synonymEmblRl);
            }

        }
        if (tgtItem != null) {
            bioEntitySet.add(tgtItem);
        }

    }

    /**
     * @param srcItem = databaseEntry item refed in BioSequence
     * @param sourceId = database id
     * @param subjectId = bioEntity identifier will probably be changed when reprocessing bioEntitySet
     * @return synonym item
     */
    protected Item createSynonym(Item srcItem, String sourceId, String subjectId) {
        Item synonym = new Item();
        synonym.setClassName(tgtNs + "Synonym");
        synonym.setIdentifier(srcItem.getIdentifier());
        synonym.setImplementations("");
        synonym.addAttribute(new Attribute("type", "accession"));
        synonym.addReference(new Reference("source", sourceId));


        synonym.addAttribute(new Attribute("value",
                                 srcItem.getAttribute("accession").getValue()));

        synonym.addReference(new Reference("subject", subjectId));

        return synonym;

    }


    /**
     * @param srcItem = mage:LabeledExtract
     * @param tgtItem = flymine:LabeledExtract
     * @throws ObjectStoreException if problem occured during translating
     */
    public void translateLabeledExtract(Item srcItem, Item tgtItem)
        throws ObjectStoreException {

        if (srcItem.hasReference("materialType")) {
            Item type = ItemHelper.convert(srcItemReader.getItemById(
                  (String) srcItem.getReference("materialType").getRefId()));
            tgtItem.addAttribute(new Attribute("materialType",
                   type.getAttribute("value").getValue()));
        }

        ReferenceList labels = srcItem.getCollection("labels");
        if (labels == null || !isSingleElementCollection(labels)) {
            throw new IllegalArgumentException("LabeledExtract (" + srcItem.getIdentifier()
                        + " does not have exactly one label");
        }
        //can't prefetch labels from LabeledExtract
        Item label = ItemHelper.convert(srcItemReader
                        .getItemById((String) labels.getRefIds().get(0)));
        tgtItem.addAttribute(new Attribute("label", label.getAttribute("name").getValue()));

        //can't prefetch treatments from LabeledExtract
        ReferenceList treatments = srcItem.getCollection("treatments");
        List treatmentList = new ArrayList();
        ReferenceList tgtTreatments = new ReferenceList("treatments", treatmentList);
        String sampleId = null;
        StringBuffer sb = new StringBuffer();
        if (treatments != null) {
            for (Iterator i = treatments.getRefIds().iterator(); i.hasNext(); ) {
                String refId = (String) i.next();
                treatmentList.add(refId);
                Item treatmentItem = ItemHelper.convert(srcItemReader.getItemById(refId));
                if (treatmentItem.hasCollection("sourceBioMaterialMeasurements")) {
                    ReferenceList sourceRl = treatmentItem.getCollection(
                        "sourceBioMaterialMeasurements");
                    for (Iterator j = sourceRl.getRefIds().iterator(); j.hasNext(); ) {
                        //bioMaterialMeausrement
                        Item bioMMItem = ItemHelper.convert(srcItemReader.getItemById(
                                       (String) j.next()));
                        if (bioMMItem.hasReference("bioMaterial")) {
                            Item bioSample = ItemHelper.convert(srcItemReader.getItemById(
                                  (String) bioMMItem.getReference("bioMaterial").getRefId()));
                            if (bioSample.hasCollection("treatments")) {
                                ReferenceList bioSampleTreatments = bioSample.getCollection(
                                              "treatments");
                                for (Iterator k = bioSampleTreatments.getRefIds().iterator();
                                     k.hasNext();) {
                                    refId = (String) k.next();
                                    treatmentList.add(refId);
                                    //create treatment2BioSourceMap
                                    Item treatItem = ItemHelper.convert(srcItemReader.
                                                     getItemById(refId));
                                    sampleId = createTreatment2BioSourceMap(treatItem);
                                    sb.append(sampleId + " ");
                                }
                            }
                        }
                    }
                }
            }
            tgtItem.addCollection(tgtTreatments);

            StringTokenizer st = new StringTokenizer(sb.toString());
            while (st.hasMoreTokens()) {
                String s = st.nextToken();
                if (!s.equals(sampleId)) {
                    throw new IllegalArgumentException ("LabeledExtract ("
                        + srcItem.getIdentifier()
                        + " does not have exactly one reference to sample");
                }
            }
            tgtItem.addReference(new Reference("sample", sampleId));
        }
    }

    /**
     * @param srcItem = mage:BioSource
     * @param tgtItem = flymine:Sample
     * @return flymine:Organism
     * @throws ObjectStoreException if problem occured during translating
     */
    protected Item translateSample(Item srcItem, Item tgtItem)
        throws ObjectStoreException {
        ReferenceList characteristics = srcItem.getCollection("characteristics");
        List list = new ArrayList();
        Item organism = new Item();
        String s = null;
        if (characteristics != null) {
            for (Iterator i = characteristics.getRefIds().iterator(); i.hasNext();) {
                String id = (String) i.next();
                //can't prefetch characteristics from BioSource
                Item charItem = ItemHelper.convert(srcItemReader.getItemById(id));
                s = charItem.getAttribute("category").getValue();
                if (s.equalsIgnoreCase("organism")) {
                    organism = createOrganism(tgtNs + "Organism", "",
                        charItem.getAttribute("value").getValue());
                    tgtItem.addReference(new Reference("organism", organism.getIdentifier()));

                } else {
                    list.add(id);
                }
            }
            ReferenceList tgtChar = new ReferenceList("characteristics", list);
            tgtItem.addCollection(tgtChar);

        }

        if (srcItem.hasReference("materialType")) {
            Item type = ItemHelper.convert(srcItemReader.getItemById(
                 (String) srcItem.getReference("materialType").getRefId()));
            tgtItem.addAttribute(new Attribute("materialType",
                  type.getAttribute("value").getValue()));
/**
     * @param className = tgtClassName
     * @param implementation = tgtClass implementation
     * @param value = attribute for organism name
     * @return organism item
     */        }

        if (srcItem.hasAttribute("name")) {
            tgtItem.addAttribute(new Attribute("name", srcItem.getAttribute("name").getValue()));
        }

        bioSource.add(tgtItem);
        return organism;

    }

    /**
     * @param srcItem = mage:Treatment
     * @param tgtItem = flymine:Treatment
     * @throws ObjectStoreException if problem occured during translating
     */
    public void translateTreatment(Item srcItem, Item tgtItem)
        throws ObjectStoreException {
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
     * @param srcItem = mage:Treatment from BioSample<Extract>
     * @return string of bioSourceId
     * @throws ObjectStoreException if problem occured during translating
     * method called when processing LabeledExtract
     */
    protected String createTreatment2BioSourceMap(Item srcItem)
        throws ObjectStoreException {
        StringBuffer bioSourceId = new StringBuffer();
        StringBuffer treatment = new StringBuffer();
        String id = null;
        if (srcItem.hasCollection("sourceBioMaterialMeasurements")) {
            ReferenceList sourceRl1 = srcItem.getCollection("sourceBioMaterialMeasurements");
            for (Iterator l = sourceRl1.getRefIds().iterator(); l.hasNext(); ) {
                // bioSampleItem, type extract
                //can't prefetch sourceBioMaterialMeausrements from Treatment
                Item bioSampleExItem = ItemHelper.convert(srcItemReader.getItemById(
                                      (String) l.next()));

                if (bioSampleExItem.hasReference("bioMaterial")) {
                    // bioSampleItem, type not-extract
                    Item bioSampleItem = ItemHelper.convert(srcItemReader.getItemById(
                            (String) bioSampleExItem.getReference("bioMaterial").getRefId()));
                    if (bioSampleItem.hasCollection("treatments")) {
                        ReferenceList bioSourceRl = bioSampleItem.getCollection("treatments");

                        for (Iterator m = bioSourceRl.getRefIds().iterator(); m.hasNext();) {
                            String treatmentList = (String) m.next();
                            treatment.append(treatmentList + " ");
                            Item bioSourceTreatmentItem = ItemHelper.convert(srcItemReader.
                                         getItemById(treatmentList));
                            if (bioSourceTreatmentItem.hasCollection(
                                     "sourceBioMaterialMeasurements")) {
                                ReferenceList sbmmRl = bioSourceTreatmentItem.getCollection(
                                           "sourceBioMaterialMeasurements");
                                for (Iterator n = sbmmRl.getRefIds().iterator(); n.hasNext();) {
                                    Item bmm = ItemHelper.convert(srcItemReader.getItemById(
                                            (String) n.next()));
                                    if (bmm.hasReference("bioMaterial")) {
                                        id = (String) bmm.getReference("bioMaterial").
                                            getRefId();
                                        bioSourceId.append(id + " ");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        StringTokenizer st = new StringTokenizer(bioSourceId.toString());
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            if (!s.equals(id)) {
                throw new IllegalArgumentException("LabeledExtract (" + srcItem.getIdentifier()
                        + " does not have exactly one reference to sample");
            }
        }

        treatment2BioSourceMap.put(id, treatment.toString());

        return id;
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
            List treatList = new ArrayList();
            String s = (String) treatment2BioSourceMap.get((String) bioSourceItem.getIdentifier());
            LOG.debug("treatmentList " + s + " for " + bioSourceItem.getIdentifier());
            if (s != null) {
                StringTokenizer st = new StringTokenizer(s);
                while (st.hasMoreTokens()) {
                    treatList.add(st.nextToken());
                }
            }

            ReferenceList treatments = new ReferenceList("treatments", treatList);
            bioSourceItem.addCollection(treatments);
            results.add(bioSourceItem);
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
     * BioAssayDatum  mage:FeatureId -> flymine:MAEResultId (MicroArrayExperimentalResults)
     * Reporter flymine: BioEntityId -> mage:FeatureId
     * BioSequece  BioEntityId -> BioEntity Item
     *                         -> extra Gene Item
     * @return add microExperimentalResults collection to BioEntity item
     * and add identifier attribute to BioEntity
     */
    protected Set processBioEntity2MAEResult() {
        Set results = new HashSet();
        Set entitySet = new HashSet();
        feature2Maer = new HashMap();
        feature2Maer = createFeature2MaerMap(maer2Feature, maerSet);
        String s = null;
        String itemId = null;
        String identifierAttribute = null;
        Set identifierSet = new HashSet();

        for (Iterator i = bioEntitySet.iterator(); i.hasNext();) {
            Item bioEntity = (Item) i.next();

            // add collection microArrayExperimentalResult
            List maerIds = new ArrayList();
            s = (String) bioEntity2Feature.get((String) bioEntity.getIdentifier());
            LOG.debug("featureId " + s + " bioEntityId " + bioEntity.getIdentifier());
            if (s != null) {
                StringTokenizer st = new StringTokenizer(s);
                String multiMaer = null;
                while (st.hasMoreTokens()) {
                    multiMaer = (String) feature2Maer.get(st.nextToken());
                    if (multiMaer != null) {
                        StringTokenizer token = new StringTokenizer(multiMaer);
                        while (token.hasMoreTokens()) {
                            maerIds.add(token.nextToken());
                        }
                    }
                }

                ReferenceList maerRl = new ReferenceList("microArrayExperimentalResults", maerIds);
                bioEntity.addCollection(maerRl);
                entitySet.add(bioEntity);
            }
        }

        for (Iterator i = entitySet.iterator(); i.hasNext();) {
            Item bioEntity = (Item) i.next();

            //add attribute identifier for bioEntity
            identifierAttribute = (String) bioEntity2IdentifierMap.get(
                                              (String) bioEntity.getIdentifier());
            if (identifier2BioEntity.containsKey(identifierAttribute)) {
                Item anotherEntity = (Item) identifier2BioEntity.get(identifierAttribute);
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
                anotherEntity.addCollection(new ReferenceList("synonyms", synonymList));

                List maerList = new ArrayList();
                if (anotherEntity.hasCollection("microArrayExperimentalResults")) {

                    ReferenceList maerList1 = anotherEntity.getCollection(
                                                "microArrayExperimentalResults");
                    for (Iterator j = maerList1.getRefIds().iterator(); j.hasNext();) {
                        String refId = (String) j.next();
                        maerList.add(refId);
                    }
                }

                if (bioEntity.hasCollection("microArrayExperimentalResults")) {
                    ReferenceList maerList2 = bioEntity.getCollection(
                                                 "microArrayExperimentalResults");
                    for (Iterator j = maerList2.getRefIds().iterator(); j.hasNext();) {
                        String refId = (String) j.next();
                        if (!maerList.contains(refId)) {
                            maerList.add(refId);
                        }
                    }
                }
                anotherEntity.addCollection(new ReferenceList("microArrayExperimentalResults",
                                          maerList));

                identifier2BioEntity.put(identifierAttribute, anotherEntity);

            } else {
                bioEntity.addAttribute(new Attribute("identifier", identifierAttribute));
                identifier2BioEntity.put(identifierAttribute, bioEntity);
                identifierSet.add(identifierAttribute);
            }

        }

        for (Iterator i = identifierSet.iterator(); i.hasNext();) {
            Item bioEntity = (Item) identifier2BioEntity.get((String) i.next());
            results.add(bioEntity);

        }
        for (Iterator i = synonymAccession.iterator(); i.hasNext();) {
            Item synonym = (Item) synonymAccessionMap.get((String) i.next());
            results.add(synonym);

        }

        return results;
    }

    /**
     * BioAssayDatum  mage:FeatureId -> flymine:MAEResultId (MicroArrayExperimentalResult)
     * Reporter flymine: BioEntityId -> mage:FeatureId
     * BioSequece  BioEntityId -> BioEntity Item
     *                         -> extra Gene Item
     * @return add microExperimentalResult collection to Gene item
     */

    protected Set processGene2MAEResult() {
        Set results = new HashSet();
        Set destGene = new HashSet();
        Set organismDbSet = new HashSet();

        feature2Maer = new HashMap();
        Map organismDbId2Gene = new HashMap();

        feature2Maer = createFeature2MaerMap(maer2Feature, maerSet);
        String organismDbId = null;

        for (Iterator i = geneSet.iterator(); i.hasNext();) {
            Item gene = (Item) i.next();
            List maerIds = new ArrayList();
            String geneId = (String) gene.getIdentifier();
            String bioEntityId = (String) gene2BioEntity.get(geneId);
            String s = (String) bioEntity2Feature.get(bioEntityId);
            LOG.debug("featureId " + s + " bioEntityId " + bioEntityId + " geneId " + geneId);
            if (s != null) {
                StringTokenizer st = new StringTokenizer(s);
                String multiMaer = null;
                while (st.hasMoreTokens()) {
                    multiMaer = (String) feature2Maer.get(st.nextToken());
                    if (multiMaer != null) {
                        StringTokenizer token = new StringTokenizer(multiMaer);
                        while (token.hasMoreTokens()) {
                            maerIds.add(token.nextToken());
                        }
                    }
                }

                ReferenceList maerRl = new ReferenceList("microArrayExperimentalResults", maerIds);
                gene.addCollection(maerRl);
                destGene.add(gene);
            }
        }

        for (Iterator i = destGene.iterator(); i.hasNext();) {
            Item gene = (Item) i.next();
            organismDbId = gene.getAttribute("organismDbId").getValue();
            if (organismDbId2Gene.containsKey(organismDbId)) {
                Item anotherGene = (Item)  organismDbId2Gene.get(organismDbId);
                List maerList = new ArrayList();
                if (anotherGene.hasCollection("microArrayExperimentalResults")) {
                    ReferenceList maerList1 = anotherGene.getCollection(
                             "microArrayExperimentalResults");
                    for (Iterator j = maerList1.getRefIds().iterator(); j.hasNext();) {
                        String refId = (String) j.next();
                        maerList.add(refId);
                    }
                }

                if (gene.hasCollection("microArrayExperimentalResults")) {
                    ReferenceList maerList2 = gene.getCollection("microArrayExperimentalResults");
                    for (Iterator j = maerList2.getRefIds().iterator(); j.hasNext();) {
                        String refId = (String) j.next();
                        if (!maerList.contains(refId)) {
                            maerList.add(refId);
                        }
                    }
                }
                anotherGene.addCollection(new ReferenceList("microArrayExperimentalResults",
                             maerList));
                organismDbId2Gene.put(organismDbId, anotherGene);
            } else {
                organismDbId2Gene.put(organismDbId, gene);
                organismDbSet.add(organismDbId);
            }

        }

        for (Iterator i = organismDbSet.iterator(); i.hasNext();) {
            Item gene = (Item) organismDbId2Gene.get((String) i.next());
            results.add(gene);
        }
        return results;

    }


    /**
     * normalised attribute is added during MageConverter
     * converting BioAssayDatum
     * true for Derived BioAssayData
     * false for Measured BioAssayData
     * removed this attribute before translateItem
     */
    private Item removeNormalisedAttribute(Item item) {
        Item newItem = new Item();
        newItem.setClassName(item.getClassName());
        newItem.setIdentifier(item.getIdentifier());
        newItem.setImplementations(item.getImplementations());
        Iterator i = item.getAttributes().iterator();
        while (i.hasNext()) {
            Attribute attr = (Attribute) i.next();
            if (!attr.getName().equals("normalised")) {
                newItem.addAttribute(attr);
            }
        }
        i = item.getReferences().iterator();
        while (i.hasNext()) {
            newItem.addReference((Reference) i.next());
        }
        i = item.getCollections().iterator();
        while (i.hasNext()) {
            newItem.addCollection((ReferenceList) i.next());
        }
        return newItem;
    }

    /**
     * @param col is ReferenceList
     * check if it is single element collection in ReferenceList
     * @return true if yes
     */
    private boolean isSingleElementCollection(ReferenceList col) {
        return (col.getRefIds().size() == 1);
    }

    /**
     * @param className = tgtClassName
     * @param implementation = tgtClass implementation
     * @param identifier = gene item identifier from database item identifier
     * @param organismDbId = attribute for gene organismDbId
     * @return gene item
     */
    private Item createGene(String className, String implementation, String identifier,
            String organismDbId) {
        Item gene = new Item();
        gene = createItem(className, implementation);
        gene.setIdentifier(identifier);
        gene.addAttribute(new Attribute("organismDbId", organismDbId));
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
        organism = createItem(className, implementation);
        organism.addAttribute(new Attribute("name", value));
        return organism;
    }

    /**
     * main method
     * @param args command line arguments
     * @throws Exception if something goes wrong
     */
    public static void main (String[] args) throws Exception {
        String srcOsName = args[0];
        String tgtOswName = args[1];
        String modelName = args[2];
        String format = args[3];
        String namespace = args[4];

        Map paths = new HashMap();
        HashSet descSet = new HashSet();

        ItemPrefetchDescriptor desc1 = new ItemPrefetchDescriptor(
                "(FeatureGroup <- Feature.featureGroup)");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic(
                                      ObjectStoreItemPathFollowingImpl.IDENTIFIER,
                                      "featureGroup"));
        desc1.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    "http://www.flymine.org/model/mage#Feature", false));
        paths.put("http://www.flymine.org/model/mage#FeatureGroup", Collections.singleton(desc1));


        desc1 = new ItemPrefetchDescriptor(
              "(Reporter <- FeatureReporterMap.reporter)");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic(
                                       ObjectStoreItemPathFollowingImpl.IDENTIFIER, "reporter"));
        desc1.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME,
                    "http://www.flymine.org/model/mage#FeatureReporterMap", false));
        paths.put("http://www.flymine.org/model/mage#Reporter", Collections.singleton(desc1));


        desc1 = new ItemPrefetchDescriptor("PhysicalArrayDesign.surfaceType");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("surfaceType",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        paths.put("http://www.flymine.org/model/mage#PhysicalArrayDesign",
                   Collections.singleton(desc1));

        desc1 = new ItemPrefetchDescriptor("BioAssayDatum.quantitationType");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("quantitationType",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));

        ItemPrefetchDescriptor desc2 = new ItemPrefetchDescriptor(
                 "(BioAssayDatum.quantitationType).scale");
        desc2.addConstraint(new ItemPrefetchConstraintDynamic("scale",
                                                 ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        desc1.addPath(desc2);
        descSet.add(desc1);

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


        desc1 = new ItemPrefetchDescriptor("BioSequence.type");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("type",
                                     ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        paths.put("http://www.flymine.org/model/mage#BioSequence",
                   Collections.singleton(desc1));

        desc1 = new ItemPrefetchDescriptor("LabeledExtract.materialType");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("materialType",
                                     ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        paths.put("http://www.flymine.org/model/mage#LabeledExtract",
                   Collections.singleton(desc1));

        desc1 = new ItemPrefetchDescriptor("BioSource.materialType");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic("materialType",
                                     ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        paths.put("http://www.flymine.org/model/mage#BioSource",
                   Collections.singleton(desc1));

        desc1 = new ItemPrefetchDescriptor("Treatment.action");
        desc1.addConstraint(new ItemPrefetchConstraintDynamic(
                                     ObjectStoreItemPathFollowingImpl.IDENTIFIER, "action"));
        paths.put("http://www.flymine.org/model/mage#Treatment",
                   Collections.singleton(desc1));

        ObjectStore osSrc = ObjectStoreFactory.getObjectStore(srcOsName);
        ItemReader srcItemReader = new ObjectStoreItemReader(osSrc, paths);
        ObjectStoreWriter oswTgt = ObjectStoreWriterFactory.getObjectStoreWriter(tgtOswName);
        ItemWriter tgtItemWriter = new ObjectStoreItemWriter(oswTgt);

        OntModel model = ModelFactory.createOntologyModel();
        model.read(new FileReader(new File(modelName)), null, format);
        DataTranslator dt = new MageDataTranslator(srcItemReader, model, namespace);
        model = null;
        dt.translate(tgtItemWriter);
        tgtItemWriter.close();
    }

}
