package org.intermine.api.searchengine;

/*
 * Copyright (C) 2002-2017 FlyMine
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
 *  *
 * @author arunans23
 */

public class KeywordSearchResultContainer<E> {

    final float score;
    final E document;
    final InterMineObject object;



    public KeywordSearchResultContainer(E document, InterMineObject object, float score) {

        //TODO: add code to check nullability

        this.score = score;
        this.document = document;
        this.object = object;
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
