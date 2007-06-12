package org.intermine.web.logic.template;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.path.Path;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.query.Constraint;
import org.intermine.web.logic.query.PathNode;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.search.WebSearchable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.tagging.TagTypes;

/**
 * Helper methods for template lists.
 * @author Thomas Riley
 */
public class TemplateListHelper
{
    private static final Logger LOG = Logger.getLogger(TemplateListHelper.class);
    private static final TemplateComparator TEMPLATE_COMPARATOR = new TemplateComparator();
    
    /**
     * Get the Set of templates for a given aspect.
     * @param aspect aspect name
     * @param context ServletContext
     * @return Set of TemplateQuerys
     */
    public static List<TemplateQuery> getAspectTemplates(String aspect, ServletContext context) {
        String sup = (String) context.getAttribute(Constants.SUPERUSER_ACCOUNT);
        ProfileManager pm = SessionMethods.getProfileManager(context);
        Profile p = pm.getProfile(sup);
        
        Map<String, TemplateQuery> templates = new TreeMap<String, TemplateQuery>();
        List tags = pm.getTags(null, null, TagTypes.TEMPLATE, sup);
        
        for (Iterator iter = tags.iterator(); iter.hasNext(); ) {
            Tag tag = (Tag) iter.next();
            if (tag.getTagName().startsWith("aspect:")) {
                String aspectFromTagName = tag.getTagName().substring(7).trim();

                if (StringUtils.equals(aspect, aspectFromTagName)) {
                    TemplateQuery tq = p.getSavedTemplates().get(tag.getObjectIdentifier());
                    if (tq != null) {
                        templates.put(tq.getName(), tq);
                    }
                }
            }
        }
        
        List<TemplateQuery> retList = new ArrayList<TemplateQuery>(templates.values());

        return retList;
    }

    /**
     * Get the Set of templates for a given aspect that contains constraints
     * that can be filled in with an attribute from the given InterMineObject.
     * @param aspect aspect name
     * @param context ServletContext
     * @param object InterMineObject
     * @param fieldExprsOut field expressions to fill in
     * @return Set of TemplateQuerys
     */
    public static List<TemplateQuery> getAspectTemplateForClass(String aspect,
                                                 ServletContext context,
                                                 InterMineObject object,
                                                 Map<TemplateQuery, List<String>> fieldExprsOut) {
        if (aspect.startsWith("aspect:")) {
            aspect = aspect.substring(7).trim();
        }
            
        List<TemplateQuery> templates = new ArrayList<TemplateQuery>();
        ObjectStore os = (ObjectStore) context.getAttribute(Constants.OBJECTSTORE);
        List<TemplateQuery> all = getAspectTemplates(aspect, context);
        Set<Class> types = new HashSet<Class>();
        types.addAll(DynamicUtil.decomposeClass(object.getClass()));
        types.addAll(Arrays.asList(object.getClass().getInterfaces()));
        Class sc = object.getClass();
        while (sc != null) {
            types.add(sc);
            sc = sc.getSuperclass();
        }
        
        
      Model model = os.getModel();
      TEMPLATE:
        for (Iterator<TemplateQuery> iter = all.iterator(); iter.hasNext();) {
            TemplateQuery template = iter.next();
            List<String> fieldExprs = new ArrayList<String>();
            // Only want one editable constraint
            for (Map.Entry<String, PathNode> entry : template.getNodes().entrySet()) {
                PathNode pathNode = entry.getValue();
                if (template.getEditableConstraints(pathNode).size() > 1) {
                    continue TEMPLATE;
                }
                for (Constraint c : pathNode.getConstraints()) {
                    if (!c.isEditable()) {
                        continue;
                    }

                    if (c.getOp().equals(ConstraintOp.LOOKUP)) {
                        try {
                            if (TypeUtil.isInstanceOf(object, model.getPackageName() + "."
                                                              + pathNode.getType())) {
                                templates.add(template);
                            }
                        } catch (ClassNotFoundException err) {
                            LOG.error(err);
                            continue TEMPLATE;
                        } 
                    } else {
                        String constraintIdentifier = c.getIdentifier();
                        String[] bits = constraintIdentifier.split("\\.");

                        if (bits.length != 2) {
                            // we can't handle anything like "Department.company.name" yet so ignore
                            // this
                            // template
                            continue TEMPLATE;
                        }

                        String className = model.getPackageName() + "." + bits[0];
                        String fieldName = bits[1];
                        String fieldExpr = TypeUtil.unqualifiedName(className) + "." + fieldName;
                        try {
                            Class iface = Class.forName(className);
                            if (types.contains(iface)
                                && model.getClassDescriptorByName(className)
                                        .getFieldDescriptorByName(fieldName) != null) {
                                Path path = new Path(model, fieldExpr);
                                if (path.resolve(object) == null) {
                                    // ignore this template because putting a null into a template
                                    // isn't
                                    // a good idea
                                    continue TEMPLATE;
                                }
                                fieldExprs.add(fieldExpr);
                            }
                        } catch (ClassNotFoundException err) {
                            LOG.error(err);
                            continue TEMPLATE;
                        }
                    }
                }
            }
            if (fieldExprs.size() > 0) {
                templates.add(template);
                fieldExprsOut.put(template, fieldExprs);
            }
        }
        
        Collections.sort(templates, TEMPLATE_COMPARATOR);
        
        return templates;
    }
    
    /**
     * Get all the templates for a given type
     * @param context the servlet context
     * @param bag the bag from which to get the type from
     * @return a Map containing the templates of the given type
     */
    public static Map<TemplateQuery, String> getTemplateListForType (ServletContext context, 
                                                                     InterMineBag bag) {
        String type = bag.getType();
        HashMap<TemplateQuery, String> templates = new HashMap<TemplateQuery, String>();
        String sup = (String) context.getAttribute(Constants.SUPERUSER_ACCOUNT);
        ProfileManager pm = SessionMethods.getProfileManager(context);
        Profile p = pm.getProfile(sup);
        Collection templateList = p.getSavedTemplates().values();
        for (Iterator iter = templateList.iterator(); iter.hasNext();) {
            TemplateQuery templateQuery = (TemplateQuery) iter.next();
            List nodes = templateQuery.getEditableNodes();
            for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
                PathNode node = (PathNode) iterator.next();
                String name = null;
                if (node.getParentType() != null) {
                    name = TypeUtil.unqualifiedName(node.getParentType());
                    Map<String, Set> classKeys =
                        (Map<String, Set>) context.getAttribute(Constants.CLASS_KEYS);
                    if (ClassKeyHelper.isKeyField(classKeys, 
                                                   node.getParentType(), node.getFieldName())) {
                        if (type.equals(name)) {
                            templates.put(templateQuery, TemplateHelper.GLOBAL_TEMPLATE);
                            break;
                        }
                    }
                }
           }
        }
        return templates;
    }
    
    /**
     * Get the Set of templates for a given aspect and a given type
     * @param aspect aspect name
     * @param context servlet context
     * @param bag the bag from which to extract the type
     * @param fieldExprsOut a map to populate with matching node names
     * @return the List of templates
     */
    public static List<TemplateQuery> getAspectTemplatesForType(String aspect, 
                              ServletContext context, InterMineBag bag, Map<TemplateQuery, 
                              List<String>> fieldExprsOut) {
        if (aspect.startsWith("aspect:")) {
            aspect = aspect.substring(7).trim();
        }
        String sup = (String) context.getAttribute(Constants.SUPERUSER_ACCOUNT);
        ProfileManager pm = SessionMethods.getProfileManager(context);
        Profile p = pm.getProfile(sup);
        Class bagClass;
        try {
            bagClass = Class.forName(bag.getQualifiedType());
        } catch (ClassNotFoundException e1) {
            // give up
            return Collections.emptyList();
        }
        SearchRepository searchRepository  = 
            (SearchRepository) context.getAttribute(Constants.GLOBAL_SEARCH_REPOSITORY);
        Map<String, ? extends WebSearchable> globalTemplates = 
            searchRepository.getWebSearchableMap(TagTypes.TEMPLATE);
        ObjectStore os = (ObjectStore) context.getAttribute(Constants.OBJECTSTORE);
        Model model = os.getModel();
        List<TemplateQuery> templates = new ArrayList<TemplateQuery>();
        List tags = pm.getTags(null, null, TagTypes.TEMPLATE, sup);
        
      TAGS:
        for (Iterator iter = tags.iterator(); iter.hasNext(); ) {
            Tag tag = (Tag) iter.next();
            if (tag.getTagName().startsWith("aspect:")) {
                String aspectFromTagName = tag.getTagName().substring(7).trim();

                if (StringUtils.equals(aspect, aspectFromTagName)) {

                    TemplateQuery templateQuery = 
                        (TemplateQuery) globalTemplates.get(tag.getObjectIdentifier());
                    if (templateQuery != null) {
                        List constraints = templateQuery.getAllConstraints();
                        Iterator constraintIter = constraints.iterator();
                        List<String> fieldExprs = new ArrayList<String>();
                        while (constraintIter.hasNext()) {
                            Constraint c = (Constraint) constraintIter.next();

                            if (!c.isEditable()) {
                                continue;
                            }
                            
                            String constraintIdentifier = c.getIdentifier();
                            String[] bits = constraintIdentifier.split("\\.");
             
                            if (bits.length != 2) {
                                // we can't handle anything like "Department.company.name" yet so
                                // ignore this template
                                continue TAGS;
                            }

                            String className = model.getPackageName() + "." + bits[0];
                            String fieldName = bits[1];
                            String fieldExpr =
                                TypeUtil.unqualifiedName(className) + "." + fieldName;

                            /*if (allNull(os, bag, fieldName)) {
                                // ignore this template because putting a null into a template isn't
                                // a good idea
                                continue TAGS;
                            }*/
                            Class identifierClass;
                            try {
                                identifierClass = Class.forName(className);
                            } catch (ClassNotFoundException e) {
                               continue TAGS;
                            }
                            
                            if (identifierClass.isAssignableFrom(bagClass)
                                && model.getClassDescriptorByName(className)
                                   .getFieldDescriptorByName(fieldName) != null) {
                                fieldExprs.add(fieldExpr);
                            }
                         }
                        if (fieldExprs.size() > 0) {
                            templates.add(templateQuery);
                            fieldExprsOut.put(templateQuery, fieldExprs);
                        }

                    }
                }
            }
        }
        
        return templates;
    }

    /**
     * Return true if and only if the ba 
     * @throws IllegalAccessException 
     */
    /*private static boolean allNull(ObjectStore os, InterMineBag bag, String fieldName) {

        Iterator iter = bag.iterator();

        while (iter.hasNext()) {
            BagElement element = (BagElement) iter.next();

            try {
                if (TypeUtil.getFieldValue(os.getObjectById(element.getId()), fieldName) != null) {
                    return false;
                }
            } catch (IllegalAccessException e) {
                // give up
                return true;
            } catch (ObjectStoreException e) {
                // give up
                return true;
            }
        }

        return true;
    }*/
}
