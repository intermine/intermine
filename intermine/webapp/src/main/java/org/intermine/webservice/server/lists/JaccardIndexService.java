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


import org.apache.commons.collections.CollectionUtils;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
        BigDecimal minimumValue = new BigDecimal(0);

        if (listName == null && ids == null) {
            throw new BadRequestException("Provide either name of a list or set of InterMine IDs");
        }

        if (min != null) {
            try {
                minimumValue = new BigDecimal(min);
            } catch (NumberFormatException e) {
                throw new BadRequestException("Min must be a valid number: '" + min + "'");
            }
        }

        ListManager listManager = new ListManager(im, getPermission().getProfile());
        Map<String, InterMineBag> lists = listManager.getListMap();
        List<String> bagOfInterest = new ArrayList<>();

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
//            bagOfInterest = getJaccardIndex(ids);
        }

        Map<String, String> results = new HashMap<String, String>();

        output.setHeaderAttributes(getHeaderAttributes());
        for (Map.Entry<String, InterMineBag> entry : lists.entrySet()) {
            String name = entry.getKey();
            InterMineBag bag = entry.getValue();
            if (bag == null) {
                continue;
            }
            if (listName != null && listName.equals(name)) {
                // don't compare list to itself
                continue;
            }
            if (!bag.getType().equalsIgnoreCase(type)) {
                // only compare bags of the same type
                continue;
            }

            List<String> comparisonList = getBagValues(bag);
            List<String> intersection = (List<String>) CollectionUtils.intersection(bagOfInterest,
                    comparisonList);
            BigDecimal denominator = new BigDecimal(bagOfInterest.size()
                    + comparisonList.size() - intersection.size());
            BigDecimal numerator = new BigDecimal(intersection.size());
            BigDecimal jaccardSimilarity = new BigDecimal(0);
            // don't divide by zero
            if (denominator.compareTo(BigDecimal.ZERO) > 0) {
                jaccardSimilarity = denominator.divide(numerator, RoundingMode.HALF_UP);
            }
            if (jaccardSimilarity.compareTo(minimumValue) >= 0) {
                results.put(name, String.valueOf(jaccardSimilarity) + " ");
                results.put(" double results ", jaccardSimilarity.toString());

                String msg = "bagOfInterest.size():" + String.valueOf(bagOfInterest.size())
                + ",comparisonList.size():" + String.valueOf(comparisonList.size())
                + ",intersection.size():" + String.valueOf(intersection.size());

                results.put("results for" + name, msg);

                String members = "";
                for (String s : intersection) {
                    members = s + ",";
                }
                results.put(name + " intersection list member", members);
            }
        }

        JSONObject jo = new JSONObject(results);
        output.addResultItem(Collections.singletonList(jo.toString()));
    }

    private List<String> getBagValues(InterMineBag bag) {
        List<String> identifiers = new ArrayList();
        for (BagValue bv: bag.getContents()) {
            identifiers.add(bv.getValue());
        }
        return identifiers;
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
