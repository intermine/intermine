package org.intermine.web;

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
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.SavedQuery;
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
    private Set<String> deletedClasses = new HashSet<String>();
    private Map<String, String> renamedClasses = new HashMap<String, String>();
    private Map<String, String> renamedFields = new HashMap<String, String>();
    public static final String DELETED = "deleted";
    public static final String RENAMED = "renamed-";

    public ModelUpdate(ObjectStore os, ObjectStoreWriter uosw) {
        this.uosw = uosw;
        pm = new ProfileManager(os, uosw);
        Model model = os.getModel();
        String keyAsString;
        String className;
        String fieldName;
        String update;

        Properties modelUpdateProps = new Properties();
        try {
            modelUpdateProps.load(this.getClass().getClassLoader()
                    .getResourceAsStream("modelUpdate.properties"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        for(Object key : modelUpdateProps.keySet()) {
            keyAsString = (String) key;
            update = (String) modelUpdateProps.getProperty(keyAsString);

            if (!keyAsString.contains(".")) {
                //it's a class update
                className = keyAsString;
                if (update.equals(DELETED)) {
                    deletedClasses.add(className);
                } else if (update.contains(RENAMED)) {
                    String newClassName = update.replace(RENAMED, "").trim();
                    if (!newClassName.equals("")
                        && model.getClassDescriptorByName(newClassName) != null) {
                        renamedClasses.put(className, newClassName);
                    }
                }
            } else {
                //it's a field update
                int index = keyAsString.indexOf(".");
                fieldName = keyAsString.substring(index + 1);
                if (update.contains(RENAMED)) {
                    update = update.replace(RENAMED, "").trim();
                    index = update.indexOf(".");
                    className = update.substring(0, index);
                    String newFieldName = update.substring(index + 1);
                    ClassDescriptor cd = model.getClassDescriptorByName(className);
                    if (!newFieldName.equals("")
                        && cd != null
                        && cd.getAttributeDescriptorByName(newFieldName) != null) {
                        renamedFields.put(className + "." + fieldName, newFieldName);
                    }
                }
            }
        }
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
            System.out.println(savedBag.getName() + " will be deleted");
/*            try {
                  profile.deleteBag(savedBag.getName());
            } catch (ObjectStoreException ose) {
                log("Problems deleting bag: " + savedBag.getName());
            }*/
        }
    }

    public void updateTypeBag() {
        Query q = new Query();
        QueryClass qc = new QueryClass(SavedBag.class);
        q.addToSelect(qc);
        q.addFrom(qc);
        QueryField typeField = new QueryField(qc, "type");
        BagConstraint constraint = new BagConstraint(typeField, ConstraintOp.IN, renamedClasses.keySet()); 
        q.setConstraint(constraint);
        Results bagsToUpdate = uosw.execute(q, 1000, false, false, true);

        for (Iterator i = bagsToUpdate.iterator(); i.hasNext();) {
            ResultsRow row = (ResultsRow) i.next();
            SavedBag savedBag = (SavedBag) row.get(0);
            String type = savedBag.getType();
            String newType = renamedClasses.get(type);
            Profile profile = pm.getProfile(savedBag.getUserProfile().getUsername());
            System.out.println(savedBag.getName() + " will be updated");
            try {
                if (newType != null) {
                    profile.updateBagType(savedBag.getName(), newType);
                }
            } catch (ObjectStoreException ose) {
                ose.printStackTrace();
            }
        }
    }

    public void updateReferredQueryAndTemplate() throws PathException {
        Map<String, SavedQuery> savedQueries;
        Map<String, TemplateQuery> templateQueries;
        String cls, prevField;
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
                for (String prevClass : renamedClasses.keySet()) {
                    pathQuery.updatePathQueryWithRenamedClass(prevClass,
                             renamedClasses.get(prevClass));
                }
                for (String key : renamedFields.keySet()) {
                    int index = key.indexOf(".");
                    cls = key.substring(0, index);
                    prevField = key.substring(index + 1);
                    pathQuery.updatePathQueryWithRenamedField(cls, prevField, renamedFields.get(key));
                }
                profile.saveQuery(savedQuery.getName(), savedQuery);
            }
            templateQueries = profile.getSavedTemplates();
            for (TemplateQuery templateQuery : templateQueries.values()) {
                for (String prevClass : renamedClasses.keySet()) {
                    templateQuery.updatePathQueryWithRenamedClass(prevClass,
                                 renamedClasses.get(prevClass));
                }
                for (String key : renamedFields.keySet()) {
                    int index = key.indexOf(".");
                    cls = key.substring(0, index);
                    prevField = key.substring(index + 1);
                    templateQuery.updatePathQueryWithRenamedField(cls, prevField, renamedFields.get(key));
                }
                profile.saveTemplate(templateQuery.getName(), templateQuery);
            }
        }
    }
}
