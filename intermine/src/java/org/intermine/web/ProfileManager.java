package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.SavedQuery;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.util.TypeUtil;
import org.intermine.util.StringUtil;

/**
 * Class to manage and persist user profile data such as saved bags
 * @author Mark Woodbridge
 */
public class ProfileManager
{
    protected ObjectStore os;
    protected ObjectStoreWriter osw;

    /**
     * Construct a ProfileManager for the webapp
     * @param os the ObjectStore to which the webapp is providing an interface
     * @throws ObjectStoreException if the user profile database cannot be found
     */
    public ProfileManager(ObjectStore os) throws ObjectStoreException {
        this.os = os;
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile");
    }

    /**
     * Check whether a user already has a Profile
     * @param username the username
     * @return true if a profile exists
     */
    public boolean hasProfile(String username) {
        return getProfile(username) != null;
    }

    /**
     * Validate a user's password
     * A check should be made prior to this call to ensure a Profile exists
     * @param username the username
     * @param password the password
     * @return true if password is valid
     */
    public boolean validPassword(String username, String password) {
        return getProfile(username).getPassword().equals(password);
    }

    /**
     * Change a user's password
     * A check should be made prior to this call to ensure a Profile exists
     * @param username the username
     * @param password the password
     */
    public void setPassword(String username, String password) {
        UserProfile userProfile = getProfile(username);
        userProfile.setPassword(password);
        try {
            osw.store(userProfile);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a user's password
     * A check should be made prior to this call to ensure a Profile exists
     * @param username the username
     * @return password the password
     */
    public String getPassword(String username) {
        UserProfile userProfile = getProfile(username);
        return userProfile.getPassword();
    }

    /**
     * Get a user's Profile
     * @param username the username
     * @param password the password
     * @return the Profile, or null if one doesn't exist
     */
    public Profile getProfile(String username, String password) {
        Profile profile = null;
        if (hasProfile(username) && validPassword(username, password)) {
            UserProfile userProfile = getProfile(username);
            Map savedBags = new HashMap();
            for (Iterator i = userProfile.getSavedBags().iterator(); i.hasNext();) {
                SavedBag bag = (SavedBag) i.next();
                savedBags.putAll(new SavedBagParser(os).process(new StringReader(bag.getBag())));
            }
            Map savedQueries = new HashMap();
            for (Iterator i = userProfile.getSavedQuerys().iterator(); i.hasNext();) {
                SavedQuery query = (SavedQuery) i.next();
                savedQueries.putAll(new SavedQueryParser().
                                    process(new StringReader(query.getQuery())));
            }
            profile = new Profile(this, username, savedQueries, savedBags);
        }
        return profile;
    }
    
    /**
     * Synchronise a user's Profile with the backing store
     * @param profile the Profile
     */
    public void saveProfile(Profile profile) {
        String username = profile.getUsername();
        if (username != null) {
            try {
                UserProfile userProfile = null;
                if (hasProfile(username)) {
                    userProfile = getProfile(username);
                    for (Iterator i = userProfile.getSavedBags().iterator(); i.hasNext();) {
                        osw.delete((InterMineObject) i.next());
                    }

                    for (Iterator i = userProfile.getSavedQuerys().iterator(); i.hasNext();) {
                        osw.delete((InterMineObject) i.next());
                    }
                } else {
                    userProfile = new UserProfile();
                    userProfile.setUsername(username);
                }

                for (Iterator i = profile.getSavedBags().entrySet().iterator(); i.hasNext();) {
                    Map.Entry entry = (Map.Entry) i.next();
                    String bagName = (String) entry.getKey();
                    InterMineBag bag = (InterMineBag) entry.getValue();
                    SavedBag savedBag = new SavedBag();
                    savedBag.setBag(toXml(bag, bagName));
                    savedBag.setUserProfile(userProfile);
                    osw.store(savedBag);
                }
                
                for (Iterator i = profile.getSavedQueries().entrySet().iterator(); i.hasNext();) {
                    Map.Entry entry = (Map.Entry) i.next();
                    String queryName = (String) entry.getKey();
                    PathQuery query = (PathQuery) entry.getValue();
                    SavedQuery savedQuery = new SavedQuery();
                    savedQuery.setQuery(toXml(query, queryName, os.getModel().getName()));
                    savedQuery.setUserProfile(userProfile);
                    osw.store(savedQuery);
                }
                 
                osw.store(userProfile);
            } catch (ObjectStoreException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    /**
     * Perform a query to retrieve a user's backing UserProfile
     * @param username the username
     * @return the relevant UserProfile
     */
    protected UserProfile getProfile(String username) {
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        Set fieldNames = new HashSet();
        fieldNames.add("username");
        try {
            profile = (UserProfile) osw.getObjectByExample(profile, fieldNames);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Unable to load user profile", e);
        }
        return profile;
    }

    /**
     * Convert an InterMine bag to XML
     * @param bag the InterMineBag
     * @param bagName the name of the bag
     * @return the corresponding XML String
     */
    protected static String toXml(InterMineBag bag, String bagName) {
        StringBuffer sb = new StringBuffer();
        sb.append("<bag name='" + bagName + "'>");
        for (Iterator j = bag.iterator(); j.hasNext();) {
            Object o = j.next();
            String type, value;
            if (o instanceof InterMineObject) {
                type = InterMineObject.class.getName();
                value = ((InterMineObject) o).getId().toString();
            } else {
                type = o.getClass().getName();
                value = TypeUtil.objectToString(o);
            }
            sb.append("<element type='" + type + "' value='" + value + "'/>");
        }
        sb.append("</bag>");
        return sb.toString();
    }

    /**
     * Convert a query to XML
     * @param query the PathQuery
     * @param queryName the name of the query
     * @param modelName the model name
     * @return the corresponding XML String
     */
    protected static String toXml(PathQuery query, String queryName, String modelName) {
        StringBuffer sb = new StringBuffer();
        sb.append("<query name='" + queryName + "' model='" + modelName
                  + "' view='" + StringUtil.join(query.getView(), " ") + "'>");
        for (Iterator j = query.getNodes().values().iterator(); j.hasNext();) {
            PathNode node = (PathNode) j.next();
            if (node.getConstraints().size() > 0) {
                sb.append("<node path='" + node.getPath() + "' type='" + node.getType() + "'>");
                for (Iterator k = node.getConstraints().iterator(); k.hasNext();) {
                    Constraint c = (Constraint) k.next();
                    sb.append("<constraint op='" + c.getOp() + "' value='" + c.getValue() + "'/>");
                }
                sb.append("</node>");
            }
        }
        sb.append("</query>");
        return sb.toString();
    }
}
