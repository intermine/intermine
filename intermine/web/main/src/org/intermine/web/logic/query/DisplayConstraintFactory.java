package org.intermine.web.logic.query;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;

import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.profile.Profile;
import org.intermine.api.template.SwitchOffAbility;
import org.intermine.api.template.TemplateQuery;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.autocompletion.AutoCompleter;


public class DisplayConstraintFactory
{
    private ObjectStoreSummary oss;
    private Map<String, List<FieldDescriptor>> classKeys;
    private BagQueryConfig bagQueryConfig;
    private AutoCompleter ac;
    private BagManager bagManager;

    public DisplayConstraintFactory(AutoCompleter ac, ObjectStoreSummary oss,
            BagQueryConfig bagQueryConfig, BagManager bagManager,
            Map<String, List<FieldDescriptor>> classKeys) {
        this.oss = oss;
        this.classKeys = classKeys;
        this.bagQueryConfig = bagQueryConfig;
        this.ac = ac;
        this.bagManager = bagManager;
    }


    public DisplayConstraint get(Path path, Profile profile, PathQuery query) {
        return new DisplayConstraint(path, profile, query, ac, oss,
                bagQueryConfig, classKeys, bagManager);

    }

    public DisplayConstraint get(PathConstraint con, Profile profile, PathQuery query)
    throws PathException {

        Path path = query.makePath(con.getPath());
        String label = null;

        boolean editableInTemplate = false;
        SwitchOffAbility switchOffAbility = null;
        if (query instanceof TemplateQuery) {
            TemplateQuery template = (TemplateQuery) query;
            editableInTemplate = template.isEditable(con);
            label = template.getConstraintDescription(con);
            switchOffAbility = template.getSwitchOffAbility(con);
        }
        return new DisplayConstraint(path, con, label, query.getConstraints().get(con),
                editableInTemplate, switchOffAbility, profile, query, ac, oss, bagQueryConfig, classKeys, bagManager);
    }
}
