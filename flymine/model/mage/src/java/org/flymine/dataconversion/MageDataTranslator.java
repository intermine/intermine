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

    private Item expItem = new Item();//assume only one experiment item presented

    protected Map reporter2FeatureMap = new HashMap();
    protected Map assay2Maer = new HashMap();
    protected Map assay2LabeledExtract = new HashMap();
    protected Map maer2Reporter = new HashMap();
    protected Map maer2Assay =  new HashMap();
    protected Map maer2Tissue =  new HashMap();
    protected Map maer2Material =  new HashMap();
    protected Map maer2Gene =  new HashMap();
    protected Set cdnaSet = new HashSet();
    protected Map geneMap =  new HashMap(); //organisamDbId, geneItem

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

        i = processBioEntity().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        i = processGene().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        i = processReporterMaterial().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        i = processBioSourceTreatment().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        i = processOrganism().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        i = dbs.values().iterator();
        while (i.hasNext()) {
            tgtItemWriter.store(ItemHelper.convert((Item) i.next()));
        }

        i = processMaer().iterator();
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
                } else if (className.equals("Database")) {
                    Attribute attr = srcItem.getAttribute("name");
                    if (attr != null) {
                        getDb(attr.getValue());
                    }
                    storeTgtItem = false;
                } else if (className.equals("FeatureReporterMap")) {
                     setReporterLocationCoords(srcItem, tgtItem);
                     storeTgtItem = false;
                } else if (className.equals("PhysicalArrayDesign")) {
                    createFeatureMap(srcItem);
                    translateMicroArraySlideDesign(srcItem, tgtItem);
                } else if (className.equals("Experiment")) {
                    // collection bioassays includes MeasuredBioAssay, PhysicalBioAssay
                    // and DerivedBioAssay, only keep DerivedBioAssay
                    //keepDBA(srcItem, tgtItem, srcNs);
                    expItem = translateMicroArrayExperiment(srcItem, tgtItem, srcNs);
                    result.add(expItem);
                    storeTgtItem = false;
                } else if (className.equals("DerivedBioAssay")) {
                    translateMicroArrayAssay(srcItem, tgtItem);
                } else if (className.equals("BioAssayDatum")) {
                    translateMicroArrayExperimentalResult(srcItem, tgtItem, normalised);
                    storeTgtItem = false;
                } else if (className.equals("Reporter")) {
                    setBioEntityMap(srcItem, tgtItem);
                    storeTgtItem = false;
                } else if (className.equals("BioSequence")) {
                    translateBioEntity(srcItem, tgtItem);
                    storeTgtItem = false;
                } else if (className.equals("LabeledExtract")) {
                    translateLabeledExtract(srcItem, tgtItem);
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
            // prefetch done
            Item featureInfo = ItemHelper.convert(srcItemReader
                                                  .getItemById(getFirstId(featureInfos)));

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
            // prefetch done
            Item featureGroup = ItemHelper.convert(srcItemReader
                                                   .getItemById(getFirstId(featureGroups)));
            Iterator featureIter = featureGroup.getCollection("features").getRefIds().iterator();
            while (featureIter.hasNext()) {
                featureToDesign.put((String) featureIter.next(), srcItem.getIdentifier());
            }
        }
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
        //prefetch done
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
        //prefetch done
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
            //prefetch done
            identifier = getFirstId(immobilizedChar);
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
                        // prefetch done
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
                    String feature = sb.substring(0, sb.length() - 1);
                    bioEntity2Feature.put(identifier, feature);
                    reporter2FeatureMap.put(srcItem.getIdentifier(), feature);
                }
                LOG.debug("bioEntity2Feature" + bioEntity2Feature.toString());

                tgtItem.addReference(new Reference("material", identifier));
            }
        }
        reporterSet.add(tgtItem);
    }

    /**
     * @param srcItem = mage:PhysicalArrayDesign
     * @param tgtItem = flymine:MicroArraySlideDesign
     * @throws ObjectStoreException if problem occured during translating
     */
    protected void translateMicroArraySlideDesign(Item srcItem, Item tgtItem)
        throws ObjectStoreException {
        // move descriptions reference list
        //prefetch done
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
        //prefetch done
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
     * @return experiment Item
     * also created a HashMap assay2LabeledExtract
     * @throws ObjectStoreException if problem occured during translating
     */
    protected Item translateMicroArrayExperiment(Item srcItem, Item tgtItem, String srcNs)
        throws ObjectStoreException {

        String derived = null;
        String physical = null;
         // prefetch done
        if (srcItem.hasCollection("bioAssays")) {
            ReferenceList rl = srcItem.getCollection("bioAssays");
            ReferenceList newRl = new ReferenceList();
            newRl.setName("assays");

            for (Iterator i = rl.getRefIds().iterator(); i.hasNext(); ) {
                Item baItem = ItemHelper.convert(srcItemReader.getItemById((String) i.next()));
                if (baItem.getClassName().equals(srcNs + "DerivedBioAssay")) {
                    derived = baItem.getIdentifier();
                    newRl.addRefId(derived);
                }
                if (baItem.getClassName().equals(srcNs + "PhysicalBioAssay")) {
                    physical = baItem.getIdentifier();
                }
            }
            tgtItem.addCollection(newRl);
        }

        //prefetch?
        List labeledExtract = new ArrayList();
        if (physical != null && derived != null) {
            Item pbaItem = ItemHelper.convert(srcItemReader.getItemById(physical));
            if (pbaItem.hasReference("bioAssayCreation")) {
                Item hybri = ItemHelper.convert(srcItemReader.getItemById(
                                 pbaItem.getReference("bioAssayCreation").getRefId()));
                if (hybri.hasCollection("sourceBioMaterialMeasurements")) {
                    ReferenceList sbmm = hybri.getCollection("sourceBioMaterialMeasurements");
                    for (Iterator i = sbmm.getRefIds().iterator(); i.hasNext(); ) {
                        Item bmm = ItemHelper.convert(srcItemReader.getItemById((String) i.next()));
                        if (bmm.hasReference("bioMaterial")) {
                            labeledExtract.add(bmm.getReference("bioMaterial").getRefId());
                        }
                    }
                }
            }
            if (labeledExtract != null) {
                assay2LabeledExtract.put(derived, labeledExtract);
            }
        }

        if (srcItem.hasAttribute("name")) {
            tgtItem.addAttribute(new Attribute("name", srcItem.getAttribute("name").getValue()));
        }
        // prefetch done
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
                                                               getFirstId(publication)));
                            pubFlag = true;
                        }
                    }
                }
            }
        }

        if (expItem.getIdentifier() != "") {
            tgtItem.setIdentifier(expItem.getIdentifier());
        }
        return tgtItem;
    }

    /**
     * @param srcItem = mage: DerivedBioAssay
     * @param tgtItem = flymine:MicroArrayAssay
     * @throws ObjectStoreException if problem occured during translating
     */
     protected void translateMicroArrayAssay(Item srcItem, Item tgtItem)
         throws ObjectStoreException {
         if (srcItem.hasCollection("derivedBioAssayData")) {
             ReferenceList dbad = srcItem.getCollection("derivedBioAssayData");
             List resultsRl = new ArrayList();
             for (Iterator j = dbad.getRefIds().iterator(); j.hasNext(); ) {
                 // prefetch done
                 Item dbadItem = ItemHelper.convert(srcItemReader.getItemById((String) j.next()));
                 if (dbadItem.hasReference("bioDataValues")) {
                     Item bioDataTuples = ItemHelper.convert(srcItemReader.getItemById(
                              dbadItem.getReference("bioDataValues").getRefId()));
                     if (bioDataTuples.hasCollection("bioAssayTupleData")) {
                         ReferenceList rl = bioDataTuples.getCollection("bioAssayTupleData");
                         for (Iterator i = rl.getRefIds().iterator(); i.hasNext(); ) {
                             String ref = (String) i.next();
                             if (!resultsRl.contains(ref)) {
                                 resultsRl.add(ref);
                             }
                         }
                     }
                 }
             }
             // possibly a bug with duplicates in this collection, causing
             // loads of deletes at load time.  remove temporarily
             //tgtItem.addCollection(new ReferenceList("results", resultsRl));
             if (resultsRl != null) {
                 assay2Maer.put(srcItem.getIdentifier(), resultsRl);
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

        if (srcItem.hasAttribute("value")) {
            tgtItem.addAttribute(new Attribute("value", srcItem.getAttribute("value").getValue()));
        }
        tgtItem.addReference(new Reference("analysis", getAnalysisRef()));

        //create maer2Feature map, and maer set
        if (srcItem.hasReference("designElement")) {
            maer2Feature.put(tgtItem.getIdentifier(),
                         srcItem.getReference("designElement").getRefId());
            //maerSet.add(tgtItem.getIdentifier());
        }
        //prefetch done
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

        maerSet.add(tgtItem);
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
        //prefetch done
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

        // prefetch done
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
                                      organismDbId, tgtItem.getIdentifier());
                     // } else if (dbName.equals("flybase") && organismDbId.startsWith("FBmc")) {
                            // tgtItem.addAttribute(new Attribute("organismDbId", organismDbId));
                        } else if (dbName.equals("embl") && dbEntryItem.hasAttribute("accession")) {
                            String accession = dbEntryItem.getAttribute("accession").getValue();
                            //make sure synonym only create once for same accession
                            if (!synonymAccessionMap.keySet().contains(accession)) {
                                emblList.add(dbEntryItem.getIdentifier());
                                synonym = createSynonym(dbEntryItem, getSourceRef("embl"),
                                                             srcItem.getIdentifier());
                                synonymMap.put(dbEntryItem.getIdentifier(), synonym);
                                synonymAccessionMap.put(accession, synonym);
                            }
                        } else {
                            //getDatabaseRef();

                        }
                    }
                }
            }

            if (emblList != null && !emblList.isEmpty()) {
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
    public void translateLabeledExtract(Item srcItem, Item tgtItem)
        throws ObjectStoreException {
        //prefetch done
        if (srcItem.hasReference("materialType")) {
            Item type = ItemHelper.convert(srcItemReader.getItemById(
                  (String) srcItem.getReference("materialType").getRefId()));
            tgtItem.addAttribute(new Attribute("materialType",
                   type.getAttribute("value").getValue()));
        }

        if (srcItem.hasCollection("labels")) {
            ReferenceList labels = srcItem.getCollection("labels");
            if (labels == null || !isSingleElementCollection(labels)) {
                throw new IllegalArgumentException("LabeledExtract (" + srcItem.getIdentifier()
                        + " does not have exactly one label");
            }
            // prefetch done
            Item label = ItemHelper.convert(srcItemReader
                                        .getItemById(getFirstId(labels)));
            tgtItem.addAttribute(new Attribute("label", label.getAttribute("name").getValue()));
        }
        // prefetch done

        List treatmentList = new ArrayList();
        String sampleId = null;
        StringBuffer sb = new StringBuffer();
        if (srcItem.hasCollection("treatments")) {
            ReferenceList treatments = srcItem.getCollection("treatments");
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
                                    if (!treatmentList.contains(refId)) {
                                        treatmentList.add(refId);
                                    }
                                    //create treatment2BioSourceMap
                                    Item treatItem = ItemHelper.convert(srcItemReader.
                                                     getItemById(refId));
                                    //sampleId should be same despite of different
                                    //intermediate value
                                    sampleId = createTreatment2BioSourceMap(treatItem,
                                                      srcItem.getIdentifier());
                                    sb.append(sampleId + " ");
                                }
                            }
                        }
                    }
                }
            }
            ReferenceList tgtTreatments = new ReferenceList("treatments", treatmentList);
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
     * extra flymine:Organism item is created and saved in  organismMap
     * @throws ObjectStoreException if problem occured during translating
     */
    protected void translateSample(Item srcItem, Item tgtItem)
        throws ObjectStoreException {

        List list = new ArrayList();
        Item organism = new Item();
        String s = null;
        if (srcItem.hasCollection("characteristics")) {
            ReferenceList characteristics = srcItem.getCollection("characteristics");
            for (Iterator i = characteristics.getRefIds().iterator(); i.hasNext();) {
                String id = (String) i.next();
                // prefetch done
                Item charItem = ItemHelper.convert(srcItemReader.getItemById(id));
                if (charItem.hasAttribute("category")) {
                    s = charItem.getAttribute("category").getValue();
                    if (s.equals("Organism")) {
                        if (charItem.hasAttribute("value")) {
                            String organismName = charItem.getAttribute("value").getValue();
                            organism = createOrganism(tgtNs + "Organism", "", organismName);
                            tgtItem.addReference(new Reference("organism",
                                                            organism.getIdentifier()));
                        }
                    } else {
                        list.add(id);
                    }
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
        }

        if (srcItem.hasAttribute("name")) {
            tgtItem.addAttribute(new Attribute("name", srcItem.getAttribute("name").getValue()));
        }

        bioSource.add(tgtItem);

    }

    /**
     * @param srcItem = mage:Treatment
     * @param tgtItem = flymine:Treatment
     * @throws ObjectStoreException if problem occured during translating
     */
    public void translateTreatment(Item srcItem, Item tgtItem)
        throws ObjectStoreException {
        //prefetch done
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
     * @param sourceId = mage:LabeledExtract srcItem identifier
     * @return string of bioSourceId
     * @throws ObjectStoreException if problem occured during translating
     * method called when processing LabeledExtract
     */
    protected String createTreatment2BioSourceMap(Item srcItem, String sourceId)
        throws ObjectStoreException {
        StringBuffer bioSourceId = new StringBuffer();
        StringBuffer treatment = new StringBuffer();
        String id = null;
        if (srcItem.hasCollection("sourceBioMaterialMeasurements")) {
            ReferenceList sourceRl1 = srcItem.getCollection("sourceBioMaterialMeasurements");
            for (Iterator l = sourceRl1.getRefIds().iterator(); l.hasNext(); ) {
                // prefetch done (but limited)
                Item sbmmItem = ItemHelper.convert(srcItemReader.getItemById(
                                      (String) l.next()));

                if (sbmmItem.hasReference("bioMaterial")) {
                    // bioSampleItem, type not-extract
                    Item bioSampleItem = ItemHelper.convert(srcItemReader.getItemById(
                            (String) sbmmItem.getReference("bioMaterial").getRefId()));
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
        //BioSample(not-extract) -> {treatments} -> {sourceBioMaterialMeasurements} ->(BioSource)
        //
        StringTokenizer st = new StringTokenizer(bioSourceId.toString());
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            if (!s.equals(id)) {
                throw new IllegalArgumentException("LabeledExtract (" + sourceId
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
     * BioAssayDatum  mage:FeatureId -> flymine:MAEResultId (MicroArrayExperimentalResult)
     * Reporter flymine: BioEntityId -> mage:FeatureId
     * BioSequece  BioEntityId -> BioEntity Item
     *                         -> extra Gene Item
     * @return add microExperimentalResult collection to Gene item
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

    /**
     * maer add reference of reporter, material(CDNAClone only), assay(MicroArrayAssay)
     * add collection of genes and tissues(LabeledExtract)
     * @return maerItem collections
     */
    protected Set processMaer() {
        Set results = new HashSet();
        maer2Reporter = createMaer2ReporterMap(maer2Feature, reporter2FeatureMap, maerSet);
        maer2Assay =  createMaer2AssayMap(assay2Maer);
        maer2Tissue = createMaer2TissueMap(maer2Assay, assay2LabeledExtract, maerSet);
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
     * @param assay2Maer = HashMap
     * @return maer2Assay
     */
    protected Map createMaer2AssayMap(Map assay2Maer) {

        for (Iterator i = assay2Maer.keySet().iterator(); i.hasNext(); ) {
            String assay = (String) i.next(); if (assay2Maer.containsKey(assay)) {
                List maers = (ArrayList) assay2Maer.get(assay);
                if (maers != null) {
                    for (Iterator j = maers.iterator(); j.hasNext(); ) {
                        String maer = (String) j.next();
                        maer2Assay.put(maer, assay);
                    }
                }
            }
        }
        return maer2Assay;
    }

    /**
     * @param maer2Assay = HashMap
     * @param assay2LabeledExtract = HashMap
     * @param maerSet = HashSet
     * @return maer2Tissue
     */
    protected Map createMaer2TissueMap(Map maer2Assay, Map assay2LabeledExtract, Set maerSet) {

        for (Iterator i = maerSet.iterator(); i.hasNext(); ) {
            List le = new ArrayList();
            String assay = null;

            Item maerItem = (Item) i.next();
            String maer = maerItem.getIdentifier();
            if (maer2Assay.containsKey(maer)) {
                assay = (String) maer2Assay.get(maer);
            }
            if (assay != null) {
                le = (ArrayList) assay2LabeledExtract.get(assay);
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
             organism = createItem(className, implementation);
             organism.addAttribute(new Attribute("name", value));
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
    private String getAnalysisRef() {

        if (expItem.getIdentifier() == "") {
            expItem = createItem(tgtNs + "MicroArrayExperiment", "");
        }
        return expItem.getIdentifier();
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










