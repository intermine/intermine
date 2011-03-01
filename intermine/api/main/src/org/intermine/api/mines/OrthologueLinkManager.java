package org.intermine.api.mines;

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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
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
 *  4. uses webservice to retrieve release version
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
    private static Map<String, Mine> mines = null;
    private static Mine localMine = null;
    private static final String WEBSERVICE_URL = "/service";
    private static final String RELEASE_VERSION_URL = "/version/release";
    private static final String AVAILABLE_ORGANISMS_URL = "/template/results?"
        + "name=im_available_organisms&size=1000&format=tab";
    private static final String AVAILABLE_HOMOLOGUES_URL = "/template/results?"
        + "name=im_available_homologues&size=1000&format=tab";
    private static final String WEB_SERVICE_CONSTRAINT =
        "constraint1=Gene.primaryIdentifier&op1=eq&value1=*";

/**
 * @param im intermine api
 * @param webProperties the web properties
 */
    public OrthologueLinkManager(InterMineAPI im, Properties webProperties) {
        String localMineName = webProperties.getProperty("project.title");
        localMine = new Mine(localMineName);

        // get list of friendly mines
        mines = readConfig(im, webProperties, localMineName);
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
            primeCache();
        }
        return orthologueLinkManager;
    }

    /**
     * Return a list of Mines listed in config.  Used for intermine links on report pages.
     *
     * @return Collection of all friendly mines listed in config
     */
    public static Collection<Mine> getFriendlyMines() {
        return mines.values();
    }

    /**
     * Returns list of friendly mines that have genes from the organism of interest.  Used for
     * the orthologue links on the gene report page.
     *
     * @param organismName list of organisms from our bag
     * @return the list of valid mines for the given list
     */
    public Set<Mine> getMines(String organismName) {

        // list of mines for genes in our list
        Set<Mine> filteredMines = new HashSet<Mine>();

        // remote mines
        for (Mine mine : mines.values()) {
            if (!mine.hasGenes()) {
                LOG.info(mine.getName() + " has no genes");
                continue;
            }
            if (mine.getOrganisms().contains(organismName)) {
                filteredMines.add(mine);
            }
        }
        return filteredMines;
    }

    /**
     * if an hour has passed, update data
     */
    public static synchronized void primeCache() {
        long timeSinceLastRefresh = System.currentTimeMillis() - lastCacheRefresh;
        if ((timeSinceLastRefresh > ONE_HOUR && !cached) || DEBUG) {
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
                // didn't get a release version this time or last time
                String msg = "Unable to retrieve release version for " + mine.getName();
                LOG.error(msg);
                continue;
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
            LOG.warn("No organisms found for " + mine.getName());
            return;
        }

        // query for which orthologues are available
        setOrthologues(mine);
    }

    // loop through properties and get mines' names, URLs and logos
    private static Map<String, Mine> readConfig(InterMineAPI im, Properties webProperties,
            String localMineName) {
        mines = new HashMap<String, Mine>();
        Properties props = PropertiesUtil.stripStart("intermines",
                PropertiesUtil.getPropertiesStartingWith("intermines", webProperties));

        Enumeration<?> propNames = props.propertyNames();

        while (propNames.hasMoreElements()) {
            String mineId =  (String) propNames.nextElement();
            mineId = mineId.substring(0, mineId.indexOf("."));
            Properties mineProps = PropertiesUtil.stripStart(mineId,
                    PropertiesUtil.getPropertiesStartingWith(mineId, props));

            String mineName = mineProps.getProperty("name");
            String url = mineProps.getProperty("url");
            String logo = mineProps.getProperty("logo");
            String organism = mineProps.getProperty("defaultOrganism");
            String mapping = mineProps.getProperty("defaultMapping");

            if (StringUtils.isEmpty(mineName) || StringUtils.isEmpty(url)
                    || StringUtils.isEmpty(logo)) {
                String msg = "InterMine configured incorrectly in web.properties.  Cannot generate "
                    + " linkouts: " + mineId;
                LOG.error(msg);
                continue;
            }

            if (mineName.equals(localMineName)) {
                if (localMine.getUrl() == null || DEBUG) {
                    localMine.setUrl(url);
                    localMine.setLogo(logo);
                    setLocalOrthologues(im);
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
        Set<String> names = new HashSet<String>();
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

    private static void setLocalOrthologues(InterMineAPI im) {

        Query q = new Query();

        QueryClass qcGene = null;
        QueryClass qcOrganism = null;
        QueryClass qcHomologue = null;
        QueryClass qcHomologueOrganism = null;
        QueryClass qcDataset = null;
        QueryClass qcGeneHomologue = null;

        try {
            qcHomologue = new QueryClass(Class.forName(im.getModel().getPackageName()
                    + ".Homologue"));
            qcGene = new QueryClass(Class.forName(im.getModel().getPackageName()
                    + ".Gene"));
            qcOrganism = new QueryClass(Class.forName(im.getModel().getPackageName()
                    + ".Organism"));
            qcHomologueOrganism = new QueryClass(Class.forName(im.getModel().getPackageName()
                    + ".Organism"));
            qcDataset = new QueryClass(Class.forName(im.getModel().getPackageName()
                    + ".DataSet.class"));
            qcGeneHomologue = new QueryClass(Class.forName(im.getModel().getPackageName()
                    + ".Gene"));
        } catch (ClassNotFoundException e) {
            LOG.info("No orthologues found.", e);
            return;
        }

        QueryField qfGeneOrganismName = new QueryField(qcOrganism, "shortName");
        QueryField qfDataset = new QueryField(qcDataset, "name");
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
        Iterator<?> it = results.iterator();

        // gene --> [homologue|datasets]
        Map<String, Map<String, HomologueMapping>> orthologues
            = new HashMap<String, Map<String, HomologueMapping>>();

        while (it.hasNext()) {

            ResultsRow<?> row = (ResultsRow<?>) it.next();

            String geneOrganismName = (String) row.get(0);
            String dataSet = (String) row.get(1);
            String homologueOrganismName = (String) row.get(2);

            // return a mapping from homologue to datasets
            HomologueMapping homologueMapping = addToMap(orthologues, geneOrganismName,
                    homologueOrganismName);

            // add dataset for this gene.organism + gene.homologue.organism pair
            homologueMapping.addLocalDataSet(dataSet);
        }
        localMine.setOrthologues(orthologues, null);
    }


    private static void setOrthologues(Mine mine) {
        URL url;
        String webserviceURL = null;
        // gene --> [homologue|datasets]
        Map<String, Map<String, HomologueMapping>> geneOrganismToOrthologues
            = new HashMap<String, Map<String, HomologueMapping>>();
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
                String dataSet = bits[1];
                String homologueOrganismName = bits[2];

                HomologueMapping homologueMapping = addToMap(geneOrganismToOrthologues,
                        geneOrganismName, homologueOrganismName);

                // add dataset for this gene.organism + gene.homologue.organism pair
                homologueMapping.addRemoteDataSet(dataSet);
            }
        } catch (MalformedURLException e) {
            LOG.info("Unable to access " + mine.getName() + " at " + webserviceURL);
        } catch (IOException e) {
            LOG.info("Unable to access " + mine.getName() + " at " + webserviceURL);
        }
        // adds orthologues for this remote mine
        // merging with any matching orthologues in the local mine
        mine.setOrthologues(geneOrganismToOrthologues, localMine);
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
    public Map<Mine, Map<String, HomologueMapping>> getMines(Collection<String> organismNames) {

        // list of mines and orthologues for genes in our list
        // mines without relevant orthologues are discarded
        Map<Mine, Map<String, HomologueMapping>> filteredMines
            = new HashMap<Mine, Map<String, HomologueMapping>>();

        // remote mines
        for (Mine mine : mines.values()) {

            if (!mine.hasGenes()) {
                LOG.info(mine.getName() + " has no genes");
                continue;
            }

            // return all orthologues for bag.type()
            Map<String, HomologueMapping> homologuesForList
                = mine.getRelevantHomologues(organismNames);

            // only add to list if this mine has orthologues for genes in this list
            if (homologuesForList != null  && !homologuesForList.isEmpty()) {
                filteredMines.put(mine, homologuesForList);
            }
        }
        return filteredMines;
    }


    /*
    * gene   --> homologue      -- > datasets
    *
    * D. rerio | H. sapiens        |   treefam
    *          |                   |   inparanoid
    *          | C. elegans        |   treefam
    */
    private static HomologueMapping addToMap(Map<String, Map<String, HomologueMapping>>
        orthologues, String geneOrganismName, String homologueOrganismName) {
        // gene.organism --> gene.homologue.organism
        Map<String, HomologueMapping> homologuesToDataSets = orthologues.get(geneOrganismName);

        // if we haven't seen this gene.organism before
        if (homologuesToDataSets == null) {
            homologuesToDataSets = new HashMap<String, HomologueMapping>();
            orthologues.put(geneOrganismName, homologuesToDataSets);
        }

        // homologue --> datasets
        HomologueMapping homologueMapping = homologuesToDataSets.get(homologueOrganismName);

        // if we haven't seen this gene.homologue.organism;
        if (homologueMapping == null) {
            homologueMapping = new HomologueMapping(homologueOrganismName);
            homologuesToDataSets.put(homologueOrganismName, homologueMapping);
        }
        return homologueMapping;
    }
}
