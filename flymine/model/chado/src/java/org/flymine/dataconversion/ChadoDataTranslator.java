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

import java.util.Collection;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Properties;

import org.intermine.InterMineException;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.dataconversion.ItemReader;
import org.intermine.dataconversion.DataTranslator;
import org.intermine.util.XmlUtil;
import org.intermine.metadata.Model;

import org.apache.log4j.Logger;

/**
 * Convert Chado data in fulldata Item format conforming to a source OWL definition
 * to fulldata Item format conforming to InterMine OWL definition.
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class ChadoDataTranslator extends DataTranslator
{
    protected static final Logger LOG = Logger.getLogger(ChadoDataTranslator.class);

    /**
     * @see DataTranslator#DataTranslator
     */
    public ChadoDataTranslator(ItemReader srcItemReader, Properties mapping, Model srcModel,
                               Model tgtModel) {
        super(srcItemReader, mapping, srcModel, tgtModel);
    }

    /**
     * @see DataTranslator#translateItem
     */
    protected Collection translateItem(Item srcItem)
        throws ObjectStoreException, InterMineException {
        Collection result = new HashSet();
        String className = XmlUtil.getFragmentFromURI(srcItem.getClassName());
        Collection translated = super.translateItem(srcItem);
        if (translated != null) {
            for (Iterator i = translated.iterator(); i.hasNext();) {
                Item tgtItem = (Item) i.next();
                if ("author".equals(className)) {
                    tgtItem.addAttribute(new Attribute("name",
                                         srcItem.getAttribute("givennames").getValue()
                                         + " " + srcItem.getAttribute("surname").getValue()));
                } else if ("feature_synonym".equals(className)) {
                    promoteField(tgtItem, srcItem, "value", "synonym", "name");
                    tgtItem.addAttribute(new Attribute("type", "accession"));
                } else if ("feature_dbxref".equals(className)) {
                    promoteField(tgtItem, srcItem, "value", "dbxref", "accession");
                    promoteField(tgtItem, srcItem, "source", "dbxref", "db");
                    tgtItem.addAttribute(new Attribute("type", "accession"));
                }
                result.add(tgtItem);
            }
        }
        return result;
    }
}
