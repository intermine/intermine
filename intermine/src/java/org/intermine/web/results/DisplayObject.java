package org.intermine.web.results;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.intermine.model.InterMineObject;
import org.intermine.metadata.PrimaryKeyUtil;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.web.config.FieldConfigHelper;
import org.intermine.web.config.FieldConfig;

import org.intermine.util.TypeUtil;

/**
 * Class to represent an object for display in the webapp
 * @author Mark Woodbridge
 */
public class DisplayObject
{
    InterMineObject object;
    Set clds;

    Map attributes = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    Map references = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    Map collections = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    Map refsAndCollections = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    List keyAttributes = new ArrayList();
    List keyReferences = new ArrayList();
    List fieldExprs = new ArrayList();
    Map verbosity = new HashMap();
    
    /**
     * Constructor
     * @param object the object to display
     * @param model the metadata for the object
     * @param webconfigTypeMap the Type Map from the webconfig file
     * @param webProperties the web properties from the session
     * @throws Exception if an error occurs
     */
    public DisplayObject(InterMineObject object, Model model,
                         Map webconfigTypeMap, Map webProperties) throws Exception {
        this.object = object;
        clds = ObjectViewController.getLeafClds(object.getClass(), model);

        for (Iterator i = clds.iterator(); i.hasNext();) {
            ClassDescriptor cld = (ClassDescriptor) i.next();
            for (Iterator j = cld.getAllFieldDescriptors().iterator(); j.hasNext();) {
                FieldDescriptor fd = (FieldDescriptor) j.next();

                if (fd.isAttribute() && !fd.getName().equals("id")) {
                    Object fieldValue = TypeUtil.getFieldValue(object, fd.getName());
                    if (fieldValue != null) {
                        attributes.put(fd.getName(), fieldValue);
                    }
                } else if (fd.isReference()) {
                    ReferenceDescriptor ref = (ReferenceDescriptor) fd;
                    //check whether reference is null without dereferencing
                    ProxyReference proxy = (ProxyReference) TypeUtil.getFieldProxy(object,
                                                                                   ref.getName());
                    if (proxy != null) {
                        DisplayReference newReference = 
                            new DisplayReference(proxy, ref.getReferencedClassDescriptor(),
                                                 webconfigTypeMap, webProperties);
                        references.put(fd.getName(), newReference);
                    }
                } else if (fd.isCollection()) {
                    Object fieldValue = TypeUtil.getFieldValue(object, fd.getName());
                    ClassDescriptor refCld =
                        ((CollectionDescriptor) fd).getReferencedClassDescriptor();
                    DisplayCollection newCollection =
                        new DisplayCollection((List) fieldValue, refCld, 
                                              webconfigTypeMap, webProperties);
                    if (newCollection.getSize() > 0) {
                        collections.put(fd.getName(), newCollection);
                    }
                }
            }

            List cldFieldConfigs = FieldConfigHelper.getClassFieldConfigs(webconfigTypeMap, cld);

            Iterator cldFieldConfigIter = cldFieldConfigs.iterator();

            while (cldFieldConfigIter.hasNext()) {
                FieldConfig fc = (FieldConfig) cldFieldConfigIter.next();

                fieldExprs.add(fc.getFieldExpr());
            }
        }

        for (Iterator i = PrimaryKeyUtil.getPrimaryKeyFields(model, object.getClass()).iterator();
             i.hasNext();) {
            FieldDescriptor fd = (FieldDescriptor) i.next();
            if (TypeUtil.getFieldValue(object, fd.getName()) != null) {
                if (fd.isAttribute() && !fd.getName().equals("id")) {
                    keyAttributes.add(fd.getName());
                } else if (fd.isReference()) {
                    keyReferences.add(fd.getName());
                }
            }
        }

        // make a combined Map
        refsAndCollections.putAll(references);
        refsAndCollections.putAll(collections);
    }
    
    /**
     * Get the real business object
     * @return the object
     */
    public InterMineObject getObject() {
        return object;
    }
    
    /**
     * Get the id of this object
     * @return the id
     */
    public int getId() {
        return object.getId().intValue();
    }
    
    /**
     * Get the class descriptors for this object
     * @return the class descriptors
     */
    public Set getClds() {
        return clds;
    }

    /**
     * Get the key attribute fields and values for this object
     * @return the key attributes
     */
    public List getKeyAttributes() {
        return keyAttributes;
    }

    /**
     * Get the key reference fields and values for this object
     * @return the key references
     */
    public List getKeyReferences() {
        return keyReferences;
    }

    /**
     * Get the attribute fields and values for this object
     * @return the attributes
     */
    public Map getAttributes() {
        return attributes;
    }

    /**
     * Get the reference fields and values for this object
     * @return the references
     */
    public Map getReferences() {
        return references;
    }

    /**
     * Get the collection fields and values for this object
     * @return the collections
     */
    public Map getCollections() {
        return collections;
    }

    /**
     * Get all the reference and collection fields and values for this object
     * @return the collections
     */
    public Map getRefsAndCollections() {
        return refsAndCollections;
    }

    /**
     * Return the path expressions for the fields that should be used when summarising this
     * DisplayObject.
     * @return the expressions
     */
    public List getFieldExprs() {
        return fieldExprs;
    }

    /**
     * Get the map indication whether individuals fields are to be display verbosely
     * @return the map
     */
    public Map getVerbosity() {
        return Collections.unmodifiableMap(verbosity);
    }

    /**
     * Set the verbosity for a field
     * @param fieldName the field name
     * @param verbose true or false
     */
    public void setVerbosity(String fieldName, boolean verbose) {
        verbosity.put(fieldName, verbose ? fieldName : null);
    }
}
