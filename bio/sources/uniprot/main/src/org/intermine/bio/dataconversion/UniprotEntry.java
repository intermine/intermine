package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.intermine.xml.full.Item;

/**
 * holder class for each entry in uniprot xml
 * @author julie
 *
 */
public class UniprotEntry
{
    private String datasetRefId = null;
    private String length, molecularWeight;
    private List<String> features = new ArrayList();
    private Map<String, String> dbrefs = new HashMap();
    private List<String> domains = new ArrayList();
    private List<String> pubs = new ArrayList();
    private List<String> comments = new ArrayList();
    private List<String> keywords = new ArrayList();
    // TODO demote this to regular list.  linked list only to make tests pass.
    private LinkedList<String> accessions = new LinkedList();
    private List<String> descriptions = new ArrayList();
    private Map<String, String> genes = new HashMap();

    private boolean isDuplicate = false;
    private String taxonId, name, isFragment;
    private String primaryIdentifier, primaryaccession;
    private String seqRefId, md5checksum;

    // temporary object that holds attribute value until the item is stored on the next line of XML
    private String temp = null;
    private Item feature = null;

    /**
     * holds the value until the item is processed/stored on the next line
     * @param value protein domain identifier, comment.type, etc
     */
    public void addAttribute(String value) {
        temp = value;
    }

    /**
     * the identifier is only held until the next line of the XML is processed, at which
     * point the item is stored
     * @return variable
     */
    public String getAttribute() {
        return temp;
    }

    /**
     * this check is necessary because the xml attributes may occur in other XML entries
     * @return true if tag is currently being processed in the XML
     */
    public boolean processing() {
        return temp != null;
    }

    /**
     * this temporary variable is set whilst we are processing items that span multiple XML tags.
     * this is a check to make sure once we are finished processing the item, this variable is set
     * to null.
     *
     * items with data spanning multiple XML tags:
     *  comments
     *  domains
     *  features
     */
    public void reset() {
        temp = null;
        feature = null;
    }

    private void addRefId(List list, String refId) {
        list.add(refId);
        reset();
    }

    /**
     * @param refId id representing protein domain intermine object
     */
    public void addDomainRefId(String refId) {
        addRefId(domains, refId);
    }

    /**
     * @return the domains
     */
    public List<String> getDomains() {
        return domains;
    }

    /**
     * @param refId id representing comment intermine object
     */
    public void addCommentRefId(String refId) {
        addRefId(comments, refId);
    }

    /**
     * @return the comments
     */
    public List<String> getComments() {
        return comments;
    }

    /**
     * this check is necessary because the xml attributes may occur in other XML entries.
     * and the config file can specify valid features, so we aren't processing all features
     * in the XML file
     * @return true if valid feature is currently being processed in the XML
     */
    public boolean processingFeature() {
        return feature != null;
    }

    /**
     * new feature for protein
     * feature.type and feature.description are set first.  then the <location> bit is processed.
     * we hold this (already stored) object just until the location object is stored.
     * @param item temporary object
     */
    public void addFeature(Item item) {
        if (item != null) {
            feature = item;
            features.add(item.getIdentifier());
        }
    }

    /**
     * @param orientation begin or end
     * @param position position
     */
    public void addFeatureLocation(String orientation, String position) {
        feature.setAttribute(orientation, position);
    }

    /**
     * @return list of ids representing feature objects for this entry
     */
    public List<String> getFeatures() {
        return features;
    }

    /**
     * @return the dbrefs
     */
    public Map<String, String> getDbrefs() {
        return dbrefs;
    }

    /**
     * add dbref to list.  these will be processed later for gene identifiers
     * eg <dbReference type="FlyBase" id="FBgn0004889" key="52">
     * @param type datasource
     * @param id identifier
     */
    public void addDbref(String type, String id) {
        dbrefs.put(type, id);
    }

    /**
     * @return list of refIds representing the publication objects
     */
    public List getPubs() {
        return pubs;
    }

    /**
     * @param pubmedId the id for the pub
     */
    public void addPub(String pubmedId) {
        pubs.add(pubmedId);
    }

    /**
     * @return list of refIds representing the keyword objects
     */
    public List getKeywords() {
        return keywords;
    }

    /**
     * @param keyword keyword refId
     */
    public void addKeyword(String keyword) {
        keywords.add(keyword);
    }

    /**
     * @param type type of variable, eg. ORF, primary
     * @param value value of variable, eg FBgn, CG
     */
    public void addGene(String type, String value) {
        genes.put(type, value);
    }

    /**
     * @return map of types (eg ORF) and gene names
     */
    public Map<String, String> getGenes() {
        return genes;
    }

    /**
     * some proteins don't have accessions.  i don't know why but we don't want them
     * @return true if protein has primary accession
     */
    public boolean hasPrimaryAccession() {
        return (primaryaccession != null);
    }

    /**
     * @param accession value
     */
    public void addAccession(String accession) {
        if (primaryaccession != null) {
            accessions.add(accession);
        } else {
            primaryaccession = accession;
        }
    }

    /**
     * @return list of accessions
     */
    public List<String> getAccessions() {
        return accessions;
    }

    /**
     * returns true if the protein has a dataset of SwissProt or Trembl.  false if it has UniParc
     * or some other rubbish.  I don't know why those are included inthe XML we use, but they are
     * so we have to check this.
     * @return true if this entry has a dataset
     */
    public boolean hasDatasetRefId() {
        return (datasetRefId != null);
    }

    /**
     * @return the datasetRefId
     */
    public String getDatasetRefId() {
        return datasetRefId;
    }

    /**
     * @param datasetRefId the datasetRefId to set
     */
    public void setDatasetRefId(String datasetRefId) {
        this.datasetRefId = datasetRefId;
    }

    /**
     * @return the taxonId
     */
    public String getTaxonId() {
        return taxonId;
    }

    /**
     * @param taxonId the taxonId to set
     */
    public void setTaxonId(String taxonId) {
        this.taxonId = taxonId;
    }

    /**
     * @return the isFragmant
     */
    public String isFragment() {
        return isFragment;
    }

    /**
     * @param isFragment the isFragmant to set
     */
    public void setFragment(String isFragment) {
        this.isFragment = isFragment;
    }

    /**
     * @return the length
     */
    public String getLength() {
        return length;
    }

    /**
     * @param length the length to set
     */
    public void setLength(String length) {
        this.length = length;
    }

    /**
     * @return the molecularWeight
     */
    public String getMolecularWeight() {
        return molecularWeight;
    }

    /**
     * @param molecularWeight the molecularWeight to set
     */
    public void setMolecularWeight(String molecularWeight) {
        this.molecularWeight = molecularWeight;
    }

    /**
     * @return the md5checksum
     */
    public String getMd5checksum() {
        return md5checksum;
    }

    /**
     * @param md5checksum the md5checksum to set
     */
    public void setMd5checksum(String md5checksum) {
        this.md5checksum = md5checksum;
    }

    /**
     * @return the seqRefId
     */
    public String getSeqRefId() {
        return seqRefId;
    }

    /**
     * @param seqRefId the seqRefId to set
     */
    public void setSeqRefId(String seqRefId) {
        this.seqRefId = seqRefId;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the primaryaccession
     */
    public String getPrimaryAccession() {
        return primaryaccession;
    }

    /**
     * @param description the description to add
     */
    public void addDescription(String description) {
        descriptions.add(description);
    }

    /**
     * @return the set of descriptions
     */
    public List<String> getDescriptions() {
        return descriptions;
    }

    /**
     * @return the primaryIdentifier
     */
    public String getPrimaryIdentifier() {
        return primaryIdentifier;
    }

    /**
     * @param primaryIdentifier the primaryIdentifier to set
     */
    public void setPrimaryIdentifier(String primaryIdentifier) {
        this.primaryIdentifier = primaryIdentifier;
    }

    /**
     * @return list of all the synonyms for this entry, including name and accessions
     */
    public List getSynonyms() {
        List<String> synonyms = new ArrayList();
        synonyms.addAll(accessions);
        synonyms.add(primaryaccession);
        synonyms.add(name);
        return synonyms;
    }

    /**
     * if duplicate, the protein will not be processed
     * @return isDuplicate - whether or not this trembl protein has a duplicate swissprot entry
     */
    public boolean isDuplicate() {
        return isDuplicate;
    }

    /**
     * @param isDuplicate the isDuplicate to set
     */
    public void setDuplicate(boolean isDuplicate) {
        this.isDuplicate = isDuplicate;
    }

}