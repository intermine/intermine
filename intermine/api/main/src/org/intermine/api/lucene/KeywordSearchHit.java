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

import org.apache.lucene.document.Document;
import org.intermine.model.InterMineObject;

/**
 * container class to hold a document found in an keyword search together with
 * its object and score
 * @author nils
 */
public class KeywordSearchHit
{
    final float score;
    final Document document;
    final InterMineObject object;

    /**
     * constructor
     * @param score lucene score
     * @param document lucene document
     * @param object intermine object
     */
    public KeywordSearchHit(float score, Document document, InterMineObject object) {
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
     * @return document
     */
    public Document getDocument() {
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
