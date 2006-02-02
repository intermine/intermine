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
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

import org.flymine.io.gff3.GFF3Record;

import org.apache.tools.ant.BuildException;

/**
 * A converter/retriever for Tfbs conserved non-coding site GFF3 files.
 *
 * @author Wenyan Ji
 */

public class CnsGFF3RecordHandler extends GFF3RecordHandler
{


    /**
     * Create a new CnsGFF3RecordHandler for the given target model.
     * @param tgtModel the model for which items will be created
     */
    public CnsGFF3RecordHandler(Model tgtModel) {
        super(tgtModel);

    }


    /**
     * @see GFF3RecordHandler#process()
     */
    public void process(GFF3Record record) throws BuildException {
        
    }

}

