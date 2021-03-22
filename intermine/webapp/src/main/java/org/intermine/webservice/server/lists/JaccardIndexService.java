package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections.ListUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.ListManager;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.output.HTMLTableFormatter;
import org.intermine.webservice.server.output.JSONFormatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


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
        List<Integer> bagOfInterest = new ArrayList<>();

        if (listName != null) {
            InterMineBag bag = lists.get(listName);
            if (bag == null) {
                throw new BadRequestException("User does not have access to list named '"
                    + listName + "'");
            }
            bagOfInterest = bag.getContentsAsIds();
            type = bag.getType();
        } else if (ids != null) {
            if (type == null) {
                // need type if we don't have a list
                throw new BadRequestException("Type of list is required");
            }

            String[] idArray = ids.split("[, ]+");
            for (int i = 0; i < idArray.length; i++) {
                bagOfInterest.add(Integer.parseInt(idArray[i]));
            }
        }

        Map<String, BigDecimal> results = new HashMap<String, BigDecimal>();

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

            List<Integer> comparisonList = bag.getContentsAsIds();
            List<Integer> intersection = (List<Integer>) ListUtils.intersection(bagOfInterest,
                    comparisonList);
            // calculate the union
            BigDecimal denominator = new BigDecimal(bagOfInterest.size()
                    + comparisonList.size() - intersection.size());
            BigDecimal numerator = new BigDecimal(intersection.size());
            BigDecimal jaccardSimilarity = new BigDecimal(0);
            // don't divide by zero
            if (denominator.compareTo(BigDecimal.ZERO) != 0
                    && numerator.compareTo(BigDecimal.ZERO) != 0) {
                jaccardSimilarity = numerator.divide(denominator, 4, RoundingMode.HALF_EVEN);
            }
            if (jaccardSimilarity.compareTo(minimumValue) >= 0) {
                results.put(name, jaccardSimilarity);
            }
        }

        // sort results. need to be in this format to preserve sort order in JavaScript
        Map<String, BigDecimal> sortedMap = sortByValue(results);
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode rootNode = mapper.createArrayNode();
        for (Map.Entry<String, BigDecimal> entry : sortedMap.entrySet()) {
            JsonNode childNode = mapper.createObjectNode();
            ((ObjectNode) childNode).put(entry.getKey(), entry.getValue().toString());
            ((ArrayNode) rootNode).add(childNode);
        }

        String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        output.addResultItem(Collections.singletonList(jsonString));
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

        String listName = request.getParameter("list");
        String ids = request.getParameter("ids");
        String listOfInterestLabel = (listName != null ? listName : "[" + ids + "]");

        Map<String, Object> attributes = new HashMap<String, Object>();
        if (formatIsJSON()) {
            String inputLabel = "\"input\": \"" + listOfInterestLabel + "\",";
            attributes.put(JSONFormatter.KEY_INTRO, inputLabel + "\"results\":");
        }
        if (formatIsJSONP()) {
            attributes.put(JSONFormatter.KEY_CALLBACK, this.getCallback());
        } else if (getFormat() == Format.HTML) {
            attributes.put(HTMLTableFormatter.KEY_COLUMN_HEADERS,
                Arrays.asList("Name", "JaccardIndex"));
        }
        return attributes;
    }

    private static Map<String, BigDecimal> sortByValue(Map<String, BigDecimal> map) {
        List<Map.Entry<String, BigDecimal>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue(new ResultsComparator()));
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static class ResultsComparator implements Comparator
    {
        public int compare(Object o1, Object o2) {
            BigDecimal d1 = (BigDecimal) o1;
            BigDecimal d2 = (BigDecimal) o2;
            if (d1.compareTo(d2) == 0) {
                return 0;
            } else if (d1.compareTo(d2) > 0) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
