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

import java.util.*;

import org.intermine.InterMineException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.ItemHelper;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.dataconversion.*;
import org.intermine.metadata.Model;
import org.intermine.util.XmlUtil;

import org.apache.log4j.Logger;

/**
 * Translates the interpro src items database into the interpro tgt items database prior to dataloading.
 *
 * @author Peter Mclaren
 */
public class InterproDataTranslator extends DataTranslator {

  protected static final String PARENTFEATURES = "parentFeatures";
  protected static final String CHILDFEATURES = "childFeatures";
  protected static final String CONTAINS = "contains";
  protected static final String FOUNDIN = "foundIn";

  protected static final String PROTEINS = "proteins";
  protected static final String PROTEINFEATURES = "proteinFeatures";

  protected static final String CV_DATABASE = "cv_database";
  protected static final String CV_ENTRY_TYPE = "cv_entry_type";
  protected static final String CV_RELATION = "cv_relation";
  protected static final String CV_EVIDENCE = "cv_evidence";
  protected static final String ABBREV = "abbrev";
  protected static final String IDENTIFIER = "identifier";
  protected static final String COMMENTS = "comments";
  protected static final String PROTEIN = "protein";
  protected static final String METHOD = "method";
  protected static final String ENTRY = "entry";
  protected static final String MATCHES = "matches";
  protected static final String ENTRY2ENTRY = "entry2entry";
  protected static final String ENTRY2COMP = "entry2comp";

  protected static final String FAMILY = "Family";
  protected static final String DOMAIN = "Domain";

  protected static final String PARENT = "parent";
  protected static final String ENTRY1 = "entry1";
  protected static final String ENTRY2 = "entry2";
  protected static final String RELATIONSHIP = "relationship";

  protected static final String PROTEIN_AC = "protein_ac";
  protected static final String NAME = "name";
  protected static final String ENTRY_AC = "entry_ac";
  protected static final String METHOD_AC = "method_ac";
  protected static final String COMMON_ANNOTATION = "common_annotation";
  protected static final String EVIDENCE = "evidence";

  protected static final Logger LOG = Logger.getLogger(InterproDataTranslator.class);

  //Maintains a map of any organisms that we have had to create indexed by the ncbi tax id
  private TreeMap organisms;
  //little evidence tag item...
  private org.intermine.xml.full.Item interproEvidenceItem = null;

  public InterproDataTranslator(ItemReader itemReader, Properties properties, Model sourceModel, Model targetModel) {
    super(itemReader, properties, sourceModel, targetModel);
    organisms = new TreeMap();
  }

  /**
   * @see DataTranslator#translate
   */
  public void translate(ItemWriter tgtItemWriter)
          throws ObjectStoreException, InterMineException {

    super.translate(tgtItemWriter);

    //Store any organism items we may have had to create...
    if (organisms != null && organisms.values() != null && (!organisms.values().isEmpty())) {

      for (Iterator orgIt = organisms.values().iterator(); orgIt.hasNext();) {

        tgtItemWriter.store(ItemHelper.convert((org.intermine.xml.full.Item) orgIt.next()));
      }
    }

    //add our internally created Interpro evidence object here...
    tgtItemWriter.store(ItemHelper.convert(getInterproEvidenceItem()));
  }

  /**
   * @see DataTranslator#translateItem
   */
  protected Collection translateItem(Item srcItem)
          throws ObjectStoreException, InterMineException {

    Collection result = new HashSet();
    String srcItemClassName = XmlUtil.getFragmentFromURI(srcItem.getClassName());


    if (srcItemClassName.equalsIgnoreCase("cv_database") && "InterPro".equalsIgnoreCase(srcItem.getAttribute("dbname").getValue())) {

      LOG.info("SKIPPING STORE FOR THE CV_DATABASE.InterPro ITEM!");
      //NOTE: we can only get away with doing this since no other items of interest in the interpro schema point to this cv_database item!!!!
    } else {

      Collection translated = super.translateItem(srcItem);

      if (translated != null) {

        for (Iterator i = translated.iterator(); i.hasNext();) {
          Item tgtItem = (Item) i.next();

          if (PROTEIN.equals(srcItemClassName)) {
            processProteinItem(srcItem, tgtItem);
          } else if (METHOD.equals(srcItemClassName)) {
            processMethodItem(srcItem, tgtItem);
          } else if (ENTRY.equals(srcItemClassName)) {
            processEntryItem(srcItem, tgtItem);
          } else if (MATCHES.equals(srcItemClassName)) {
            processMatchesItem(srcItem, tgtItem);
          }

          result.add(tgtItem);
        }
      }
    }
    return result;
  }

  private void processProteinItem(org.intermine.xml.full.Item srcItem, org.intermine.xml.full.Item tgtItem)
          throws ObjectStoreException {

    org.intermine.xml.full.Item taxItem = getItemViaItemPath(srcItem, taxonomyFromProtein, srcItemReader);

    if (taxItem != null) {

      tgtItem.setReference("organism", taxItem.getIdentifier());
      LOG.debug("PROTEIN.PROTEIN_AC:" + srcItem.getAttribute("protein_ac") + " has tax_id:" + taxItem.getAttribute("taxa_id"));
    } else {
      LOG.warn("!!! PROTEIN.PROTEIN_AC:" + srcItem.getAttribute("protein_ac") + " has does NOT have a taxonomy object ref !!!");
    }

    java.util.List matchesList = getItemsViaItemPath(srcItem, proteinFromMatches, srcItemReader);

    for (Iterator matchIt = matchesList.iterator(); matchIt.hasNext();) {

      org.intermine.xml.full.Item nextMatch = (org.intermine.xml.full.Item) matchIt.next();
      org.intermine.xml.full.Reference methodRef = nextMatch.getReference(METHOD);
      org.intermine.xml.full.Item nextMethod = ItemHelper.convert(this.srcItemReader.getItemById(methodRef.getRefId()));

      tgtItem.addToCollection(PROTEINFEATURES, nextMethod);
    }

    java.util.List supermatchList = getItemsViaItemPath(srcItem, proteinFromSuperMatch, srcItemReader);

    for (Iterator supermatchIt = supermatchList.iterator(); supermatchIt.hasNext();) {

      org.intermine.xml.full.Item nextSupermatch = (org.intermine.xml.full.Item) supermatchIt.next();
      org.intermine.xml.full.Reference entryRef = nextSupermatch.getReference(ENTRY);
      org.intermine.xml.full.Item nextEntry = ItemHelper.convert(this.srcItemReader.getItemById(entryRef.getRefId()));

      tgtItem.addToCollection(PROTEINFEATURES, nextEntry);
    }
  }

  private void processMethodItem(org.intermine.xml.full.Item srcItem, org.intermine.xml.full.Item tgtItem)
          throws ObjectStoreException {

    //NOTE: This extended path only work as the bridge table has a 1 to 1 row count with the method table.
    //HERE WE WANT TO ESTABLISH A LINK BETWEEN THE METHOD AND ITS RELATED INTERPRO_ID FROM THE ENTRY TABLE
    org.intermine.xml.full.Item entryItem = getItemViaItemPath(srcItem, methodToEntryViaEntry2Method, srcItemReader);

    if (entryItem != null) {

      //Get the interpro id for the related entry...
      org.intermine.xml.full.Attribute interproIdAttribute = new Attribute();
      interproIdAttribute.setName("interproId");
      interproIdAttribute.setValue(entryItem.getAttribute("entry_ac").getValue());
      tgtItem.addAttribute(interproIdAttribute);
      LOG.info("ADDED AN INTERPRO ID:" + interproIdAttribute.getValue() + " TO A METHOD ITEM");

      //CHECK THE ENTRY RELATING TO THIS METHOD TO SEE IF IT IS A FAMILY OR AN EQIVALENT DOMAIN...
      tryAndSetEntryTypeInAnEntry(entryItem);

      //TGT item is a ProteinFamily
      if (entryItem.getAttribute(ABBREV).getValue().equalsIgnoreCase(FAMILY)) {

        LOG.info("SETTING A METHOD TO BE A PROTEINFAMILY");
        tgtItem.setClassName("http://www.flymine.org/model/genomic#ProteinFamily");
      }
      //TGT item is a ProteinDomain
      else if (entryItem.getAttribute(ABBREV).getValue().equalsIgnoreCase(DOMAIN)) {

        LOG.info("SETTING A METHOD TO BE A PROTEINDOMAIN");
        tgtItem.setClassName("http://www.flymine.org/model/genomic#ProteinDomain");
      }
      //Hmmm, what's this doing here... should have filtered out these types...
      else {
        LOG.info("IGNORED AN METHOD-ENTRY MAPPING WITH TYPE:" + entryItem.getAttribute(ABBREV).getValue());
      }
    } else {
      LOG.warn("METHOD WITH NO MAPPED ENTRY ITEM FOUND - CHECK THE METHOD & ENTRY2METHOD & ENTRY TABLES!");
    }

    //NOTE: This extended path only work as the bridge table has a 1 to 1 row count with the method table.
    //Here we want to fetch the evidence field that describes the relationship between the method and entry items
    org.intermine.xml.full.Item cvEvidenceItem = getItemViaItemPath(srcItem, methodToCvEvidenceViaEntry2Method, srcItemReader);

    if (cvEvidenceItem != null) {

      tgtItem.addToCollection(EVIDENCE, cvEvidenceItem);

      //CHECK THE ENTRY RELATING TO THIS METHOD TO SEE IF IT IS A FAMILY OR AN EQIVALENT DOMAIN...
    } else {
      LOG.warn("METHOD WITH NO MAPPED CV_EVIDENCE ITEM FOUND - CHECK THE METHOD & ENTRY2METHOD & CV_EVIDENCE TABLES!");
    }






    //CHECK THAT THERE IS A REFERENCE TO THE CV_DATABASE ITEM FOR THIS METHOD ITEM - WARN IF NONE FOUND!
    org.intermine.xml.full.Reference cvdbRefSrc = srcItem.getReference(CV_DATABASE);
    if (cvdbRefSrc != null) {

      org.intermine.xml.full.Item cvdbItem = ItemHelper.convert(this.srcItemReader.getItemById(cvdbRefSrc.getRefId()));
      tgtItem.addToCollection(EVIDENCE, cvdbItem);
    } else {
      LOG.warn("!!! FOUND A METHOD WITHOUT A REFERENCED CV_DATABASE !!!");
    }

    //Fill in the proteins collection which we can reach via the matches table...
    java.util.List matchesList = getItemsViaItemPath(srcItem, methodFromMatches, srcItemReader);

    for (Iterator matchIt = matchesList.iterator(); matchIt.hasNext();) {

      org.intermine.xml.full.Item nextMatch = (org.intermine.xml.full.Item) matchIt.next();
      org.intermine.xml.full.Reference proteinRef = nextMatch.getReference(PROTEIN);
      org.intermine.xml.full.Item nextProtein = ItemHelper.convert(this.srcItemReader.getItemById(proteinRef.getRefId()));

      if (nextProtein != null) {

        if (nextProtein.hasAttribute(PROTEIN_AC)) {

          tgtItem.addToCollection(PROTEINS, nextProtein);
        } else {
          LOG.warn("METHOD-MATCHES-PROTEIN - PROTEIN_AC NOT FOUND!");
        }
      } else {
        LOG.warn("METHOD-MATCHES-PROTEIN - NULL PROTEIN FROM REFERENCE!");
      }
    }
    {
      //Make sure that the target item has it's IDENTIFIER and NAME fields set...
      org.intermine.xml.full.Attribute methodAcAttribute = srcItem.getAttribute(METHOD_AC);
      org.intermine.xml.full.Attribute nuIdentifierAttribute = new org.intermine.xml.full.Attribute();
      nuIdentifierAttribute.setName(IDENTIFIER);
      nuIdentifierAttribute.setValue(methodAcAttribute.getValue());
      tgtItem.addAttribute(nuIdentifierAttribute);
    }
    {
      //Make sure that the target item has it's IDENTIFIER and NAME fields set...
      org.intermine.xml.full.Attribute nameAttribute = srcItem.getAttribute(NAME);
      org.intermine.xml.full.Attribute nuNameAttribute = new org.intermine.xml.full.Attribute();
      nuNameAttribute.setName(NAME);
      nuNameAttribute.setValue(nameAttribute.getValue());
      tgtItem.addAttribute(nuNameAttribute);
    }
  }

  private void processEntryItem(org.intermine.xml.full.Item srcItem, org.intermine.xml.full.Item tgtItem)
          throws ObjectStoreException {

    //Make sure that the entry object has a type !!!
    tryAndSetEntryTypeInAnEntry(srcItem);


    tgtItem.addToCollection(EVIDENCE, getInterproEvidenceItem());

    //LINK ACCROSS THE SUPERMATCH BRIDGE TABLE TO SET THE RELATED PROTEINS FOR THIS ENTRY/PROTEIN-DOMAIN/FAMILY
    java.util.List supermatchList = getItemsViaItemPath(srcItem, entryFromSuperMatch, srcItemReader);
    for (Iterator supermatchIt = supermatchList.iterator(); supermatchIt.hasNext();) {

      org.intermine.xml.full.Item nextSuperMatch = (org.intermine.xml.full.Item) supermatchIt.next();
      org.intermine.xml.full.Reference proteinRef = nextSuperMatch.getReference(PROTEIN);
      org.intermine.xml.full.Item nextProtein = ItemHelper.convert(this.srcItemReader.getItemById(proteinRef.getRefId()));

      if (nextProtein != null) {

        if (nextProtein.hasAttribute(PROTEIN_AC)) {

          tgtItem.addToCollection(PROTEINS, nextProtein);
        } else {
          LOG.warn("ENTRY-SUPERMATCH-PROTEIN - PROTEIN_AC NOT FOUND!");
        }
      } else {
        LOG.warn("ENTRY-SUPERMATCH-PROTEIN - NULL PROTEIN FROM REFERENCE!");
      }
    }

    //SET THE INTERPRO ACCESSION (ENTRY_AC) TO BE THE IDENTIFIER IN THE TGT ITEM
    if (tgtItem.hasAttribute(IDENTIFIER)) {
      LOG.debug("ENTRY HAS AN IDENTIFIER:" + tgtItem.getAttribute(IDENTIFIER).getValue());
    } else {

      org.intermine.xml.full.Attribute srcEntryAcAttribute = srcItem.getAttribute(ENTRY_AC);

      if (srcEntryAcAttribute != null) {
        org.intermine.xml.full.Attribute tgtEntryAcAttribute = new Attribute();
        tgtEntryAcAttribute.setName(IDENTIFIER);
        tgtEntryAcAttribute.setValue(srcEntryAcAttribute.getValue());
        tgtItem.addAttribute(tgtEntryAcAttribute);
      } else { //unlikely - but you never know...
        LOG.warn("!!! ENTRY (identifier:" + (tgtItem.getAttribute(IDENTIFIER) != null ?
                tgtItem.getAttribute(IDENTIFIER).getValue() : "_NO_ID_FOUND_") +
                ") IS WITHOUT AN ENTRY_AC !!!");
      }
    }

    //SET THE COMMENTS COLLECTION FROM THE COMMON_ANNOTATION TABLE.
    if (tgtItem.hasCollection(COMMENTS)) {
      LOG.debug("ENTRY WITH A COMMENTS LIST FOUND - SKIPPING COMMENT GENERATION");
    } else {

      java.util.List entry2commmon_annoList = getItemsViaItemPath(srcItem, entryFromEntry2CommonAnnotation, srcItemReader);

      for (Iterator e2caIt = entry2commmon_annoList.iterator(); e2caIt.hasNext();) {
        org.intermine.xml.full.Item nextE2CAItem = (org.intermine.xml.full.Item) e2caIt.next();
        org.intermine.xml.full.Reference caRef = nextE2CAItem.getReference(COMMON_ANNOTATION);
        org.intermine.xml.full.Item caItem = ItemHelper.convert(this.srcItemReader.getItemById(caRef.getRefId()));
        tgtItem.addToCollection(COMMENTS, caItem);
      }
    }

    //Link to the entry2entry & entry2comp tables to set PARENT/CHILD or EQUIVALENT relations.
    setupEntryRelations(entryFromEntry2EntryViaParent, srcItem, tgtItem, ENTRY, CHILDFEATURES);
    setupEntryRelations(entryFromEntry2EntryViaEntry, srcItem, tgtItem, PARENT, PARENTFEATURES);

    setupEntryRelations(entryFromEntry2CompViaEntry1, srcItem, tgtItem, ENTRY2, CONTAINS);
    setupEntryRelations(entryFromEntry2CompViaEntry2, srcItem, tgtItem, ENTRY1, FOUNDIN);
  }


  private void processMatchesItem(org.intermine.xml.full.Item srcItem, org.intermine.xml.full.Item tgtItem)
          throws ObjectStoreException {

    //CHECK THAT THERE IS A REFERENCE TO THE CV_DATABASE ITEM FOR THIS MATCHES ITEM - WARN IF NONE FOUND!
    org.intermine.xml.full.Reference cvdbRefSrc = srcItem.getReference(CV_DATABASE);
    if (cvdbRefSrc != null) {

      org.intermine.xml.full.Item cvdbItem = ItemHelper.convert(this.srcItemReader.getItemById(cvdbRefSrc.getRefId()));
      tgtItem.addToCollection(EVIDENCE, cvdbItem);
    } else {
      LOG.warn("!!! FOUND A MATCHES ITEM WITHOUT A REFERENCED CV_DATABASE ITEM !!!");
    }

    //CHECK THAT THERE IS A REFERENCE TO THE CV_EVIDENCE ITEM FOR THIS MATCHES ITEM - WARN IF NONE FOUND!
    org.intermine.xml.full.Reference cvevRefSrc = srcItem.getReference(CV_EVIDENCE);
    if (cvevRefSrc != null) {

      org.intermine.xml.full.Item cvevItem = ItemHelper.convert(this.srcItemReader.getItemById(cvevRefSrc.getRefId()));
      tgtItem.addToCollection(EVIDENCE, cvevItem);
    } else {
      LOG.warn("!!! FOUND A MATCHES ITEM WITHOUT A REFERENCED CV_EVIDENCE ITEM !!!");
    }
  }


  //Private helper method to assist in setting any reverse self relations in the ProteinFamily object.
  private void setupEntryRelations(ItemPath familyFeatureItemPath, org.intermine.xml.full.Item srcItem, org.intermine.xml.full.Item tgtItem,
                                   String relationToMapTo, String targetCollectionName)
          throws ObjectStoreException {

    java.util.List familyList = getItemsViaItemPath(srcItem, familyFeatureItemPath, srcItemReader);

    if (familyList != null && familyList.size() > 0) {

      for (Iterator familyIterator = familyList.iterator(); familyIterator.hasNext();) {

        org.intermine.xml.full.Item relationshipItem = (org.intermine.xml.full.Item) familyIterator.next();

        //check to see that the entry2xxxx item has a reference to the cv_relation table so we can get it's type
        if (relationshipItem.hasReference(relationToMapTo)) {

          org.intermine.xml.full.Reference relativeRef = relationshipItem.getReference(relationToMapTo);

          org.intermine.xml.full.Item nextRelative = ItemHelper.convert(this.srcItemReader.getItemById(relativeRef.getRefId()));

          tgtItem.addToCollection(targetCollectionName, nextRelative);
        } else {
          LOG.warn("NO " + relationToMapTo + " RELATION FOUND FOR PATH" + familyFeatureItemPath.toString());
        }
      }
    }
  }


  /**
   * Factored this out so I can use it when I am not initially working on an entry item, but when I
   * am dealing with a protein via the supermatch table and need to make sure the entry_type is set in the
   * entry that the protein refers to so we can classify it as either a family or domain and put into the
   * correct collection for each protein...
   * <p/>
   *
   * @return a boolean indicating if we managed to set the entry_type or not...
   */
  private boolean tryAndSetEntryTypeInAnEntry(org.intermine.xml.full.Item entry)
          throws ObjectStoreException {

    //Perhaps we've already set the type for this entry already...
    if (entry.hasAttribute(ABBREV)) {
      return true;
    }

    if (entry.getReference(CV_ENTRY_TYPE) != null) {

      org.intermine.xml.full.Reference cvetRef = entry.getReference(CV_ENTRY_TYPE);

      org.intermine.model.fulldata.Item cvetItem = this.srcItemReader.getItemById(cvetRef.getRefId());

      Set cvetAttrSet = cvetItem.getAttributes();

      Iterator cvetAttrIt = cvetAttrSet.iterator();

      org.intermine.model.fulldata.Attribute cvetAttrNext = null;

      while (cvetAttrIt.hasNext()) {

        cvetAttrNext = (org.intermine.model.fulldata.Attribute) cvetAttrIt.next();

        if (cvetAttrNext.getName().equalsIgnoreCase(ABBREV)) {
          org.intermine.xml.full.Attribute srcEntryTypeAttr = new Attribute();
          srcEntryTypeAttr.setName(ABBREV);
          srcEntryTypeAttr.setValue(cvetAttrNext.getValue());
          entry.addAttribute(srcEntryTypeAttr);
          return true;
        }
      }

      LOG.warn("!!! DATA BUG - ENTRY WITH REFERENCE TO CV_ENTRY_TYPE WITH NO ABBREV FOUND !!!");
      return false;
    } else {
      LOG.warn("POSSIBLE DATA BUG - ENTRY WITHOUT A REFERENCE TO A CV_ENTRY_TYPE OBJECT FOUND!");
      return false;
    }
  }

  /***/
  private org.intermine.xml.full.Item getItemViaItemPath(org.intermine.xml.full.Item sourceItem,
                                                         org.intermine.dataconversion.ItemPath itemPath,
                                                         org.intermine.dataconversion.ItemReader sourceItemReader) throws ObjectStoreException {

    //Have to convert from 'org.intermine.xml.full.Item' to 'org.intermine.model.fulldata.Item' and back again!!!
    org.intermine.model.fulldata.Item modelItem = ItemHelper.convert(sourceItem);

    org.intermine.model.fulldata.Item targetItemToConvert = sourceItemReader.getItemByPath(itemPath, modelItem);

    org.intermine.xml.full.Item targetItemToReturn = null;

    if (targetItemToConvert != null) {
      targetItemToReturn = ItemHelper.convert(targetItemToConvert);
    }

    return targetItemToReturn;
  }

  private java.util.List getItemsViaItemPath(org.intermine.xml.full.Item sourceItem,
                                             org.intermine.dataconversion.ItemPath itemPath,
                                             org.intermine.dataconversion.ItemReader sourceItemReader) throws ObjectStoreException {

    //Have to convert from 'org.intermine.xml.full.Item' to 'org.intermine.model.fulldata.Item' and back again!!!
    org.intermine.model.fulldata.Item modelItem = ItemHelper.convert(sourceItem);

    java.util.List targetItemListToConvert = sourceItemReader.getItemsByPath(itemPath, modelItem);

    java.util.List itemList = new ArrayList();

    if (targetItemListToConvert != null) {

      for (Iterator itemIterator = targetItemListToConvert.iterator(); itemIterator.hasNext();) {
        itemList.add(ItemHelper.convert((org.intermine.model.fulldata.Item) itemIterator.next()));
      }
    }
    return itemList;
  }


  /**
   * WE HAVE OVERRIDDEN THE PARENT METHOD TO SET THE BATCH SIZE TO A LOWER VALUE TO AVOID GETTING TO MANY QUERY ITEMS
   *
   * Returns the Iterator over Items that the DataTranslator will translate.
   *
   * @return an Iterator
   * @throws ObjectStoreException if something goes wrong
   */
  public Iterator getItemIterator() throws ObjectStoreException {

      ((ObjectStoreItemReader)srcItemReader).setBatchSize(500);  

      return srcItemReader.itemIterator();
  }



//----------------------------------------------------------------------------------------------------------------------

  protected static final String pathNameSpace = "http://www.flymine.org/model/interpro#";

  protected static final ItemPath taxonomyFromProtein = new ItemPath("(protein <- protein2taxonomy.protein).taxonomy", pathNameSpace);
  protected static final ItemPath methodToEntryViaEntry2Method = new ItemPath("(method <- entry2method.method).entry", pathNameSpace);
  protected static final ItemPath methodToCvEvidenceViaEntry2Method = new ItemPath("(method <- entry2method.method).cv_evidence", pathNameSpace);

  protected static final ItemPath entryViaEntry2Method = new ItemPath("(entry <- entry2method.entry)", pathNameSpace);

  protected static final ItemPath entryFromEntry2CommonAnnotation = new ItemPath("(entry <- entry2common_annotation.entry)", pathNameSpace);

  protected static final ItemPath proteinFromMatches = new ItemPath("(protein <- matches.protein)", pathNameSpace);
  protected static final ItemPath methodFromMatches = new ItemPath("(method <- matches.method)", pathNameSpace);

  protected static final ItemPath proteinFromSuperMatch = new ItemPath("(protein <- supermatch.protein)", pathNameSpace);
  protected static final ItemPath entryFromSuperMatch = new ItemPath("(entry <- supermatch.entry)", pathNameSpace);

  protected static final ItemPath entryFromEntry2CompViaEntry1 = new ItemPath("(entry <- entry2comp.entry1)", pathNameSpace);
  protected static final ItemPath entryFromEntry2CompViaEntry2 = new ItemPath("(entry <- entry2comp.entry2)", pathNameSpace);

  protected static final ItemPath entryFromEntry2EntryViaEntry = new ItemPath("(entry <- entry2entry.entry)", pathNameSpace);
  protected static final ItemPath entryFromEntry2EntryViaParent = new ItemPath("(entry <- entry2entry.parent)", pathNameSpace);

  protected static final ItemPath entry2entryToCvRelation = new ItemPath("entry2entry.cv_relation", pathNameSpace);
  protected static final ItemPath entry2compToCvRelation = new ItemPath("entry2comp.cv_relation", pathNameSpace);


  public static Map getPrefetchDescriptors() {
    Map paths = new HashMap();

    {
      Set proteinSet = new HashSet();

      proteinSet.add(taxonomyFromProtein.getItemPrefetchDescriptor());

      ItemPrefetchDescriptor p2mDesc = new ItemPrefetchDescriptor("(protein <- matches.protein)");
      p2mDesc.addConstraint(new ItemPrefetchConstraintDynamic(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "protein"));
      p2mDesc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME, "http://www.flymine.org/model/interpro#matches", false));
      ItemPrefetchDescriptor p2m2mDesc = new ItemPrefetchDescriptor("(protein <- matches.protein).method");
      p2m2mDesc.addConstraint(new ItemPrefetchConstraintDynamic("method", ObjectStoreItemPathFollowingImpl.IDENTIFIER));
      p2mDesc.addPath(p2m2mDesc);
      proteinSet.add(p2mDesc);

      ItemPrefetchDescriptor p2smDesc = new ItemPrefetchDescriptor("(protein <- supermatch.protein)");
      p2smDesc.addConstraint(new ItemPrefetchConstraintDynamic(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "protein"));
      p2smDesc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME, "http://www.flymine.org/model/interpro#supermatch", false));
      ItemPrefetchDescriptor p2sm2eDesc = new ItemPrefetchDescriptor("(protein <- supermatch.protein).entry");
      p2sm2eDesc.addConstraint(new ItemPrefetchConstraintDynamic("entry", ObjectStoreItemPathFollowingImpl.IDENTIFIER));
      p2smDesc.addPath(p2sm2eDesc);
      proteinSet.add(p2smDesc);

      paths.put("http://www.flymine.org/model/interpro#protein", proteinSet);
    }
    {
      Set methodSet = new HashSet();

      ItemPrefetchDescriptor m2cvdDesc = new ItemPrefetchDescriptor("(method.cv_database)");
      m2cvdDesc.addConstraint(new ItemPrefetchConstraintDynamic("cv_database", ObjectStoreItemPathFollowingImpl.IDENTIFIER));
      methodSet.add(m2cvdDesc);

      ItemPrefetchDescriptor m2mDesc = new ItemPrefetchDescriptor("(method <- matches.method)");
      m2mDesc.addConstraint(new ItemPrefetchConstraintDynamic(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "method"));
      m2mDesc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME, "http://www.flymine.org/model/interpro#matches", false));
      ItemPrefetchDescriptor m2m2pDesc = new ItemPrefetchDescriptor("(method <- matches.method).protein");
      m2m2pDesc.addConstraint(new ItemPrefetchConstraintDynamic("protein", ObjectStoreItemPathFollowingImpl.IDENTIFIER));
      m2mDesc.addPath(m2m2pDesc);
      methodSet.add(m2mDesc);

      ItemPrefetchDescriptor m2e2eDesc = new ItemPrefetchDescriptor("(method <- entry2method.method)");
      m2e2eDesc.addConstraint(new ItemPrefetchConstraintDynamic(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "method"));
      m2e2eDesc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME, "http://www.flymine.org/model/interpro#entry2method", false));
      ItemPrefetchDescriptor m2e2m2eDesc = new ItemPrefetchDescriptor("(method <- entry2method.method).entry");
      m2e2m2eDesc.addConstraint(new ItemPrefetchConstraintDynamic("entry", ObjectStoreItemPathFollowingImpl.IDENTIFIER));
      m2e2eDesc.addPath(m2e2m2eDesc);
      ItemPrefetchDescriptor m2e2m2cveDesc = new ItemPrefetchDescriptor("(method <- entry2method.method).cv_evidence");
      m2e2m2cveDesc.addConstraint(new ItemPrefetchConstraintDynamic("cv_evidence", ObjectStoreItemPathFollowingImpl.IDENTIFIER));
      m2e2eDesc.addPath(m2e2m2cveDesc);
      methodSet.add(m2e2eDesc);

      paths.put("http://www.flymine.org/model/interpro#method", methodSet);
    }
    {
      Set entrySet = new HashSet();

      ItemPrefetchDescriptor e2cvetDesc = new ItemPrefetchDescriptor("(entry.cv_entry_type)");
      e2cvetDesc.addConstraint(new ItemPrefetchConstraintDynamic("cv_entry_type", ObjectStoreItemPathFollowingImpl.IDENTIFIER));
      entrySet.add(e2cvetDesc);

      ItemPrefetchDescriptor e2cDesc = new ItemPrefetchDescriptor("(entry <- entry2common_annotation.entry)");
      e2cDesc.addConstraint(new ItemPrefetchConstraintDynamic(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "entry"));
      e2cDesc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME, "http://www.flymine.org/model/interpro#entry2common_annotation", false));
      ItemPrefetchDescriptor e2ec2cDesc = new ItemPrefetchDescriptor("(entry <- entry2common_annotation.entry).common_annotation");
      e2ec2cDesc.addConstraint(new ItemPrefetchConstraintDynamic("common_annotation", ObjectStoreItemPathFollowingImpl.IDENTIFIER));
      e2cDesc.addPath(e2ec2cDesc);
      entrySet.add(e2cDesc);

      ItemPrefetchDescriptor e2e2mDesc = new ItemPrefetchDescriptor("(entry <- entry2method.entry)");
      e2e2mDesc.addConstraint(new ItemPrefetchConstraintDynamic(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "entry"));
      e2e2mDesc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME, "http://www.flymine.org/model/interpro#entry2method", false));
      ItemPrefetchDescriptor e2e2m2mDesc = new ItemPrefetchDescriptor("(entry <- entry2method.entry).method");
      e2e2m2mDesc.addConstraint(new ItemPrefetchConstraintDynamic("method", ObjectStoreItemPathFollowingImpl.IDENTIFIER));
      e2e2mDesc.addPath(e2e2m2mDesc);
      entrySet.add(e2e2mDesc);

      /*
        ItemPrefetchDescriptor e2ecvdDesc = new ItemPrefetchDescriptor("(entry <- entry2cv_database.entry)");
        e2ecvdDesc.addConstraint(new ItemPrefetchConstraintDynamic(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "entry"));
        e2ecvdDesc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME, "http://www.flymine.org/model/interpro#entry2cv_database", false));
        ItemPrefetchDescriptor e2e2cvd2cvd = new ItemPrefetchDescriptor("(entry <- entry2cv_database.entry).cv_database");
        e2e2cvd2cvd.addConstraint(new ItemPrefetchConstraintDynamic("cv_database", ObjectStoreItemPathFollowingImpl.IDENTIFIER));
        e2ecvdDesc.addPath(e2e2cvd2cvd);
        entrySet.add(e2ecvdDesc);
      */

      ItemPrefetchDescriptor e2smDesc = new ItemPrefetchDescriptor("(entry <- supermatch.entry)");
      e2smDesc.addConstraint(new ItemPrefetchConstraintDynamic(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "entry"));
      e2smDesc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME, "http://www.flymine.org/model/interpro#supermatch", false));
      ItemPrefetchDescriptor e2sm2pDesc = new ItemPrefetchDescriptor("(entry <- supermatch.entry).protein");
      e2sm2pDesc.addConstraint(new ItemPrefetchConstraintDynamic("protein", ObjectStoreItemPathFollowingImpl.IDENTIFIER));
      e2smDesc.addPath(e2sm2pDesc);
      entrySet.add(e2smDesc);

      ItemPrefetchDescriptor eToe2cViaE1Desc = new ItemPrefetchDescriptor("(entry <- entry2comp.entry1)");
      eToe2cViaE1Desc.addConstraint(new ItemPrefetchConstraintDynamic(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "entry1"));
      eToe2cViaE1Desc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME, "http://www.flymine.org/model/interpro#entry2comp", false));
      ItemPrefetchDescriptor eToe2cViaE1ToE2Desc = new ItemPrefetchDescriptor("(entry <- entry2comp.entry1).entry2");
      eToe2cViaE1ToE2Desc.addConstraint(new ItemPrefetchConstraintDynamic("entry2", ObjectStoreItemPathFollowingImpl.IDENTIFIER));
      eToe2cViaE1Desc.addPath(eToe2cViaE1ToE2Desc);
      entrySet.add(eToe2cViaE1Desc);

      ItemPrefetchDescriptor eToe2cViaE2Desc = new ItemPrefetchDescriptor("(entry <- entry2comp.entry2)");
      eToe2cViaE2Desc.addConstraint(new ItemPrefetchConstraintDynamic(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "entry2"));
      eToe2cViaE2Desc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME, "http://www.flymine.org/model/interpro#entry2comp", false));
      ItemPrefetchDescriptor eToe2cViaE2ToE1Desc = new ItemPrefetchDescriptor("(entry <- entry2comp.entry2).entry1");
      eToe2cViaE2ToE1Desc.addConstraint(new ItemPrefetchConstraintDynamic("entry1", ObjectStoreItemPathFollowingImpl.IDENTIFIER));
      eToe2cViaE2Desc.addPath(eToe2cViaE2ToE1Desc);
      entrySet.add(eToe2cViaE2Desc);

      ItemPrefetchDescriptor eToe2eViaEDesc = new ItemPrefetchDescriptor("(entry <- entry2entry.entry)");
      eToe2eViaEDesc.addConstraint(new ItemPrefetchConstraintDynamic(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "entry"));
      eToe2eViaEDesc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME, "http://www.flymine.org/model/interpro#entry2entry", false));
      ItemPrefetchDescriptor eToe2eViaEToPDesc = new ItemPrefetchDescriptor("(entry <- entry2entry.entry).parent");
      eToe2eViaEToPDesc.addConstraint(new ItemPrefetchConstraintDynamic("parent", ObjectStoreItemPathFollowingImpl.IDENTIFIER));
      eToe2eViaEDesc.addPath(eToe2eViaEToPDesc);
      entrySet.add(eToe2eViaEDesc);

      ItemPrefetchDescriptor eToe2eViaPDesc = new ItemPrefetchDescriptor("(entry <- entry2entry.parent)");
      eToe2eViaPDesc.addConstraint(new ItemPrefetchConstraintDynamic(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "parent"));
      eToe2eViaPDesc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME, "http://www.flymine.org/model/interpro#entry2entry", false));
      ItemPrefetchDescriptor eToe2eViaPToEDesc = new ItemPrefetchDescriptor("(entry <- entry2entry.parent).entry");
      eToe2eViaPToEDesc.addConstraint(new ItemPrefetchConstraintDynamic("entry", ObjectStoreItemPathFollowingImpl.IDENTIFIER));
      eToe2eViaPDesc.addPath(eToe2eViaPToEDesc);
      entrySet.add(eToe2eViaPDesc);

      paths.put("http://www.flymine.org/model/interpro#entry", entrySet);
    }
    {
      Set matchesSet = new HashSet();

      ItemPrefetchDescriptor m2cvdbDesc = new ItemPrefetchDescriptor("(matches.cv_database)");
      m2cvdbDesc.addConstraint(new ItemPrefetchConstraintDynamic("cv_database", ObjectStoreItemPathFollowingImpl.IDENTIFIER));
      matchesSet.add(m2cvdbDesc);

      ItemPrefetchDescriptor m2cvevDesc = new ItemPrefetchDescriptor("(matches.cv_evidence)");
      m2cvevDesc.addConstraint(new ItemPrefetchConstraintDynamic("cv_evidence", ObjectStoreItemPathFollowingImpl.IDENTIFIER));
      matchesSet.add(m2cvevDesc);

      paths.put("http://www.flymine.org/model/interpro#matches", matchesSet);
    }
    {
      Set entry2entrySet = new HashSet();

      entry2entrySet.add(entry2entryToCvRelation.getItemPrefetchDescriptor());

      paths.put("http://www.flymine.org/model/interpro#entry2entry", entry2entrySet);
    }
    {
      Set entry2compSet = new HashSet();

      entry2compSet.add(entry2compToCvRelation.getItemPrefetchDescriptor());

      paths.put("http://www.flymine.org/model/interpro#entry2comp", entry2compSet);
    }
    return paths;
  }

  /**
   * @return Creates a Database item with the description set to Interpro - used for evidence tagging...
   */
  private Item getInterproEvidenceItem() {

    if (interproEvidenceItem == null) {
      interproEvidenceItem = createItem("Database");
      interproEvidenceItem.setAttribute("title", "InterPro");
    }

    return interproEvidenceItem;
  }
}
