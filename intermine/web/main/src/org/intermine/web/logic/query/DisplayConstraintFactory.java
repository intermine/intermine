package org.intermine.web.logic.query;

/*
 * Copyright (C) 2002-2012 FlyMine
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

public class DisplayConstraintFactory
{
    private InterMineAPI im;
    private AutoCompleter ac;

    public DisplayConstraintFactory(InterMineAPI im, AutoCompleter ac) {
        this.im = im;
        this.ac = ac;
    }

    public DisplayConstraint get(Path path, Profile profile, PathQuery query) {
        return new DisplayConstraint(path, profile, query, ac, im.getObjectStoreSummary(),
                im.getBagQueryConfig(), im.getClassKeys(), im.getBagManager());
    }

    public DisplayConstraint get(PathConstraint con, Profile profile, PathQuery query)
        throws PathException {

        Path path = query.makePath(con.getPath());
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
        return new DisplayConstraint(path, con, label, query.getConstraints().get(con),
                editableInTemplate, switchOffAbility, profile, query, ac,
                im.getObjectStoreSummary(), im.getBagQueryConfig(), im.getClassKeys(),
                im.getBagManager(), templateSummary);
    }
}
