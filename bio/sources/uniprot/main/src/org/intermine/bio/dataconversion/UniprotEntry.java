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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.xml.full.Item;

/**
 * holder class representing an entry in uniprot xml
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
    private List<String> accessions = new ArrayList();
    private List<String> isoforms = new ArrayList();
    private List<String> isoformSynonyms = new ArrayList();
    private List<String> components = new ArrayList();
    private List<String> proteinNames = new ArrayList(); // used for synonyms only
    private Map<String, String> geneNames = new HashMap();  // type, value eg primary, UBI3
    // other gene identifiers are stored in dbrefs map

    // map of gene designation to dbref
    private Map<String, Map<String, String>> geneDesignationToDbrefs = new HashMap();

    private boolean isDuplicate = false, isIsoform = false, hasMultipleGenes = false;
    private boolean duplicateGene = false;
    private String taxonId, name, isFragment;
    private String primaryAccession, uniprotAccession, primaryIdentifier;
    private String seqRefId, md5checksum;

    // temporary object that holds attribute value until the item is stored on the next line of XML
    private String temp = null;
    private Item feature = null;

    // only used if entry has multiple genes
    private List<UniprotGene> geneEntries = new ArrayList();
    private UniprotGene geneEntry = null; // <gene><name> ... being processed

    /**
     * constructor used for non-isoform entries
     */
    public UniprotEntry() {
        // constructor used for non-isoform entries
    }

    /**
     * @param primaryAccession for this entry
     */
    public UniprotEntry(String primaryAccession) {
        this.primaryAccession = primaryAccession;
    }

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
        String attribute = temp;
        temp = null;
        return attribute;
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
     *
     *  difficulties with genes only arise when there are multiple genes for one protein.  in that
     *  case the XML contains many identifiers for several genes, and it becomes difficult to
     *  match each gene to the corresponding identifiers
     */
    public void reset() {
        temp = null;
        feature = null;
        geneEntry = null;
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
        reset();
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
     * used to get the feature to store.  feature can't be stored until the location has been
     * processed
     * @return uniprot feature
     */
    public Item getFeature() {
        Item currentFeature = feature;
        feature = null; // we are storing this feature, so reset temp var
        return currentFeature;
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
     *
     * if a protein has multiple genes, the gene designation is needed too.
     * <dbReference type="SGD" id="S000004157" key="95">
     *   <property type="gene designation" value="RPS31"/>
     * </dbReference>
     *
     * @param type datasource
     * @param id identifier
     */
    public void addDbref(String type, String id) {
        if (hasMultipleGenes) {
            dbrefs.clear();
            /* since the data is on a different line in the XML, there is a chance badly formed
             * data could cause the wrong identifier to be matched with the wrong gene.  this
             * id will be checked on the next line to ensure we still have the same name/value
             * pair */
            addAttribute(id);
        }
        dbrefs.put(type, id);
    }

    /**
     * geneDesignation is required in the case of a single protein having multiple gene identifiers.
     * the geneDesignation (often the "primary" name, but it can be a synonym) is used to match
     * the dbref entries to the correct gene.
     *
     * this is only important when multiple identifiers are assigned, as in the case of yeast.
     * @param geneDesignation "gene designation" for this gene.  usually the "primary" name
     */
    public void addGeneDesignation(String geneDesignation) {
        // safety check
        Collection<String> values = dbrefs.values();
        if (!values.contains(temp)) {
            throw new RuntimeException("error processing multiple genes - " + temp);
        }
        if (geneDesignationToDbrefs.get(geneDesignation) == null) {
            geneDesignationToDbrefs.put(geneDesignation, dbrefs);
        } else {
            geneDesignationToDbrefs.get(geneDesignation).putAll(dbrefs);
        }
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
     * @return list of refIds representing the keyword objects
     */
    public List<String> getComponents() {
        return components;
    }

    /**
     * @param component name of component
     */
    public void addComponent(String component) {
        components.add(component);
    }


    /**
     * @param type type of variable, eg. ORF, primary
     * @param value value of variable, eg FBgn, CG
     */
    public void addGene(String type, String value) {
        if (hasMultipleGenes) {
            if (geneEntry == null) {
                geneEntry = new UniprotGene();
            }
            geneEntry.addGene(type, value);
        } else {
            geneNames.put(type, value);
        }
    }

    /**
     * used when making a dummy entry only.  when a protein is parsed that has multiple genes,
     * a dummy entry is created for each gene so the identifiers can be assigned correctly.
     * @param geneNames list of identifiers, type to value
     */
    public void setGeneNames(Map<String, String> geneNames) {
        this.geneNames = geneNames;
    }

    /**
     * @return map of types (eg ORF) and gene names
     */
    public Map<String, String> getGeneNames() {
        return geneNames;
    }

    /**
     * @return list of genes that need to be processed for this uniprot entry.  this list should
     * be empty for proteins that have a single gene.  in those cases, the values for the single
     * gene are in the geneNames and dbrefs collections.
     */
    public List<Map<String, String>> getGeneEntries() {
        Iterator<UniprotEntry.UniprotGene> it = geneEntries.iterator();
        List<Map<String, String>> identifiers = new ArrayList();
        while (it.hasNext()) {
            UniprotGene uniprotGene = it.next();
            identifiers.add(uniprotGene.getGenes());
        }
        return identifiers;
    }

    /**
     * @return map of "gene designation" values (usually the "primary" value) to dbref entries
     */
    public Map<String, Map<String, String>> getGeneDesignationsToDbrefs() {
        return geneDesignationToDbrefs;
    }


    /**
     * some proteins don't have accessions.  i don't know why but we don't want them
     * @return true if protein has primary accession
     */
    public boolean hasPrimaryAccession() {
        return (primaryAccession != null);
    }

    /**
     * @param accession value
     */
    public void addAccession(String accession) {
        if (primaryAccession != null) {
            accessions.add(accession);
        } else {
            primaryAccession = accession;
            uniprotAccession = accession;
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
        return primaryAccession;
    }

    /**
     * @return the uniprotAccession
     */
    public String getUniprotAccession() {
        return uniprotAccession;
    }

    private void setUniprotAccession(String accession) {
        uniprotAccession = accession;
    }

    /**
     * used to assign sequences
     * @return list of all the synonyms for this entry, including name and accessions but not
     * isoform synonyms
     */
    public List getSynonyms() {
        List<String> synonyms = new ArrayList();
        synonyms.addAll(accessions);
        synonyms.add(primaryAccession);
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

    /**
     * @return the isIsoform
     */
    public boolean isIsoform() {
        return isIsoform;
    }

    /**
     * sets isIsoform to be false.  This is the isoform, but it's the canonical one so it is not
     * processed any different from a regular uniprot protein.
     * A synonym is made for the isoform accession, usually something like Q1234-1.  The
     * primary accession is not changed for this protein due to integration issues.
     *
     * The uniprotAccession for all other isoforms will be the same as this entry's primaryaccession
     * @param accession for this isoform
     */
    public void setCanonicalIsoform(String accession) {
        isIsoform = false;
        accessions.add(accession);
    }

    /**
     * @param isIsoform whether or not this protein an isoform
     */
    public void setIsoform(boolean isIsoform) {
        this.isIsoform = isIsoform;
    }

    /**
     * @param accession of the isoform
     */
    public void addIsoform(String accession) {
        isoforms.add(accession);
    }

    /**
     * @return list of isoform accessions for this uniprot entry
     */
    public List<String> getIsoforms() {
        return isoforms;
    }

    /**
     * if an isoform has two ID tags, then the first one is used and the second one is added
     * as a synonym
     * @param accession of the isoform
     */
    public void addIsoformSynonym(String accession) {
        isoformSynonyms.add(accession);
    }

    /**
     * if an isoform has two ID tags, then the first one is used and the second one is added
     * as a synonym
     * @return list of isoform synonyms
     */
    public List<String> getIsoformSynonyms() {
        return isoformSynonyms;
    }


    /**
     * the name section in uniprot can contain several names, eg. recommendedName, alternateName,
     * etc.  all of these should be synonyms
     * @param proteinName name for the protein, eg. recommendedName, alternateName, etc
     */
    public void addProteinName(String proteinName) {
        proteinNames.add(proteinName);
    }

    /**
     * if an isoform has two ID tags, then the first one is used and the second one is added
     * as a synonym
     * @return list of isoform synonyms
     */
    public List<String> getProteinNames() {
        return proteinNames;
    }

    /**
     * @param proteinNames the proteinNames to set
     */
    public void setProteinNames(List<String> proteinNames) {
        this.proteinNames = proteinNames;
    }

    /**
     * @param dbrefs the dbrefs to set
     */
    public void setDbrefs(Map<String, String> dbrefs) {
        this.dbrefs = dbrefs;
    }

    /**
     * @param domains the domains to set
     */
    public void setDomains(List<String> domains) {
        this.domains = domains;
    }

    /**
     * @param pubs the pubs to set
     */
    public void setPubs(List<String> pubs) {
        this.pubs = pubs;
    }

    /**
     * @param comments the comments to set
     */
    public void setComments(List<String> comments) {
        this.comments = comments;
    }

    /**
     * @param keywords the keywords to set
     */
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    /**
     * @param accessions the accessions to set
     */
    public void setAccessions(List<String> accessions) {
        this.accessions = accessions;
    }

    /**
     * @return true if this entry contains more than one gene
     */
    public boolean hasMultipleGenes() {
        return hasMultipleGenes;
    }

    /**
     * @param hasMultipleGenes true if this entry contains more than one gene
     */
    public void setHasMultipleGenes(boolean hasMultipleGenes) {
        this.hasMultipleGenes = hasMultipleGenes;

        if (hasMultipleGenes) {
            UniprotGene uniprotGene = new UniprotGene();
            geneEntries.add(uniprotGene);
            // first gene has already been parsed, so add these identifiers to new object
            uniprotGene.geneIdentifiers.putAll(geneNames);
        }
    }


    /**
     * class representing a gene in a uniprot entry
     * used for holding multiple identifiers for a single gene.  only used
     * when there are multiple genes, otherwise the identifiers are held in
     * the converter
     * @author julie
     */
    public class UniprotGene
    {
        /* same as geneNames map above, but we need one map per gene in cases where there is
         * more than one gene per protein */
        protected Map<String, String> geneIdentifiers = new HashMap();

        /**
         * constructor
         */
        public UniprotGene() {
            // nothing to do
        }

        /**
         * @param type type of variable, eg. ORF, primary
         * @param value value of variable, eg FBgn, CG
         */
        protected void addGene(String type, String value) {
            geneIdentifiers.put(type, value);
        }

        /**
         * @return map of types (eg ORF) and gene names
         */
        protected Map<String, String> getGenes() {
            return geneIdentifiers;
        }
    }

    /**
     * @return the duplicateGene
     */
    public boolean isDuplicateGene() {
        return duplicateGene;
    }

    /**
     * @param duplicateGene the duplicateGene to set
     */
    public void setDuplicateGene(boolean duplicateGene) {
        this.duplicateGene = duplicateGene;
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
     * no:
     *  features
     *  gene items, just identifiers  - for memory reasons
     *  sequence, length, molecular weight, md5checksum
     *  components - per rachel
     *
     *
     * @param accession for isoform
     * @return cloned uniprot entry, an isoform of original entry
     */
    public UniprotEntry clone(String accession) {
        UniprotEntry entry = new UniprotEntry(accession);
        entry.setIsoform(true);
        entry.setDuplicate(false);
        entry.setDatasetRefId(datasetRefId);
        entry.setPrimaryIdentifier(primaryIdentifier);
        entry.setTaxonId(taxonId);
        entry.setName(name);
        entry.setFragment(isFragment);
        entry.setUniprotAccession(uniprotAccession);
        entry.setDbrefs(dbrefs);
        entry.setAccessions(accessions);
        entry.setComments(comments);
        entry.setDomains(domains);
        entry.setPubs(pubs);
        entry.setKeywords(keywords);
        entry.setGeneNames(geneNames);
        entry.setProteinNames(proteinNames);
        entry.setHasMultipleGenes(hasMultipleGenes);
        return entry;
    }
}
