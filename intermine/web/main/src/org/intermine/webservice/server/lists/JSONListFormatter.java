package org.intermine.webservice.server.lists;

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

public class JSONListFormatter implements ListFormatter {

    private final InterMineAPI im;
    private final Profile profile;

    public JSONListFormatter(InterMineAPI im, Profile profile, boolean jsDates) {
        super();
        this.im = im;
        this.profile = profile;
    }

    private final boolean hasCallback = false;
    private int rowsLeft = 0;
    private final SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public Map<String, Object> bagToMap(InterMineBag list) {
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
        if (profile.getSavedBags().get(list.getName()) == list) {
            listMap.put("authorized", true);
        } else {
            listMap.put("authorized", false);
        }
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
