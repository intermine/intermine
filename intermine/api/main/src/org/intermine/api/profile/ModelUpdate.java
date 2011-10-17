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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.intermine.api.template.TemplateQuery;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;

public class ModelUpdate {
    private ObjectStoreWriter uosw;
    private ProfileManager pm;
    private Model model;
    private Model oldModel;
    private Set<String> deletedClasses = new HashSet<String>();
    private Map<String, String> renamedClasses = new HashMap<String, String>();
    private Map<String, String> renamedFields = new HashMap<String, String>();
    public static final String DELETE = "delete";
    public static final String RENAME = "rename-";

    public ModelUpdate(ObjectStore os, ObjectStoreWriter uosw, String oldModelName) {
        this.uosw = uosw;
        this.pm = new ProfileManager(os, uosw);
        this.model = os.getModel();
        this.oldModel = Model.getInstanceByName(oldModelName);
        
        Properties modelUpdateProps = new Properties();
        try {
            modelUpdateProps.load(this.getClass().getClassLoader()
                    .getResourceAsStream("modelUpdate.properties"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadModelUpdate(modelUpdateProps);
    }

    private void loadModelUpdate(Properties modelUpdateProps) {
        String keyAsString;
        String className;
        String fieldName;
        String update;

        for(Object key : modelUpdateProps.keySet()) {
            keyAsString = (String) key;
            update = (String) modelUpdateProps.getProperty(keyAsString);

            if (!keyAsString.contains(".")) {
                //it's a class update
                className = keyAsString;
                verifyClassAndField(className, null, oldModel);
                if (update.equals(DELETE)) {
                    deletedClasses.add(className);
                } else if (update.contains(RENAME)) {
                    String newClassName = update.replace(RENAME, "").trim();
                    verifyClassAndField(newClassName, null, model);
                    renamedClasses.put(className, newClassName);
                } else {
                    throw new BuildException("For the class " + className
                                      + " only deleted or renamed- permitted.");
                }
            } else {
                //it's a field update
                int index = keyAsString.indexOf(".");
                className = keyAsString.substring(0, index);
                fieldName = keyAsString.substring(index + 1);
                verifyClassAndField(className, fieldName, oldModel);

                if (update.contains(RENAME)) {
                    update = update.replace(RENAME, "").trim();
                    index = update.indexOf(".");
                    if (index == -1) {
                        throw new BuildException("Field " + keyAsString + " has to contain class.newfield");
                    }
                    String newClassName = update.substring(0, index);
                    String newFieldName = update.substring(index + 1);
                    if (fieldName.equals(newFieldName)) {
                        throw new BuildException(keyAsString + " = " + RENAME + update +
                                " not permitted. Field has to be renamed. Please check" +
                                " modelUpdate.properties file");
                    }
                    verifyClassAndField(newClassName, newFieldName, model);
                    if (!className.equals(newClassName)) {
                        //there is a renamed attribute in a renamed class.
                        //add in renamedClasses this renamed class
                        if (!renamedClasses.containsKey(className)) {
                            renamedClasses.put(className, newClassName);
                        } else {
                            if (!renamedClasses.get(className).equals(newClassName)) {
                                throw new BuildException("Class " + className + " has been " +
                                    "renamed in two different classes. Please check" +
                                    " modelUpdate.properties file");
                            }
                        }
                    }
                    renamedFields.put(className + "." + fieldName, newFieldName);
                } else if (!update.contains(DELETE)) {
                    throw new BuildException("For the field " + keyAsString
                            + " only " + DELETE + " or " + RENAME + " permitted.");
                }
            }
        }
    }

    private void verifyClassAndField(String className, String fieldName, Model model)
        throws BuildException {
        String checkFileMsg = "Please check modelUpdate.properties file";
        if ("".equals(className)) {
            throw new BuildException("Class " + className + " can not be blank. " + checkFileMsg);
        }
        ClassDescriptor cd = model.getClassDescriptorByName(className);
        if (cd == null) {
            if (fieldName != null) {
                throw new BuildException("Class " + className + " containing " + fieldName
                              + " not defined in the model " + model.getName() + ". "
                              + checkFileMsg);
            } else {
                throw new BuildException("Class " + className + " not defined in the model "
                        + model.getName() + ". " + checkFileMsg);
            }
        }
        if (fieldName != null) {
            if (fieldName.equals("")) {
                throw new BuildException("Attribute " + fieldName + " in the class " + className
                    + " can not be blank. " + checkFileMsg);
            }
            if (cd.getAttributeDescriptorByName(fieldName) == null
                && cd.getReferenceDescriptorByName(fieldName) == null
                && cd.getCollectionDescriptorByName(fieldName) == null) {
                throw new BuildException("The " + fieldName + " in the class " + className
                               + " not defined in the model " + model.getName() + ". " + checkFileMsg);
            }
        }
    }

    public Set<String> getDeletedClasses() {
        return deletedClasses;
    }

    public Map<String, String> getRenamedClasses() {
        return renamedClasses;
    }

    public Map<String, String> getRenamedFields() {
        return renamedFields;
    }

    public void update() throws PathException {
        if(!deletedClasses.isEmpty()) {
            deleteBags();
        }

        if(!renamedClasses.isEmpty()) {
            updateTypeBag();
        }

        if(!renamedClasses.isEmpty() || !renamedFields.isEmpty()) {
            updateReferredQueryAndTemplate();
        }
    }

    public void deleteBags() {
        Query q = new Query();
        QueryClass qc = new QueryClass(SavedBag.class);
        q.addToSelect(qc);
        q.addFrom(qc);
        QueryField typeField = new QueryField(qc, "type");
        BagConstraint constraint = new BagConstraint(typeField, ConstraintOp.IN, deletedClasses);
        q.setConstraint(constraint);
        Results bagsToDelete = uosw.execute(q, 1000, false, false, true);

        for (Iterator i = bagsToDelete.iterator(); i.hasNext();) {
            ResultsRow row = (ResultsRow) i.next();
            SavedBag savedBag = (SavedBag) row.get(0);
            Profile profile = pm.getProfile(savedBag.getUserProfile().getUsername());
            try {
                profile.deleteBag(savedBag.getName());
            } catch (ObjectStoreException ose) {
                System.out.println("Problems deleting bag: " + savedBag.getName());
            }
        }
    }

    public void updateTypeBag() {
        Query q = new Query();
        QueryClass qc = new QueryClass(SavedBag.class);
        q.addToSelect(qc);
        q.addFrom(qc);
        QueryField typeField = new QueryField(qc, "type");
        BagConstraint constraint = new BagConstraint(typeField, ConstraintOp.IN,
                                                     renamedClasses.keySet());
        q.setConstraint(constraint);
        Results bagsToUpdate = uosw.execute(q, 1000, false, false, true);

        for (Iterator i = bagsToUpdate.iterator(); i.hasNext();) {
            ResultsRow row = (ResultsRow) i.next();
            SavedBag savedBag = (SavedBag) row.get(0);
            String type = savedBag.getType();
            String newType = renamedClasses.get(type);
            Profile profile = pm.getProfile(savedBag.getUserProfile().getUsername());
            try {
                if (newType != null) {
                    profile.updateBagType(savedBag.getName(), newType);
                }
            } catch (ObjectStoreException ose) {
                System.out.println("Problems updating savedBag " + savedBag.getName()
                                   + ose.getMessage());
            }
        }
    }

    public void updateReferredQueryAndTemplate() throws PathException {
        Map<String, SavedQuery> savedQueries;
        Map<String, TemplateQuery> templateQueries;
        String cls, prevField;
        List<String> problems;
        Query q = new Query();
        QueryClass qc = new QueryClass(UserProfile.class);
        q.addToSelect(qc);
        q.addFrom(qc);
        Results userprofiles = uosw.execute(q, 1000, false, false, true);
        for (Iterator i = userprofiles.iterator(); i.hasNext();) {
            ResultsRow row = (ResultsRow) i.next();
            UserProfile user = (UserProfile) row.get(0);
            Profile profile = pm.getProfile(user.getUsername());
            savedQueries = profile.getSavedQueries();
            for (SavedQuery savedQuery : savedQueries.values()) {
                PathQuery pathQuery = savedQuery.getPathQuery();
                PathQueryUpdate pathQueryUpdate = new PathQueryUpdate(pathQuery, model, oldModel);
                problems = pathQueryUpdate.update(renamedClasses, renamedFields);
                if (!problems.isEmpty()) {
                    System.out.println("Problems updating pathQuery in savedQuery "
                                     + savedQuery.getName() + ". " + problems);
                    continue;
                }
                if (pathQueryUpdate.isUpdated()) {
                    SavedQuery updatedSavedQuery = new SavedQuery(savedQuery.getName(),
                        savedQuery.getDateCreated(), pathQueryUpdate.getUpdatedPathQuery());
                    profile.saveQuery(savedQuery.getName(), updatedSavedQuery);
                }
            }
            templateQueries = profile.getSavedTemplates();
            for (TemplateQuery templateQuery : templateQueries.values()) {
                TemplateQueryUpdate templateQueryUpdate = new TemplateQueryUpdate(templateQuery, model,
                                                                          oldModel);
                problems = templateQueryUpdate.update(renamedClasses, renamedFields);
                if (!problems.isEmpty()) {
                    System.out.println("Problems updating pathQuery in templateQuery "
                                      + templateQuery.getName() + ". " + problems);
                    continue;
                }
                if (templateQueryUpdate.isUpdated()) {
                    TemplateQuery updatedTemplateQuery = templateQueryUpdate.getNewTemplateQuery();
                    profile.saveTemplate(templateQuery.getName(), updatedTemplateQuery);
                }
            }
        }
    }
}
