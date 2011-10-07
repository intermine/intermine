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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.mines.FriendlyMineManager;
import org.intermine.api.mines.FriendlyMineQueryRunner;
import org.intermine.api.mines.Mine;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.CacheMap;
import org.intermine.util.StringUtil;
import org.intermine.util.Util;
import org.intermine.web.util.InterMineLinkGenerator;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helper class for intermine links generated on report pages
 *
 * @author Julie Sullivan
 */
public final class FriendlyMineReportLinkGenerator extends InterMineLinkGenerator
{
    private static final Logger LOG = Logger.getLogger(FriendlyMineReportLinkGenerator.class);
    private static Map<MultiKey, Collection<JSONObject>> intermineLinkCache
        = new CacheMap<MultiKey, Collection<JSONObject>>();
    private boolean debug = false;

    /**
     * Constructor
     */
    public FriendlyMineReportLinkGenerator() {
        super();
    }

    /**
     * Generate a list of genes and orthologues for remote mines.
     *
     * 1. Query local mine for orthologues for gene given.
     * 2. if orthologue found query remote mine for that gene
     * 3. if orthologue not found query remote mine for orthologues
     *
     * @param olm LinkManager
     * @param organismShortName organism.shortName, eg. C. elegans
     * @param primaryIdentifier identifier for gene
     * @param mineName name of mine to query
     * @return map from mine to organism-->genes
     */
    public Collection<JSONObject> getLinks(FriendlyMineManager olm, String mineName,
            String organismShortName, String primaryIdentifier) {
        MultiKey key = new MultiKey(mineName, primaryIdentifier, organismShortName);
        if (intermineLinkCache.get(key) != null && !debug) {
            return intermineLinkCache.get(key);
        }
        Map<String, Set<JSONObject>> organismToGenes = new HashMap<String, Set<JSONObject>>();
        Mine mine = olm.getMine(mineName);
        if (mine == null || mine.getReleaseVersion() == null) {
            // mine is dead
            return null;
        }
        try {
            queryForGenes(mine, organismToGenes, organismShortName, primaryIdentifier);
            queryForHomologues(olm, mine, organismToGenes, organismShortName, primaryIdentifier);
        } catch (UnsupportedEncodingException e) {
            LOG.error("error encoding organism name", e);
            return null;
        } catch (JSONException e) {
            LOG.error("error generating JSON objects", e);
            return null;
        }

        Collection<JSONObject> organisms = new ArrayList<JSONObject>();

        // now we have a list of orthologues, add to JSON Organism object
        for (Entry<String, Set<JSONObject>> entry : organismToGenes.entrySet()) {
            String organismName = entry.getKey();
            Set<JSONObject> homologues = entry.getValue();
            JSONObject organism = new JSONObject();
            try {
                organism.put("shortName", organismName);
                organism.put("genes", homologues);
            } catch (JSONException e) {
                LOG.error("Problem reading JSON", e);
                return null;
            }
            organisms.add(organism);
        }
        intermineLinkCache.put(key, organisms);
        return organisms;
    }

    /**
     * Query mine for gene
     *
     * 1. Query local mine for orthologues for gene given.
     * 2. if orthologue found query remote mine for that gene
     * 3. if orthologue not found query remote mine for orthologues
     *
     * @param mine to query
     * @param organismShortName organism.shortName, eg. C. elegans
     * @param primaryIdentifier identifier for gene
     * @throws JSONException
     * @throws UnsupportedEncodingException
     */
    private static void queryForGenes(Mine mine, Map<String, Set<JSONObject>> organisms,
            String organismShortName, String primaryIdentifier)
        throws JSONException, UnsupportedEncodingException {
        if (organismShortName == null) {
            LOG.error("error created links to other mines for this gene, no organism provided");
            return;
        }
        if (primaryIdentifier == null) {
            LOG.error("error created links to other mines for this gene, no primary identifier "
                    + "provided");
            return;
        }
        String encodedOrganism = URLEncoder.encode("" + organismShortName, "UTF-8");
        String[] identifiers = getObjectInOtherMines(mine, encodedOrganism, primaryIdentifier);
        if (identifiers == null) {
            return;
        }
        JSONObject gene = getJSONGene(identifiers);
        Util.addToSetMap(organisms, organismShortName, gene);
    }

    /**
     * generate a list of orthologues for remote mine
     *
     * 1. Query local mine for orthologues for gene given.
     * 2. if orthologue found query remote mine for that gene
     * 3. if orthologue not found query remote mine for orthologues
     *
     * @param mine mine to query
     * @param organismShortName organism.shortName, eg. C. elegans
     * @param primaryIdentifier identifier for gene
     * @throws JSONException if we have JSON problems
     * @throws UnsupportedEncodingException
     */
    private static void queryForHomologues(FriendlyMineManager olm, Mine mine,
            Map<String, Set<JSONObject>> organisms, String organismShortName,
            String primaryIdentifier)
        throws JSONException, UnsupportedEncodingException {
        Map<String, Set<String>> localHomologues = getLocalOrthologues(olm, organismShortName,
                primaryIdentifier);
        boolean queryRemoteMine = true;
        for (String remoteMineOrganism : mine.getDefaultValues()) {
            // check if local mine has orthologues for organism in remote mine
            Set<String> matchingHomologues = localHomologues.get(remoteMineOrganism);
            if (matchingHomologues != null && !matchingHomologues.isEmpty()) {
                // test if remote mine has these genes
                Map<String, Set<String[]>> results = getObjectsInOtherMine(mine, remoteMineOrganism,
                        matchingHomologues);
                if (results != null && !results.isEmpty()) {
                    queryRemoteMine = false;
                    organisms.putAll(processHomologues(results));
                }
            }
        }

        /**
         * Query the remote mine if:
         * - local mine has no orthologues
         * - remote mine does not have corresponding gene for the orthologue found in local mine
         */
        if (queryRemoteMine) {
            String encodedOrganism = URLEncoder.encode("" + organismShortName, "UTF-8");
            Map<String, Set<String[]>> results = FriendlyMineQueryRunner.runRelatedDataQuery(mine,
                    encodedOrganism, primaryIdentifier);
            if (results != null && !results.isEmpty()) {
                organisms = processHomologues(results);
            }
        }
    }

    private static Map<String, Set<JSONObject>> processHomologues(
            Map<String, Set<String[]>> results)
        throws JSONException {
        Map<String, Set<JSONObject>> homologues = new HashMap<String, Set<JSONObject>>();
        for (Map.Entry<String, Set<String[]>> entry : results.entrySet()) {
            String organismName = entry.getKey();
            Set<String[]> identifierSets = entry.getValue();
            for (String[] identifiers : identifierSets) {
                JSONObject gene = getJSONGene(identifiers);
                Util.addToSetMap(homologues, organismName, gene);
            }
        }
        return homologues;
    }

    // does remote mine have these genes, query run for each organism
    private static Map<String, Set<String[]>> getObjectsInOtherMine(Mine mine,
            String remoteMineDefaultOrganism, Set<String> matchingHomologues) {
        Map<String, Set<String[]>> homologues = new HashMap<String, Set<String[]>>();
        String homologueString = StringUtil.join(matchingHomologues, ",");
        Set<String[]> homologueIdentifiers;
        try {
            homologueIdentifiers = FriendlyMineQueryRunner.getObjectsInOtherMine(mine,
                    URLEncoder.encode("" + remoteMineDefaultOrganism, "UTF-8"), homologueString);
        } catch (UnsupportedEncodingException e) {
            return null;
        }

        // query isn't guaranteed to return valid identifiers, filter
        boolean hasValidResults = false;
        Set<String[]> validIdentifiers = new HashSet<String[]>();
        for (String[] homologuePair : homologueIdentifiers) {
            if (homologuePair != null && homologuePair.length == 2 && homologuePair[0] != null) {
                validIdentifiers.add(homologuePair);
                hasValidResults = true;
            }
        }
        if (hasValidResults) {
            homologues.put(remoteMineDefaultOrganism, validIdentifiers);
            return homologues;
        }
        return null;
    }

    private static JSONObject getJSONGene(String[] identifiers)
        throws JSONException {
        JSONObject gene = new JSONObject();
        gene.put("primaryIdentifier", identifiers[0]);
        gene.put("displayIdentifier", identifiers[1]);
        return gene;
    }


    // query local mine for orthologues
    private static Map<String, Set<String>> getLocalOrthologues(FriendlyMineManager olm,
            String constraintValue, String identifier) {
        Map<String, Set<String>> relatedDataMap = new HashMap<String, Set<String>>();
        InterMineAPI im = olm.getInterMineAPI();
        ProfileManager profileManager = im.getProfileManager();
        PathQueryExecutor executor = im.getPathQueryExecutor(profileManager.getSuperuserProfile());
        PathQuery q = getLocalOrthologueQuery(im, identifier);
        if (!q.isValid()) {
            return Collections.emptyMap();
        }
        ExportResultsIterator it = executor.execute(q);
        while (it.hasNext()) {
            List<ResultElement> row = it.next();
            String geneOrganismName = (String) row.get(0).getField();
            String orthologuePrimaryIdentifier = (String) row.get(1).getField();
            String organismName = (String) row.get(2).getField();
            if (geneOrganismName.equals(organismName)) {
                // ignore paralogues for now
                continue;
            }
            if (!StringUtils.isEmpty(orthologuePrimaryIdentifier)) {
                Util.addToSetMap(relatedDataMap, organismName, orthologuePrimaryIdentifier);
            }
        }
        return relatedDataMap;
    }

    private static PathQuery getLocalOrthologueQuery(InterMineAPI im, String identifier) {
        PathQuery q = new PathQuery(im.getModel());
        q.addViews("Gene.organism.shortName",
                "Gene.homologues.homologue.primaryIdentifier",
                "Gene.homologues.homologue.organism.shortName");
        q.addOrderBy("Gene.homologues.homologue.organism.shortName", OrderDirection.ASC);
        q.addConstraint(Constraints.eq("Gene.primaryIdentifier", identifier));
        q.addConstraint(Constraints.neq("Gene.homologues.type", "paralogue"));
        return q;
    }

    private static String[] getObjectInOtherMines(Mine mine, String constraintValue,
            String identifier) {
        String[] identifiers = FriendlyMineQueryRunner.getObjectInOtherMine(mine, constraintValue,
                identifier);
        if (identifiers != null && identifiers.length == 2 && identifiers[0] != null) {
            return identifiers;
        }
        return null;
    }
}
