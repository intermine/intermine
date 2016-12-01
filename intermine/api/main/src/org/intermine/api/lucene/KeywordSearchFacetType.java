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

/**
 * type of faceting to use
 * see http://snaprojects.jira.com/wiki/display/BOBO/Create+a+Browse+Index
 * @author nils
 */
public enum KeywordSearchFacetType {
    /**
     * single value per document
     */
    SINGLE,
    /**
     * multiple values per document
     */
    MULTI,
    /**
     * path (takes array of field names, converts them to A/B/C format expected by bobo)
     */
    PATH
}
