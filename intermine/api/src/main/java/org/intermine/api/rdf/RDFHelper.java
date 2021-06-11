package org.intermine.api.rdf;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.lang.StringUtils;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.intermine.metadata.AttributeDescriptor;
import java.util.Map;

/**
 * Utility class for RDF generation
 * @author Daniela Butano
 */
public final class RDFHelper
{
    /**
     * InterMine vocabulary namespace
     */
    public static final String VOC_NAMESPACE = Namespaces.getNamespaces().get("im");

    /**
     * InterMine resource namespace
     */
    public static final String RES_NAMESPACE = "http://intermine.org/resource/";

    private static Map<String, String> namespaces = null;

    /**
     * default constructor
     */
    private RDFHelper() {
        //hidden
    }

    /**
     * Create a RDF property given the attribute
     * @param attribute the attribute
     * @return the RDF property
     */
    public static final Property createProperty(String attribute) {
        return ResourceFactory.createProperty(VOC_NAMESPACE,
                "has" + StringUtils.capitalize(attribute));
    }

    /**
     * Create a RDF property given the attribute
     * @param attributeDescriptor the field Descriptor
     * @return the RDF property
     */
    public static final Property createProperty(AttributeDescriptor attributeDescriptor) {
        if (attributeDescriptor.getOntologyTerm() != null) {
            return ResourceFactory.createProperty(attributeDescriptor.getOntologyTerm());
        } else {
            return ResourceFactory.createProperty(VOC_NAMESPACE,
            "has" + StringUtils.capitalize(attributeDescriptor.getName()));
        }
    }
}
