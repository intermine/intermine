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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.mines.LinkManager;
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
public final class BioInterMineLinkGenerator extends InterMineLinkGenerator
{
    private static final Logger LOG = Logger.getLogger(BioInterMineLinkGenerator.class);
    public BioInterMineLinkGenerator() {
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
     * @param filteredMines list of mines with genes
     * @param organismShortName organism.shortName, eg. C. elegans
     * @param primaryIdentifier identifier for gene
     */
    public void getLinks(LinkManager olm, Map<String, JSONObject> filteredMines,
            String organismShortName, String primaryIdentifier) {
        Map<String, JSONObject> minesWithGene = new HashMap<String, JSONObject>();
        try {
            getMinesWithThisGene(olm, minesWithGene, organismShortName, primaryIdentifier);
            getMinesWithOrthologues(olm, filteredMines, minesWithGene, organismShortName,
                    primaryIdentifier);
        } catch (UnsupportedEncodingException e) {
            LOG.error("error encoding organism name", e);
            return;
        } catch (JSONException e) {
            LOG.error("error generating JSON objects", e);
            return;
        }
    }

    /**
     * Generate a list of genes and orthologues for remote mines.
     *
     * 1. Query local mine for orthologues for gene given.
     * 2. if orthologue found query remote mine for that gene
     * 3. if orthologue not found query remote mine for orthologues
     *
     * @param olm LinkManager
     * @param mines list of mines
     * @param organismShortName organism.shortName, eg. C. elegans
     * @param primaryIdentifier identifier for gene
     * @throws JSONException
     * @throws UnsupportedEncodingException
     */
    private static void getMinesWithThisGene(LinkManager olm, Map<String, JSONObject> organisms,
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

        // query each friendly mine, return matches.  value can be identifier or symbol
        // we only return one value even if there are multiple matches, the portal at the remote
        // mine will handle duplicates
        Map<Mine, String> minesWithGene = olm.getObjectInOtherMines(encodedOrganism,
                primaryIdentifier);

        if (minesWithGene == null) {
            return;
        }

        for (Map.Entry<Mine, String> entry : minesWithGene.entrySet()) {
            Mine mine = entry.getKey();
            String identifier = entry.getValue();
            organisms.put(mine.getName(), getJSONOrganism(organismShortName, identifier, false));
        }
    }

    /**
     * generate a list of genes and orthologues for remote mines.
     *
     * 1. Query local mine for orthologues for gene given.
     * 2. if orthologue found query remote mine for that gene
     * 3. if orthologue not found query remote mine for orthologues
     *
     * @param olm LinkManager
     * @param filteredMines list of mines with genes
     * @param organismShortName organism.shortName, eg. C. elegans
     * @param primaryIdentifier identifier for gene
     * @throws JSONException if we have JSON problems
     * @throws UnsupportedEncodingException
     */
    private static void getMinesWithOrthologues(LinkManager olm,
            Map<String, JSONObject> filteredMines, Map<String, JSONObject> mines,
            String organismShortName, String primaryIdentifier)
        throws JSONException, UnsupportedEncodingException {

        String encodedOrganism = URLEncoder.encode("" + organismShortName, "UTF-8");
        Map<String, Set<String>> localHomologues = getLocalOrthologues(olm, organismShortName,
                primaryIdentifier);

        for (Mine mine : LinkManager.getFriendlyMines()) {
            final JSONObject jsonMine = getJSONMine(filteredMines, mine.getName());
            Set<JSONObject> organisms = new HashSet<JSONObject>();
            addCurrentOrganism(organisms, mines, mine.getName(), organismShortName);
            String remoteMineDefaultOrganism = mine.getDefaultValue();
            boolean queryRemoteMine = true;
            Set<String> matchingHomologues = localHomologues.get(remoteMineDefaultOrganism);

            // for default organism, do we have local orthologues?
            if (matchingHomologues != null && !matchingHomologues.isEmpty()) {
                Map<String, Set<String>> orthologueMap = new HashMap<String, Set<String>>();
                for (String homologue : matchingHomologues) {
                    // if so, does remote mine have this gene?
                    String homologueIdentifier = olm.getObjectInOtherMine(mine, URLEncoder.encode(""
                            + remoteMineDefaultOrganism, "UTF-8"), homologue);
                    // not sure it will ever be isEmpty as we pass "" beforehand
                    if (!StringUtils.isEmpty(homologueIdentifier)
                            && !"\"\"".equals(homologueIdentifier)) {
                        Util.addToSetMap(orthologueMap, remoteMineDefaultOrganism,
                                homologueIdentifier);
                    }
                }
                if (!orthologueMap.isEmpty()) {
                    queryRemoteMine = false;
                    JSONObject organism = getJSONOrganism(orthologueMap, false);
                    organisms.add(organism);
                }
            }

            /**
             * Query the remote mine if:
             * - local mine has no orthologues
             * - remote mine does not have corresponding gene for the orthologue found in local mine
             */
            if (queryRemoteMine) {
                Map<String, Set<String>> remoteOrthologues = olm.runRelatedDataQuery(mine,
                        encodedOrganism, primaryIdentifier);
                if (remoteOrthologues != null && !remoteOrthologues.isEmpty()) {
                    JSONObject organism = getJSONOrganism(remoteOrthologues, true);
                    organisms.add(organism);
                }
            }
            if (!organisms.isEmpty()) {
                jsonMine.put("organisms", organisms);
            }
        }
    }

    private static void addCurrentOrganism(Set<JSONObject> organisms, Map<String, JSONObject> mines,
            String mineName, String organismShortName)
        throws JSONException {
        JSONObject genes = mines.get(mineName);
        if (genes != null) {
            JSONObject organism = new JSONObject();
            organism.put("shortName", organismShortName);
            organism.put("genes", genes);
            organisms.add(organism);
        }
    }

    private static JSONObject getJSONGene(String identifier, boolean isConverted)
        throws JSONException {
        JSONObject gene = new JSONObject();
        gene.put("identifier", identifier);
        gene.put("isConverted", isConverted);
        return gene;
    }

    private static JSONObject getJSONOrganism(String organismName, String identifier,
            boolean isConverted)
        throws JSONException {
        JSONObject gene = getJSONGene(identifier, isConverted);
        JSONObject organism = new JSONObject();
        organism.put("shortName", organismName);
        organism.put("orthologues", gene);
        return organism;
    }

    private static JSONObject getJSONOrganism(Map<String, Set<String>> orthologueMap,
            boolean isConverted)
        throws JSONException {
        String organismName = null;
        Set<JSONObject> genes = new HashSet<JSONObject>();
        for (Map.Entry<String, Set<String>> entry : orthologueMap.entrySet()) {
            organismName = entry.getKey();
            Set<String> identifiers = entry.getValue();
            for (String identifier : identifiers) {
                JSONObject gene = getJSONGene(identifier, isConverted);
                genes.add(gene);
            }
        }
        JSONObject organism = new JSONObject();
        organism.put("shortName", organismName);
        organism.put("orthologues", genes);
        return organism;
    }

    // query local mine for orthologues
    private static Map<String, Set<String>> getLocalOrthologues(LinkManager olm,
            String constraintValue, String identifier) {
        Map<String, Set<String>> relatedDataMap = new HashMap<String, Set<String>>();
        InterMineAPI im = olm.getInterMineAPI();
        ProfileManager profileManager = im.getProfileManager();
        PathQueryExecutor executor = im.getPathQueryExecutor(profileManager.getSuperuserProfile());
        ExportResultsIterator it = executor.execute(getLocalOrthologueQuery(im, identifier));
        while (it.hasNext()) {
            List<ResultElement> row = it.next();
            String orthologuePrimaryIdentifier = (String) row.get(0).getField();
            String orthologueSymbol = (String) row.get(1).getField();
            String organismName = (String) row.get(2).getField();
            String orthologueIdentifer = null;
            if (!StringUtils.isEmpty(orthologuePrimaryIdentifier)) {
                orthologueIdentifer = orthologuePrimaryIdentifier;
            }
            if (!StringUtils.isEmpty(orthologueSymbol)) {
                orthologueIdentifer = orthologueSymbol;
            }
            if (!StringUtils.isEmpty(orthologueIdentifer)) {
                Util.addToSetMap(relatedDataMap, organismName, orthologueIdentifer);
            }
        }
        return relatedDataMap;
    }

    private static PathQuery getLocalOrthologueQuery(InterMineAPI im, String identifier) {
        PathQuery q = new PathQuery(im.getModel());
        q.addViews("Gene.homologues.homologue.primaryIdentifier",
                "Gene.homologues.homologue.symbol",
                "Gene.homologues.homologue.organism.shortName");
        q.addOrderBy("Gene.homologues.homologue.organism.shortName", OrderDirection.ASC);
        q.addConstraint(Constraints.eq("Gene.primaryIdentifier", identifier));
        q.addConstraint(Constraints.neq("Gene.homologues.type", "paralogue"));
        return q;
    }

    private static JSONObject getJSONMine(Map<String, JSONObject> mines, String mineName)
        throws JSONException {
        JSONObject jsonMine = mines.get(mineName);
        if (jsonMine == null) {
            jsonMine = new JSONObject();
            jsonMine.put("mineName", mineName);
            mines.put(mineName, jsonMine);
        }
        return jsonMine;
    }
}
