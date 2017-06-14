package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.json.JSONObject;

/** @author Alex Kalderimis **/
public class JSONListFormatter implements ListFormatter
{

    private final InterMineAPI im;
    private final Profile profile;

    /**
     * Construct a list formatter.
     * @param im The InterMine state object.
     * @param profile The current user.
     * @param jsDates Format dates suitable for JavaScript.
     */
    public JSONListFormatter(InterMineAPI im, Profile profile, boolean jsDates) {
        super();
        this.im = im;
        this.profile = profile;
    }

    private int rowsLeft = 0;
    private final SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    /**
     * Transform a bag into a mapping of its properties, for easy serialisation
     * @param list The bag to read
     * @return Its properties
     **/
    Map<String, Object> bagToMap(InterMineBag list) {
        Map<String, Object> listMap = new HashMap<String, Object>();
        listMap.put("name", list.getName());
        listMap.put("type", list.getType());
        listMap.put("title", list.getTitle());
        listMap.put("description", list.getDescription());
        listMap.put("status", list.getState());
        if (list.getDateCreated() != null) {
            Date createdOn = list.getDateCreated();

            listMap.put("timestamp", createdOn.getTime());
            listMap.put("dateCreated", iso8601.format(createdOn));
        }
        BagManager bm = im.getBagManager();
        List<Tag> tags = bm.getTagsForBag(list, profile);
        List<String> tagNames = new ArrayList<String>();
        for (Tag t: tags) {
            tagNames.add(t.getTagName());
        }
        listMap.put("tags", tagNames);

        try {
            listMap.put("size", list.getSize());
        } catch (ObjectStoreException e) {
            throw new ServiceException("Error getting list size:" + e);
        }
        boolean belongsToMe = list == profile.getSavedBags().get(list.getName());
        listMap.put("authorized", belongsToMe);
        return listMap;
    }

    @Override
    public List<String> format(InterMineBag list) {
        rowsLeft -= 1;
        JSONObject listObj = new JSONObject(bagToMap(list));
        String ret = listObj.toString();
        if (rowsLeft > 0) {
            return Arrays.asList(ret, "");
        } else {
            return Arrays.asList(ret);
        }
    }

    @Override
    public void setSize(int size) {
        this.rowsLeft = size;
    }

}
