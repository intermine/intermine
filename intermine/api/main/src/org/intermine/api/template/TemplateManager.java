package org.intermine.api.template;

/*
 * Copyright (C) 2002-2009 FlyMine
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.TagManagerFactory;
import org.intermine.api.tag.AspectTagUtil;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.PathNode;


/**
 * A TemplateManager provides access to all global and/or user templates and methods to fetch them
 * by type, etc.
 * @author Richard Smith
 *
 */
public class TemplateManager 
{

    private static final TemplateComparator TEMPLATE_COMPARATOR = new TemplateComparator();
    private Profile superProfile;
    private Model model;
    private TagManager tagManager;
    
    /**
     * The BagManager references the super user profile to fetch global bags.
     * @param superProfile the super user profile
     * @param model the object model
     */
    public TemplateManager(Profile superProfile, Model model) {
        this.model = model;
        this.superProfile = superProfile;
        this.tagManager = new TagManagerFactory(superProfile.getProfileManager()).getTagManager();
    }
    
    
    /**
     * Get a list of template queries that should appear on report pages for the given type
     * under the specified aspect.
     * @param aspect the aspect to fetch templates for
     * @param type the type of report page
     * @return a list of template queries
     */
    public List<TemplateQuery> getReportPageTemplatesForAspect(String aspect, String type) {
        List<TemplateQuery> templates = new ArrayList<TemplateQuery>();
        
        List<TemplateQuery> aspectTemplates = getAspectTemplates(aspect);
      
        ClassDescriptor thisCld = model.getClassDescriptorByName(type);
        Set<String> allClasses = new HashSet<String>();
        for (ClassDescriptor cld : model.getClassDescriptorsForClass(thisCld.getType())) {
            allClasses.add(cld.getUnqualifiedName());
        }

        for (TemplateQuery template : aspectTemplates) {
           if (isValidReportTemplate(template, allClasses)) {
               templates.add(template);
           }
        }
      
        Collections.sort(templates, TEMPLATE_COMPARATOR);
        return templates;
    }
    
    
    /**
     * Return true if a template should appear on a report page.  All logic should be contained
     * in this method.
     * @param template the template to check
     * @param classes the class of the report page and it's superclasses
     * @return true if template should be displayed on the report page
     */
    private boolean isValidReportTemplate(TemplateQuery template, Collection<String> classes) {
        
        // is this template tagged to be hidden on report pages
        List<Tag> noReportTags = tagManager.getTags(TagNames.IM_NO_REPORT, template.getName(), 
                TagTypes.TEMPLATE, superProfile.getUsername());
        if (noReportTags.size() > 0) {
            return false;
        }
        
        // we can only provide a value for one editable constraint
        if (template.getAllEditableConstraints().size() != 1) {
            return false;
        }
        
        // there is only one editable constraint but need to loop to get pathNode
        for (Map.Entry<String, PathNode> entry : template.getNodes().entrySet()) {
            PathNode pathNode = entry.getValue();
            for (Constraint c : pathNode.getConstraints()) {
                if (!c.isEditable()) {
                    continue;
                }

                if (c.getOp().equals(ConstraintOp.LOOKUP)) {
                    if (!classes.contains(pathNode.getType())) {
                        return false;
                    }
                } else {
                    // TODO do we still want restriction on constraint identifier?
                    // buggy because if a LOOKUP used the constraint identifier isn't checked
                    String constraintIdentifier = c.getIdentifier();
                    if (constraintIdentifier == null) {
                        // if this template doesn't have an identifier, then the superuser
                        // likely doesn't want it displayed on the object details page
                        return false;
                    }
                    String[] bits = constraintIdentifier.split("\\.");

                    if (bits.length != 2) {
                        // we can't handle anything like "Department.company.name" yet so ignore
                        // this template
                        return false;
                    }

                    if (!classes.contains(pathNode.getPathString())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
 
    
    /**
     * Get public templates for a particular aspect.
     * @param aspect name of aspect tag
     * @return a list of template queries
     */
    public List<TemplateQuery> getAspectTemplates(String aspect) {
        List<TemplateQuery> aspectTemplates = new ArrayList<TemplateQuery>();
        if (AspectTagUtil.isAspectTag(aspect)) {
            aspect = AspectTagUtil.getAspect(aspect);
        }
        
        Map<String, TemplateQuery> globalTemplates = getValidGlobalTemplates();
        
        List<Tag> tags = tagManager.getTags(null, null, TagTypes.TEMPLATE, 
                superProfile.getUsername());
        for (Tag tag : tags) {
            if (AspectTagUtil.isAspectTag(tag.getTagName())) {
                if (StringUtils.equals(aspect, AspectTagUtil.getAspect(tag.getTagName()))) {
                    if (globalTemplates.containsKey(tag.getObjectIdentifier())) {
                        aspectTemplates.add(globalTemplates.get(tag.getObjectIdentifier()));
                    }
                }
            }
        }   
        return aspectTemplates;
    }
    

    /**
     * Return a map from template name to template query containing superuser templates that are
     * tagged as public and are valid for the current data model.
     * @return a map from template name to template query
     */
    public Map<String, TemplateQuery> getValidGlobalTemplates() {
        Map<String, TemplateQuery> validTemplates = new HashMap<String, TemplateQuery>();

        for (Map.Entry<String, TemplateQuery> entry : getGlobalTemplates().entrySet()) {
            if (entry.getValue().isValid()) {
                validTemplates.put(entry.getKey(), entry.getValue());
            }
        }
        return validTemplates;
    }
    
    /**
     * Return a map from template name to template query containing superuser templates that are
     * tagged as public.
     * @return a map from template name to template query
     */
    public Map<String, TemplateQuery> getGlobalTemplates() {
        return getTemplatesWithTag(superProfile, TagNames.IM_PUBLIC);
    }
    
    /**
     * Return a map from template name to template query of template in the given profile that are
     * tagged with a particular tag.
     * @param profile a user profile to get templates from
     * @param tag the tag to search for
     * @return a map from template name to template query
     */
    protected Map<String, TemplateQuery> getTemplatesWithTag(Profile profile, String tag) {
        Map<String, TemplateQuery> templatesWithTag = new HashMap<String, TemplateQuery>();
        
        for (Map.Entry<String, TemplateQuery> entry : profile.getSavedTemplates().entrySet()) {
            TemplateQuery bag = entry.getValue();
            List<Tag> tags = tagManager.getTags(tag, bag.getName(), TagTypes.TEMPLATE,
                    profile.getUsername());
            if (tags.size() > 0) {
                templatesWithTag.put(entry.getKey(), entry.getValue());
            }
        }
        return templatesWithTag;
    }
    
}
