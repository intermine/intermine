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

import org.intermine.bio.io.gff3.GFF3Record;

/**
 * Handle special cases when converting malaria GFF3 files.
 *
 * @author Richard Smith
 */

public class MalariaGFF3RecordHandler extends GFF3RecordHandler
{
    /**
     * Create a new MalariaGFF3RecordHandler object.
     * @param tgtModel the target Model
     */
    public MalariaGFF3RecordHandler(Model tgtModel) {
        super(tgtModel);
    }

    /**
     * {@inheritDoc}
     */
    public void process(GFF3Record record) {
        Item feature = getFeature();

        String clsName = XmlUtil.getFragmentFromURI(feature.getClassName());

        if ("Gene".equals(clsName)) {
            String identifier = null;
            String symbol = null;
            if (feature.getAttribute("symbol") != null) {
                identifier = feature.getAttribute("symbol").getValue();
            }
            if (feature.getAttribute("identifier") != null) {
                symbol = feature.getAttribute("identifier").getValue();
            }
            if (identifier != null) {
                feature.setAttribute("primaryIdentifier", identifier);
            }
            if (symbol != null) {
                feature.setAttribute("primaryIdentifier", symbol);
                feature.removeAttribute("symbol");
            }
        }

    }
}
