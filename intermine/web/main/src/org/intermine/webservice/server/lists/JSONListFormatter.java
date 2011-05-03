package org.intermine.webservice.server.lists;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.api.profile.InterMineBag;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.json.JSONObject;

public class JSONListFormatter implements ListFormatter {

    private final boolean hasCallback = false;
    private int rowsLeft = 0;
    private final SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public List<String> format(InterMineBag list) {
        rowsLeft -= 1;
        Map<String, Object> listMap = new HashMap<String, Object>();
        listMap.put("name", list.getName());
        listMap.put("type", list.getType());
        listMap.put("title", list.getTitle());
        listMap.put("description", list.getDescription());
        if (list.getDateCreated() != null) {
            Date createdOn = list.getDateCreated();
            listMap.put("dateCreated", iso8601.format(createdOn));
        }
        try {
            listMap.put("size", list.getSize());
        } catch (ObjectStoreException e) {
            throw new ServiceException("Error getting list size:" + e);
        }
        JSONObject listObj = new JSONObject(listMap);
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
