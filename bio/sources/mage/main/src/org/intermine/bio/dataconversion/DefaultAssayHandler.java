package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * Perform additional operations of assay and provide a way of ordering
 * assays within an experiment.  By default do nothing and order assays
 * by their name.
 * @author Richard Smith
 */
public class DefaultAssayHandler implements AssayHandler
{
    MageDataTranslator translator;
    String tgtNs;

    /**
     * Construct with a MageDataTranslator as access may be needed to ItemReader
     * or maps.
     * @param translator the translator this method is being used in
     */
    public DefaultAssayHandler(MageDataTranslator translator) {
        this.translator = translator;
        this.tgtNs = translator.getTgtNamespace();
    }

    /**
     * @see AssayHandler#process
     */
    public void process(Item assay) {
        // empty
    }

    /**
     * Return an object with which an assay can be assigned an order within
     * a particular experiment.  By default order by the assay name attribute.
     * @param assay the assay item to get orderable field for
     * @return an object that can be used for ordering
     */
    public Object getAssayOrderable(Item assay) {
        return assay.getAttribute("name").getValue();
    }
}
