package org.intermine.api.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.bag.BagQueryHelper;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.api.template.TemplateManager;
import org.intermine.util.PropertiesUtil;

/**
 * Provides methods to obtain PathQueryExecutors and WebResultsExecutors
 *
 * @author Matthew Wakeling
 */
public final class PathQueryAPI
{
    private PathQueryAPI() {
    }

    /**
     * Returns a PathQueryExecutor, using properties.
     *
     * @return a PathQueryExecutor
     */
    public static PathQueryExecutor getPathQueryExecutor() {
        return new PathQueryExecutor(getObjectStore(), getProfile(),
                getBagQueryRunner(), getBagManager());
    }

    /**
     * Returns the properties accessible for this system.
     *
     * @return a Properties object
     */
    public static Properties getProperties() {
        return PropertiesUtil.getProperties();
    }

    private static ObjectStore objectStore = null;
    /**
     * Returns the production ObjectStore configured in webapp.os.alias.
     *
     * @return an ObjectStore
     */
    public static ObjectStore getObjectStore() {
        if (objectStore == null) {
            Properties props = getProperties();
            String osAlias = (String) props.get("webapp.os.alias");
            try {
                objectStore = ObjectStoreFactory.getObjectStore(osAlias);
            } catch (Exception e) {
                throw new IllegalArgumentException("Error getting objectstore", e);
            }
        }
        return objectStore;
    }

    private static Map<String, List<FieldDescriptor>> classKeys = null;
    /**
     * Returns classkeys configured in class_keys.properties.
     *
     * @return a Map
     */
    public static Map<String, List<FieldDescriptor>> getClassKeys() {
        if (classKeys == null) {
            try {
                Properties props = new Properties();
                props.load(PathQueryAPI.class.getClassLoader()
                        .getResourceAsStream("class_keys.properties"));
                classKeys = ClassKeyHelper.readKeys(getObjectStore().getModel(), props);
            } catch (IOException e) {
                throw new IllegalArgumentException("Error getting class keys", e);
            }
        }
        return classKeys;
    }

    private static BagQueryConfig bagQueryConfig = null;
    /**
     * Returns a BagQueryConfig configured in WEB-INF/bag-queries.xml.
     *
     * @return a BagQueryConfig
     */
    public static BagQueryConfig getBagQueryConfig() {
        if (bagQueryConfig == null) {
            try {
                InputStream is = PathQueryAPI.class.getClassLoader()
                    .getResourceAsStream("WEB-INF/bag-queries.xml");
                bagQueryConfig = BagQueryHelper.readBagQueryConfig(getObjectStore().getModel(), is);
            } catch (Exception e) {
                throw new IllegalArgumentException("Error getting bag query config", e);
            }
        }
        return bagQueryConfig;
    }

    private static Profile profile = null;
    /**
     * Returns the superuser Profile.
     *
     * @return a Profile
     */
    public static Profile getProfile() {
        if (profile == null) {
            String superuser = getProperties().getProperty("superuser.account");
            if (superuser == null) {
                throw new RuntimeException("superuser.account has not been set in properties");
            }
            profile = getProfileManager().getProfile(superuser);
            if (profile == null) {
                throw new IllegalArgumentException("Cannot find superuser account "
                        + superuser);
            }
        }
        return profile;
    }

    private static ProfileManager profileManager = null;
    /**
     * Returns the ProfileManager configured in webapp.userprofile.os.alias.
     *
     * @return a ProfileManager
     */
    public static ProfileManager getProfileManager() {
        if (profileManager == null) {
            try {
                String userProfileAlias
                    = getProperties().getProperty("webapp.userprofile.os.alias");
                ObjectStoreWriter userProfileOs = ObjectStoreWriterFactory
                    .getObjectStoreWriter(userProfileAlias);
                profileManager = new ProfileManager(getObjectStore(), userProfileOs);
            } catch (ObjectStoreException e) {
                throw new IllegalArgumentException("Error creating profile manager", e);
            }
        }
        return profileManager;
    }

    private static TemplateManager templateManager = null;
    /**
     * Returns the TemplateManager for access to global and user template queries.
     *
     * @return a List
     */
    public static TemplateManager getTemplateManager() {
        if (templateManager == null) {
            templateManager = new TemplateManager(getProfile(),
                    getObjectStore().getModel());
        }
        return templateManager;
    }

    private static BagQueryRunner bagQueryRunner = null;
    /**
     * Return the BagQueryRunner for executing LOOKUP queries.
     * @return the bag query runnner
     */
    private static BagQueryRunner getBagQueryRunner() {
        if (bagQueryRunner == null) {
            bagQueryRunner = new BagQueryRunner(getObjectStore(), getClassKeys(),
                    getBagQueryConfig(), getTemplateManager());
        }
        return bagQueryRunner;
    }

    private static BagManager bagManager = null;
    /**
     * Returns a BagManager.
     * @return a BagManager
     */
    public static BagManager getBagManager() {
        if (bagManager == null) {
            bagManager = new BagManager(getProfile(), getObjectStore().getModel());
        }
        return bagManager;
    }
}
