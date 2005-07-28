package org.intermine.web.dataset;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

/**
 * Routines for unmarshaling DataSet objects from XML.
 * 
 * @see org.intermine.web.dataset.DataSet
 * @author Thomas Riley
 */
public class DataSetBinding
{
    private static final Logger LOG = Logger.getLogger(DataSetBinding.class);
    
    /**
     * Read in data set configuration from XML. The keys (set names) in the returned map
     * are ordered to match the ordering in the XML file.
     * 
     * @param reader data set xml reader
     * @return Map from set name to DataSet object
     */
    public static Map unmarshal(Reader reader) {
        Digester digester = new Digester();
        digester.addObjectCreate("data-sets", "java.util.ArrayList");
        digester.addObjectCreate("data-sets/data-set", "org.intermine.web.dataset.DataSet");
        digester.addSetProperties("data-sets/data-set");
        digester.addCallMethod("data-sets/data-set/subtitle", "setSubTitle", 0);
        digester.addCallMethod("data-sets/data-set/icon-image", "setIconImage", 0);
        digester.addCallMethod("data-sets/data-set/large-image", "setLargeImage", 0);
        digester.addCallMethod("data-sets/data-set/tile-name", "setTileName", 0);
        digester.addCallMethod("data-sets/data-set/intro-text", "setIntroText", 0);
        digester.addCallMethod("data-sets/data-set/starting-points", "setStartingPoints", 0);
        digester.addObjectCreate("data-sets/data-set/data-source", "org.intermine.web.dataset.DataSetSource");
        digester.addSetProperties("data-sets/data-set/data-source");
        digester.addSetNext("data-sets/data-set/data-source", "addDataSetSource", "org.intermine.web.dataset.DataSetSource");
        digester.addSetNext("data-sets/data-set", "add", "java.lang.Object");
        try {
            List list = (List) digester.parse(reader);
            if (list == null) {
                LOG.error("Failed to unmashal datasets (digester returned null)");
                return Collections.EMPTY_MAP;
            }
            Map map = new LinkedHashMap();
            Iterator iter = list.iterator();
            while (iter.hasNext()) {
                DataSet set = (DataSet) iter.next();
                map.put(set.getName(), set);
            }
            return map;
        } catch (IOException e) {
            LOG.error(e);
            return null;
        } catch (SAXException e) {
            LOG.error(e);
            return null;
        }
    }
    
    /**
     * Read data set configuration from an input stream.
     * 
     * @param is an InputStream to the XML document
     * @return Map from data set name to DataSet object
     * @see #unmarshal(Reader)
     */
    public static Map unmarhsal(InputStream is) {
        return unmarshal(new InputStreamReader(is));
    }
}
