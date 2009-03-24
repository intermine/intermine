package org.intermine.web.logic.query;

/*
 * Copyright (C) 2002-2009 FlyMine
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

import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.util.PropertiesUtil;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.logic.bag.BagConversionHelper;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryHelper;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.profile.TagManager;
import org.intermine.web.logic.profile.TagManagerFactory;
import org.intermine.web.logic.search.SearchFilterEngine;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.template.TemplateQuery;
import org.intermine.web.logic.template.TemplateHelper;
import static org.intermine.web.struts.InitialiserPlugin.PUBLIC_TAG_LIST;

/**
 * Provides methods to obtain PathQueryExecutors and WebResultsExecutors
 *
 * @author Matthew Wakeling
 */
public class PathQueryAPI
{
    /**
     * Returns a PathQueryExecutor, using properties.
     *
     * @return a PathQueryExecutor
     */
    public static PathQueryExecutor getPathQueryExecutor() {
        return new PathQueryExecutor(getObjectStore(), getClassKeys(), getBagQueryConfig(),
                getProfile(), getConversionTemplates(), getSearchRepository());
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

    private static List<TemplateQuery> conversionTemplates = null;
    /**
     * Returns the list of TemplateQueries that are conversion templates.
     *
     * @return a List
     */
    public static List<TemplateQuery> getConversionTemplates() {
        if (conversionTemplates == null) {
            conversionTemplates = BagConversionHelper.getConversionTemplates(getProfile());
        }
        return conversionTemplates;
    }

    private static SearchRepository searchRepository = null;
    /**
     * Returns a SearchRepository.
     *
     * @return a SearchRepository
     */
    public static SearchRepository getSearchRepository() {
        if (searchRepository == null) {
            searchRepository = new SearchRepository(TemplateHelper.GLOBAL_TEMPLATE);
            TagManager tagManager = new TagManagerFactory(getProfileManager()).getTagManager();
            Map<String, TemplateQuery> templateSearchableMap = new SearchFilterEngine()
                .filterByTags(getProfile().getSavedTemplates(), PUBLIC_TAG_LIST, TagTypes.TEMPLATE,
                        getProfile().getUsername(), tagManager);
            searchRepository.addWebSearchables(TagTypes.TEMPLATE, templateSearchableMap);
            Map<String, InterMineBag> bagSearchableMap = new SearchFilterEngine()
                .filterByTags(getProfile().getSavedBags(), PUBLIC_TAG_LIST, TagTypes.BAG,
                        getProfile().getUsername(), tagManager);
            searchRepository.addWebSearchables(TagTypes.BAG, bagSearchableMap);
            searchRepository.setProfile(getProfile());
        }
        return searchRepository;
    }
}
