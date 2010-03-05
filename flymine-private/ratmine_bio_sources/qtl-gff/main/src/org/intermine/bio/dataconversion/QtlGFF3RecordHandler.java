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
 * Handle QTL GFF3 Files
 *
 * @author Andrew Vallejos
 */

public class QtlGFF3RecordHandler extends GFF3RecordHandler
{
    /**
     * Create a new QtlGFF3RecordHandler object.
     * @param tgtModel the target Model
     */
    public QtlGFF3RecordHandler(Model tgtModel) {
		super(tgtModel);        
    }

    /**
     * {@inheritDoc}
     */
    public void process(GFF3Record record) {
        Item feature = getFeature();
    }
    
}
