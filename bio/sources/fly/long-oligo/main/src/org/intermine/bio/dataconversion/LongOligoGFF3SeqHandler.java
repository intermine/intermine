package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.intermine.xml.full.Item;

/**
 * A handler for long-oligo GFF features.
 * @author Kim Rutherford
 */
public class LongOligoGFF3SeqHandler extends GFF3SeqHandler
{
    private static final String TAXON_FLY = "7227";
    private static final String CLASS_NAME = "mRNA";
    protected IdResolver rslv;
    protected static final Logger LOG = Logger.getLogger(LongOligoGFF3SeqHandler.class);


    /**
     * Construct the seq handler.
     */
    public LongOligoGFF3SeqHandler() {
        rslv = IdResolverService.getFlyIdResolver(CLASS_NAME);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getSeqIdentifier(String id) {
        if (rslv == null || !rslv.hasTaxonAndClassName(TAXON_FLY, CLASS_NAME)) {
            return null;
        }

        String updatedId = null;
        int resCount = rslv.countResolutions(TAXON_FLY, id);
        if (resCount == 1) {
            updatedId = rslv.resolveId(TAXON_FLY, id).iterator().next();
        }

        return updatedId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Item makeSequenceItem(GFF3Converter converter, String identifier) {
        Item seq = null;
        String primaryIdentifier = getSeqIdentifier(identifier);
        if (primaryIdentifier != null) {
            seq = createItem(converter);
            seq.setAttribute("primaryIdentifier", primaryIdentifier);
            LOG.info("RESOLVER: updated " + identifier + " to " + primaryIdentifier);
        } else {
            LOG.info("Could not resolve " + identifier + " to one identifier.");
        }

        return seq;
    }
}
