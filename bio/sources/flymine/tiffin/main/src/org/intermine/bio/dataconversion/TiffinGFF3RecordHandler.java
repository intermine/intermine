package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

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
     * {@inheritDoc}
     */
    @Override
    public void process(GFF3Record record) {
        Item bindingSite = getFeature();
        String name = record.getNames().get(0);
        Item motif = getMotif(name);
        bindingSite.setReference("motif", motif);
    }

    private Item getMotif(String name) {
        Item motif = motifs.get(name);
        if (motif == null) {
            motif = converter.createItem("Motif");
            motif.setAttribute("primaryIdentifier", name);

            motifs.put(name, motif);
            addEarlyItem(motif);
        }
        return motif;
    }
}
