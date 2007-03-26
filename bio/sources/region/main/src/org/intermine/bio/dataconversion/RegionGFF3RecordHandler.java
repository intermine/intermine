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


import org.intermine.bio.dataconversion.GFF3RecordHandler;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.intermine.util.XmlUtil;


/**
 * A converter/retriever for EncodeRegion GFF3 files.
 *
 * @author Wenyan Ji
 */

public class RegionGFF3RecordHandler extends GFF3RecordHandler
{


    /**
     * Create a new RegionGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public RegionGFF3RecordHandler(Model tgtModel) {
        super(tgtModel);

    }



    /**
     * @see GFF3RecordHandler#process(GFF3Record)
     */
    public void process(GFF3Record record) {
        Item feature = getFeature();

        String namespace = XmlUtil.getNamespaceFromURI(feature.getClassName());
        String className = XmlUtil.getFragmentFromURI(feature.getClassName());
        if (className.equals("Region")) {
            feature.setClassName(namespace + "DefinedRegion");
            feature.setAttribute("type", "encodeRegion");
        }

    }
}
