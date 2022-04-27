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
import org.intermine.api.tag.TagTypes;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.userprofile.Tag;
import org.intermine.api.util.AnonProfile;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.output.JSONFormatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.collections.CollectionUtils.collect;
import static org.apache.commons.collections.TransformerUtils.invokerTransformer;

/**
 * A service for getting the tags of a template.
 *
 * @author Julie Sullivan
 *
 */
public class TemplateTagService extends JSONService
{

    /**
     * Constructor.
     * @param im The InterMine application object.
     */
    public TemplateTagService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = super.getHeaderAttributes();
        attributes.put(JSONFormatter.KEY_INTRO, "\"tags\":[");
        attributes.put(JSONFormatter.KEY_OUTRO, "]");
        attributes.put(JSONFormatter.KEY_QUOTE, true);
        return attributes;
    }

    @Override
    protected void execute() throws Exception {
        Profile profile = getPermission().getProfile();
        String templateName = getOptionalParameter("name", null);


        Set<String> tags = new HashSet<String>();
        // if not logged in, return empty. See #1222
        if (!AnonProfile.USERNAME.equals(profile.getUsername())) {
            if (templateName == null) {
                tags = getAllTags(profile);
            } else {
                tags = getTagsForSingleTemplate(templateName, profile);
            }
        }

        output.addResultItem(new ArrayList<String>(tags));
    }

    private Set<String> getTagsForSingleTemplate(String name, Profile profile) {
        Map<String, ApiTemplate> templates = im.getTemplateManager()
                .getUserAndGlobalTemplates(profile);
        ApiTemplate template = templates.get(name);
        if (template == null) {
            throw new ResourceNotFoundException(
                    "You do not have access to a template called '" + name + "'");
        }

        List<Tag> tags = im.getTagManager().getObjectTags(template, profile);
        Collection<String> tagNames = collect(tags, invokerTransformer("getTagName"));
        return new HashSet<String>(tagNames);
    }

    private Set<String> getAllTags(Profile profile) {
        return im.getTagManager().getUserTagNames(TagTypes.TEMPLATE, profile);
    }

}
