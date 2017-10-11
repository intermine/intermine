package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.metadata.Util;
import org.intermine.xml.full.Item;

/**
 * holder class representing an entry in uniprot xml
 * @author julie
 *
 */
public class UniprotEntry
{
    private static final Logger LOG = Logger.getLogger(UniprotEntry.class);
    private String datasetRefId = null;
    private String length, molecularWeight;
    private Set<Item> features = new HashSet<Item>();
    private Map<Integer, List<String>> commentEvidence = new HashMap<Integer, List<String>>();
    private boolean isIsoform = false, isFragment = false;
    private String taxonId, name;
    private String primaryAccession, uniprotAccession, primaryIdentifier;
    private String sequence, md5checksum;
    private Map<String, List<String>> collections = new HashMap<String, List<String>>();
    private Map<String, String> pubEvidenceCodeToRef = new HashMap<String, String>();
    private boolean isDuplicate = false;
    private Map<String, Set<String>> dbrefs = new HashMap<String, Set<String>>();
    private Map<String, Set<String>> geneNames = new HashMap<String, Set<String>>();
    private Map<String, String> goTermToEvidenceCode = new HashMap<String, String>();

    // map of gene designation (normally the primary name) to dbref (eg. FlyBase, FBgn001)
    // this map is used when there is more than one gene but the dbref is needed to set an
    // identifier
    private Map<String, String> geneDesignationToDbref = new HashMap<String, String>();

    // temporary objects that hold attribute value until the item is stored
    // usually on the next line of XML
    private String temp = null;
    private Item feature = null;
    private Dbref dbref = null;
    private Comment comment = null; //<comment><text> ... being processed

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
     *  dbrefs (eg. gene designation)
     *  genes (eg. ORF and primary names)
     *
     *  difficulties with genes only arise when there are multiple genes for one protein.  in that
     *  case the XML contains many identifiers for several genes, and it becomes difficult to
     *  match each gene to the corresponding identifiers
     */
    public void reset() {
        temp = null;
        feature = null;
        dbref = null;
    }

    private void addRefId(String collectionName, String refId) {
        addToCollection(collectionName, refId);
        reset();
    }

    /**
     * @param collectionName name of collection
     * @return the collection specified
     */
    public List<String> getCollection(String collectionName) {
        List<String> values = collections.get(collectionName);
        if (values == null) {
            values = new ArrayList();
            collections.put(collectionName, values);
        }
        return values;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void addToCollection(String collectionName, String value) {
        getCollection(collectionName).add(value);
    }

    /**
     * @param refId id representing comment intermine object
     * @param objectId id representing the object in the database.  used later to add pub collection
     */
    public void addCommentRefId(String refId, Integer objectId) {
        commentEvidence.put(objectId, new ArrayList(comment.evidence));
        addRefId("comments", refId);
    }

    /**
     * Creates a new temporary object to hold the type and later the publications for this comment.
     *
     * @param type "type" of comment
     */
    public void setCommentType(String type) {
        comment = new Comment(type);
    }

    /**
     * @return type "type" of comment
     */
    public String getCommentType() {
        return comment.type;
    }

    /**
     *
     * @param evidence space delimited list of evidence codes
     */
    public void setCommentEvidence(String evidence) {
        String[] bits = evidence.split(" ");
        for (String bit : bits) {
            comment.addEvidence(bit);
        }
    }

    /**
     * @return map from commentRefId to evidence codes
     */
    public Map<Integer, List<String>> getCommentEvidence() {
        return commentEvidence;
    }

    /**
     * @return true if this entry has comments
     */
    public boolean hasComments() {
        return collections.get("comments") != null;
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
            features.add(item);
        }
    }

    /**
     * used to get the feature to store.  feature can't be stored until the location has been
     * processed
     * @return uniprot feature
     */
    public Item getFeature() {
        Item currentFeature = feature;
        reset();
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
     * @return list of items representing feature objects for this entry
     */
    public Set<Item> getFeatures() {
        return features;
    }

    /**
     * @return list of refIds representing the publication objects
     */
    public List<String> getPubs() {
        return collections.get("pubs");
    }

    /**
     * @param pubmedId the id for the pub
     */
    public void addPub(String pubmedId) {
        addToCollection("pubs", pubmedId);
    }

    /**
     * @return list of ecNumbers for this protein
     */
    public List<String> getECNumbers() {
        return collections.get("ecNumbers");
    }

    /**
     * @param ecNumber for this protein
     */
    public void addECNumber(String ecNumber) {
        addToCollection("ecNumbers", ecNumber);
    }

    /**
     * @return list of refIds representing the keyword objects
     */
    public List<String> getKeywords() {
        return collections.get("keywords");
    }

    /**
     * @param keyword keyword refId
     */
    public void addKeyword(String keyword) {
        addToCollection("keywords", keyword);
    }

    /**
     * @return list of refIds representing the keyword objects
     */
    public List<String> getComponents() {
        return collections.get("components");
    }

    /**
     * @param component name of component
     */
    public void addComponent(String component) {
        addToCollection("components", component);
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
            addToCollection("accessions", accession);
        } else {
            primaryAccession = accession;
            uniprotAccession = accession;
        }
    }

    /**
     * @return list of accessions
     */
    @SuppressWarnings("unchecked")
    public List<String> getAccessions() {
        List<String> accessions = collections.get("accessions");
        if (accessions == null) {
            return Collections.EMPTY_LIST;
        }
        return collections.get("accessions");
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
        return Boolean.toString(isFragment);
    }

    /**
     * dev can configure converter to load these or not.  default is to ignore fragments
     * @param isFragment true of the protein is a fragment
     */
    public void setFragment(boolean isFragment) {
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
     * @param sequence the sequence to set
     */
    public void setSequence(String sequence) {
        this.sequence = sequence;
        this.md5checksum = Util.getMd5checksum(sequence);
    }

    /**
     * @return the protein sequence
     */
    public String getSequence() {
        return sequence;
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
    public List<String> getSynonyms() {
        List<String> synonyms = new ArrayList<String>();
        collections.get("synonyms").addAll(collections.get("accessions"));
        return synonyms;
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
    public void addCanonicalIsoform(String accession) {
        isIsoform = false;
        addToCollection("canonicalIsoformAccessions", accession);
        collections.get("canonicalIsoformAccessions").addAll(getIsoformSynonyms());
        collections.remove("isoformSynonyms");
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
        List<String> synonyms = getIsoformSynonyms();
        for (String s : synonyms) {
            accession += ("|" + s);
        }
        addToCollection("isoforms", accession);
        collections.remove("isoformSynonyms");
    }

    /**
     * @return list of isoform accessions for this uniprot entry
     */
    @SuppressWarnings("unchecked")
    public List<String> getIsoforms() {
        List<String> isoforms = collections.get("isoforms");
        if (isoforms == null) {
            return Collections.EMPTY_LIST;
        } else {
            return collections.get("isoforms");
        }
    }

    /**
     * if an isoform has two ID tags, then the first one is used and the second one is added
     * as a synonym
     * @param accession of the isoform
     */
    public void addIsoformSynonym(String accession) {
        addToCollection("isoformSynonyms", accession);
    }

    /**
     * if an isoform has two ID tags, then the first one is used and the second one is added
     * as a synonym
     * @return list of isoform synonyms
     */
    @SuppressWarnings("unchecked")
    public List<String> getIsoformSynonyms() {
        if (collections.get("isoformSynonyms") == null) {
            return Collections.EMPTY_LIST;
        }
        return collections.get("isoformSynonyms");
    }

    /**
     * @return the isDuplicate
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
     * the name section in uniprot can contain several names, eg. recommendedName, alternateName,
     * etc.  all of these should be synonyms
     * @param proteinName name for the protein, eg. recommendedName, alternateName, etc
     */
    public void addProteinName(String proteinName) {
        addToCollection("proteinNames", proteinName);
    }

    /**
     * if an isoform has two ID tags, then the first one is used and the second one is added
     * as a synonym
     * @return list of isoform synonyms
     */
    @SuppressWarnings("unchecked")
    public List<String> getProteinNames() {
        if (collections.get("proteinNames") == null) {
            return Collections.EMPTY_LIST;
        }
        return collections.get("proteinNames");
    }

    /**
     * @param proteinNames the proteinNames to set
     */
    public void setProteinNames(List<String> proteinNames) {
        collections.put("proteinNames", proteinNames);
    }

    /**
     * @param dbrefs the dbrefs to set
     */
    public void setDbrefs(Map<String, Set<String>> dbrefs) {
        this.dbrefs = dbrefs;
    }

    /**
     * @param geneDesignationToDbref map of gene designations to dbref
     */
    private void setGeneDesignations(Map<String, String> geneDesignationToDbref) {
        this.geneDesignationToDbref = geneDesignationToDbref;
    }

    /**
     * @param domains the domains to set
     */
    public void setDomains(List<String> domains) {
        collections.put("domains", domains);
    }

    /**
     * @param pubs the pubs to set
     */
    public void setPubs(List<String> pubs) {
        collections.put("pubs", pubs);
    }

    /**
     * @param comments the comments to set
     */
    protected void setComments(List<String> comments) {
        collections.put("comments", comments);
    }

    /**
     * @return list of comment RefIds
     */
    public List<String> getComments() {
        return collections.get("comments");
    }

    /**
     * @param keywords the keywords to set
     */
    public void setKeywords(List<String> keywords) {
        collections.put("keywords", keywords);
    }

    /**
     * @param accessions the accessions to set
     */
    public void setAccessions(List<String> accessions) {
        collections.put("accessions", accessions);
    }

    /**
     * @return the goterms
     */
    public List<String> getGOTerms() {
        List<String> goterms = collections.get("goTerms");
        if (goterms == null) {
            return Collections.emptyList();
        }
        return collections.get("goTerms");
    }

    /**
     * @param refId id representing a go term object
     */
    public void addGOTerm(String refId) {
        addToCollection("goTerms", refId);
    }

    /**
     * @param goTerm go term
     * @return evidence code for this go term
     */
    public String getGOEvidence(String goTerm) {
        return goTermToEvidenceCode.get(goTerm);
    }

    /**
     * @param goTerm go term
     * @param code evidence code, eg. NAS
     */
    public void addGOEvidence(String goTerm, String code) {
        goTermToEvidenceCode.put(goTerm, code);
    }

    /**
     * @param code the evidence code
     * @param pubRefId id representing publication object
     */
    public void addPubEvidence(String code, String pubRefId) {
        pubEvidenceCodeToRef.put(code, pubRefId);
    }

    /**
     * @param code evidence code, eg. EC1
     * @return the refId for publication associated with this evidence code
     */
    public String getPubRefId(String code) {
        return pubEvidenceCodeToRef.get(code);
    }

    /**
     * @param goterms list of go term refIds for this protein
     */
    public void setGOTerms(List<String> goterms) {
        collections.put("goTerms", goterms);
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

    // ============== genes =========================

    /**
     * From <genes>
     *
     * @param type type of variable, eg. ORF, primary
     * @param value value of variable, eg FBgn, CG
     */
    public void addGeneName(String type, String value) {
        // See #1199 - remove organism prefixes ("AgaP_" or "Dmel_")
        String geneName = value.replaceAll("^[A-Z][a-z][a-z][A-Za-z]_", "");
        Util.addToSetMap(geneNames, type, geneName);
        testForMultipleGenes(type);
    }

    // type is ORF, if there are multiple names saved as this type, we have multiple genes
    // assigned to this protein
    private boolean testForMultipleGenes(String type) {
        Set<String> values = geneNames.get(type);
        if (values.size() > 1) {
            return true;
        }
        return false;
    }

    /**
     * @return the dbrefs
     */
    public Map<String, Set<String>> getDbrefs() {
        return dbrefs;
    }

    /**
     * @return value of db reference currently being processed, eg. GO:001
     */
    public String getDbref() {
        return dbref.value;
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
        /* since the data is on a different line in the XML, there is a chance badly formed
         * data could cause the wrong identifier to be matched with the wrong gene.  this
         * id will be checked on the next line to ensure we still have the same name/value
         * pair */
        dbref = new Dbref(type, id);
        if (dbrefs.get(type) == null) {
            dbrefs.put(type, new HashSet<String>());
        }
        dbrefs.get(type).add(id);
    }

    /**
     * geneDesignation is required in the case of a single protein having multiple gene identifiers.
     * the geneDesignation (often the "primary" name, but it can be a synonym) is used to match
     * the dbref entries to the correct gene.
     *
     * this is especially important when multiple identifiers are assigned, as in the case of yeast.
     * @param identifier "gene designation" for this gene from the XML.
     */
    public void addGeneDesignation(String identifier) {
        if (dbref != null) {
            geneDesignationToDbref.put(identifier, dbref.type);
        } else {
            LOG.debug("Could not set 'gene designation' for dbref:" + dbref.value);
        }

    }

    /**
     *  <dbReference type="Ensembl" key="23" id="FBtr0082909">
     *      <property value="FBgn0010340" type="gene designation"/>
     * </dbReference>
     *
     * @param dbrefName name of database, eg Ensembl
     * @return gene designation for a certain dbref.type, eg Ensembl
     */
    public Set<String> getGeneDesignation(String dbrefName) {
        Set<String> identifiers = new HashSet<String>();
        for (Map.Entry<String, String> entry : geneDesignationToDbref.entrySet()) {
            if (entry.getValue().equals(dbrefName)) {
                identifiers.add(entry.getKey());
            }
        }
        return identifiers;
    }

    /**
     * @return true if entry has gene names
     */
    public Map<String, Set<String>> getGeneNames() {
        return geneNames;
    }

    /**
     * @param map original map of gene names
     */
    private void setGeneNames(Map<String, Set<String>> map) {
        geneNames = new HashMap<String, Set<String>>(map);
    }

    /**
     * Class representing a comment in a uniprot entry.
     */
    public class Comment
    {
        protected String type;
        // list of evidence, eg EC1.  to be replaced with pubRefIds when evidence is processed
        protected List<String> evidence = new ArrayList<String>();

        /**
         * @param type of comment
         */
        protected Comment(String type) {
            this.type = type;
        }

        /**
         * Will be replaced later with publication
         *
         * @param code evidence code, eg. "EC1"
         */
        protected void addEvidence(String code) {
            evidence.add(code);
        }
    }

    /**
     * class representing a dbref entry in a uniprot entry
     */
    public class Dbref
    {
        private String type;
        private String value;

        /**
         * @param type eg. FlyBase, SGD
         * @param value eg. FBgn
         */
        public Dbref(String type, String value) {
            this.type = type;
            this.value = value;
        }

        /**
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * @return the value
         */
        public String getValue() {
            return value;
        }

        /**
         * @return map representing dbref
         */
        public Map<String, String> toMap() {
            Map<String, String> dbrefMap = new HashMap<String, String>();
            dbrefMap.put(type, value);
            return dbrefMap;
        }
    }


    /**
     * no:
     *  features
     *  gene items, just identifiers  - for memory reasons
     *  sequence, length, molecular weight, md5checksum
     *  components - per rachel
     *  isoforms - per mike
     *
     * @param accession for isoform
     * @return cloned uniprot entry, an isoform of original entry
     */
    public UniprotEntry createIsoformEntry(String accession) {
        String[] bits = accession.split("\\|");
        accession = bits[0];
        UniprotEntry entry = new UniprotEntry(accession);
        entry.setIsoform(true);
        entry.setDatasetRefId(datasetRefId);
        entry.setPrimaryIdentifier(primaryIdentifier);
        entry.setTaxonId(taxonId);
        entry.setName(name);
        entry.setFragment(isFragment);
        entry.setUniprotAccession(uniprotAccession);
        entry.setDbrefs(dbrefs);
        entry.getCollection("accessions").addAll(getCollection("accessions"));
        for (int i = 1; i < bits.length; i++) {
            entry.addAccession (bits[i]);
        }
        entry.setComments(collections.get("comments"));
//        entry.setCommentEvidence(commentEvidence);
        entry.setDomains(collections.get("domains"));
        entry.setPubs(collections.get("pubs"));
        entry.setKeywords(collections.get("keywords"));
        entry.setProteinNames(collections.get("proteinNames"));
        entry.setGeneNames(geneNames);
        entry.setGeneDesignations(geneDesignationToDbref);
        entry.setGOTerms(collections.get("goTerms"));
        return entry;
    }
}
