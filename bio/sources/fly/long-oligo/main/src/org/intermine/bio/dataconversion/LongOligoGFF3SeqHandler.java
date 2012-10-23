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

import org.apache.log4j.Logger;
import org.intermine.xml.full.Item;

/**
 * A handler for long-oligo GFF features.
 * @author Kim Rutherford
 */
public class LongOligoGFF3SeqHandler extends GFF3SeqHandler
{
    protected IdResolverFactory resolverFactory = null;
    private IdResolver resolver = null;
    protected static final Logger LOG = Logger.getLogger(LongOligoGFF3SeqHandler.class);


    /**
     * Construct the seq handler.
     */
    public LongOligoGFF3SeqHandler() {
        resolverFactory = new FlyBaseIdResolverFactory("mRNA");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getSeqIdentifier(String id) {
        // resolve id from file to valid FlyBase primaryIdentifier
        if (resolver == null) {
            resolver = resolverFactory.getIdResolver();
        }

        String updatedId = null;
        int resCount = resolver.countResolutions("7227", id);
        if (resCount == 1) {
            updatedId = resolver.resolveId("7227", id).iterator().next();
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
