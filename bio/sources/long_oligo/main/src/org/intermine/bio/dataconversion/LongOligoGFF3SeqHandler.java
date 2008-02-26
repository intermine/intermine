package org.intermine.bio.dataconversion;

import org.intermine.xml.full.Item;


public class LongOligoGFF3SeqHandler extends GFF3SeqHandler
{

    /**
     * {@inheritDoc}
     */
    public Item makeSequenceItem(GFF3Converter converter, String identifier) {
        Item seq = createItem(converter);
        seq.setAttribute("secondaryIdentifier", identifier);
        return seq;
    }  
}
