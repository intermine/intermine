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
 * A converter/retriever for Tfbs GFF3 files.
 *
 * @author Wenyan Ji
 */

public class TfbsGFF3RecordHandler extends GFF3RecordHandler
{

    /**
     * Create a new TfbsGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public TfbsGFF3RecordHandler(Model tgtModel) {
        super(tgtModel);
    }


    /**
     * @see GFF3RecordHandler#process()
     */
    public void process(GFF3Record record) {
        if (record.getAttributes().get("zscore") != null) {
            String crNamespace = getTargetModel().getNameSpace() + "ComputationalResult";
            String analysisId = analysis.getIdentifier();
            String zscore = (String) ((List) record.getAttributes().get("zscore")).get(0);
            Item computationalResult = getItemFactory().makeItemForClass(crNamespace);
            computationalResult.setAttribute("type", "zscore");
            computationalResult.setAttribute("score", zscore);
            computationalResult.setReference("analysis", analysisId);
            addItem(computationalResult);
        }
    }
}

