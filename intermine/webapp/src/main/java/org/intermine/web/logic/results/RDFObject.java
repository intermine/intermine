package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL;
import org.intermine.api.InterMineAPI;
import org.intermine.api.rdf.Namespaces;
import org.intermine.api.rdf.PurlConfig;
import org.intermine.api.rdf.RDFHelper;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ClobAccess;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.PermanentURIHelper;
import org.intermine.web.uri.InterMineLUI;
import org.intermine.web.uri.InterMineLUIConverter;

import javax.servlet.http.HttpServletRequest;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Object to represent an intermine imObject as RDF
 *
 * @author Daniela Butano
 */
public class RDFObject
{
    private InterMineObject imObject;
    private boolean isValid = true;
    private String resourceURI;
    private InterMineLUIConverter urlConverter;
    private String baseUrl;
    private ClassDescriptor objectClassDescriptor;
    private Model model;
    private Set<String> nullRefsCols;

    /**
     * Setup internal ReportObject
     * @param lui the InterMine lui
     * @param request the http request
     * @param im the InterMineAPI
     * @throws Exception Exception
     */
    public RDFObject(InterMineLUI lui, InterMineAPI im, HttpServletRequest request)
            throws Exception {
        if (lui != null) {
            urlConverter = new InterMineLUIConverter();
            imObject = urlConverter.getInterMineObject(lui);
            if (imObject == null) {
                isValid = false;
                return;
            }
            baseUrl = (new PermanentURIHelper(request)).getPermanentBaseURI();
            resourceURI = baseUrl.concat(lui.toString());
            String objectType = DynamicUtil.getSimpleClass(imObject).getSimpleName();
            objectClassDescriptor = im.getModel().getClassDescriptorByName(objectType);
            nullRefsCols = im.getObjectStoreSummary()
                .getNullReferencesAndCollections(objectClassDescriptor.getName());
        }
    }


    /**
     * Return if it's valid entity, matching an existing intermine object
     * @return if it 's valid or not
     */
    public boolean isValid() {
        return isValid;
    }

    private void initialise() {
        model = ModelFactory.createDefaultModel();
        setKnownPrefixes();

        Resource resource = RDFHelper.createResource(resourceURI, objectClassDescriptor, model);
        //sameAs
        String externalIdentifier = PurlConfig.getExternalIdentifier(imObject);
        if (externalIdentifier != null) {
            resource.addProperty(OWL.sameAs, ResourceFactory.createProperty(externalIdentifier));
        }
        for (FieldDescriptor fd : objectClassDescriptor.getAllFieldDescriptors()) {
            if (fd.isAttribute() && !"id".equals(fd.getName())) {
                initialiseAttribute(resource, imObject, fd);
            } else if (fd.isReference()) {
                initialiseReference(resource, fd);
            } else if (fd.isCollection()) {
                initialiseCollection(resource, fd);
            }
        }
    }

    private void setKnownPrefixes() {
        Map<String, String> namespaces = Namespaces.getNamespaces();
        for (String prefix : namespaces.keySet()) {
            model.setNsPrefix(prefix, namespaces.get(prefix));
        }
    }

    private void initialiseAttribute(Resource resource, InterMineObject obj, FieldDescriptor fd) {
        Object fieldValue = null;
        try {
            fieldValue = obj.getFieldValue(fd.getName());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        if (fieldValue != null) {
            if (fieldValue instanceof ClobAccess) {
                ClobAccess fieldClob = (ClobAccess) fieldValue;
                fieldValue = fieldClob.toString();
            }
            AttributeDescriptor attributeDescriptor = (AttributeDescriptor) fd;
            resource.addProperty(RDFHelper.createIMProperty(attributeDescriptor),
                    fieldValue.toString());
        }
    }

    private void initialiseReference(Resource resource,  FieldDescriptor fieldDescriptor) {
        ReferenceDescriptor ref = (ReferenceDescriptor) fieldDescriptor;

        String refName = ref.getName();
        if (!nullRefsCols.contains(refName)) {
            Object proxyObject = null;
            ProxyReference proxy = null;
            try {
                proxyObject = imObject.getFieldProxy(refName);
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
            }
            if (proxyObject instanceof org.intermine.objectstore.proxy.ProxyReference) {
                proxy = (ProxyReference) proxyObject;
            }

            if (proxy != null) {
                InterMineObject referenceObj = proxy.getObject();
                addReferenceResource(referenceObj, ref, resource);
            }
        }
    }

    private void addReferenceResource(InterMineObject referenceObj, ReferenceDescriptor ref,
                                      Resource resource) {
        InterMineLUI lui = urlConverter.getInterMineLUI(referenceObj.getId());
        if (lui != null) {
            resourceURI = baseUrl.concat(lui.toString());
            Resource referenceObjResource = model.createResource(resourceURI);
            resource.addProperty(RDFHelper.createIMProperty(ref), referenceObjResource);
        }
    }

    private void initialiseCollection(Resource resource, FieldDescriptor fieldDescriptor) {
        String colName = fieldDescriptor.getName();
        if (!nullRefsCols.contains(colName)) {
            Object proxyObject = null;
            ProxyCollection proxyCollection = null;
            try {
                proxyObject = imObject.getFieldProxy(colName);
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
            }
            if (proxyObject instanceof org.intermine.objectstore.proxy.ProxyCollection) {
                proxyCollection = (ProxyCollection) proxyObject;
            }

            Iterator it = proxyCollection.iterator();
            while (it.hasNext()) {
                InterMineObject referenceObj = (InterMineObject) it.next();
                addReferenceResource(referenceObj, (ReferenceDescriptor) fieldDescriptor,
                        resource);
            }
        }
    }

    /**
     * Write the model as an XML document.
     * @param out the writer to which the RDF/XML will be written
     */
    public void serializeAsRDF(Writer out) {
        if (model == null) {
            initialise();
        }
        model.write(out);
    }
}
