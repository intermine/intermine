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
    protected Map gene2BioEntity = new HashMap();

    //mage: Feature id -> flymine:MicroArrayExperimentResult id when processing BioAssayDatum
    protected Map maer2Feature = new HashMap();
    protected Map feature2Maer = new HashMap();
    protected Set maerSet = new HashSet();
    //flymine: BioEntity id -> mage:Feature id when processing Reporter
    protected Map bioEntity2Feature = new HashMap();
    //BioSequence ->BioEntity and extra Gene
    //set bioEntity item -> MicroArrayExperimentalResult
    protected Set bioEntitySet = new HashSet();
    //    protected Set nuclearDNASet = new HashSet();
    //    protected Set cDNACloneSet = new HashSet();
    protected Set geneSet = new HashSet();

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
                } else if (className.equals("Reporter")) {
                    setBioEntity2FeatureMap(srcItem, tgtItem);
                } else if (className.equals("BioSequence")) {
                    Item gene = new Item();
                    gene = translateBioEntity(srcItem, tgtItem);
                    result.add(gene);
                    bioEntitySet.add(tgtItem);
                    //                    if (tgtItem.className.equals("NuclearDNA")) {
                    //  nuclearDNASet.add(tgtItem);
                    //} else if (tgtItem.className.equals("CDNAClone")) {
                    //    cDNACloneSet.add(tgtItem);
                    //}
                    geneSet.add(gene);
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

    protected Map createFeature2MaerMap(Map maer2FeatureMap, Set maerSet) {
        // System.out.println("maer "+maer.toString());
        for (Iterator i = maerSet.iterator(); i.hasNext(); ) {
            String maer = (String)i.next();
            String feature = (String)maer2FeatureMap.get(maer);
            if (feature2Maer.containsKey(feature)) {
                String multiMaer = ((String) feature2Maer.get(feature)).concat(" "+maer);
                feature2Maer.put(feature, multiMaer);
            } else {
                feature2Maer.put(feature, maer);
            }

        }
        System.out.println("feature2maer " + feature2Maer.toString());
        return feature2Maer;

    }

    protected void setBioEntity2FeatureMap(Item srcItem, Item tgtItem)
        throws ObjectStoreException {
        ReferenceList featureReporterMaps = srcItem.getCollection("featureReporterMaps");
        ReferenceList immobilizedChar = srcItem.getCollection("immobilizedCharacteristics");

        StringBuffer sb = new StringBuffer();
        if (featureReporterMaps != null && immobilizedChar != null) {
            for (Iterator i = featureReporterMaps.getRefIds().iterator(); i.hasNext(); ) {
                //FeatureReporterMap
                Item frm = ItemHelper.convert(srcItemReader.getItemById((String)i.next()));
                if ( frm.hasCollection("featureInformationSources")) {
                    Iterator j = frm.getCollection("featureInformationSources").getRefIds().iterator();
                    while (j.hasNext()) {
                        Item fis = ItemHelper.convert(srcItemReader.getItemById((String)j.next()));
                        if (fis.hasReference("feature")) {
                            System.out.println("feature "+fis.getReference("feature").getRefId());
                            sb.append(fis.getReference("feature").getRefId()+" ");
                        }
                    }
                }
            }
            String id =  (String) immobilizedChar.getRefIds().get(0);
            bioEntity2Feature.put(id, sb.toString());
            System.out.println("bioEntity2Feature" + bioEntity2Feature.toString());
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
                    tgtItem.addReference(new Reference("scale",
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

        if (srcItem.hasReference("designElement")) {

            maer2Feature.put(tgtItem.getIdentifier(),srcItem.getReference("designElement").getRefId());
            maerSet.add(tgtItem.getIdentifier());
        }
        //System.out.println("maer2Feature " + maer2Feature.toString());

    }


    protected Item translateBioEntity(Item srcItem, Item tgtItem)
        throws ObjectStoreException {
        Item gene = new Item();
        String s = null;
        if (srcItem.hasReference("type")) {
            Item item = ItemHelper.convert(srcItemReader.getItemById(
                               srcItem.getReference("type").getRefId()));
            if (item.hasAttribute("value")) {
                s = item.getAttribute("value").getValue();
                System.out.println("bioentity: " +s);
                if (s.equals("genomic_DNA")){
                   tgtItem.setClassName(tgtNs + "NuclearDNA");
                } else if (s.equals("cDNA_clone")) {
                    tgtItem.setClassName(tgtNs + "CDNAClone");
                }
            }
        }
        if (srcItem.hasCollection("sequenceDatabases")) {
            ReferenceList rl = srcItem.getCollection("sequenceDatabases");
            if (rl != null) {
                boolean emblFlag = false;
                String identifier = null;
                ReferenceList synonymGeneRl = new ReferenceList();
                ReferenceList synonymEmblRl = new ReferenceList();

                for (Iterator i = rl.getRefIds().iterator(); i.hasNext(); ) {
                    Item dbEntryItem = ItemHelper.convert(srcItemReader.getItemById((String) i.next()));
                    if (dbEntryItem.hasReference("database")) {
                        identifier = dbEntryItem.getAttribute("accession").getValue();
                        Item dbItem =  ItemHelper.convert(srcItemReader.getItemById((String) dbEntryItem.getReference("database").getRefId()));
                        if (dbItem.hasAttribute("name")) {
                            String dbName = dbItem.getAttribute("name").getValue();
                            if (dbName.equals("flybase") && identifier != null) {
                                gene = createGene(tgtNs + "Gene", "", identifier);
                                synonymGeneRl.setName("synonym");
                                synonymGeneRl.addRefId(dbItem.getIdentifier());

                            } else if (dbName.equals("embl") && identifier != null) {
                                if (emblFlag == false) {
                                    tgtItem.addAttribute(new Attribute("identifier", identifier));
                                    emblFlag = true;
                                }
                                synonymEmblRl.setName("synonym");
                                synonymEmblRl.addRefId(dbEntryItem.getIdentifier());
                            }
                        }
                    }
                }
                gene.addCollection(synonymGeneRl);
                tgtItem.addCollection(synonymEmblRl);
            }
        }
        gene2BioEntity.put(gene.getIdentifier(), srcItem.getIdentifier());
        return gene;

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

    protected Set processBioEntity2MAEResult() {
        Set results = new HashSet();
        feature2Maer = createFeature2MaerMap(maer2Feature, maerSet);
        for (Iterator i = bioEntitySet.iterator(); i.hasNext();) {
            Item bioEntity = (Item) i.next();
            List maerIds = new ArrayList();
            String s = (String) bioEntity2Feature.get((String) bioEntity.getIdentifier());
            System.out.println("featureId "+s + " bioEntityId "+ bioEntity.getIdentifier());
            if (s != null) {
                StringTokenizer st = new StringTokenizer(s);
                String multiMaer = null;
                while (st.hasMoreTokens()) {
                    multiMaer = (String) feature2Maer.get(st.nextToken());
                    System.out.println("multiMaer " + multiMaer);
                    if (multiMaer != null) {
                        StringTokenizer token = new StringTokenizer(multiMaer);
                        while (token.hasMoreTokens()) {
                            maerIds.add(token.nextToken());
                        }
                    }
                }
                ReferenceList maerRl = new ReferenceList("microArrayExperimentalResult", maerIds);
                bioEntity.addCollection(maerRl);
                results.add(bioEntity);
            }
        }
        return results;
    }

    protected Set processGene2MAEResult() {
        Set results = new HashSet();
        feature2Maer = createFeature2MaerMap(maer2Feature, maerSet);
        for (Iterator i = geneSet.iterator(); i.hasNext();) {
            Item gene = (Item) i.next();
            List maerIds = new ArrayList();
            String geneId = (String) gene.getIdentifier();
            String bioEntityId = (String) gene2BioEntity.get(geneId);
            String s = (String) bioEntity2Feature.get(bioEntityId);
            System.out.println("featureId "+s + " bioEntityId "+ bioEntityId +" geneId "+ geneId);
            if (s != null) {
                StringTokenizer st = new StringTokenizer(s);
                String multiMaer = null;
                while (st.hasMoreTokens()) {
                    multiMaer = (String) feature2Maer.get(st.nextToken());
                    System.out.println("multiMaer " + multiMaer);
                    if (multiMaer != null) {
                        StringTokenizer token = new StringTokenizer(multiMaer);
                        while (token.hasMoreTokens()) {
                            maerIds.add(token.nextToken());
                        }
                    }
                }

                ReferenceList maerRl = new ReferenceList("microArrayExperimentalResult", maerIds);
                gene.addCollection(maerRl);
                results.add(gene);
            }
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

    private Item createGene(String className, String implementation, String identifier) {
        Item gene = new Item();
        gene = createItem(className, implementation);
        gene.addAttribute(new Attribute("organismDbId", identifier));
        return gene;
    }

}
