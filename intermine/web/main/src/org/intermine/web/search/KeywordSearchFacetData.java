package org.intermine.web.search;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * container class for a facet field and its settings
 * @author nils
 */
public class KeywordSearchFacetData
{
    final String[] fields;
    final String name;
    final KeywordSearchFacetType type;

    /**
     * convenience constructor for single field
     * @param field
     *            field to use as a value
     * @param name
     *            name that is displayed to the user
     * @param type
     *            type of the facet (single, multi, path, ...)
     */
    public KeywordSearchFacetData(String field, String name, KeywordSearchFacetType type) {
        this(new String[] {field}, name, type);
    }

    /**
     * constructor
     * @param fields
     *            fields to use as values (usually just one for single/multi
     *            facet, several for path)
     * @param name
     *            name that is displayed to the user
     * @param type
     *            type of the facet (single, multi, path, ...)
     */
    public KeywordSearchFacetData(String[] fields, String name, KeywordSearchFacetType type) {
        super();
        this.fields = fields;
        this.name = name;
        this.type = type;
    }

    /**
     * get single/first field or null
     * @return field or null
     */
    public String getField() {
        if (fields.length <= 0) {
            return null;
        }

        return fields[0];
    }

    /**
     * array of field names, only used for path right now
     * @return fields
     */
    public String[] getFields() {
        return fields;
    }

    /**
     * name of the facet (displayed on page)
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * type of facet
     * @return type
     */
    public KeywordSearchFacetType getType() {
        return type;
    }
}
