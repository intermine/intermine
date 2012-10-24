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

import org.intermine.xml.full.Item;

/**
 * An interface used by GFF3Converter to choose the type of sequence Items.
 * @author Kim Rutherford
 */
public class GFF3SeqHandler
{
    /**
     * For the given GFF3Converter and sequence identifier, make a new sequence object.  By default
     * this method calls GFF3Converter.getSeqClsName() to get the class name of the Item to return.
     * Override to choose the class name based on the identifier.
     * @param converter the current GFF3Converter
     * @param identifier the identifier of the sequence from the GFF file
     * @return a new sequence Item
     */
    public Item makeSequenceItem(GFF3Converter converter, String identifier) {
        Item seq = createItem(converter);
        seq.setAttribute("primaryIdentifier", identifier);
        return seq;
    }


    /**
     * Return the identifier of this sequence, default implementation returns the id passed to it.
     * Subclasses can override to update identifiers to be used, e.g. if using an IdResolver.  This
     * method is used by GFF3Converter to make sequence items unique.
     * @param id the id to lookup
     * @return the identifier to use
     */
    public String getSeqIdentifier(String id) {
        return id;
    }

    /**
     * Create the sequence item from the converter.
     * @param converter that we are handling the sequence for
     * @return the new sequence item
     */
    protected Item createItem(GFF3Converter converter) {
        return converter.createItem(converter.getSeqClsName());
    }
}
