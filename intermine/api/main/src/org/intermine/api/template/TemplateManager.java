package org.intermine.api.template;

/*
 * Copyright (C) 2002-2010 FlyMine
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.TagManagerFactory;
import org.intermine.api.search.Scope;
import org.intermine.api.tag.AspectTagUtil;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.metadata.Model;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathException;

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
     * Fetch a global template by name.
     * @param templateName name of the template to fetch
     * @return the template or null
     */
    public TemplateQuery getGlobalTemplate(String templateName) {
        return getGlobalTemplates().get(templateName);
    }

    /**
     * Fetch a user template by name.
     * @param profile the user to fetch the template from
     * @param templateName name of the template to fetch
     * @return the template or null
     */
    public TemplateQuery getUserTemplate(Profile profile, String templateName) {
        return profile.getSavedTemplates().get(templateName);
    }

    /**
     * Fetch a user or global template by name.  If there are name collisions the user template
     * take precedence.
     * @param profile the user to fetch the template from
     * @param templateName name of the template to fetch
     * @return the template or null
     */
    public TemplateQuery getUserOrGlobalTemplate(Profile profile, String templateName) {
        return getUserAndGlobalTemplates(profile).get(templateName);
    }

    /**
     * Fetch a template with the scope provided, possible values are defined in Scope class.  If
     * the scope is ALL and there name collisions between user and global templates, the user
     * template takes precedence.
     * @param profile the user to fetch the template from
     * @param templateName name of the template to fetch
     * @param scope user, global or all templates
     * @return the template or null
     */
    public TemplateQuery getTemplate(Profile profile, String templateName, String scope) {
        if (scope.equals(Scope.GLOBAL)) {
            return getGlobalTemplate(templateName);
        } else if (scope.equals(Scope.USER)) {
            return getUserTemplate(profile, templateName);
        } else if (scope.equals(Scope.ALL)) {
            return getUserOrGlobalTemplate(profile, templateName);
        } else {
            throw new RuntimeException("Scope '" + scope + "' not recognised attempting to find "
                    + "template: " + templateName + ".");
        }
    }


    private Map<String, TemplateQuery> getUserAndGlobalTemplates(Profile profile) {
        // where name collisions occur user templates take precedence
        Map<String, TemplateQuery> allTemplates = new HashMap<String, TemplateQuery>();

        allTemplates.putAll(getGlobalTemplates());
        allTemplates.putAll(profile.getSavedTemplates());

        return allTemplates;
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

        Set<String> allClasses;
        if (type.contains(",")) {
            allClasses = new HashSet<String>(Arrays.asList(type.split(",")));
        } else {
            allClasses = Collections.singleton(type);
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
        List<Tag> noReportTags = tagManager.getTags(TagNames.IM_REPORT, template.getName(),
                TagTypes.TEMPLATE, superProfile.getUsername());
        if (noReportTags.size() < 1) {
            return false;
        }

        // we can only provide a value for one editable constraint
        if (template.getEditableConstraints().size() != 1) {
            return false;
        }
        PathConstraint constraint = template.getEditableConstraints().get(0);

        if (constraint.getOp().equals(ConstraintOp.LOOKUP)) {
            if (!classes.contains(constraint.getPath())) {
                return false;
            }
        } else {
            Path path = null;
            try {
                path = template.makePath(constraint.getPath());
            } catch (PathException e) {
                // not a valid path
                return false;
            }
            // if this a root path can only be a LOOKUP constraint
            if (path.isRootPath()) {
                return false;
            }
            String parentPath = path.getPrefix().getNoConstraintsString();
            if (!classes.contains(parentPath)) {
                return false;
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
     * Return template queries used for converting between types in bag upload and lookup queries,
     * these are Superuser templates that have been tagged with TagNames.IM_CONVERTER.
     * @return a list of conversion template queries
     */
    public List<TemplateQuery> getConversionTemplates() {
        List<TemplateQuery> conversionTemplates = new ArrayList<TemplateQuery>();
        Map<String, TemplateQuery> templatesMap =
                getTemplatesWithTag(superProfile, TagNames.IM_CONVERTER);
        if (templatesMap.size() > 0) {
            conversionTemplates.addAll(templatesMap.values());
        }
        return conversionTemplates;
    }



    /**
     * Return a map from template name to template query of template in the given profile that are
     * tagged with a particular tag.
     * @param profile a user profile to get templates from
     * @param tag the tag to search for
     * @return a map from template name to template query
     */
    private Map<String, TemplateQuery> getTemplatesWithTag(Profile profile, String tag) {
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
