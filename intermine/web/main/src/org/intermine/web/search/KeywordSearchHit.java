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
