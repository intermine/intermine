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
    private static final boolean DEBUG = true;
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
    private static final String REMOTE_MAPPING = "remote";
    private static final String LOCAL_MAPPING = "local";

/**
 * @param webProperties the web properties
 */
    public OrthologueLinkManager(Properties webProperties) {

        String localMineName = webProperties.getProperty("project.title");

        localMine = new Mine(localMineName);

        // get list of friendly mines
        mines = readConfig(webProperties, localMineName);
    }

    /**
     * @param webProperties the web properties
     * @return OrthologueLinkManager the link manager
     */
    public static synchronized OrthologueLinkManager getInstance(Properties webProperties) {
        if (orthologueLinkManager == null) {
            orthologueLinkManager = new OrthologueLinkManager(webProperties);
        }
        primeCache();
        return orthologueLinkManager;
    }

    /**
     * if an hour has passed, update data
     */
    public static synchronized void primeCache() {
        long timeSinceLastRefresh = System.currentTimeMillis() - lastCacheRefresh;
        // TODO hardcoded for testing.
        if (timeSinceLastRefresh > ONE_HOUR || DEBUG) {
            // if release version is different, update homologue mappings in cache
            updateMaps();
            lastCacheRefresh = System.currentTimeMillis();
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
                    LOG.error(msg);
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
            LOG.error("Unable to access " + urlString);
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            LOG.error("Unable to access " + urlString);
            e.printStackTrace();
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
        checkLocalOrthologues(mine);
    }

    private static boolean setOrganisms(Mine mine) {

        String mineName = mine.getName();
        LOG.error("querying " + mineName + " for genes");

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
            LOG.error("Unable to access " + mine.getName() + " at " + webserviceURL);
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            LOG.error("Unable to access " + mine.getName() + " at " + webserviceURL);
            e.printStackTrace();
            return false;
        }

        mine.setOrganisms(names);
        return !names.isEmpty();
    }

    private static void setOrthologues(Mine mine) {

        String mineName = mine.getName();
        LOG.error("querying " + mineName + " for orthologues");

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
                    LOG.error(msg);
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
                    datasets[0] = new HashSet();
                    datasets[1] = new HashSet();
                    homologueMapping.put(homologueOrganismName, datasets);
                }
                datasets[1].add(dataset);
            }
        } catch (MalformedURLException e) {
            LOG.error("Unable to access " + mine.getName() + " at " + webserviceURL);
            e.printStackTrace();
            return;
        } catch (IOException e) {
            LOG.error("Unable to access " + mine.getName() + " at " + webserviceURL);
            e.printStackTrace();
            return;
        }
        mine.setOrthologues(orthologues);
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
                localMine.setUrl(url);
                localMine.setLogo(logo);
//                setOrganisms(localMine);
                setOrthologues(localMine);
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

            String mineName = mine.getName();
            LOG.error("testing " + mineName + " for orthologues");

            /* unique list of organisms available for conversion
             * orthologue --> mine (local/remote) --> datasets
             */
            Map<String, Set[]> uniqueOrthologuesToDatasets = new HashMap<String, Set[]>();

            // gene --> orthologue --> datasets
            Map<String, Map<String, Set[]>> geneToOrthologues = mine.getOrthologues();

            if (geneToOrthologues.isEmpty()) {
                LOG.error(mineName + " has no orthologues");
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
                            previousDatasets[0].addAll(datasets[0]);
                            previousDatasets[1].addAll(datasets[1]);
                        }
                    }
                }
            }
            if (isValidMine(mine, uniqueOrthologuesToDatasets)) {
                filteredMines.put(mine, uniqueOrthologuesToDatasets);
            }
        }
        return filteredMines;
    }

    /* add to our list of mines to display on list analysis page.  but only if mine has
    /* orthologues.
     * check that defaults are valid.  if not, pick a random one
     */
    private static boolean isValidMine(Mine mine, Map<String, Set[]> uniqueOrthologuesToDatasets) {

        boolean isValid = false;

        // only add to list if this mine has orthologues
        if (uniqueOrthologuesToDatasets.isEmpty()) {
            return isValid;
        }

        // TODO may not need to validate mappings
        
        String organism = mine.getDefaultOrganismName();
        String mapping = mine.getDefaultMapping();

        Set[] mappings = uniqueOrthologuesToDatasets.get(organism);
        if (mappings == null || mappings.length == 0) {
            // default is invalid, choose another.
            isValid = false;
        } else {
            // valid organism, does it have this mapping?
            int index = (mapping.equals(LOCAL_MAPPING) ? 0 : 1);
            if (mappings[index] == null || mappings[index].isEmpty()) {
                // try the other mapping?
                index = (mapping.equals(REMOTE_MAPPING) ? 0 : 1);
                if (mappings[index] == null || mappings[index].isEmpty()) {
                    // remote && local mappings are empty
                    isValid = false;
                } else {
                    // default organism is valid, mapping is not
                    String validMapping = (mapping.equals(LOCAL_MAPPING)
                            ? REMOTE_MAPPING : LOCAL_MAPPING);
                    mine.setDefaultMapping(validMapping);
                    return true;
                }
            } else {
                // default mapping is valid!
                return true;
            }
        }

        // choose another organism/mapping randomly
        if (!isValid) {
            for (Map.Entry<String, Set[]> entry : uniqueOrthologuesToDatasets.entrySet()) {
                organism = entry.getKey();
                mappings = entry.getValue();
                if (mappings[1] != null && !mappings[1].isEmpty()) {
                    mine.setDefaultMapping(REMOTE_MAPPING);
                    mine.setDefaultOrganismName(organism);
                    return true;
                } else if (mappings[0] != null && !mappings[0].isEmpty()) {
                    mine.setDefaultMapping(LOCAL_MAPPING);
                    mine.setDefaultOrganismName(organism);
                    return true;
                }
                // keep going until we find an organism with some data
                continue;
            }
        }
        // shouldn't reach here 
        return isValid;
    }

    /* for all the genes available in the remote mine, see if the local mine has orthologues
    /* if so, we'll convert the genes then post the converted orthologues to the remote mine
     * NB this assumes the remote mine has its orthologues populated already
     */
    private static void checkLocalOrthologues(Mine mine) {

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

        // check each orthologue in the local mine against the list of organisms in the remote mine
        for (Map.Entry<String, Map<String, Set[]>> entry : localOrthologues.entrySet()) {

            String geneOrganismName = entry.getKey();

            // orthologue --> datasets
            Map<String, Set[]> orthologueToDatasets = entry.getValue();

            // for every orthologue.organism in the local mine
            // is there a matching gene.organism in the remote mine?
            for (Map.Entry<String, Set[]> orthologueToDataset : orthologueToDatasets.entrySet()) {

                String orthologueOrganismName = orthologueToDataset.getKey();
                Set datasets = orthologueToDataset.getValue()[0];

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
