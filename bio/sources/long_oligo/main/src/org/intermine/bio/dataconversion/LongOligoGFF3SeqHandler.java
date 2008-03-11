package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.xml.full.Item;

/**
 * A handler for long-oligo GFF features.
 * @author Kim Rutherford
 */
public class LongOligoGFF3SeqHandler extends GFF3SeqHandler
{
    /**
     * {@inheritDoc}
     */
    @Override
    public Item makeSequenceItem(GFF3Converter converter, String identifier) {
        Item seq = createItem(converter);
        seq.setAttribute("secondaryIdentifier", identifier);
        return seq;
    }
}
