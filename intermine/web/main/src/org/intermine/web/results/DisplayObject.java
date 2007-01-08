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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.PrimaryKeyUtil;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.config.FieldConfig;
import org.intermine.web.config.FieldConfigHelper;
import org.intermine.web.config.WebConfig;

/**
 * Class to represent an object for display in the webapp. Various maps and collections
 * are calculated here the first time an object is viewed and cached for later use. Only
 * information that remains static throughout the session should be precalcualted here.
 * Anything dynamic is computed in ObjectDetailsController.
 * 
 * @author Mark Woodbridge
 */
public class DisplayObject
{
    private InterMineObject object;
    private WebConfig webConfig;
    private Map webProperties;
    private Model model;
    
    private Set clds;

    private Map attributes = null;
    private Map attributeDescriptors = null;
    private Map references = null;
    private Map collections = null;
    private Map refsAndCollections = null;
    private List keyAttributes = null;
    private List keyReferences = null;
    private Map fieldConfigMap = null;
    private List fieldExprs = null;
    //private Map classTemplateExprs = null;
    //private HttpSession session;
    private Map verbosity = new HashMap();
    private final Map classKeys;
    
    /**
     * Create a new DisplayObject.
     * @param object the object to display
     * @param model the metadata for the object
     * @param webConfig the WebConfig object for this webapp
     * @param webProperties the web properties from the session
     * @param classKeys map of classname to set of keys
     * @throws Exception if an error occurs
     */
    public DisplayObject(/*HttpSession session,*/ InterMineObject object, Model model,
                         WebConfig webConfig,
                         Map webProperties,
                         Map classKeys) throws Exception {
        this.object = object;
        this.model = model;
        this.webConfig = webConfig;
        this.webProperties = webProperties;
        //this.session = session;
        this.classKeys = classKeys;

        //ServletContext servletContext = session.getServletContext();
        /*this.classTemplateExprs = 
            (Map) servletContext.getAttribute(Constants.CLASS_TEMPLATE_EXPRS);*/
        
        clds = getLeafClds(object.getClass(), model);
    }

    /**
     * Get the set of leaf ClassDescriptors for a given InterMineObject class.
     * @param clazz object type
     * @param model model
     * @return Set of ClassDescriptor objects
     */
    public static Set getLeafClds(Class clazz, Model model) {
        if (!InterMineObject.class.isAssignableFrom(clazz)) {
            return Collections.EMPTY_SET;
        }
        Set leafClds = new LinkedHashSet();
        for (Iterator j = DynamicUtil.decomposeClass(clazz).iterator();
            j.hasNext();) {
            leafClds.add(model.getClassDescriptorByName(((Class) j.next()).getName()));
        }
        return leafClds;
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
        if (keyAttributes == null) {
            initialise();
        }
        return keyAttributes;
    }

    /**
     * Get attribute descriptors.
     * @return map of attribute descriptors
     */
    public Map getAttributeDescriptors() {
        if (attributeDescriptors == null) {
            initialise();
        }
        return attributeDescriptors;
    }
    
    /**
     * Get the key reference fields and values for this object
     * @return the key references
     */
    public List getKeyReferences() {
        if (keyReferences == null) {
            initialise();
        }
        return keyReferences;
    }

    /**
     * Get the attribute fields and values for this object
     * @return the attributes
     */
    public Map getAttributes() {
        if (attributes == null) {
            initialise();
        }
        return attributes;
    }

    /**
     * Get the reference fields and values for this object
     * @return the references
     */
    public Map getReferences() {
        if (references == null) {
            initialise();
        }
        return references;
    }

    /**
     * Get the collection fields and values for this object
     * @return the collections
     */
    public Map getCollections() {
        if (collections == null) {
            initialise();
        }
        return collections;
    }

    /**
     * Get all the reference and collection fields and values for this object
     * @return the collections
     */
    public Map getRefsAndCollections() {
        if (refsAndCollections == null) {
            initialise();
        }
        return refsAndCollections;
    }

    /*
    public Map getDisplayTemplates() {
        if (displayTemplateMap == null) {
            displayTemplateMap = new HashMap();
        }
        return displayTemplateMap;
    }
    */
    /**
     * Return a Map from template name to a count of the number of results will be returned if the\
     * template is run using this DisplayObject to fill in the editable fields.
     * @return a Map of template counts
     */
    /*public Map getTemplateCounts() {
        if (templateCounts != null) {
            return templateCounts;
        }
        Map newTemplateCounts = new TreeMap();
        
        Map templateExprMap = new HashMap();

        for (Iterator i = clds.iterator(); i.hasNext();) {
            ClassDescriptor cld = (ClassDescriptor) i.next();
            Map thisCldTemplateExprMap = (Map) classTemplateExprs.get(cld.getName());

            if (thisCldTemplateExprMap != null) {
                templateExprMap.putAll(thisCldTemplateExprMap);
            }
        }

        Iterator templateNameIter = templateExprMap.keySet().iterator();

        while (templateNameIter.hasNext()) {
            String templateName = (String) templateNameIter.next();

            List exprList = (List) templateExprMap.get(templateName);

            TemplateQuery template =
                TemplateHelper.findTemplate(session, templateName, "global");

            if (template == null) {
                throw new IllegalStateException("Could not find template \""
                                                + templateName + "\"");
            }

            List templateConstraints = template.getAllConstraints();
            List editableConstraints = new ArrayList();

            Iterator templateConstraintIter = templateConstraints.iterator();

            while (templateConstraintIter.hasNext()) {
                Constraint thisConstraint = (Constraint) templateConstraintIter.next();

                if (thisConstraint.isEditableInTemplate()) {
                    editableConstraints.add(thisConstraint);
                }
            }

            if (editableConstraints.size() != exprList.size()) {
                continue;
            }

            TemplateForm tf = new TemplateForm();

            for (int i = 0; i < editableConstraints.size(); i++) {
                Constraint thisConstraint = (Constraint) editableConstraints.get(i);

                String constraintIdentifier = thisConstraint.getIdentifier();

                int dotIndex = constraintIdentifier.indexOf('.');

                if (dotIndex == -1) {
                    throw new RuntimeException("constraint identifier is not in current "
                                               + "format");
                }

                String fieldName = constraintIdentifier.substring(dotIndex + 1);

                Object fieldValue;
                try {
                    fieldValue = TypeUtil.getFieldValue(object, fieldName);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("cannot set field " + fieldName + " of object "
                                               + object.getId(), e);
                }

                if (exprList.contains(constraintIdentifier)) {
                    tf.setAttributeOps("" + (1 + i), ConstraintOp.EQUALS.getIndex().toString());
                    tf.setAttributeValues("" + (1 + i), fieldValue.toString());
                } else {
                    // too many editable constraints
                    continue;
                }
            }

            tf.parseAttributeValues(template, session, new ActionErrors(), false);

            PathQuery pathQuery = TemplateHelper.templateFormToQuery(tf, template);
            Query query;
            try {
                query = MainHelper.makeQuery(pathQuery, Collections.EMPTY_MAP);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ServletContext servletContext = session.getServletContext();
            ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
            Results results;
            try {
                results = os.execute(query);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("cannot find results of template query " 
                                           + templateName + " for object " + object.getId());
            }

            newTemplateCounts.put(templateName, new Integer(results.size()));
        }
        templateCounts = newTemplateCounts;
        return templateCounts;
    }*/

    /**
     * Return the path expressions for the fields that should be used when summarising this
     * DisplayObject.
     * @return the expressions
     */
    public List getFieldExprs() {
        if (fieldExprs == null) {
            fieldExprs = new ArrayList();

            for (Iterator i = getFieldConfigMap().keySet().iterator(); i.hasNext();) {
                String fieldExpr = (String) i.next();
                fieldExprs.add(fieldExpr);
            }
        }
        return fieldExprs;
    }

    /**
     * Get map from field expr to FieldConfig.
     * @return map from field expr to FieldConfig
     */
    public Map getFieldConfigMap() {
        if (fieldConfigMap == null) {
            fieldConfigMap = new LinkedHashMap();

            for (Iterator i = clds.iterator(); i.hasNext();) {
                ClassDescriptor cld = (ClassDescriptor) i.next();
                List cldFieldConfigs = FieldConfigHelper.getClassFieldConfigs(webConfig, cld);
                Iterator cldFieldConfigIter = cldFieldConfigs.iterator();

                while (cldFieldConfigIter.hasNext()) {
                    FieldConfig fc = (FieldConfig) cldFieldConfigIter.next();

                    fieldConfigMap.put(fc.getFieldExpr(), fc);
                }
            }
        }

        return fieldConfigMap;
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
    
    /**
     * Get verbosity of a field
     * @param placementAndField a String that combines the name of the current placement/aspect and
     * a fieldName
     * @return true or false
     */
    public boolean isVerbose(String placementAndField) {
        return verbosity.get(placementAndField) != null;
    }

    /**
     * Create the Maps and Lists returned by the getters in this class.
     */
    private void initialise() {
        attributes = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        references = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        collections = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        refsAndCollections = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        keyAttributes = new ArrayList();
        keyReferences = new ArrayList();
        attributeDescriptors = new HashMap();
        
        try {
            for (Iterator i = clds.iterator(); i.hasNext();) {
                ClassDescriptor cld = (ClassDescriptor) i.next();
                
                for (Iterator j = cld.getAllFieldDescriptors().iterator(); j.hasNext();) {
                    FieldDescriptor fd = (FieldDescriptor) j.next();

                    if (fd.isAttribute() && !fd.getName().equals("id")) {
                        Object fieldValue = TypeUtil.getFieldValue(object, fd.getName());
                        if (fieldValue != null) {
                            attributes.put(fd.getName(), fieldValue);
                            attributeDescriptors.put(fd.getName(), fd);
                        }
                    } else if (fd.isReference()) {
                        ReferenceDescriptor ref = (ReferenceDescriptor) fd;
                        //check whether reference is null without dereferencing
                        ProxyReference proxy =
                            (ProxyReference) TypeUtil.getFieldProxy(object, ref.getName());
                        //if (proxy != null) {
                            DisplayReference newReference =
                                new DisplayReference(proxy, ref,
                                                     webConfig, webProperties, classKeys);
                            references.put(fd.getName(), newReference);
                        //}

                    } else if (fd.isCollection()) {
                        Object fieldValue = TypeUtil.getFieldValue(object, fd.getName());
                        DisplayCollection newCollection =
                            new DisplayCollection((Collection) fieldValue,
                                    (CollectionDescriptor) fd, webConfig, webProperties,
                                    classKeys);
                        //if (newCollection.getSize() > 0) {
                            collections.put(fd.getName(), newCollection);
                        //}
                    }
                }
            }

            Iterator i = PrimaryKeyUtil.getPrimaryKeyFields(model, object.getClass()).iterator();

            while (i.hasNext()) {
                FieldDescriptor fd = (FieldDescriptor) i.next();
                if (TypeUtil.getFieldValue(object, fd.getName()) != null) {
                    if (fd.isAttribute() && !fd.getName().equals("id")) {
                        keyAttributes.add(fd.getName());
                    } else if (fd.isReference()) {
                        keyReferences.add(fd.getName());
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception while creating a DisplayObject", e);
        }

        // make a combined Map
        refsAndCollections.putAll(references);
        refsAndCollections.putAll(collections);
    }
}
