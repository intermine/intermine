package org.intermine.api.lucene;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.lucene.store.Directory;

/**
 * container for the lucene index to hold field list and directory
 * TODO: we could probably get rid of this whole thing and just use the directory since
 * we can get the indexed fields from the directory itself
 * @author nils
 */
class LuceneIndexContainer implements Serializable
{
    private static final long serialVersionUID = 1L;
    private transient Directory directory;
    private String directoryType;
    private HashSet<String> fieldNames = new HashSet<String>();
    private HashMap<String, Float> fieldBoosts = new HashMap<String, Float>();

    /**
     * get lucene directory for this index
     * @return directory
     */
    public Directory getDirectory() {
        return directory;
    }

    /**
     * set lucene directory
     * @param directory
     *            directory
     */
    public void setDirectory(Directory directory) {
        this.directory = directory;
    }

    /**
     * get type of directory
     * @return 'FSDirectory' or 'RAMDirectory'
     */
    public String getDirectoryType() {
        return directoryType;
    }

    /**
     * set type of directory
     * @param directoryType
     *            class name of lucene directory
     */
    public void setDirectoryType(String directoryType) {
        this.directoryType = directoryType;
    }

    /**
     * get list of fields in the index
     * @return fields
     */
    public HashSet<String> getFieldNames() {
        return fieldNames;
    }

    /**
     * set list of fields in the index
     * @param fieldNames
     *            fields
     */
    public void setFieldNames(HashSet<String> fieldNames) {
        this.fieldNames = fieldNames;
    }

    /**
     * get list of boost associated with fields
     * @return boosts
     */
    public HashMap<String, Float> getFieldBoosts() {
        return fieldBoosts;
    }

    /**
     * set boost associated with fields
     * @param fieldBoosts
     *            boosts
     */
    public void setFieldBoosts(HashMap<String, Float> fieldBoosts) {
        this.fieldBoosts = fieldBoosts;
    }

    @Override
    public String toString() {
        return "INDEX [[" + directory + "" + ", fields = " + fieldNames + "" + ", boosts = "
                + fieldBoosts + "" + "]]";
    }
}
