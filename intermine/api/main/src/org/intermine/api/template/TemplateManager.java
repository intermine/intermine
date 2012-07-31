package org.intermine.api.template;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.TagManagerFactory;
import org.intermine.api.search.Scope;
import org.intermine.api.tag.AspectTagUtil;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.api.tracker.TemplateTracker;
import org.intermine.metadata.Model;
import org.intermine.model.userprofile.Tag;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathException;
import org.intermine.template.TemplateComparator;
import org.intermine.template.TemplateQuery;

/**
 * A TemplateManager provides access to all global and/or user templates and methods to fetch them
 * by type, etc.
 * @author Richard Smith
 *
 */
public class TemplateManager
{

    private static final TemplateComparator TEMPLATE_COMPARATOR = new TemplateComparator();
    private final Profile superProfile;
    @SuppressWarnings("unused")
    private final Model model;
    private final TagManager tagManager;
    private TemplateTracker templateTracker;

    /**
     * The TemplateManager references the super user profile to fetch global templates.
     * @param superProfile the super user profile
     * @param model the object model
     */
    public TemplateManager(Profile superProfile, Model model) {
        this.model = model;
        this.superProfile = superProfile;
        this.tagManager = new TagManagerFactory(superProfile.getProfileManager()).getTagManager();
    }

    /**
     * The TemplateManager references the super user profile to fetch global templates.
     * @param superProfile the super user profile
     * @param model the object model
     * @param templateTracker the template tracker
     */
    public TemplateManager(Profile superProfile, Model model, TemplateTracker templateTracker) {
        this.model = model;
        this.superProfile = superProfile;
        this.tagManager = new TagManagerFactory(superProfile.getProfileManager()).getTagManager();
        this.templateTracker = templateTracker;
    }

    /**
     * Fetch a global template by name.
     * @param templateName name of the template to fetch
     * @return the template or null
     */
    public ApiTemplate getGlobalTemplate(String templateName) {
        return getGlobalTemplates().get(templateName);
    }

    /**
     * Fetch a user template by name.
     * @param profile the user to fetch the template from
     * @param templateName name of the template to fetch
     * @return the template or null
     */
    public ApiTemplate getUserTemplate(Profile profile, String templateName) {
        return profile.getSavedTemplates().get(templateName);
    }

    /**
     * Fetch a user or global template by name.  If there are name collisions the user template
     * take precedence.
     * @param profile the user to fetch the template from
     * @param templateName name of the template to fetch
     * @return the template or null
     */
    public ApiTemplate getUserOrGlobalTemplate(Profile profile, String templateName) {
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
    public ApiTemplate getTemplate(Profile profile, String templateName, String scope) {
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

    /**
     * Fetch all user and global templates in a single map for the given user.  If there are naming
     * collisions between user and global templates user templates take precedence.
     * @param profile the user to fetch templates
     * @return a map of template name to template query, the map will be empty if no templates found
     */
    public Map<String, ApiTemplate> getUserAndGlobalTemplates(Profile profile) {
        // where name collisions occur user templates take precedence
        Map<String, ApiTemplate> allTemplates = new HashMap<String, ApiTemplate>();

        allTemplates.putAll(getGlobalTemplates());
        allTemplates.putAll(profile.getSavedTemplates());

        return allTemplates;
    }

    /**
     * Fetch all user and global templates that are valid. These templates can be expected
     * to work with the mine in their current state.
     * @param profile The user to fetch templates for.
     * @return A map of templates and their names.
     */
    public Map<String, ApiTemplate> getWorkingTemplates(Profile profile) {
        Map<String, ApiTemplate> allTemplates = getUserAndGlobalTemplates(profile);
        Map<String, ApiTemplate> workingTemplates = new TreeMap<String, ApiTemplate>();
        filterOutBrokenTemplates(allTemplates, workingTemplates);
        return workingTemplates;
    }

    /**
     * Fetch all global templates that are valid. These templates can be expected
     * to work with the mine in their current state.
     * @return A map of templates and their names.
     */
    public Map<String, ApiTemplate> getWorkingTemplates() {
        Map<String, ApiTemplate> publicTemplates = getGlobalTemplates();
        Map<String, ApiTemplate> workingTemplates = new TreeMap<String, ApiTemplate>();
        filterOutBrokenTemplates(publicTemplates, workingTemplates);
        return workingTemplates;
    }

    private void filterOutBrokenTemplates(Map<String, ApiTemplate> in,
            Map<String, ApiTemplate> out) {
        for (Entry<String, ApiTemplate> pair: in.entrySet()) {
            if (pair.getValue().isValid()) {
                out.put(pair.getKey(), pair.getValue());
            }
        }
    }

    /**
     * Get a list of template queries that should appear on report pages for the given type
     * under the specified aspect.
     * @param aspect the aspect to fetch templates for
     * @param allClasses the type of report page and superclasses
     * @return a list of template queries
     */
    public List<ApiTemplate> getReportPageTemplatesForAspect(String aspect,
            Set<String> allClasses) {
        List<ApiTemplate> templates = new ArrayList<ApiTemplate>();
        List<ApiTemplate> aspectTemplates = getAspectTemplates(aspect);
        for (ApiTemplate template : aspectTemplates) {
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
     * @param classes the class of the report page and its superclasses
     * @return true if template should be displayed on the report page
     */
    private boolean isValidReportTemplate(ApiTemplate template, Collection<String> classes) {

        // must be tagged to be on report page
        if (!hasTag(superProfile, TagNames.IM_REPORT, template)) {
            return false;
        }

        // we can only provide a value for one editable constraint
        if (template.getEditableConstraints().size() != 1) {
            return false;
        }
        PathConstraint constraint = template.getEditableConstraints().get(0);

        Path path = null;
        try {
            path = template.makePath(constraint.getPath());
        } catch (PathException e) {
            // not a valid path
            return false;
        }

        // find if type that is being constrained inherits from type of report page
        String constrainedType;
        if (path.endIsAttribute()) {
            constrainedType = path.getPrefix().getEndType().getSimpleName();
        } else {
            // this is a LOOKUP constraint
            constrainedType = path.getEndType().getSimpleName();
        }
        if (!classes.contains(constrainedType)) {
            return false;
        }
        return true;
    }

    /**
     * Get public templates for a particular aspect.
     * @param aspect name of aspect tag
     * @return a list of template queries
     */
    public List<ApiTemplate> getAspectTemplates(String aspect) {
        return getAspectTemplates(aspect, null);
    }

    /**
     * Get public templates for a particular aspect.
     *
     * @param size maximum number of templates to return.
     * @param aspectTag name of aspect tag
     * @return a list of template queries
     */
    public List<ApiTemplate> getAspectTemplates(String aspectTag, Integer size) {

        int i = 0;
        String aspect = aspectTag;
        if (AspectTagUtil.isAspectTag(aspectTag)) {
            aspect = AspectTagUtil.getAspect(aspectTag);
        }

        List<ApiTemplate> aspectTemplates = new ArrayList<ApiTemplate>();
        Map<String, ApiTemplate> globalTemplates = getValidGlobalTemplates();

        List<Tag> tags = tagManager.getTags(null, null, TagTypes.TEMPLATE,
                superProfile.getUsername());
        for (Tag tag : tags) {
            if (AspectTagUtil.isAspectTag(tag.getTagName())) {
                if (StringUtils.equals(aspect, AspectTagUtil.getAspect(tag.getTagName()))) {
                    if (globalTemplates.containsKey(tag.getObjectIdentifier())) {
                        aspectTemplates.add(globalTemplates.get(tag.getObjectIdentifier()));
                        i++;
                        if (size != null && i >= size.intValue()) {
                            break;
                        }
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
    public Map<String, ApiTemplate> getValidGlobalTemplates() {
        Map<String, ApiTemplate> validTemplates = new HashMap<String, ApiTemplate>();
        for (Map.Entry<String, ApiTemplate> entry : getGlobalTemplates().entrySet()) {
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
    public Map<String, ApiTemplate> getGlobalTemplates() {
        return getTemplatesWithTag(superProfile, TagNames.IM_PUBLIC);
    }

    /**
     * Return a map from template name to template query containing superuser templates that are
     * tagged as public.
     * @param filterOutAdmin if true, filter out templates tagged with IM_ADMIN
     * @return a map from template name to template query
     */
    public Map<String, ApiTemplate> getGlobalTemplates(boolean filterOutAdmin) {
        return getTemplatesWithTag(superProfile, TagNames.IM_PUBLIC, filterOutAdmin);
    }

    /**
     * Return template queries used for converting between types in bag upload and lookup queries,
     * these are Superuser templates that have been tagged with TagNames.IM_CONVERTER.
     * @return a list of conversion template queries
     */
    public List<ApiTemplate> getConversionTemplates() {
        List<ApiTemplate> conversionTemplates = new ArrayList<ApiTemplate>();
        Map<String, ApiTemplate> templatesMap =
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
    private Map<String, ApiTemplate> getTemplatesWithTag(Profile profile, String tag) {
        return getTemplatesWithTag(profile, tag, false);
    }

    /**
     * Return a map from template name to template query of template in the given profile that are
     * tagged with a particular tag.
     * @param profile a user profile to get templates from
     * @param tag the tag to search for
     * @param filterOutAdmin if true, filter out templates tagged with IM_ADMIN
     * @return a map from template name to template query
     */
    private Map<String, ApiTemplate> getTemplatesWithTag(Profile profile, String tag,
            boolean filterOutAdmin) {
        Map<String, ApiTemplate> templatesWithTag = new HashMap<String, ApiTemplate>();

        for (Map.Entry<String, ApiTemplate> entry : profile.getSavedTemplates().entrySet()) {
            ApiTemplate template = entry.getValue();
            List<Tag> tags = tagManager.getTags(tag, template.getName(), TagTypes.TEMPLATE,
                    profile.getUsername());
            if (tags.size() > 0) {
                // if filtering by admin tag, don't include this template if it's tagged with ADMIN
                if (filterOutAdmin && hasTag(profile, TagNames.IM_ADMIN, template)) {
                    continue;
                }
                templatesWithTag.put(entry.getKey(), entry.getValue());
            }
        }
        return templatesWithTag;
    }

    private boolean hasTag(Profile profile, String tagName, ApiTemplate template) {
        List<Tag> tags = tagManager.getTags(tagName, template.getName(), TagTypes.TEMPLATE,
                profile.getUsername());
        if (tags.size() > 0) {
            return true;
        }
        return false;
    }

    /**
     * Return the list of public templates ordered by rank descendant.
     * @param size maximum number of templates to return
     * @return List of template names
     */
    public List<String> getMostPopularTemplateOrder(Integer size) {
        return getMostPopularTemplateOrder(null, null, size);
    }

    /**
     * Return the template list ordered by rank descendant for the user/sessionid specified
     * in the input
     * @param userName the user name
     * @param sessionId the session id
     * @param size maximum number of templates to return
     * @return List of template names
     */
    public List<String> getMostPopularTemplateOrder(String userName, String sessionId,
            Integer size) {
        List<String> mostPopularTemplateOrder = new ArrayList<String>();
        Map<String, Double> templateLnRank = templateTracker.getLogarithmMap(userName, sessionId,
                                                                             this);
        //create an entry list ordered
        List<Entry<String, Double>> listOrdered =
            new LinkedList<Entry<String, Double>>(templateLnRank.entrySet());
        Collections.sort(listOrdered, new Comparator<Entry<String, Double>>() {
            @Override
            public int compare (Entry<String, Double> e1, Entry<String, Double> e2) {
                return -e1.getValue().compareTo(e2.getValue());
            }
        });
        for (Entry<String, Double> entry : listOrdered) {
            mostPopularTemplateOrder.add(entry.getKey());
        }
        if (size != null) {
            if (mostPopularTemplateOrder.size() > size.intValue()) {
                mostPopularTemplateOrder = mostPopularTemplateOrder.subList(0, size.intValue());
            }
        }
        return mostPopularTemplateOrder;
    }

    /**
     * Return the template list for a particular aspect given in input, ordered by rank descendant
     * @param aspectTag name of aspect tag
     * @param size maximum number of templates to return
     * @return List of template names
     */
    public List<ApiTemplate> getPopularTemplatesByAspect(String aspectTag, Integer size) {
        return getPopularTemplatesByAspect(aspectTag, size, null, null);
    }

    /**
     * Return the template list for a particular aspect, ordered by rank descendant for
     * the user/sessionid specified in the input
     * @param aspectTag name of aspect tag
     * @param size maximum number of templates to return
     * @param userName the user name
     * @param sessionId the session id
     * @return List of template names
     */
    public List<ApiTemplate> getPopularTemplatesByAspect(String aspectTag, Integer size,
                                                           String userName, String sessionId) {
        List<ApiTemplate> templates = getAspectTemplates(aspectTag, null);
        List<String> mostPopularTemplateNames;
        if (userName != null && sessionId != null) {
            mostPopularTemplateNames = getMostPopularTemplateOrder(userName, sessionId, null);
        } else {
            mostPopularTemplateNames = getMostPopularTemplateOrder(null);
        }

        if (mostPopularTemplateNames != null) {
            Collections.sort(templates, new MostPopularTemplateComparator(
                                            mostPopularTemplateNames));
        }
        if (templates != null && size != null) {
            if (templates.size() > size.intValue()) {
                templates = templates.subList(0, size.intValue());
            }
        }
        return templates;
    }

    private class MostPopularTemplateComparator implements Comparator<TemplateQuery>
    {
        private final List<String> mostPopularTemplateNames;

        public MostPopularTemplateComparator(List<String> mostPopularTemplateNames) {
            this.mostPopularTemplateNames = mostPopularTemplateNames;
        }
        @Override
        public int compare(TemplateQuery template1, TemplateQuery template2) {
            String templateName1 = template1.getName();
            String templateName2 = template2.getName();
            if (!mostPopularTemplateNames.contains(templateName1)
                && !mostPopularTemplateNames.contains(templateName2)) {
                if (template1.getTitle().equals(template2.getTitle())) {
                    return template1.getName().compareTo(template2.getName());
                } else {
                    return template1.getTitle().compareTo(template2.getTitle());
                }
            }
            if (!mostPopularTemplateNames.contains(templateName1)) {
                return +1;
            }
            if (!mostPopularTemplateNames.contains(templateName2)) {
                return -1;
            }
            return (mostPopularTemplateNames.indexOf(templateName1)
                   < mostPopularTemplateNames.indexOf(templateName2)) ? -1 : 1;
        }
    }


}
