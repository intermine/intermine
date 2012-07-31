package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.intermine.bio.util.OrganismData;
import org.intermine.xml.full.Item;

/**
 * Data about one feature from the feature table in chado.  This exists to avoid having lots of
 * Item objects in memory.
 *
 * @author Kim Rutherford
 */
class FeatureData
{
    private String md5checksum;
    OrganismData organismData;
    private String uniqueName;
    private String chadoFeatureName;
    private Set<String> existingSynonyms = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    private String itemIdentifier;
    private String interMineType;
    private Integer intermineObjectId;

    private short flags = 0;
    static final short EVIDENCE_CREATED = 0;
    static final short IDENTIFIER_SET = 1;
    static final short LENGTH_SET = 2;
    static final short DATASET_SET = 3;
    static final short SYMBOL_SET = 4;
    static final short SECONDARY_IDENTIFIER_SET = 5;
    static final short NAME_SET = 6;
    static final short SEQUENCE_SET = 7;
    static final short CAN_HAVE_SEQUENCE = 8;
    static final short CAN_HAVE_SYMBOL = 9;
    private static final Map<String, Short> NAME_MAP = new HashMap<String, Short>();

    static {
        NAME_MAP.put(SequenceProcessor.PRIMARY_IDENTIFIER_STRING, new Short(IDENTIFIER_SET));
        NAME_MAP.put(SequenceProcessor.SECONDARY_IDENTIFIER_STRING,
                     new Short(SECONDARY_IDENTIFIER_SET));
        NAME_MAP.put(SequenceProcessor.SYMBOL_STRING, new Short(SYMBOL_SET));
        NAME_MAP.put(SequenceProcessor.NAME_STRING, new Short(NAME_SET));
        NAME_MAP.put(SequenceProcessor.SEQUENCE_STRING, new Short(SEQUENCE_SET));
        NAME_MAP.put(SequenceProcessor.LENGTH_STRING, new Short(LENGTH_SET));
    }

    /**
     * Return the id of the Item representing this feature.
     * @return the ID
     */
    public Integer getIntermineObjectId() {
        return intermineObjectId;
    }

    /**
     * Set the intermine objectId for this feature - found when the feature is
     * stored.
     * @param intermineObjectId the intermineObjectId to set
     */
    public void setIntermineObjectId(Integer intermineObjectId) {
        this.intermineObjectId = intermineObjectId;
    }

    /**
     * Set flags needed by canHaveReference() and checkAttribute().
     * @param item the Item to test
     */
    public void setFieldExistenceFlags(Item item) {
        if (item.canHaveReference(SequenceProcessor.SEQUENCE_STRING)) {
            setFlag(CAN_HAVE_SEQUENCE, true);
        }
        if (item.checkAttribute(SequenceProcessor.SYMBOL_STRING)) {
            setFlag(CAN_HAVE_SYMBOL, true);
        }
    }

    /**
     * Return true if the Item for this FeatureData has a field with the given name.
     * @param fieldName the field name
     * @return true if the field name is valid
     */
    public boolean checkField(String fieldName) {
        if (fieldName.equals(SequenceProcessor.SEQUENCE_STRING)) {
            return getFlag(CAN_HAVE_SEQUENCE);
        }
        if (fieldName.equals(SequenceProcessor.SYMBOL_STRING)) {
            return getFlag(CAN_HAVE_SYMBOL);
        }
        throw new RuntimeException("unknown field name: " + fieldName);
    }

    /**
     * Return the value of the flag corresponding to the given attribute name.
     * @param attributeName the attribute name
     * @return the flag value
     */
    public boolean getFlag(String attributeName) {
        return getFlag(getFlagId(attributeName));
    }

    /**
     * Set the flag corresponding to the given attribute name.
     * @param attributeName the attribute name
     * @param value the new flag value
     */
    public void setFlag(String attributeName, boolean value) {
        setFlag(getFlagId(attributeName), value);
    }

    private short getFlagId(String attributeName) {
        if (NAME_MAP.containsKey(attributeName)) {
            return NAME_MAP.get(attributeName).shortValue();
        }
        throw new RuntimeException("unknown attribute name: " + attributeName);
    }

    /**
     * Get the String read from the name column of the feature table.
     * @return the name
     */
    public String getChadoFeatureName() {
        return chadoFeatureName;
    }

    /**
     * Set the chadoFeatureName.
     * @param chadoFeatureName the chadoFeatureName to set
     */
    public void setChadoFeatureName(String chadoFeatureName) {
        this.chadoFeatureName = chadoFeatureName;
    }

    /**
     * Get the String read from the uniquename column of the feature table.
     * @return the uniquename
     */
    public String getChadoFeatureUniqueName() {
        return uniqueName;
    }

    /**
     * Get the uniqueName.
     * @return the uniqueName
     */
    public  String getUniqueName() {
        return uniqueName;
    }

    /**
     * Set the uniqueName.
     * @param uniqueName the new uniqueName
     */
    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    /**
     * Return the InterMine Item identifier for this feature.
     * @return the new InterMine Item identifier
     */
    public String getItemIdentifier() {
        return itemIdentifier;
    }

    /**
     * Set the item identifier for this feature.
     * @param itemIdentifier the new item identifier
     */
    public void setItemIdentifier(String itemIdentifier) {
        this.itemIdentifier = itemIdentifier;
    }

    /**
     * Return the OrganismData object for the organism this feature comes from.
     * @return the OrganismData object
     */
    public OrganismData getOrganismData() {
        return organismData;
    }

    /**
     * Return the InterMine type of this object
     * @return the InterMine type
     */
    public String getInterMineType() {
        return interMineType;
    }

    /**
     * Set the InterMine type (class) of this feature.
     * @param interMineType the type to set
     */
    public void setInterMineType(String interMineType) {
        this.interMineType = interMineType;
    }

    private int shift(short flag) {
        return (2 << flag);
    }

    /**
     * Get the given flag.
     * @param flag the flag constant eg. LENGTH_SET_BIT
     * @return true if the flag is set
     */
    public boolean getFlag(short flag) {
        return (flags & shift(flag)) != 0;
    }

    /**
     * Set a flag using the flag constants like LENGTH_SET_BIT.
     * @param flag the flag constant
     * @param value the new value
     */
    public void setFlag(short flag, boolean value) {
        if (value) {
            flags |= shift(flag);
        } else {
            flags &= ~shift(flag);
        }
    }

    /**
     * Return the MD5 checksum of the residues of this feature.
     * @return the checksum
     */
    public String getChecksum() {
        return md5checksum;
    }

    /**
     * Set the md5checksum for this feature.
     * @param md5checksum the new md5checksum
     */
    public void setMd5checksum(String md5checksum) {
        this.md5checksum = md5checksum;
    }


    /**
     * Return the set of identifiers for which Synonyms have been created.
     * @return the existingSynonyms
     */
    public final Set<String> getExistingSynonyms() {
        return existingSynonyms;
    }


    /**
     * this is a newer method, we are now creating synonyms later than we used to (because of
     * duplicate sequences and discarded identifiers), so we need to be able to add this to the
     * synonyms collection.
     * @param synonym identifier that has just been created as a synonym
     */
    public void addExistingSynonym(String synonym) {
        existingSynonyms.add(synonym);
    }

}
