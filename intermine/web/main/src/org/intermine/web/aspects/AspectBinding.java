package org.intermine.web.aspects;

/*
 * Copyright (C) 2002-2007 FlyMine
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
 * Routines for unmarshaling Aspect objects from XML.
 * 
 * @see org.intermine.web.dataset.Aspect
 * @author Thomas Riley
 */
public class AspectBinding
{
    private static final Logger LOG = Logger.getLogger(AspectBinding.class);
    
    /**
     * Read in data set configuration from XML. The keys (set names) in the returned map
     * are ordered to match the ordering in the XML file.
     * 
     * @param reader data set xml reader
     * @return Map from set name to Aspect object
     */
    public static Map unmarshal(Reader reader) {
        Digester digester = new Digester();
        digester.addObjectCreate("aspects", "java.util.ArrayList");
        digester.addObjectCreate("aspects/aspect", "org.intermine.web.aspects.Aspect");
        digester.addSetProperties("aspects/aspect");
        digester.addCallMethod("aspects/aspect/subtitle", "setSubTitle", 0);
        digester.addCallMethod("aspects/aspect/icon-image", "setIconImage", 0);
        digester.addCallMethod("aspects/aspect/large-image", "setLargeImage", 0);
        digester.addCallMethod("aspects/aspect/tile-name", "setTileName", 0);
        digester.addCallMethod("aspects/aspect/intro-text", "setIntroText", 0);
        digester.addCallMethod("aspects/aspect/starting-points", "setStartingPoints", 0);
        digester.addObjectCreate("aspects/aspect/aspect-source",
                "org.intermine.web.aspects.AspectSource");
        digester.addSetProperties("aspects/aspect/aspect-source");
        digester.addSetNext("aspects/aspect/aspect-source", "addAspectSource",
                "org.intermine.web.aspects.AspectSource");
        digester.addSetNext("aspects/aspect", "add", "java.lang.Object");
        try {
            List list = (List) digester.parse(reader);
            if (list == null) {
                LOG.error("Failed to unmashal aspects (digester returned null)");
                return Collections.EMPTY_MAP;
            }
            Map map = new LinkedHashMap();
            Iterator iter = list.iterator();
            while (iter.hasNext()) {
                Aspect set = (Aspect) iter.next();
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
     * @return Map from data set name to Aspect object
     * @see #unmarshal(Reader)
     */
    public static Map unmarhsal(InputStream is) {
        return unmarshal(new InputStreamReader(is));
    }
}
