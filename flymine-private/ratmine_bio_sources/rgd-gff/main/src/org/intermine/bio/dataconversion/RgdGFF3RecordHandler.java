package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.intermine.util.XmlUtil;
import java.util.regex.*;
import java.util.Map;
import java.util.HashMap;

import org.intermine.bio.io.gff3.GFF3Record;

/**
 * Handle special cases when converting RGD GFF3 files.
 *
 * @original author Richard Smith
 * @modified by Andrew Vallejos
 */

public class RgdGFF3RecordHandler extends GFF3RecordHandler
{
    /**
     * Create a new RgdGFF3RecordHandler object.
     * @param tgtModel the target Model
     */
    public RgdGFF3RecordHandler(Model tgtModel) {
		super(tgtModel);        
    }

    /**
     * {@inheritDoc}
     */
    public void process(GFF3Record record) {
        Item feature = getFeature();
		String clsName = XmlUtil.getFragmentFromURI(feature.getClassName());
		if("Exon".equals(clsName)){
			Map refMap = new HashMap();
			refMap.put("Exon","gene");
			setReferences(refMap);
		}
    }
    
}
