package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2019 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.BagValue;
import org.intermine.api.profile.InterMineBag;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.ListManager;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.output.HTMLTableFormatter;
import org.intermine.webservice.server.output.JSONFormatter;
import org.json.JSONObject;

import org.apache.commons.text.similarity.JaccardSimilarity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * A service to compare the given list to all other available lists using the Jaccard Index
 * @author Julie Sullivan
 *
 */
public class JaccardIndexService extends WebService
{

    /**
     * Constructor
     * @param im A reference to the InterMine API settings bundle
     */
    public JaccardIndexService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {

        String listName = request.getParameter("list");
        String ids = request.getParameter("ids");
        String min = request.getParameter("min");
        String type = request.getParameter("type");
        Double minimumValue = new Double(0);

        if (listName == null && ids == null) {
            throw new BadRequestException("Provide either name of a list or set of InterMine IDs");
        }

        if (min != null) {
            try {
                minimumValue = new Double(min);
            } catch (NumberFormatException e) {
                throw new BadRequestException("Min must be a valid number: '" + min + "'");
            }
        }

        ListManager listManager = new ListManager(im, getPermission().getProfile());
        Map<String, InterMineBag> lists = listManager.getListMap();
        CharSequence bagOfInterest = null;

        if (listName != null) {
            InterMineBag bag = lists.get(listName);
            if (bag == null) {
                throw new BadRequestException("User does not have access to list named '"
                    + listName + "'");
            }
            bagOfInterest = getBagValues(bag);
            type = bag.getType();
        } else if (ids != null) {
            if (type == null) {
                // need type if we don't have a list
                throw new BadRequestException("Type of list is required");
            }
            bagOfInterest = getJaccardIndex(ids);
        }

        Map<String, String> results = new HashMap<String, String>();
        JaccardSimilarity jaccard = new JaccardSimilarity();

        output.setHeaderAttributes(getHeaderAttributes());
        for (Map.Entry<String, InterMineBag> entry : lists.entrySet()) {
            String name = entry.getKey();
            if (listName != null && listName.equals(name)) {
                // don't compare list to itself
                continue;
            }
            InterMineBag bag = entry.getValue();
            if (bag == null) {
                continue;
            }
            if (!bag.getType().equals(type)) {
                // only compare bags of the same type
                continue;
            }
            CharSequence comparisonList = getBagValues(bag);
            Double result = jaccard.apply(bagOfInterest, comparisonList);
            if (result > minimumValue) {
                results.put(name, result.toString());
            }
        }

        JSONObject jo = new JSONObject(results);
        output.addResultItem(Collections.singletonList(jo.toString()));
    }

    private CharSequence getBagValues(InterMineBag bag) {
        CharSequence cs = "";
        for (BagValue bv: bag.getContents()) {
            cs = cs + bv.getValue();
        }
        return cs;
    }

    private CharSequence getJaccardIndex(String ids) {
        String[] idArray = ids.split(", ");
        CharSequence cs = "";
        for (String id : idArray) {

        }
        return cs;
    }

    /**
     * Get the lists for this request.
     * @return The lists that are available.
     */
    protected Map<String, InterMineBag> getLists() {
        ListManager listManager = new ListManager(im, getPermission().getProfile());
        return listManager.getListMap();
    }

    @Override
    protected Format getDefaultFormat() {
        return Format.JSON;
    }

    @Override
    protected boolean canServe(Format format) {
        return format == Format.JSON
                || format == Format.HTML
                || format == Format.TEXT;
    }

    private Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"results\":[");
            attributes.put(JSONFormatter.KEY_OUTRO, "]");
        }
        if (formatIsJSONP()) {
            attributes.put(JSONFormatter.KEY_CALLBACK, this.getCallback());
        } else if (getFormat() == Format.HTML) {
            attributes.put(HTMLTableFormatter.KEY_COLUMN_HEADERS,
                Arrays.asList("Name", "JaccardIndex"));
        }
        return attributes;
    }

}
