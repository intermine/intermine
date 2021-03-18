package org.intermine.api.searchengine;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.model.InterMineObject;

/**
 * Container for individual Keyword Search Result
 *
 * The document type has been given as generic type.
 *
 * @param <E> This is generic type for document.
 *          Currenly it used as a SolrDocument in solr.
 *
 * @author arunans23
 */
public class KeywordSearchResultContainer<E>
{

    final float score;
    final E document;
    final InterMineObject object;

    /**
     * constructor
     * @param document
     *            Individual Document returned from the search
     * @param object
     *            Intermine Object associated with the document. Matched by ID
     * @param score
     *            score value for that particular document. (Level of relation)
     */
    public KeywordSearchResultContainer(E document, InterMineObject object, float score) {

        this.score = score;
        this.document = document;
        this.object = object;

        if (score < 0) {
            throw new IllegalArgumentException("score must be >= 0, got: " + score);
        }
        if (document == null) {
            throw new NullPointerException("document must not be null.");
        }
        if (object == null) {
            throw new NullPointerException("object must not be null.");
        }
    }
    /**
     * score
     * @return score
     */
    public float getScore() {
        return score;
    }

    /**
     * document
     * Type generic
     * Eg: SolrInputDocument, Document
     * @return document
     */
    public E getDocument() {
        return document;
    }

    /**
     * object
     * @return intermineobject
     */
    public InterMineObject getObject() {
        return object;
    }
}
