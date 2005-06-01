package org.intermine.web;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.RAMDirectory;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;

/**
 * Respository object for TemplateQueries.
 * 
 * @author Thomas Riley
 */
public class TemplateRepository
{
    private static final Logger LOG = Logger.getLogger(InitialiserPlugin.class);
    
    private ServletContext servletContext;
    
    /**
     * Construct a new instance of TemplateRepository.
     * 
     * @param servletContext the servlet context
     */
    public TemplateRepository(ServletContext servletContext) {
        // index global templates
        this.servletContext = servletContext;
        reloadGlobalTemplateQueries(servletContext);
    }
    
    /**
     * Get the singleton TemplateRespository.
     * 
     * @param context the servlet context
     * @return the singleton TemplateRepository object
     */
    public static final TemplateRepository getTemplateRepository(ServletContext context) {
        return (TemplateRepository) context.getAttribute(Constants.TEMPLATE_REPOSITORY);
    }
    
    /**
     * Called to tell the repository that a global template has been added to
     * the superuser user profile.
     * 
     * @param template the TemplateQuery added
     */
    public void globalTemplateAdded(TemplateQuery template) {
        reloadGlobalTemplateQueries(servletContext);
    }
    
    /**
     * Called to tell the repository that a global template has been removed from
     * the superuser user profile.
     * 
     * @param template the TemplateQuery removed
     */
    public void globalTemplateRemoved(TemplateQuery template) {
        reloadGlobalTemplateQueries(servletContext);
    }
    
    /**
     * Called to tell the repository that a global template has been updated in
     * the superuser user profile.
     * 
     * @param template the TemplateQuery updated
     */
    public void globalTemplateUpdated(TemplateQuery template) {
        reloadGlobalTemplateQueries(servletContext);
    }

    /**
     * Called to tell the repository that the set of global templates in the superuser
     * profile has changed.
     */
    public void globalTemplatesChanged() {
        reloadGlobalTemplateQueries(servletContext);
    }
    
    /**
     * Read the template queries into the GLOBAL_TEMPLATE_QUERIES servlet context attribute and set
     * CATEGORY_TEMPLATES and CLASS_CATEGORY_TEMPLATES. 
     * This is also called when the superuser updates his or her templates.
     *
     * @param servletContext  servlet context in which to place template map
     */
    private static void reloadGlobalTemplateQueries(ServletContext servletContext) {   
        Map templateMap = Collections.synchronizedMap(new HashMap());
        ProfileManager pm = (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);
        String superuser = (String) servletContext.getAttribute(Constants.SUPERUSER_ACCOUNT);
        
        if (superuser != null) {
            if (pm.hasProfile(superuser)) {
                Profile profile = pm.getProfile(superuser, pm.getPassword(superuser));
                templateMap = Collections.synchronizedMap(new TreeMap(profile.getSavedTemplates()));
            } else {
                LOG.warn("failed to get profile for superuser " + superuser);
            }
        } else {
            LOG.error("superuser.account not specified");
        }
        servletContext.setAttribute(Constants.GLOBAL_TEMPLATE_QUERIES, templateMap);
        
        // Sort into categories
        Map categoryTemplates = new HashMap();
        // a Map from class name to a Map from category to template
        Map classCategoryTemplates = new HashMap();
        // a Map from class name to a Map from template name to field name List - the field
        // names/expressions are the ones that should be set when a template is linked to from the
        // object details page eg. Gene.identifier
        Map classTemplateExprs = new HashMap();
        Iterator iter = templateMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            TemplateQuery template = (TemplateQuery) entry.getValue();
            List list = (List) categoryTemplates.get(template.getCategory());
            if (list == null) {
                list = new ArrayList();
                categoryTemplates.put(template.getCategory(), list);
            }
            list.add(template);

            Object osObject = servletContext.getAttribute(Constants.OBJECTSTORE);
            ObjectStore os = (ObjectStore) osObject;
            
            setClassesForTemplate(os, template, classCategoryTemplates, classTemplateExprs);
        }
        servletContext.setAttribute(Constants.CATEGORY_TEMPLATES, categoryTemplates);
        servletContext.setAttribute(Constants.CLASS_CATEGORY_TEMPLATES, classCategoryTemplates);
        servletContext.setAttribute(Constants.CLASS_TEMPLATE_EXPRS, classTemplateExprs);
        
        reindexGlobalTemplates(servletContext);
    }
    
    /**
     * Return two Maps with information about the relations between classnames, a given template and
     * its template categories.
     * 
     * @param classCategoryTemplates a Map from class name to a Map from category to template
     * @param classTemplateExprs a Map from class name to a Map from template name to field name
     * List - the field names/expressions are the ones that should be set when a template is linked
     * to from the object details page eg. Gene.identifier
     */
    private static void setClassesForTemplate(ObjectStore os, TemplateQuery template,
                                              Map classCategoryTemplates,
                                              Map classTemplateExprs) {
        List constraints = template.getAllConstraints();
        Model model = os.getModel();
        Iterator constraintIter = constraints.iterator();
        
        // look for ClassName.fieldname  (Gene.identifier)
        // or ClassName.fieldname.fieldname.fieldname...  (eg. Gene.organism.name)
        while (constraintIter.hasNext()) {
            Constraint c = (Constraint) constraintIter.next();

            String constraintIdentifier = c.getIdentifier();
            String[] bits = constraintIdentifier.split("\\.");

            if (bits.length == 2) {
                String className = model.getPackageName() + "." + bits[0];
                String fieldName = bits[1];
                String fieldExpr = TypeUtil.unqualifiedName(className) + "." + fieldName;
                ClassDescriptor cd = model.getClassDescriptorByName(className);

                if (cd != null && cd.getFieldDescriptorByName(fieldName) != null) {
                    Set subClasses = model.getAllSubs(cd);

                    Set thisAndSubClasses = new HashSet();
                    thisAndSubClasses.addAll(subClasses);
                    thisAndSubClasses.add(cd);

                    Iterator thisAndSubClassesIterator = thisAndSubClasses.iterator();
                    
                    while (thisAndSubClassesIterator.hasNext()) {
                        ClassDescriptor thisCD = (ClassDescriptor) thisAndSubClassesIterator.next();
                        String thisClassName = thisCD.getName();
                        if (!classCategoryTemplates.containsKey(thisClassName)) {
                            classCategoryTemplates.put(thisClassName, new HashMap());
                        }
                    
                        Map categoryTemplatesMap = (Map) classCategoryTemplates.get(thisClassName);
                    
                        if (!categoryTemplatesMap.containsKey(template.getCategory())) {
                            categoryTemplatesMap.put(template.getCategory(), new ArrayList());
                        }
                    
                        ((List) categoryTemplatesMap.get(template.getCategory())).add(template);
                    
                        if (!classTemplateExprs.containsKey(thisClassName)) {
                            classTemplateExprs.put(thisClassName, new HashMap());
                        }
                    
                        Map fieldNameTemplatesMap = (Map) classTemplateExprs.get(thisClassName);
                    
                        if (!fieldNameTemplatesMap.containsKey(template.getName())) {
                            fieldNameTemplatesMap.put(template.getName(), new ArrayList());
                        }
                    
                        ((List) fieldNameTemplatesMap.get(template.getName())).add(fieldExpr);
                    }
               }
            }
        }
    }
    
    /**
     * Create the lucene search index of all global template queries.
     * 
     * @param servletContext the servlet context
     */
    private static void reindexGlobalTemplates(ServletContext servletContext) {
        Map templates = (Map) servletContext.getAttribute(Constants.GLOBAL_TEMPLATE_QUERIES);
        RAMDirectory ram = indexTemplates(templates, "global");
        servletContext.setAttribute(Constants.TEMPLATE_INDEX_DIR, ram);
    }
    
    /**
     * Index some TemplateQueries and return the RAMDirectory containing the index.
     * 
     * @param templates Map from template name to TemplateQuery
     * @param type template type (see TemplateHelper)
     * @return a RAMDirectory containing the index
     */
    public static RAMDirectory indexTemplates(Map templates, String type) {
        long time = System.currentTimeMillis();
        LOG.info("Indexing template queries");
        
        RAMDirectory ram = new RAMDirectory();
        IndexWriter writer;
        try {
            writer = new IndexWriter(ram,
                    new SnowballAnalyzer("English", StopAnalyzer.ENGLISH_STOP_WORDS), true);
        } catch (IOException err) {
            throw new RuntimeException("Failed to create lucene IndexWriter", err);
        }
        
        // step global templates, indexing a Document for each template
        Iterator iter = templates.values().iterator();
        int indexed = 0;
        
        while (iter.hasNext()) {
            TemplateQuery template = (TemplateQuery) iter.next();
            
            Document doc = new Document();
            doc.add(Field.Text("name", template.getName()));
            doc.add(Field.UnStored("content", template.getDescription() + " "
                    + template.getCategory() + " " + template.getKeywords()));
            doc.add(Field.UnIndexed("type", type));
            
            try {
                writer.addDocument(doc);
                indexed++;
            } catch (IOException e) {
                LOG.error("Failed to add template " + template.getName()
                        + " to the index", e);
            }
        }
        
        try {
            writer.close();
        } catch (IOException e) {
            LOG.error("IOException while closing IndexWriter", e);
        }
        
        time = System.currentTimeMillis() - time;
        LOG.info("Indexed " + indexed + " out of " + templates.size() + " templates in "
                + time + " milliseconds");
        
        return ram;
    }

}
