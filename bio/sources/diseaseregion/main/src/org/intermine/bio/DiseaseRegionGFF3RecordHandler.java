package org.intermine.bio;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import org.intermine.bio.dataconversion.GFF3RecordHandler;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.intermine.util.XmlUtil;


/**
 * A converter/retriever for DiseaseRegion GFF3 files (T1DBase).
 *
 * @author Wenyan Ji
 */

public class DiseaseRegionGFF3RecordHandler extends GFF3RecordHandler
{


    /**
     * Create a new DiseaseRegionGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public DiseaseRegionGFF3RecordHandler(Model tgtModel) {
        super(tgtModel);

    }



    /**
     * @see GFF3RecordHandler#process()
     */
    public void process(GFF3Record record) {
        Item feature = getFeature();

        String namespace = XmlUtil.getNamespaceFromURI(feature.getClassName());
        String className = XmlUtil.getFragmentFromURI(feature.getClassName());
        if (className.equals("Region")) {
            feature.setClassName(namespace + "DefinedRegion");
            feature.setAttribute("type", "diseaseRegion");
        }

    }
}


