package org.intermine.bio.web.util;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.mines.FriendlyMineManager;
import org.intermine.api.mines.Mine;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.Util;
import org.intermine.web.util.InterMineLinkGenerator;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helper class for intermine links generated on report pages
 *
 * @author Julie Sullivan
 */
public final class FriendlyMineListLinkGenerator extends InterMineLinkGenerator
{
    private static final Logger LOG = Logger.getLogger(FriendlyMineListLinkGenerator.class);

    /**
     * Constructor
     */
    public FriendlyMineListLinkGenerator() {
        super();
    }

    /**
     * Generate a list of genes and orthologues for remote mines.
     *
     * 1. Query local mine for orthologues for gene given.
     * 2. if orthologue found query remote mine for that gene
     * 3. if orthologue not found query remote mine for orthologues
     *
     * @param linkManager LinkManager
     * @param organisms organism.shortName, eg. C. elegans
     * @param identifiers unused for now
     * @param mineName name of mine to query
     * @return map from mine to organism-->genes
     */
    @Override
    public Collection<JSONObject> getLinks(FriendlyMineManager linkManager, String mineName,
            String organisms, String identifiers) {
        Collection<JSONObject> results = null;
        try {
            results = getRelatedValues(linkManager, mineName, organisms, identifiers);
        } catch (JSONException e) {
            LOG.error("error generating JSON objects", e);
        }
        return results;
    }

    /**
     * used on LIST ANALYSIS page
     *
     * For a given mine and list of values, return filtered list of values contained in mine.  eg.
     * for a list of organisms, return the list of organism present in the given mine.
     *
     * Returns NULL if invalid mine or values not found.
     *
     * @param mineName name of mine to test
     * @param values list of values to check for in the mine
     * @return subset of values given that are present in the mine
     * @throws JSONException
     */
    private Collection<JSONObject> getRelatedValues(FriendlyMineManager linkManager,
            String mineName, String values, String identifiers) throws JSONException {
        if (StringUtils.isEmpty(mineName) || StringUtils.isEmpty(values)) {
            return null;
        }
        List<String> organismsInList = Arrays.asList(values.split(","));
        List<String> identifierSet = Arrays.asList(identifiers.split(","));
        Mine mine = linkManager.getMine(mineName);
        if (mine == null || mine.getReleaseVersion() == null) {
            // mine is dead
            return Collections.emptyList();
        }

        // check local mine first because we trust our mine most
        Collection<JSONObject> results = checkLocalMine(linkManager, mine, organismsInList,
                identifierSet);
        if (!results.isEmpty()) {
            return results;
        }

        // trust remote mine to convert
        Set<String> homologues = mine.getMatchingMapKeys(null, organismsInList);
        if (!homologues.isEmpty()) {
            return convertToJSON(homologues, identifierSet, true);
        }
        return Collections.emptyList();
    }

    // test if local mine has orthologues
    private Collection<JSONObject> checkLocalMine(FriendlyMineManager linkManager,
            Mine mine, List<String> organismsInList, List<String> identifierSet)
        throws JSONException {
        boolean isOrthologue = true;
        Mine localMine = linkManager.getLocalMine();

        // D. rerio --> Dmel
        Set<String> homologues = localMine.getMatchingMapKeys(mine.getDefaultValues(),
                organismsInList);
        if (homologues.isEmpty()) {
            // one more try
            // look for Dmel --> D. rerio
            homologues = localMine.getMatchingMapValues(mine.getDefaultValues(), organismsInList);
            isOrthologue = false;
        }
        if (homologues.isEmpty()) {
            // neither mine has orthologues
            return null;
        }
        // convert
        InterMineAPI im = linkManager.getInterMineAPI();
        ProfileManager profileManager = im.getProfileManager();
        Map<String, List<String>> results =  runLocalQuery(im, profileManager,
                identifierSet, homologues, isOrthologue);
        if (results.isEmpty()) {
            return null;
        }
        return convertToJSON(results, isOrthologue);
    }

    private static Map<String, List<String>> runLocalQuery(InterMineAPI im,
            ProfileManager profileManager, List<String> identifiers,  Set<String> organisms,
            boolean isOrthologue) {
        Map<String, List<String>> results = new HashMap<String, List<String>>();
        PathQueryExecutor executor = im.getPathQueryExecutor(profileManager.getSuperuserProfile());
        PathQuery q = getLocalOrthologueQuery(im, identifiers, organisms, isOrthologue);
        if (!q.isValid()) {
            return Collections.emptyMap();
        }
        ExportResultsIterator it = executor.execute(q);
        while (it.hasNext()) {
            List<ResultElement> row = it.next();
            String orthologuePrimaryIdentifier = (String) row.get(0).getField();
//            String orthologueSymbol = (String) row.get(1).getField();
            String organismName = (String) row.get(2).getField();
            String orthologueIdentifer = null;
            if (!StringUtils.isEmpty(orthologuePrimaryIdentifier)) {
                orthologueIdentifer = orthologuePrimaryIdentifier;
            }
//            if (!StringUtils.isEmpty(orthologueSymbol)) {
//                orthologueIdentifer = orthologueSymbol;
//            }
            if (!StringUtils.isEmpty(orthologueIdentifer)) {
                Util.addToListMap(results, organismName, orthologueIdentifer);
            }
        }
        return results;
    }

    private static PathQuery getLocalOrthologueQuery(InterMineAPI im, List<String> identifiers,
            Set<String> organisms, boolean isOrthologue) {
        PathQuery q = new PathQuery(im.getModel());
        q.addConstraint(Constraints.neq("Gene.homologues.type", "paralogue"));
        if (isOrthologue) {
            q.addViews("Gene.primaryIdentifier",
                    "Gene.symbol",
                    "Gene.organism.shortName");
            q.addConstraint(Constraints.oneOfValues("Gene.homologues.homologue.primaryIdentifier",
                    identifiers));
            q.addConstraint(Constraints.oneOfValues("Gene.organism.shortName",
                    organisms));
            q.addOrderBy("Gene.organism.shortName", OrderDirection.ASC);
        } else {
            q.addViews("Gene.homologues.homologue.primaryIdentifier",
                    "Gene.homologues.homologue.symbol",
                    "Gene.homologues.homologue.organism.shortName");
            q.addConstraint(Constraints.oneOfValues("Gene.primaryIdentifier", identifiers));
            q.addConstraint(Constraints.oneOfValues("Gene.homologues.homologue.organism.shortName",
                    organisms));
            q.addOrderBy("Gene.homologues.homologue.organism.shortName", OrderDirection.ASC);
        }
        return q;
    }

    private Collection<JSONObject> convertToJSON(Set<String> homologues, List<String> identifiers,
            boolean isHomologue)
        throws JSONException {
        Set<JSONObject> jsonObjects = new HashSet<JSONObject>();
        for (String homologue : homologues) {
            JSONObject json = getJSONOrganism(homologue, identifiers, isHomologue);
            jsonObjects.add(json);
        }
        return jsonObjects;
    }

    private Collection<JSONObject> convertToJSON(Map<String, List<String>> results,
            boolean isHomologue)
        throws JSONException {
        List<JSONObject> jsonObjects = new ArrayList<JSONObject>();
        for (Map.Entry<String, List<String>> entry : results.entrySet()) {
            String homologue = entry.getKey();
            List<String> identifiers = entry.getValue();
            JSONObject json = getJSONOrganism(homologue, identifiers, isHomologue);
            jsonObjects.add(json);
        }
        return jsonObjects;
    }

    private static JSONObject getJSONOrganism(String organismName, List<String> identifiers,
            boolean isHomologue)
        throws JSONException {
        JSONObject organism = new JSONObject();
        organism.put("shortName", organismName);
        organism.put("identifiers", identifiers);
        organism.put("isHomologue", isHomologue);
        return organism;
    }

}
