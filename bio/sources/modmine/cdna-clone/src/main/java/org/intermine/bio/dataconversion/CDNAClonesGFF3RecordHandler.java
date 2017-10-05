package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

/**
 * A converter/retriever for the CDNAClones dataset via GFF files.
 *
 * @author Fengyuan
 */
public class CDNAClonesGFF3RecordHandler extends GFF3RecordHandler
{

    /**
     * Create a new CDNAClonesGFF3RecordHandler for the given data model.
     * @param model the model for which items will be created
     */
    public CDNAClonesGFF3RecordHandler (Model model) {
        super(model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(GFF3Record record) {
        // This method is called for every line of GFF3 file(s) being read.  Features and their
        // locations are already created but not stored so you can make changes here.  Attributes
        // are from the last column of the file are available in a map with the attribute name as
        // the key.
        //
        Item feature = getFeature();
        if ("cDNA_match".equals(record.getType())) {
            feature.setClassName("CDNA");
            String[] target = record.getTarget().split("\\s");
            feature.setAttribute("primaryIdentifier", target[0]);
            String id = record.getId();
            feature.setAttribute("secondaryIdentifier", id);
        } else {
            clear();
        }
    }
}
