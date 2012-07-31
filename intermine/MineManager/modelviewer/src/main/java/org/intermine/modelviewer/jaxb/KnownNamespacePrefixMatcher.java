package org.intermine.modelviewer.jaxb;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import com.sun.xml.bind.v2.WellKnownNamespace;

/**
 * Implementation of the internal Sun JAXB <code>NamespacePrefixMapper</code> to make
 * sure the XML documents created from JAXB have user friendly name spaces in their
 * headers.
 */
class KnownNamespacePrefixMatcher extends NamespacePrefixMapper
{
    /**
     * Map of namespace declarations to reader friendly short tags.
     */
    private static final Map<String, String> PREFIX_MAP;
    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put(WellKnownNamespace.XML_SCHEMA_INSTANCE, "xsi");
        map.put(WellKnownNamespace.XML_SCHEMA, "xsd");
        map.put(WellKnownNamespace.XML_MIME_URI, "xmime");
        map.put(ConfigParser.GENOMIC_CORE_NAMESPACE, "gc");
        map.put(ConfigParser.CORE_NAMESPACE, "core");
        map.put(ConfigParser.GENOMIC_NAMESPACE, "genomic");
        map.put(ConfigParser.PROJECT_NAMESPACE, "project");
        PREFIX_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * Returns a preferred prefix for the given name space URI.
     * This overridden method checks to see if the given name space URI is one that
     * is known to this class. If it is, the "nice" tag is returned. If not, the
     * suggestion is returned.
     * 
     * @param namespaceUri The name space URI for which the prefix needs to be found.
     * @param suggestion The suggested tag for the name space URI.
     * @param requirePrefix Whether a non-empty name space is required (i.e. not the
     * default name space).
     * 
     * @return The "nice" tag for the name space URI, or the suggested one if the
     * URI is not recognised.
     */
    @Override
    public String getPreferredPrefix(String namespaceUri,
                                     String suggestion,
                                     boolean requirePrefix) {
        String ns = PREFIX_MAP.get(namespaceUri);
        return ns == null ? suggestion : ns;
    }
}
