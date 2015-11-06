package org.intermine.webservice.server.template;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.template.TemplateManager;
import org.intermine.model.userprofile.Tag;
import org.json.JSONObject;

/** @author Julie **/
public class JSONTemplateFormatter
{

    private final InterMineAPI im;
    private final Profile profile;

    /**
     * Construct a template formatter.
     * @param im The InterMine state object.
     * @param profile The current user.
     */
    public JSONTemplateFormatter(InterMineAPI im, Profile profile) {
        super();
        this.im = im;
        this.profile = profile;
    }

    private int rowsLeft = 0;


    /**
     * Transform a template into a mapping of its properties, for easy serialisation
     * @param template The list to read
     * @return Its properties
     **/
    Map<String, Object> templateToMap(ApiTemplate template) {
        Map<String, Object> templateMap = new HashMap<String, Object>();
        templateMap.put("model", template.getModel().getName());
        templateMap.put("title", template.getTitle());
        templateMap.put("description", template.getDescription());
        templateMap.put("select", template.getView());
        templateMap.put("name", template.getName());
        templateMap.put("comment", template.getComment());
        templateMap.put("orderBy", template.toJsonSortOrder());
        templateMap.put("where", template.toJsonConstraints(true));

        TemplateManager manager = im.getTemplateManager();
        List<Tag> tags = manager.getTags(template, profile);
        List<String> tagNames = new ArrayList<String>();
        for (Tag t: tags) {
            tagNames.add(t.getTagName());
        }
        templateMap.put("tags", tagNames);

        boolean belongsToMe = templateMap == profile.getSavedTemplates().get(template.getName());
        templateMap.put("authorized", belongsToMe);
        return templateMap;
    }

    /**
     * @param template template to format
     * @return template in JSON format with metadata
     */
    public List<String> format(ApiTemplate template) {
        rowsLeft -= 1;
        JSONObject listObj = new JSONObject(templateToMap(template));
        String ret = listObj.toString();
        if (rowsLeft > 0) {
            return Arrays.asList("\"" + template.getName() + "\"", ret, "");
        } else {
            return Arrays.asList("\"" + template.getName() + "\"", ret);
        }
    }


}
