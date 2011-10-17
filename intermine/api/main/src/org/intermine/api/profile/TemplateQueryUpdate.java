package org.intermine.api.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.intermine.api.template.TemplateQuery;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathException;

public class TemplateQueryUpdate extends PathQueryUpdate {
    private TemplateQuery templateQuery;
    private TemplateQuery newTemplateQuery;

    public TemplateQueryUpdate(TemplateQuery templateQuery, Model newModel, Model oldModel) {
        super(templateQuery.getPathQuery(), newModel, oldModel);
        this.templateQuery = templateQuery;
    }

    public TemplateQuery getNewTemplateQuery() {
        return newTemplateQuery;
    }

    public synchronized List<String> update(Map<String, String> renamedClasses,
            Map<String, String> renamedFields) throws PathException {
        List<String> problems = new ArrayList<String>();
        problems = super.update(renamedClasses, renamedFields);
        if (!problems.isEmpty()) {
            return problems;
        }
        newTemplateQuery = new TemplateQuery(templateQuery.getName(),
            templateQuery.getTitle(), templateQuery.getComment(), newPathQuery);
        updateEditableConstraints(renamedClasses, renamedFields);
        updateConstraintDescriptions(renamedClasses, renamedFields);
        updateConstraintSwitchOffAbility(renamedClasses, renamedFields);
        return problems;
    }

    private void updateEditableConstraints(Map<String, String> renamedClasses,
            Map<String, String> renamedFields) throws PathException {
        String path, newPath;
        Path p;
        for (PathConstraint pathConstraint : templateQuery.getEditableConstraints()) {
            path = pathConstraint.getPath();
            newPath = getPathUpdated(path, renamedClasses, renamedFields);
            if (!newPath.equals(path)) {
                newTemplateQuery.setEditable(createPathConstraint(pathConstraint, newPath), true);
            } else {
                newTemplateQuery.setEditable(pathConstraint, true);
            }
        }
    }

    private void updateConstraintDescriptions(Map<String, String> renamedClasses,
            Map<String, String> renamedFields) throws PathException {
        String path, newPath;
        Path p;
        for (PathConstraint pathConstraint : templateQuery.getConstraintDescriptions().keySet()) {
            path = pathConstraint.getPath();
            newPath = getPathUpdated(path, renamedClasses, renamedFields);
            if (!newPath.equals(path)) {
                newTemplateQuery.setConstraintDescription(createPathConstraint(pathConstraint, newPath),
                    templateQuery.getConstraintDescription(pathConstraint));
            } else {
                newTemplateQuery.setConstraintDescription(pathConstraint,
                    templateQuery.getConstraintDescription(pathConstraint));
            }
        }
    }

    private void updateConstraintSwitchOffAbility(Map<String, String> renamedClasses,
            Map<String, String> renamedFields) throws PathException {
        String path, newPath;
        Path p;
        for (PathConstraint pathConstraint : templateQuery.getConstraintSwitchOffAbility().keySet()) {
            path = pathConstraint.getPath();
            newPath = getPathUpdated(path, renamedClasses, renamedFields);
            if (!newPath.equals(path)) {
                newTemplateQuery.setSwitchOffAbility((createPathConstraint(pathConstraint, newPath)),
                    templateQuery.getSwitchOffAbility(pathConstraint));
            } else {
                newTemplateQuery.setSwitchOffAbility(pathConstraint,
                    templateQuery.getSwitchOffAbility(pathConstraint));
            }
        }
    }
}
