package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

import org.flymine.io.gff3.GFF3Record;

/**
 * A converter/retriever for the long oligo dataset.
 *
 * @author Kim Rutherford
 */

public class LongOligoGFF3RecordHandler extends GFF3RecordHandler
{
    /**
     * Create a new LongOligoGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public LongOligoGFF3RecordHandler (Model tgtModel) {
        super(tgtModel);
    }

    /**
     * @see GFF3RecordHandler#process()
     */
    public void process(GFF3Record record) {
        Item oligo = getFeature();

        String dist3p = (String) ((List) record.getAttributes().get("dist3p")).get(0);
        oligo.setAttribute("distance3Prime", dist3p);
        String olen = (String) ((List) record.getAttributes().get("olen")).get(0);
        oligo.setAttribute("length", olen);
        String oaTm = (String) ((List) record.getAttributes().get("oaTm")).get(0);
        oligo.setAttribute("tm", oaTm);

        oligo.setReference("transcript", getSequence().getIdentifier());
    }
}
