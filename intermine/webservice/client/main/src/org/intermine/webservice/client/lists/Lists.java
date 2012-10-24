package org.intermine.webservice.client.lists;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utilities for parsing lists from their JSON representation.
 * @author Alex Kalderimis.
 *
 */
public final class Lists
{

    private Lists() {
        // Hidden constructor.
    }

    private static final SimpleDateFormat ISO_8601_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Parse a list from its representation in JSON.
     * @param fac The factory for the service that this list belongs to.
     * @param data The data in the list.
     * @return A representation of the list.
     */
    public static ItemList parseList(ServiceFactory fac, JSONObject data) {
        try {
            String name = data.getString("name");
            String description = data.getString("description");
            String type = data.getString("type");
            String status = data.optString("status", null);
            int size = data.getInt("size");
            boolean authorized = data.getBoolean("authorized");
            Date createdAt = null;
            if (data.has("dateCreated")) {
                createdAt = ISO_8601_DATE_FORMAT.parse(data.getString("dateCreated"));
            }
            JSONArray tagArray = data.getJSONArray("tags");
            List<String> tags = new ArrayList<String>();
            int noOfTags = tagArray.length();
            for (int i = 0; i < noOfTags; i++) {
                tags.add(tagArray.getString(i));
            }
            return new ItemList(fac, name, description, size,
                    type, tags, authorized, createdAt, status);
        } catch (JSONException e) {
            throw new ServiceException("Error parsing '" + data.toString() + "'", e);
        } catch (ParseException e) {
            throw new ServiceException("Error parsing date '" + data.toString() + "'", e);
        }
    }

    /**
     * Construct a representation of a list from the data returned by a call to create
     * a new list.
     * @param factory The factory for the service that this list belongs to.
     * @param data The data in the list.
     * @return A representation of the list.
     */
    public static ItemList parseListCreationInfo(ServiceFactory factory,
            String data) {
        // TODO - make list creation info and list info the same format.
        try {
            JSONObject jo = new JSONObject(data);
            if (!jo.isNull("error")) {
                throw new ServiceException(jo.getString("error"));
            }
            String name = jo.getString("listName");
            int size = 0;
            ItemList il = new ItemList(factory, name, null, size, "dummy",
                    null, true, new Date(), null);
            if (jo.has("unmatchedIdentifiers")) {
                JSONArray badIds = jo.getJSONArray("unmatchedIdentifiers");
                for (int i = 0; i < badIds.length(); i++) {
                    il.addUnmatchedId(badIds.getString(i));
                }
            }
            return il;
        } catch (JSONException e) {
            throw new ServiceException("Error parsing '" + data + "'", e);
        }
    }

}
