package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.util.Util;
import org.intermine.web.displayer.InterMineLinkGenerator;
import org.intermine.webservice.client.results.XMLTableResult;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helper class for intermine links generated on report and lists pages
 *
 * @author Julie Sullivan
 */
public final class FriendlyMineLinkGenerator extends InterMineLinkGenerator
{
    private static final String WEBSERVICE_URL = "/service";
    private static final Logger LOG = Logger.getLogger(FriendlyMineLinkGenerator.class);
    private boolean debug = false;
    private static final String EMPTY = "\"\""; // webservices returns "" as empty
    private static final String QUERY_PATH = "/query/results?size=1000&format=xml&query=";

    /**
     * Constructor
     */
    public FriendlyMineLinkGenerator() {
        super();
    }

    /**
     * Generate a list of genes and orthologues for remote mines.
     *
     * 1. Query local mine for orthologues for genes given.
     * 2. if orthologue found query remote mine for genes
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

        if (StringUtils.isEmpty(mineName) || StringUtils.isEmpty(organismShortName)
                || StringUtils.isEmpty(primaryIdentifier)) {
            return null;
        }

        // FIXME temporarily ignoring lists with more than one organism
        if (organismShortName.contains(",")) {
            return null;
        }

        MultiKey key = new MultiKey(mineName, primaryIdentifier, organismShortName);
        Collection<JSONObject> cachedResults = olm.getLink(key);
        if (cachedResults != null && !debug) {
            return cachedResults;
        }

        Mine mine = olm.getMine(mineName);
        if (mine == null || mine.getReleaseVersion() == null) {
            LOG.info(mineName + " seems to be dead");
            return null;
        }

        Model model = olm.getInterMineAPI().getModel();

        Map<String, Set<String[]>> genes = new HashMap<String, Set<String[]>>();
        try {
            // query for homologues in remote mine
            PathQuery q = getHomologueQuery(model, organismShortName, primaryIdentifier);
            Map<String, Set<String[]>> results = runQuery(mine, q, organismShortName);
            if (results != null && !results.isEmpty()) {
                genes.putAll(results);
            } else {
                // no luck, query local mine
                Map<String, Set<String>> localHomologues = getLocalHomologues(olm,
                        organismShortName, primaryIdentifier);
                for (String remoteMineOrganism : mine.getDefaultValues()) {
                    Set<String> matchingHomologues = localHomologues.get(remoteMineOrganism);
                    if (matchingHomologues != null && !matchingHomologues.isEmpty()) {
                        String identifiers = StringUtil.join(matchingHomologues, ",");
                        // query remote mine for genes found in local mine
                        q = getGeneQuery(model, remoteMineOrganism, identifiers);
                        results = runQuery(mine, q, remoteMineOrganism);
                        if (results != null && !results.isEmpty()) {
                            genes.putAll(results);
                        }
                    }
                }
            }
            q = getGeneQuery(model, organismShortName, primaryIdentifier);
            results = runQuery(mine, q, organismShortName);
            if (results != null && !results.isEmpty()) {
                genes.putAll(results);
            }
        } catch (Exception e) {
            LOG.warn("error generating friendly mine links", e);
            return null;
        }
        Collection<JSONObject> results = resultsToJSON(key, genes);
        olm.addLink(key, results);
        return results;
    }

    // TODO just get JSON back from webservice instead
    private Collection<JSONObject> resultsToJSON(MultiKey key, Map<String, Set<String[]>> results) {
        Collection<JSONObject> organisms = new ArrayList<JSONObject>();
        // now we have a list of orthologues, add to JSON Organism object
        for (Entry<String, Set<String[]>> entry : results.entrySet()) {
            String organismName = entry.getKey();
            Set<String[]> genes = entry.getValue();
            JSONObject organism = new JSONObject();
            try {
                organism.put("shortName", organismName);
                // used on report pages
                Set<JSONObject> jsonGenes = genesToJSON(genes);
                if (jsonGenes != null && !jsonGenes.isEmpty()) {
                    organism.put("genes", jsonGenes);
                }
                // used on list analysis pages
                List<String> identifiers = identifiersToJSON(genes);
                if (identifiers != null && !identifiers.isEmpty()) {
                    organism.put("identifiers", identifiers);
                }
            } catch (JSONException e) {
                LOG.warn("Problem reading JSON", e);
                return null;
            }
            organisms.add(organism);
        }
        return organisms;
    }

    private static List<String> identifiersToJSON(Set<String[]> genes)
        throws JSONException {
        List<String> identifiers = new ArrayList<String>();
        for (String[] identifier : genes) {
            identifiers.add(identifier[0]);
        }
        return identifiers;
    }

    private static Set<JSONObject> genesToJSON(Set<String[]> genes)
        throws JSONException {
        Set<JSONObject> jsonGenes = new HashSet<JSONObject>();
        for (String[] identifier : genes) {
            JSONObject gene = new JSONObject();
            gene.put("primaryIdentifier", identifier[0]);
            gene.put("displayIdentifier", identifier[1]);
            jsonGenes.add(gene);
        }
        return jsonGenes;
    }

    /*****************************************************************************************
                GENES
     *****************************************************************************************/

    private static Map<String, Set<String[]>> runQuery(Mine mine, PathQuery q, String organism) {
        Map<String, Set<String[]>> results = new HashMap<String, Set<String[]>>();
        Set<String> mineOrganisms = mine.getDefaultValues();
        try {
            final String webserviceURL = mine.getUrl() + WEBSERVICE_URL + QUERY_PATH
                    + URLEncoder.encode("" + q.toXml(), "UTF-8");
            BufferedReader reader = FriendlyMineQueryRunner.runWebServiceQuery(webserviceURL);
            if (reader == null) {
                LOG.warn(mine.getName() + " could not run query " + webserviceURL);
                return null;
            }
            Iterator<List<String>> table = new XMLTableResult(reader).getIterator();
            while (table.hasNext()) {
                List<String> row = table.next();
                String[] identifiers = new String[2];

                String organismName = row.get(2);
                if (!mineOrganisms.contains(organismName)) {
                    // we only want genes and homologues that are relevant to mine
                    continue;
                }
                final String ident = StringUtils.isBlank(row.get(0)) ? row.get(1) : row.get(0);
                final String symbol = StringUtils.isBlank(row.get(1)) ? row.get(0) : row.get(1);
                identifiers[0] = ident;
                identifiers[1] = symbol;

                Util.addToSetMap(results, organismName, identifiers);
            }
        } catch (Exception e) {
            LOG.error("Unable query " + mine.getName() + " for genes", e);
            return null;
        }
        return results;
    }

    private static PathQuery getGeneQuery(Model model, String organism, String identifier) {
        PathQuery q = new PathQuery(model);
        q.addViews("Gene.primaryIdentifier", "Gene.symbol", "Gene.organism.shortName");
        q.addOrderBy("Gene.symbol", OrderDirection.ASC);
        q.addConstraint(Constraints.lookup("Gene", identifier, organism));
        return q;
    }

    /*****************************************************************************************
        HOMOLOGUES
     *****************************************************************************************/

    // query local mine for orthologues - results cache is handling
    private static Map<String, Set<String>> getLocalHomologues(FriendlyMineManager olm,
            String constraintValue, String identifier) {
        Map<String, Set<String>> organismToHomologues = new HashMap<String, Set<String>>();
        InterMineAPI im = olm.getInterMineAPI();
        ProfileManager profileManager = im.getProfileManager();
        PathQueryExecutor executor = im.getPathQueryExecutor(profileManager.getSuperuserProfile());
        PathQuery q = getHomologueQuery(im.getModel(), constraintValue, identifier);
        if (!q.isValid()) {
            return Collections.emptyMap();
        }
        ExportResultsIterator it = executor.execute(q);
        while (it.hasNext()) {
            List<ResultElement> row = it.next();
            String orthologuePrimaryIdentifier = (String) row.get(0).getField();
            String organismName = (String) row.get(2).getField();
            if (!StringUtils.isEmpty(orthologuePrimaryIdentifier)) {
                Util.addToSetMap(organismToHomologues, organismName, orthologuePrimaryIdentifier);
            }
        }
        return organismToHomologues;
    }

    private static PathQuery getHomologueQuery(Model model, String organism, String identifier) {
        PathQuery q = new PathQuery(model);
        q.addViews("Gene.homologues.homologue.primaryIdentifier",
                "Gene.homologues.homologue.symbol", "Gene.homologues.homologue.organism.shortName");
        q.addOrderBy("Gene.homologues.homologue.organism.shortName", OrderDirection.ASC);
        q.addConstraint(Constraints.lookup("Gene", identifier, organism));
        q.addConstraint(Constraints.neq("Gene.homologues.type", "paralogue"));
        return q;
    }
}
