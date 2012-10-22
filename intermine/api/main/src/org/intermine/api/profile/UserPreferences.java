package org.intermine.api.profile;

import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.util.PropertiesUtil;

public class UserPreferences extends AbstractMap<String, String> {

//    /* Some commonly used preference names */
//    public static final String NO_SPAM = "do_not_spam"; // If this key is set at all, then we should not send extra emails to the user.
//    public static final String HIDDEN = "hidden"; // If this key is set at all, then we should not let other users discover this one.
//    public static final String ALIAS = "alias"; // The alias of this user.

    // This is known to the API as the Profile needs to read this to provide getEmailAddress().
    public static final String EMAIL = "email"; // The preferred address to send emails to.

    public static final Set<String> COMMON_KEYS;
    public static final Set<String> BOOLEAN_KEYS;
    public static final Set<String> UNIQUE_KEYS;

    static {
        Properties props = PropertiesUtil.getPropertiesStartingWith("api.profile.preferences.names");
        Set<String> all = new LinkedHashSet<String>(),
                bools = new LinkedHashSet<String>(),
                uniques = new LinkedHashSet<String>();
        Enumeration<Object> keys = props.keys();
        while (keys.hasMoreElements()) {
            String key = String.valueOf(keys.nextElement());
            String value = props.getProperty(key);
            all.add(value);
            if (StringUtils.contains(key, "bool")) {
                bools.add(value);
            } else if (StringUtils.contains(key, "unique")) {
                uniques.add(value);
            }
        }
        /* 
         * START OF HACK
         * For now, this is a total hack. But this should be replaced by a working
         * properties based solution.
         * TODO: rip out this hack and make all these properties configurable.
         */
        bools.add("do_not_spam");
        bools.add("hidden");
        uniques.add("alias");
        all.addAll(bools);
        all.addAll(uniques);
        /* END OF HACK */
        all.add(EMAIL);
        COMMON_KEYS = Collections.unmodifiableSet(all);
        BOOLEAN_KEYS = Collections.unmodifiableSet(bools);
        UNIQUE_KEYS = Collections.unmodifiableSet(uniques);
    }

    public static final Logger LOG = Logger.getLogger(UserPreferences.class);

    private final Map<String, String> backingMap;
    private final PreferencesManager manager;
    private final Profile profile;

    protected UserPreferences(PreferencesManager manager, Profile profile) throws SQLException {
        this.backingMap = new HashMap<String, String>();
        this.manager = manager;
        this.profile = profile;
        backingMap.putAll(manager.getPreferences(profile));
    }

    @Override
    public String put(String key, String value) {
        synchronized (manager) {
            try {
                if (UNIQUE_KEYS.contains(key)) {
                    // Special case. Must be globally unique.
                    if (manager.mappingExists(key, value)) {
                        throw new DuplicateMappingException(key, value);
                    }
                }
                manager.setPreference(profile, key, value);
            } catch (SQLException e) {
                throw new RuntimeException("Could not store preference.", e);
            }
        }
        return backingMap.put(key, value);
    }

    @Override
    public String remove(Object key) {
        try {
            manager.deletePreference(profile, key == null ? null : key.toString());
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete preference", e);
        }
        return backingMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> mapping) {
        for (String key: mapping.keySet()) {
            put(key, mapping.get(key)); // inserts into DB
        }
    }

    @Override
    public Set<java.util.Map.Entry<String, String>> entrySet() {
        return backingMap.entrySet();
    }

    @Override
    public void clear() {
        try {
            manager.deleteAllPreferences(profile);
        } catch (SQLException e) {
            throw new RuntimeException("Could not clear preferences", e);
        }
        backingMap.clear();
    }

}
