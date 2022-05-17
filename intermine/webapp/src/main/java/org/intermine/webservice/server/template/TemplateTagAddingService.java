package org.intermine.webservice.server.template;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.userprofile.Tag;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.collections.CollectionUtils.collect;
import static org.apache.commons.collections.TransformerUtils.invokerTransformer;
import static org.apache.commons.lang.StringUtils.split;

/**
 * A service for adding tags to a template.
 *
 * @author Julie Sullivan
 *
 */
public class TemplateTagAddingService extends TemplateTagService
{

    private static final String TAGS = "tags";

    /**
     * Constructor.
     * @param im The InterMine API settings.
     */
    public TemplateTagAddingService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        String templateName = getRequiredParameter("name");
        Set<String> tags = getTags();

        Profile profile = getPermission().getProfile();

        Map<String, ApiTemplate> templates = im.getTemplateManager()
                .getUserAndGlobalTemplates(profile);
        ApiTemplate template = templates.get(templateName);
        if (template == null) {
            throw new ResourceNotFoundException(
                    "You do not have access to a template called '" + templateName + "'");
        }

        modifyTemplate(tags, template);

        List<Tag> allTags = im.getTagManager().getObjectTags(template, profile);
        List<String> tagNames = (List<String>) collect(allTags, invokerTransformer(
                "getTagName"));
        output.addResultItem(tagNames);
    }

    /**
     * Modify the list by adding the tags.
     * @param tags The tags to add.
     * @param template The template to tag
     */
    protected void modifyTemplate(Set<String> tags, ApiTemplate template) {
        Profile profile = getPermission().getProfile();
        try {
            for (String tagName : tags) {
                im.getTagManager().addTag(tagName, template, profile);
            }
        } catch (TagManager.TagNameException e) {
            throw new BadRequestException(e.getMessage());
        } catch (TagManager.TagNamePermissionException e) {
            throw new ServiceForbiddenException(e.getMessage());
        }
    }

    private Set<String> getTags() {
        String[] tagValues = request.getParameterValues(TAGS);
        if (tagValues == null) {
            throw new BadRequestException("No tags supplied");
        }
        Set<String> tags = new HashSet<String>();
        for (String tagValue: tagValues) {
            String[] subvalues = split(tagValue, ';');
            Collections.addAll(tags, subvalues);
        }
        return tags;
    }

}
