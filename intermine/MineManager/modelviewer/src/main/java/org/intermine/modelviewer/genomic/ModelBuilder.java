package org.intermine.modelviewer.genomic;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.intermine.modelviewer.model.ForeignKey;
import org.intermine.modelviewer.model.Model;
import org.intermine.modelviewer.model.ModelClass;

/**
 * Class to assemble the simple JAXB objects read from core and genomic addition
 * files into a single, coherent Intermine object model.
 * <p>Each class, attribute and relationship is tagged to indicate where that
 * item came from.</p>
 */
public class ModelBuilder
{
    /**
     * The standard tag for classes and attributes from the core file.
     */
    public static final String CORE_TAG = "core.xml";

    /**
     * Logger.
     */
    protected Log logger = LogFactory.getLog(getClass());
    
    /**
     * Builds a model from the given core and the given additions.
     * <p>This is a convenience method taking a collection rather than an array.</p>
     * 
     * @param model The core model.
     * @param additions The additions to embellish the model.
     * 
     * @return A coherent Intermine object model.
     * 
     * @see #buildHierarchy(org.intermine.modelviewer.genomic.Model, GenomicAddition...)
     */
    public Model buildHierarchy(org.intermine.modelviewer.genomic.Model model,
                                Collection<GenomicAddition> additions) {
        GenomicAddition[] array = new GenomicAddition[additions.size()];
        additions.toArray(array);
        return buildHierarchy(model, array);
    }
    
    /**
     * Builds a model from the given core and the given additions.
     * <p>The simple JAXB objects passed in are converted into the interlinked Intermine
     * model objects in the package {@link org.intermine.modelviewer.model}. Relationships
     * between the classes become references between the relevant objects. All classes,
     * attributes and references are tagged to indicate their origin.</p>
     * 
     * @param model The core model.
     * @param additions The additions to embellish the model.
     * 
     * @return A coherent Intermine object model.
     */
    public Model buildHierarchy(org.intermine.modelviewer.genomic.Model model,
                                GenomicAddition... additions) {
        
        Model finalModel = new Model();
        finalModel.setName(model.getName());
        finalModel.setPackage(model.getPackage());
        Map<String, ModelClass> classes = finalModel.getClasses();
        
        // Start the model with the core classes.
        for (org.intermine.modelviewer.genomic.Class c : model.getClazz()) {
            ModelClass mc = new ModelClass(c.getName(), CORE_TAG);
            mc.setInterface(c.isIsInterface());
            
            // Set up attributes.
            for (org.intermine.modelviewer.genomic.Attribute a : c.getAttribute()) {
                mc.addAttribute(a.getName(),
                        new org.intermine.modelviewer.model.Attribute(
                                a.getName(), a.getType(), CORE_TAG));
            }
            classes.put(c.getName(), mc);
        }
        
        // Wire up inheritance and inter-class references in the core.
        for (org.intermine.modelviewer.genomic.Class c : model.getClazz()) {
            ModelClass mc = classes.get(c.getName());
            
            String superclass = c.getExtends();
            if (superclass != null) {
                mc.setSuperclass(classes.get(superclass));
            }
            
            for (org.intermine.modelviewer.genomic.ClassReference r : c.getReference()) {
                ForeignKey ref = makeForeignKey(classes, r, CORE_TAG);
                if (ref != null) {
                    mc.addReference(r.getName(), ref);
                } else {
                    logger.error("This was from reference " + r.getName()
                            + " on class " + c.getName());
                }
            }
            
            for (org.intermine.modelviewer.genomic.ClassReference r : c.getCollection()) {
                ForeignKey ref = makeForeignKey(classes, r, CORE_TAG);
                if (ref != null) {
                    mc.addCollection(r.getName(), ref);
                } else {
                    logger.error("This was from collection " + r.getName()
                            + " on class " + c.getName());
                }
            }
        }
        
        // Embellish the model with new classes from the additions.
        for (GenomicAddition addition : additions) {
            mergeAdditionClasses(classes, addition);
        }
        
        // Finally, wire up inter-class relationships between these additions.
        for (GenomicAddition addition : additions) {
            mergeAdditionRelations(classes, addition);
        }
        
        return finalModel;
    }
    
    /**
     * Create new classes and set up inheritance relationships for classes in the
     * given set of additions.
     * 
     * @param classes A map of class name to ModelClass object for classes currently known.
     * @param addition The addition to merge in.
     */
    protected void mergeAdditionClasses(Map<String, ModelClass> classes, GenomicAddition addition) {
        final String tag = addition.getTag();
        
        Map<String, org.intermine.modelviewer.genomic.Class> newClasses =
            new HashMap<String, org.intermine.modelviewer.genomic.Class>();
        
        for (org.intermine.modelviewer.genomic.Class c : addition.getAdditions().getClazz()) {
            ModelClass mc = classes.get(c.getName());
            
            // Check and if necessary create a new model class.
            if (mc == null) {
                mc = new ModelClass(c.getName(), tag);
                mc.setInterface(c.isIsInterface());
                classes.put(c.getName(), mc);
                newClasses.put(c.getName(), c);
            }

            // Initialise the attributes on this class.
            for (org.intermine.modelviewer.genomic.Attribute a : c.getAttribute()) {
                if (mc.getAttributes().containsKey(a.getName())) {
                    logger.warn("Have duplicate attribute " + a.getName()
                            + " on class " + mc.getName());
                } else {
                    mc.addAttribute(a.getName(),
                            new org.intermine.modelviewer.model.Attribute(
                                    a.getName(), a.getType(), tag));
                }
            }
        }

        // Wire up inheritance.
        for (org.intermine.modelviewer.genomic.Class c : newClasses.values()) {
            ModelClass mc = classes.get(c.getName());
            
            String superclass = c.getExtends();
            if (superclass != null) {
                mc.setSuperclass(classes.get(superclass));
            }
        }
    }
        
    /**
     * Connect the relations and collections between classes as per the given
     * additions. This can affect both classes created in the addition file and
     * add existing relationships to existing classes.
     * 
     * @param classes A map of class name to ModelClass object for classes currently known.
     * @param addition The addition to merge in.
     */
    protected void mergeAdditionRelations(Map<String, ModelClass> classes,
                                          GenomicAddition addition) {
        final String tag = addition.getTag();
        
        for (org.intermine.modelviewer.genomic.Class c : addition.getAdditions().getClazz()) {
            ModelClass mc = classes.get(c.getName());
            assert mc != null : "No class " + c.getName();
            
            for (org.intermine.modelviewer.genomic.ClassReference r : c.getReference()) {
                if (mc.getReferences().containsKey(r.getName())) {
                    logger.warn("Have duplicate reference " + r.getName()
                            + " on class " + mc.getName());
                } else {
                    ForeignKey ref = makeForeignKey(classes, r, tag);
                    if (ref != null) {
                        mc.addReference(r.getName(), ref);
                    } else {
                        logger.error("This was from reference " + r.getName() + " on class "
                                     + c.getName() + " from " + tag);
                    }
                }
            }
            
            for (org.intermine.modelviewer.genomic.ClassReference r : c.getCollection()) {
                if (mc.getReferences().containsKey(r.getName())) {
                    logger.warn("Have duplicate collection " + r.getName()
                            + " on class " + mc.getName());
                } else {
                    ForeignKey ref = makeForeignKey(classes, r, tag);
                    if (ref != null) {
                        mc.addCollection(r.getName(), ref);
                    } else {
                        logger.error("This was from collection " + r.getName() + " on class "
                                     + c.getName() + " from " + tag);
                    }
                }
            }
        }
    }
    
    /**
     * Create a foreign key relationship between two model classes. These objects become
     * the links for a class's relationships and collections.
     * 
     * @param classes A map of class name to ModelClass object for classes currently known.
     * @param r The reference from the JAXB objects to create the foreign key for.
     * @param tag The tag to label the foreign key.
     * 
     * @return The ForeignKey object.
     */
    protected ForeignKey makeForeignKey(Map<String, ModelClass> classes,
                                        org.intermine.modelviewer.genomic.ClassReference r, 
                                        String tag) {

        ForeignKey ref = new ForeignKey(r.getName(), tag);
        ref.setReverseReference(r.getReverseReference());
        String refType = r.getReferencedType();
        if (refType != null) {
            ModelClass refClass = classes.get(refType);
            if (refClass == null) {
                logger.error("There is no class " + refType + " known.");
            } else {
                ref.setReferencedType(refClass);
            }
        }
        return ref;
    }
}
