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
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.hp.hpl.jena.ontology.OntModel;

import org.intermine.InterMineException;
import org.intermine.util.XmlUtil;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.dataconversion.ItemReader;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.DataTranslator;

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
    }

    /**
     * @see DataTranslator#translatetem
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
        if (translated != null) {
            for (Iterator i = translated.iterator(); i.hasNext();) {
                boolean storeTgtItem = true;
                Item tgtItem = (Item) i.next();
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
                } else if (className.equals("PhysicalArrayDesign")) {
                    createFeatureMap(srcItem, tgtItem);
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
                } else if (className.equals("DatabaseEntry")) {
                    tgtItem.addAttribute(new Attribute("type", "accession"));
                }
                result.add(tgtItem);
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
        ReferenceList featureInfos = srcItem.getCollection("featureInformationSources");
        if (featureInfos == null || !isSingleElementCollection(featureInfos)) {
            throw new IllegalArgumentException("FeatureReporterMap (" + srcItem.getIdentifier()
                        + " does not have exactly one featureInformationSource");
        }
        Item featureInfo = ItemHelper.convert(srcItemReader
                        .getItemById((String) featureInfos.getRefIds().get(0)));
        if (featureInfo != null && featureInfo.hasReference("feature")) {
            Item feature = ItemHelper.convert(srcItemReader
                          .getItemById(featureInfo.getReference("feature").getRefId()));
            if (feature != null) {
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
        }
    }

    /**
     * @param srcItem = mage:PhysicalArrayDesign
     * @param tgtItem = flymine:MicroArraySlideDesign
     * @throws ObjectStoreException if problem occured during translating
     */
    protected void createFeatureMap(Item srcItem, Item tgtItem)
        throws ObjectStoreException {
        ReferenceList featureGroups = srcItem.getCollection("featureGroups");
        if (featureGroups == null || !isSingleElementCollection(featureGroups)) {
            throw new IllegalArgumentException("PhysicalArrayDesign (" + srcItem.getIdentifier()
                        + ") does not have exactly one featureGroup");
        }
        Item featureGroup = ItemHelper.convert(srcItemReader
                  .getItemById((String) featureGroups.getRefIds().get(0)));
        Iterator featureIter = featureGroup.getCollection("features").getRefIds().iterator();
        while (featureIter.hasNext()) {
            featureToDesign.put((String) featureIter.next(), tgtItem.getIdentifier());
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
        promoteCollection(srcItem, "descriptions", "annotations", tgtItem, "descriptions");
        // change substrateType reference to attribute

        Item surfaceType = ItemHelper.convert(srcItemReader
                                .getItemById(srcItem.getReference("surfaceType").getRefId()));
        if (surfaceType != null && surfaceType.hasAttribute("value")) {
            tgtItem.addAttribute(new Attribute("surfaceType",
                                               surfaceType.getAttribute("value").getValue()));

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
        if (srcItem.hasReference("quantitationType")) {
            Item qtItem = ItemHelper.convert(srcItemReader.getItemById(
                            srcItem.getReference("quantitationType").getRefId()));
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
                    tgtItem.addAttribute(new Attribute("scale",
                                                       msItem.getReference("scale").getRefId()));
                } else {
                    LOG.error("srcItem (" + msItem.getIdentifier()
                              + "( does not have scale reference ");
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


    /**
     * set ReporterLocation.design reference, don't need to set
     * MicroArraySlideDesign.locations explicitly
     * @return results set
     */
    protected Set processReporterLocs() {
        Set results = new HashSet();
        Iterator i = reporterLocs.iterator();
        while (i.hasNext()) {
            Item rl = (Item) i.next();
            String designId = (String) featureToDesign.get(rlToFeature.get(rl.getIdentifier()));
            Reference designRef = new Reference();
            designRef.setName("design");
            designRef.setRefId(designId);
            rl.addReference(designRef);
            results.add(rl);
        }
        return results;
    }

    /**
     * @param srcItem = mage:BibliographicReference
     * @return author set
     */
    protected Set createAuthors(Item srcItem) {
        Set result = new HashSet();
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
        return result;
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

}
