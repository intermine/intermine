package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

import org.intermine.api.template.TemplateQuery;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;

public class TemplateQueryUpdate extends PathQueryUpdate {
    private TemplateQuery templateQuery;

    public TemplateQueryUpdate(TemplateQuery templateQuery, Model newModel, Model oldModel) {
        super.pathQuery = templateQuery.getPathQuery();
        this.oldModel = oldModel;
        this.templateQuery = templateQuery;
        this.newPathQuery = new TemplateQuery(templateQuery.getName(),
            templateQuery.getTitle(), templateQuery.getComment(), new PathQuery(newModel));
    }

    public TemplateQuery getNewTemplateQuery() {
        return (TemplateQuery) newPathQuery;
    }

    protected void updateConstraints (Map<String, String> renamedClasses, Map<String, String> renamedFields)
        throws PathException {
        String path, newPath;
        Path p;
        for (PathConstraint pathConstraint : pathQuery.getConstraints().keySet()) {
            path = pathConstraint.getPath();
            newPath = getPathUpdated(path, renamedClasses, renamedFields);
            PathConstraint newPathConstraint;
            if (!newPath.equals(path)) {
                newPathConstraint = createPathConstraint(pathConstraint, newPath);
            } else {
                newPathConstraint = pathConstraint;
            }
            newPathQuery.addConstraint(newPathConstraint);
            //update editable constraints
            if (templateQuery.getEditableConstraints().contains(pathConstraint)) {
                ((TemplateQuery) newPathQuery).setEditable(newPathConstraint, true);
            }
            //update constraint descriptions
            String description = templateQuery.getConstraintDescription(pathConstraint);
            if (description != null) {
                ((TemplateQuery) newPathQuery).setConstraintDescription(newPathConstraint, description);
            }
            // update switch off ability
            ((TemplateQuery) newPathQuery).setSwitchOffAbility(newPathConstraint, templateQuery.getSwitchOffAbility(pathConstraint));
        }
    }
}
