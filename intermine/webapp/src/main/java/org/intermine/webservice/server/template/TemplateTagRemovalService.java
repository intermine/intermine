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

import java.util.Set;

/**
 * A service for removing tags from a template.
 * @author Julie Sullivan
 *
 */
public class TemplateTagRemovalService extends TemplateTagAddingService
{

    /**
     * Constructor.
     * @param im The InterMine application object.
     */
    public TemplateTagRemovalService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void modifyTemplate(Set<String> tags, ApiTemplate template) {
        Profile profile = getPermission().getProfile();
        TagManager tm = im.getTagManager();

        for (Tag tag: tm.getObjectTags(template, profile)) {
            if (tags.contains(tag.getTagName())) {
                tm.deleteTag(tag.getTagName(), template, profile);
            }
        }
    }

}
