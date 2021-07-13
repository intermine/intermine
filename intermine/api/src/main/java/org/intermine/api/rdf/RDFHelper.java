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
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;

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

    private static Map<String, String> namespaces = null;

    /**
     * default constructor
     */
    private RDFHelper() {
        //hidden
    }

    /**
     * Create a RDF property given the attribute
     * @param fieldDescriptor the attribute
     * @return the RDF property
     */
    public static final Property createIMProperty(FieldDescriptor fieldDescriptor) {
        return ResourceFactory.createProperty(VOC_NAMESPACE,
                "has" + StringUtils.capitalize(fieldDescriptor.getName()));
    }

    /**
     * Create a RDF property given the attribute
     * @param classDescriptor the attribute
     * @return the RDF property
     */
    public static final Resource createIMTypeResource(ClassDescriptor classDescriptor) {
        return ResourceFactory.createResource(VOC_NAMESPACE + classDescriptor.getSimpleName());
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
            return createIMProperty(attributeDescriptor);
        }
    }

    /**
     * Create a RDF property given a reference
     * @param referenceDescriptor the field Descriptor
     * @return the RDF property
     */
    public static final Property createProperty(ReferenceDescriptor referenceDescriptor) {
        if (referenceDescriptor.getOntologyTerm() != null) {
            return ResourceFactory.createProperty(referenceDescriptor.getOntologyTerm());
        } else {
            return createIMProperty(referenceDescriptor);
        }
    }
}
