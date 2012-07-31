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
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;

/**
 * A converter/retriever for the EcoliGff dataset via GFF files.
 */

public class EcoliGffGFF3RecordHandler extends GFF3RecordHandler
{
    protected static final Logger LOG = Logger.getLogger(EcoliGffGFF3RecordHandler.class);

    /**
     * Create a new EcoliGffGFF3RecordHandler for the given data model.
     * @param model the model for which items will be created
     */
    public EcoliGffGFF3RecordHandler (Model model) {
        super(model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(GFF3Record record) {

        // print out the record details to check

        //TODO find the EcoGene id and set it as a gene attr
//        String geneIdentifier = record.getAttributes().get("gene");
//        ene.setAttribute("primaryIdentifier", geneIdentifier);
    }

}
