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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

/**
 * A converter/retriever for Tiffin motifs.  See http://servlet.sanger.ac.uk/tiffin/
 *
 * @author Kim Rutherford
 */

public class TiffinGFF3RecordHandler extends GFF3RecordHandler
{
    private Map<String, Item> motifs = new HashMap<String, Item>();

    /**
     * Create a new TiffinGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public TiffinGFF3RecordHandler (Model tgtModel) {
        super(tgtModel);
    }

    /**
     * @see GFF3RecordHandler#process(GFF3Record)
     */
    @Override
    public void process(GFF3Record record) {
        Item bindingSite = getFeature();
        bindingSite.setAttribute("curated", "false");

        String name = record.getNames().get(0);
        Item motif = getMotif(name);
        bindingSite.setReference("motif", motif);
    }

    /**
     * Return the Motif objects created by process().
     * {@inheritDoc}
     */
    @Override
    public Collection<Item> getFinalItems() {
        return motifs.values();
    }

    private Item getMotif(String name) {
        Item motif = motifs.get(name);
        if (motif == null) {
            motif = createItem("Motif");
            motif.setAttribute("identifier", name);
            motif.setAttribute("curated", "false");
            motifs.put(name, motif);
        }
        return motif;
    }
}
