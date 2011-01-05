package org.intermine.web.task;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.api.bag.UnknownBagTypeException;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

/**
 * Task to load bagvalues table in the userprofile database.
 *
 * @author dbutano
 */

public class LoadBagValuesTask extends Task
{
    private static final Logger LOG = Logger.getLogger(LoadBagValuesTask.class);
    private String osAlias;
    private String userProfileAlias;

    /**
     * Set the alias of the main object store.
     * @param osAlias the object store alias
     */
    public void setOSAlias(String osAlias) {
        this.osAlias = osAlias;
    }

    /**
     * Set the alias of the userprofile object store.
     * @param userProfileAlias the object store alias of the userprofile database
     */
    public void setUserProfileAlias(String userProfileAlias) {
        this.userProfileAlias = userProfileAlias;
    }

    /**
     * Execute the task - load bagvalues table.
     * @throws BuildException if there is a problem while
     */
    public void execute() {
        ObjectStore os = null;
        ObjectStoreWriter uosw = null;
        try {
            os = ObjectStoreFactory.getObjectStore(osAlias);
            uosw = ObjectStoreWriterFactory.getObjectStoreWriter(userProfileAlias);
        } catch (Exception e) {
            throw new BuildException("Exception while creating ObjectStore", e);
        }

        Query q = new Query();
        QueryClass qc = new QueryClass(SavedBag.class);
        q.addFrom(qc);
        q.addToSelect(qc);

        Results bags = uosw.execute(q, 1000, false, false, true);
        for (Iterator i = bags.iterator(); i.hasNext();) {
            ResultsRow row = (ResultsRow) i.next();
            SavedBag savedBag = (SavedBag) row.get(0);
            if (StringUtils.isBlank(savedBag.getName())) {
                LOG.warn("Failed to load bag with blank name");
            } else {
                try {
                    InterMineBag bag = new InterMineBag(os, savedBag.getId(), uosw);
                    Properties classKeyProps = new Properties();
                    try {
                        classKeyProps.load(this.getClass().getClassLoader()
                                .getResourceAsStream("class_keys.properties"));
                    } catch (Exception e) {
                        LOG.error("Error loading class descriptions", e);
                    }
                    Map<String, List<FieldDescriptor>>  classKeys =
                        ClassKeyHelper.readKeys(os.getModel(), classKeyProps);
                    if (!classKeys.isEmpty()) {
                        bag.setPrimaryIdentifierField(classKeys);
                    }
                    bag.addBagValues();
                    bag.setCurrent(true);
                } catch (UnknownBagTypeException e) {
                    LOG.warn("Ignoring a bag '" + savedBag.getName() + " because type: "
                             + savedBag.getType() + " is not in the model.", e);
                } catch (ObjectStoreException ose) {
                    throw new BuildException("Exception while creating InterMineBag", ose);
                }
            }
        }
    }
}
