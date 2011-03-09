package org.intermine.api.template;

/*
 * Copyright (C) 2002-2011 FlyMine
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
import java.util.Set;
import java.util.Map.Entry;

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

    /**
     * Fetch all user and global templates in a single map for the given user.  If there are naming
     * collisions between user and global templates user templates take precedence.
     * @param profile the user to fetch templates
     * @return a map of template name to template query, the map will be empty if no templates found
     */
    protected Map<String, TemplateQuery> getUserAndGlobalTemplates(Profile profile) {
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
     * @param allClasses the type of report page and superclasses
     * @return a list of template queries
     */
    public List<TemplateQuery> getReportPageTemplatesForAspect(String aspect,
            Set<String> allClasses) {
        List<TemplateQuery> templates = new ArrayList<TemplateQuery>();
        List<TemplateQuery> aspectTemplates = getAspectTemplates(aspect);
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
     * @param classes the class of the report page and its superclasses
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
        return getAspectTemplates(aspect, null);
    }


    /**
     * Get public templates for a particular aspect.
     *
     * @param size maximum number of templates to return.
     * @param aspectTag name of aspect tag
     * @return a list of template queries
     */
    public List<TemplateQuery> getAspectTemplates(String aspectTag, Integer size) {

        int i = 0;
        String aspect = aspectTag;
        if (AspectTagUtil.isAspectTag(aspectTag)) {
            aspect = AspectTagUtil.getAspect(aspectTag);
        }

        List<TemplateQuery> aspectTemplates = new ArrayList<TemplateQuery>();
        Map<String, TemplateQuery> globalTemplates = getValidGlobalTemplates();

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
            public int compare (Entry<String, Double> e1, Entry<String, Double> e2) {
                return -e1.getValue().compareTo(e2.getValue());
            }
        });
        for (Entry<String, Double> entry : listOrdered) {
            mostPopularTemplateOrder.add(entry.getKey());
        }
        if (mostPopularTemplateOrder != null && size != null) {
            if (mostPopularTemplateOrder != null && mostPopularTemplateOrder.size() > size) {
                mostPopularTemplateOrder = mostPopularTemplateOrder.subList(0, size);
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
    public List<TemplateQuery> getPopularTemplatesByAspect(String aspectTag, Integer size) {
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
    public List<TemplateQuery> getPopularTemplatesByAspect(String aspectTag, Integer size,
                                                           String userName, String sessionId) {
        List<TemplateQuery> templates = getAspectTemplates(aspectTag, null);
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
            if (templates.size() > size) {
                templates = templates.subList(0, size);
            }
        }
        return templates;
    }

    private class MostPopularTemplateComparator implements Comparator<TemplateQuery>
    {
        private List<String> mostPopularTemplateNames;

        public MostPopularTemplateComparator(List<String> mostPopularTemplateNames) {
            this.mostPopularTemplateNames = mostPopularTemplateNames;
        }
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
