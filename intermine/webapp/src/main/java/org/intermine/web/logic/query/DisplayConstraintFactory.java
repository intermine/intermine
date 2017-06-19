package org.intermine.web.logic.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.template.TemplateSummariser;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.SwitchOffAbility;
import org.intermine.api.template.TemplateManager;
import org.intermine.template.TemplateQuery;
import org.intermine.web.autocompletion.AutoCompleter;

/**
 * A class that will help you manufacture instances of DisplayConstraint.
 * @author Alex Kalderimis
 *
 */
public class DisplayConstraintFactory
{
    private InterMineAPI im;
    private AutoCompleter ac;

    /**
     * Get an object that creates display constraints.
     * @param im The InterMine state object.
     * @param ac Something that can provide autocomplete hints.
     */
    public DisplayConstraintFactory(InterMineAPI im, AutoCompleter ac) {
        this.im = im;
        this.ac = ac;
    }

    /**
     * Get a display constraint.
     * @param path The path this constraint constrains.
     * @param profile The profile of the current user (needed so we know what bags are available)
     * @param query The query this constraint constrains.
     * @return A display constraint.
     */
    public DisplayConstraint get(Path path, Profile profile, PathQuery query) {
        return new DisplayConstraint(path, profile, query, ac, im.getObjectStoreSummary(),
                im.getBagQueryConfig(), im.getClassKeys(), im.getBagManager());
    }

    /**
     * Get a display constraint.
     * @param con The existing constraint we want to wrap.
     * @param profile The profile of the current user (needed so we know what bags are available)
     * @param query The query this constraint constrains.
     * @return A display constraint.
     * @throws PathException if the existing constraint is invalid.
     */
    public DisplayConstraint get(PathConstraint con, Profile profile, PathQuery query)
        throws PathException {

        Path path = query.makePath(con.getPath());
        DisplayConstraint dc = get(path, profile, query);
        String label = null;
        List<Object> templateSummary = null;

        boolean editableInTemplate = false;
        SwitchOffAbility switchOffAbility = null;
        if (query instanceof TemplateQuery) {
            TemplateQuery template = (TemplateQuery) query;
            editableInTemplate = template.isEditable(con);
            label = template.getConstraintDescription(con);
            switchOffAbility = template.getSwitchOffAbility(con);

            // we need to find the original template to retrieve the summary
            TemplateManager templateManager = im.getTemplateManager();

            ApiTemplate originalTemplate =
                templateManager.getUserOrGlobalTemplate(profile, template.getName());
            TemplateSummariser templateSummariser = im.getTemplateSummariser();
            if (templateSummariser.isSummarised(originalTemplate)) {
                templateSummary =
                    templateSummariser.getPossibleValues(originalTemplate, con.getPath());
            }
        }
        dc.setSwitchOffAbility(switchOffAbility);
        dc.setLabel(label);
        dc.setOriginalConstraint(con);
        dc.setCode(query.getConstraints().get(con));
        dc.setEditableInTemplate(editableInTemplate);
        dc.setTemplatesummary(templateSummary);
        return dc;
    }
}
