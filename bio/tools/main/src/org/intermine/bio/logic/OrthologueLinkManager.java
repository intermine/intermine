package org.intermine.bio.logic;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.util.PropertiesUtil;

/**
 * Class to manage orthologue linkouts to other intermines on the list analysis page
 *
 *  1. works out friendly mines
 *  2. for friendly mines and local mine, runs two queries:
 *     a. which genes for which organisms are available to query
 *     b. which organisms and datasets for orthologues are available
 *  3. Cache the results of these two queries and update every day/hour
 *  4. uses webservice to retrieve releasve version
 * @author Julie Sullivan
 */
public class OrthologueLinkManager
{
    private static final boolean DEBUG = false;
    private static boolean cached = false;
    private static OrthologueLinkManager orthologueLinkManager = null;
    private static long lastCacheRefresh = 0;
    private static final long ONE_HOUR = 3600000;
    private static final Logger LOG = Logger.getLogger(OrthologueLinkManager.class);
    static Map<String, Mine> mines = null;
    private static Mine localMine = null;
    private static final String WEBSERVICE_URL = "/service";
    private static final String RELEASE_VERSION_URL = "/version/release";
    private static final String AVAILABLE_ORGANISMS_URL = "/template/results?"
        + "name=im_available_organisms&size=1000&format=tab";
    private static final String AVAILABLE_HOMOLOGUES_URL = "/template/results?"
        + "name=im_available_homologues&size=1000&format=tab";
    private static final String WEB_SERVICE_CONSTRAINT =
        "constraint1=Gene.primaryIdentifier&op1=eq&value1=*";
    private static InterMineAPI im = null;

/**
 * @param im intermine api
 * @param webProperties the web properties
 */
    public OrthologueLinkManager(InterMineAPI im, Properties webProperties) {
        this.im = im;
        String localMineName = webProperties.getProperty("project.title");

        localMine = new Mine(localMineName);

        // get list of friendly mines
        mines = readConfig(webProperties, localMineName);
    }

    /**
     * @param im intermine api
     * @param webProperties the web properties
     * @return OrthologueLinkManager the link manager
     */
    public static synchronized OrthologueLinkManager getInstance(InterMineAPI im,
            Properties webProperties) {
        if (orthologueLinkManager == null || DEBUG) {
            orthologueLinkManager = new OrthologueLinkManager(im, webProperties);
        }
        primeCache();
        return orthologueLinkManager;
    }

    /**
     * if an hour has passed, update data
     */
    public static synchronized void primeCache() {
        long timeSinceLastRefresh = System.currentTimeMillis() - lastCacheRefresh;
        if (timeSinceLastRefresh > ONE_HOUR && !cached) {
            lastCacheRefresh = System.currentTimeMillis();
            cached = true;
            updateMaps();
        }
    }

    /**
     * get release version number for each mine.  if release number is different from the one
     * we have locally, run queries to populate homologue maps
     */
    private static void updateMaps() {
        for (Mine mine : mines.values()) {
            String currentReleaseVersion = mine.getReleaseVersion();
            String url = mine.getUrl() + WEBSERVICE_URL + RELEASE_VERSION_URL;
            BufferedReader reader = runWebServiceQuery(url);
            String newReleaseVersion = null;
            try {
                if (reader != null) {
                    newReleaseVersion = reader.readLine();
                } else {
                    String msg = "Unable to retrieve release version for " + mine.getName();
                    LOG.info(msg);
                    continue;
                }
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            if (StringUtils.isEmpty(newReleaseVersion)
                    && StringUtils.isEmpty(currentReleaseVersion)) {

                // FIXME remove when 0.93 is released
                // for now we are going to ignore the fact we didn't get a release version
                // versioning was added in 0.93, which some don't have yet
                currentReleaseVersion = mine.getName();

                // didn't get a release version this time or last time
//                String msg = "Unable to retrieve release version for " + mine.getName();
//                LOG.error(msg);
//                continue;
            }

            // if release version is different
            if (StringUtils.isEmpty(newReleaseVersion)
                    || StringUtils.isEmpty(currentReleaseVersion)
                    || !newReleaseVersion.equals(currentReleaseVersion)
                    || DEBUG) {

                // update release version
                mine.setReleaseVersion(newReleaseVersion);

                // update orthologues
                updateRemoteMines(mine);
            }
        }
    }

    private static BufferedReader runWebServiceQuery(String urlString) {
        try {
            URL url = new URL(urlString);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            return reader;
        } catch (MalformedURLException e) {
            LOG.info("Unable to access " + urlString);
            return null;
        } catch (IOException e) {
            LOG.info("Unable to access " + urlString);
            return null;
        }
    }

    private static void updateRemoteMines(Mine mine) {

        // query for which organisms are available;
        if (!setOrganisms(mine)) {
            return;
        }

        // query for which orthologues are available
        setOrthologues(mine);

        // check if local mine has orthologues for genes in this remote mine
        // has to be done last so we know which genes to check for
        getLocalOrthologues(mine);
    }

    // loop through properties and get mines' names, URLs and logos
    private static Map<String, Mine> readConfig(Properties webProperties, String localMineName) {

        mines = new HashMap();

        Properties props = PropertiesUtil.getPropertiesStartingWith("intermines", webProperties);
        Iterator<Object> propIter = props.keySet().iterator();
        while (propIter.hasNext()) {

            // intermine.flymine.url
            String key = (String) propIter.next();
            String[] bits = key.split("[\\.]+");

            if (bits.length != 3) {
                String msg = "InterMine configured incorrectly in web.properties.  Cannot generate "
                    + " linkouts: " + key;
                LOG.error(msg);
                continue;
            }

            String mineId = bits[1];
            String mineName = webProperties.getProperty("intermines." + mineId + ".name");
            String url = webProperties.getProperty("intermines." + mineId + ".url");
            String logo = webProperties.getProperty("intermines." + mineId + ".logo");
            String organism
                = webProperties.getProperty("intermines." + mineId + ".defaultOrganism");
            String mapping = webProperties.getProperty("intermines." + mineId + ".defaultMapping");

            if (StringUtils.isEmpty(mineName) || StringUtils.isEmpty(url)
                    || StringUtils.isEmpty(logo)) {
                String msg = "InterMine configured incorrectly in web.properties.  Cannot generate "
                    + " linkouts: " + key;
                LOG.error(msg);
                continue;
            }

            if (mineName.equals(localMineName)) {
                if (localMine.getUrl() == null) {
                    localMine.setUrl(url);
                    localMine.setLogo(logo);
                    setLocalOrthologues();
                }
                // skip, this is the local intermine.
                continue;
            }

            Mine mine = mines.get(mineId);
            if (mine == null) {
                mine = new Mine(mineName);
                mine.setUrl(url);
                mine.setLogo(logo);
                mine.setDefaultOrganismName(organism);
                mine.setDefaultMapping(mapping);
                mines.put(mineId, mine);
            }
        }
        return mines;
    }

    private static boolean setOrganisms(Mine mine) {
        Set<String> names = new HashSet();
        String webserviceURL = null;
        URL url;
        try {
            webserviceURL = mine.getUrl() + WEBSERVICE_URL + AVAILABLE_ORGANISMS_URL + "&"
                + WEB_SERVICE_CONSTRAINT;
            url = new URL(webserviceURL);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                names.add(line);
            }
        } catch (MalformedURLException e) {
            LOG.info("Unable to access " + mine.getName() + " at " + webserviceURL);
            return false;
        } catch (IOException e) {
            LOG.info("Unable to access " + mine.getName() + " at " + webserviceURL);
            return false;
        }

        mine.setOrganisms(names);
        return !names.isEmpty();
    }

    private static void setLocalOrthologues() {

            Map<String, Map<String, Set[]>> orthologues = new HashMap();

            Query q = new Query();

            QueryClass qcGene = new QueryClass(Gene.class);
            QueryClass qcOrganism = new QueryClass(Organism.class);
            QueryClass qcHomologue = null;
            QueryClass qcHomologueOrganism = new QueryClass(Organism.class);
            QueryClass qcDataset = new QueryClass(DataSet.class);
            QueryClass qcGeneHomologue = new QueryClass(Gene.class);

            try {
                qcHomologue = new QueryClass(Class.forName(im.getModel().getPackageName()
                        + ".Homologue"));
            } catch (ClassNotFoundException e) {
                LOG.info("No orthologues found.", e);
                return;
            }

            QueryField qfGeneOrganismName = new QueryField(qcOrganism, "shortName");
            QueryField qfDataset = new QueryField(qcDataset, "title");
            QueryField qfHomologueOrganismName = new QueryField(qcHomologueOrganism, "shortName");
            QueryField qfType = new QueryField(qcHomologue, "type");

            q.setDistinct(true);

            q.addToSelect(qfGeneOrganismName);
            q.addToSelect(qfDataset);
            q.addToSelect(qfHomologueOrganismName);

            q.addFrom(qcGene);
            q.addFrom(qcHomologue);
            q.addFrom(qcOrganism);
            q.addFrom(qcHomologueOrganism);
            q.addFrom(qcDataset);
            q.addFrom(qcGeneHomologue);

            ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

            // gene.organism.name
            QueryObjectReference c1 = new QueryObjectReference(qcGene, "organism");
            cs.addConstraint(new ContainsConstraint(c1, ConstraintOp.CONTAINS, qcOrganism));

            // gene.homologues.homologue
            QueryCollectionReference c2 = new QueryCollectionReference(qcGene, "homologues");
            cs.addConstraint(new ContainsConstraint(c2, ConstraintOp.CONTAINS, qcHomologue));

            // gene.homologues.homologue.datasets.title
            QueryCollectionReference c3 = new QueryCollectionReference(qcHomologue, "dataSets");
            cs.addConstraint(new ContainsConstraint(c3, ConstraintOp.CONTAINS, qcDataset));

            // gene.homologues.homologue
            QueryObjectReference c4 = new QueryObjectReference(qcHomologue, "homologue");
            cs.addConstraint(new ContainsConstraint(c4, ConstraintOp.CONTAINS, qcGeneHomologue));

            // gene.homologues.homologue.organism.shortName
            QueryObjectReference c5 = new QueryObjectReference(qcGeneHomologue, "organism");
            cs.addConstraint(new ContainsConstraint(c5, ConstraintOp.CONTAINS,
                    qcHomologueOrganism));
            q.setConstraint(cs);

            // gene.homologues.type = 'orthologue'
            QueryExpression c6 = new QueryExpression(QueryExpression.LOWER, qfType);
            cs.addConstraint(new SimpleConstraint(c6, ConstraintOp.EQUALS,
                    new QueryValue("orthologue")));

            q.addToOrderBy(qfGeneOrganismName);

            Results results = im.getObjectStore().execute(q);
            Iterator it = results.iterator();
            while (it.hasNext()) {

                ResultsRow row = (ResultsRow) it.next();

                String geneOrganismName = (String) row.get(0);
                String dataset = (String) row.get(1);
                String homologueOrganismName = (String) row.get(2);

                /**
                 * gene --> homologue --> datasets
                 *
                 * D. rerio | H. sapiens        |   treefam
                 *          |                   |   inparanoid
                 *          | C. elegans        |   treefam
                 */

                // gene --> homologue|dataset
                Map<String, Set[]> homologueMapping = orthologues.get(geneOrganismName);
                if (homologueMapping == null) {
                    homologueMapping = new HashMap();
                    orthologues.put(geneOrganismName, homologueMapping);
                }

                // homologue --> datasets
                Set[] datasets = homologueMapping.get(homologueOrganismName);
                if (datasets == null) {
                    datasets = new HashSet[2];
                    datasets[1] = new HashSet();
                    homologueMapping.put(homologueOrganismName, datasets);
                }
                datasets[1].add(dataset);
            }
            localMine.setOrthologues(orthologues);
    }

    private static void setOrthologues(Mine mine) {
        Map<String, Map<String, Set[]>> orthologues = new HashMap();
        URL url;
        String webserviceURL = null;
        try {
            webserviceURL = mine.getUrl() + WEBSERVICE_URL + AVAILABLE_HOMOLOGUES_URL + "&"
            + WEB_SERVICE_CONSTRAINT;
            url = new URL(webserviceURL);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] bits = line.split("\\t");
                if (bits.length != 3) {
                    String msg = "Couldn't process orthologue links for " + mine.getName()
                    + ".  Expected three columns, found " + bits.length + " columns instead."
                    + webserviceURL;
                    LOG.info(msg);
                    return;
                }
                String geneOrganismName = bits[0];
                String dataset = bits[1];
                String homologueOrganismName = bits[2];

                /**
                 * gene --> homologue --> datasets
                 *
                 * D. rerio | H. sapiens        |   treefam
                 *          |                   |   inparanoid
                 *          | C. elegans        |   treefam
                 */

                // gene --> homologue|dataset
                Map<String, Set[]> homologueMapping = orthologues.get(geneOrganismName);
                if (homologueMapping == null) {
                    homologueMapping = new HashMap();
                    orthologues.put(geneOrganismName, homologueMapping);
                }

                // homologue --> datasets
                Set[] datasets = homologueMapping.get(homologueOrganismName);
                if (datasets == null) {
                    datasets = new HashSet[2];
                    datasets[1] = new HashSet();
                    homologueMapping.put(homologueOrganismName, datasets);
                }
                datasets[1].add(dataset);
            }
        } catch (MalformedURLException e) {
            LOG.info("Unable to access " + mine.getName() + " at " + webserviceURL);
            return;
        } catch (IOException e) {
            LOG.info("Unable to access " + mine.getName() + " at " + webserviceURL);
            return;
        }
        mine.setOrthologues(orthologues);
    }

    /**
     * check each remote mine to see:
     *      a. does remote mine have orthologues for genes in list
     *      b. does local intermine have orthologues for genes in the list that correspond
     *         to genes in remote mine
     *
     *
     * for genes from our list
     * mine --> gene.homologue -- [0] local mine dataset(s)
     *                         -- [1] remote mine dataset(s)
     * @param organismNames list of organisms from our bag
     * @return the list of valid mines for the given list
     */
    public static Map<Mine, Map<String, Set[]>> getMines(Collection<String> organismNames) {

        // list of mines and orthologues for genes in our list
        // mines without relevant orthologues are discarded
        Map<Mine, Map<String, Set[]>> filteredMines = new HashMap();

        // remote mines
        for (Mine mine : mines.values()) {
            /* unique list of organisms available for conversion
             * orthologue --> mine (local/remote) --> datasets
             */
            Map<String, Set[]> uniqueOrthologuesToDatasets = new HashMap<String, Set[]>();

            // gene --> orthologue --> datasets
            Map<String, Map<String, Set[]>> geneToOrthologues = mine.getOrthologues();

            if (geneToOrthologues.isEmpty()) {
                LOG.info(mine.getName() + " has no orthologues");
                continue;
            }

            for (Map.Entry<String, Map<String, Set[]>> entry : geneToOrthologues.entrySet()) {

                String geneOrganismName = entry.getKey();
                Map<String, Set[]> orthologuesToDatasets = entry.getValue();

                // organism is in user's bag
                if (organismNames.contains(geneOrganismName)) {

                    // gene is in bag, so add all orthologues to our list to be displayed on
                    // list analysis page
                    for (Map.Entry<String, Set[]> orthologueEntry
                            : orthologuesToDatasets.entrySet()) {

                        String orthologueOrganismName = orthologueEntry.getKey();
                        Set[] datasets = orthologueEntry.getValue();

                        if (uniqueOrthologuesToDatasets.get(orthologueOrganismName) == null) {
                            uniqueOrthologuesToDatasets.put(orthologueOrganismName, datasets);
                        } else {
                            // this list has more than one organism
                            // these orthologues have been added previously, just merge datasets
                            Set[] previousDatasets
                            = uniqueOrthologuesToDatasets.get(orthologueOrganismName);
                            for (int i = 0; i <= 1; i++) {
                                if (datasets[i] != null && !datasets[i].isEmpty()) {
                                    if (previousDatasets[i] == null) {
                                        previousDatasets[i] = new HashSet();
                                    }
                                    previousDatasets[i].addAll(datasets[i]);
                                }
                            }
                        }
                    }
                }
            }
            // only add to list if this mine has orthologues
            if (!uniqueOrthologuesToDatasets.isEmpty()) {
                filteredMines.put(mine, uniqueOrthologuesToDatasets);
            }
        }
        return filteredMines;
    }

    /* for all the genes available in the remote mine, see if the local mine has orthologues
    /* if so, we'll convert the genes then post the converted orthologues to the remote mine
     * NB this assumes the remote mine has its orthologues populated already
     */
    private static void getLocalOrthologues(Mine mine) {

        // list of organisms for which this mine has genes.
        // does the local mine have orthologues?
        // gene.orthologue.organismName [local mine] = gene.organismName [remote mine]
        Set<String> organismNames = mine.getOrganisms();

        // gene --> gene.orthologue --> datasets for current intermine
        Map<String, Map<String, Set[]>> localOrthologues = localMine.getOrthologues();

        // no local orthologues
        if (localOrthologues.isEmpty()) {
            return;
        }

        // gene --> orthologue --> datasets
        // check each orthologue in the local mine against the list of organisms in the remote mine
        for (Map.Entry<String, Map<String, Set[]>> entry : localOrthologues.entrySet()) {

            String geneOrganismName = entry.getKey();

            // orthologue --> datasets
            Map<String, Set[]> orthologueToDatasets = entry.getValue();

            // for every orthologue.organism in the local mine
            // is there a matching gene.organism in the remote mine?
            for (Map.Entry<String, Set[]> orthologueToDataset : orthologueToDatasets.entrySet()) {

                String orthologueOrganismName = orthologueToDataset.getKey();
                Set datasets = orthologueToDataset.getValue()[1];

                // do we have a corresponding gene in the remote mine?
                if (organismNames.contains(orthologueOrganismName)) {
                    addLocalOrthologues(mine, geneOrganismName, orthologueOrganismName, datasets);
                }
            }
        }
        return;
    }

    private static void addLocalOrthologues(Mine mine, String geneOrganismName,
            String orthologueOrganismName, Set localDatasets) {

        // gene --> gene.orthologue --> datasets
        Map<String, Map<String, Set[]>> orthologues = mine.getOrthologues();
        Map<String, Set[]> datasetsToOrthos = orthologues.get(geneOrganismName);

        Set[] remoteAndLocalDatasets = null;

        // NO ORTHOLOGUES
        if (datasetsToOrthos == null) {
            remoteAndLocalDatasets = new HashSet[2];
            remoteAndLocalDatasets[0] = localDatasets;
            datasetsToOrthos = new HashMap();
            datasetsToOrthos.put(orthologueOrganismName, remoteAndLocalDatasets);
            orthologues.put(geneOrganismName, datasetsToOrthos);
        } else {
            // ORTHOLOGUES, BUT NOT FOR THIS ORGANISM
            if (datasetsToOrthos.get(orthologueOrganismName) == null) {
                remoteAndLocalDatasets = new HashSet[2];
                remoteAndLocalDatasets[0] = localDatasets;
                datasetsToOrthos.put(orthologueOrganismName, remoteAndLocalDatasets);
            // ORTHOLOGUES FOR THIS ORGANISM
            // add local orthologues, remote orthologues were added previously
            } else {
                datasetsToOrthos.get(orthologueOrganismName)[0] = localDatasets;
            }
        }
    }
}
