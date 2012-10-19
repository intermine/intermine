package org.intermine.api.profile;

import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

public class UserPreferences extends AbstractMap<String, String> {

    /* Some commonly used preference names */
    public static final String NO_SPAM = "do_not_spam"; // If this key is set at all, then we should not send extra emails to the user.
    public static final String HIDDEN = "hidden"; // If this key is set at all, then we should not let other users discover this one.
    public static final String ALIAS = "alias"; // The alias of this user.
    public static final String EMAIL = "email"; // The preferred address to send emails to.
    public static final Set<String> COMMON_KEYS =
            Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(NO_SPAM, HIDDEN, ALIAS, EMAIL)));
    public static final Set<String> BOOLEAN_KEYS =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(NO_SPAM, HIDDEN)));
    public static final Set<String> UNIQUE_KEYS =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(ALIAS)));
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
