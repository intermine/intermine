package org.intermine.bio.dataconversion;

/* 
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.xml.full.Item;

import java.lang.String;

/**
 * Implementation of GFF3SeqHandler for the flybase source.
 * @author Kim Rutherford
 */
public class FlyBaseGFF3SeqHandler extends GFF3SeqHandler
{

    /**
     * @see GFF3SeqHandler#makeSequenceItem(GFF3Converter, String)
     * {@inheritDoc}
     */
    @Override
    public Item makeSequenceItem(GFF3Converter converter, String identifier) {
        Item item = super.makeSequenceItem(converter, identifier);
        if (identifier.toLowerCase().indexOf("unknown") == -1) {
            return item;
        } else {
            item.setClassName(converter.getTgtModel().getNameSpace() + "Assembly");
            return item;
        }
    }

}
