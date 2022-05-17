package org.intermine.api.searchengine;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

/**
 * container for one faceting field, the current faceting value and the list of
 * possible items
 *
 * @param <E> This is generic type for items variable.
 *          Currenly it used as a list FacetField.Count in solr.
 *
 * @author nils
 * @author arunans23
 */
public class KeywordSearchFacet<E>
{
    final String field;
    final String name;
    final String value;
    final List<E> items;

    /**
     * constructor
     * @param field
     *            name of the field
     * @param name
     *            name that is displayed to user
     * @param value
     *            current value selected by user (or null)
     * @param items
     *            list of possible values and their counts as BrowseFacets
     */
    public KeywordSearchFacet(String field, String name, String value, List<E> items) {
        super();
        this.field = field;
        this.name = name;
        this.value = value;
        this.items = items;
    }

    /**
     * internal field name
     * @return field
     */
    public String getField() {
        return field;
    }

    /**
     * user-friendly name
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * selected value
     * @return value
     */
    public String getValue() {
        return value;
    }

    /**
     * list of all values and counts
     * @return items
     */
    public List<E> getItems() {
        return items;
    }
}
