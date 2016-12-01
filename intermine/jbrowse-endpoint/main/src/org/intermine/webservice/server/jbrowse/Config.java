package org.intermine.webservice.server.jbrowse;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.intermine.webservice.server.jbrowse.Queries.pathQueryToOSQ;
import static org.intermine.webservice.server.jbrowse.Queries.resolveValue;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.jbrowse.util.ArrayFormatter;
import org.intermine.webservice.server.jbrowse.util.NameSpacedProperties;
import org.intermine.webservice.server.jbrowse.util.ObjectFormatter;
import org.intermine.webservice.server.output.Formatter;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;

/**
 * Provide standard JBrowse configuration files (<code>trackList.json</code> and
 * <code>refSeqs.json</code>) that are required to run a JBrowse instance.
 *
 * This service automatically generates the configuration required to run JBrowse for a
 * given domain (usually an Organism), exposing all the reference sequences for which
 * there are data, and providing a description of all the feature tracks.
 *
 * This service requires appropriate values to be configured in the web-properties
 * of a mine. For most biological genomic webservices, the default configuration
 * will suffice, but this can be altered on a per installation basis.
 *
 * See <a href="http://gmod.org/wiki/JBrowse_Configuration_Guide#Other_Dynamically-
 * Servable_Formats">
 * the JBrowse wiki</a> for a guide to the formats of the files generated here.
 *
 * @author Alex Kalderimis
 *
 */
public class Config extends JSONService
{
    /**
     * The domain we are operating within.
     * This will generally refer to an organism, but this can
     * be configured in the property files.
     */
    private String domain = "";

    /** The file we are being asked for. **/
    private String fileName = "";

    /** The data type that represents a reference sequence in this model.
     *  Usually a sub-type of featType **/
    private String refType;

    /** The data type that represents a feature in this model **/
    private String featType;

    /** The path from the feature to the domain **/
    private String domainPath;

    /** The path from the feature to the value used to identify it. **/
    private String identPath;

    /** The path from the feature to its length **/
    private String lengthPath;

    /** The human readable label for the reference sequence track **/
    private String referenceLabel;

    /** The category that reference sequences go in. **/
    private String referenceCat;

    /** The machine-readable key to associate the reference sequence track with. **/
    private String referenceKey;

    /** The identifier for the data set **/
    private String dataset;

    /** The category that feature tracks go in. **/
    private String featureCat;

    /** The tracks that we will serve **/
    private List<Map<String, Object>> tracks;

    /** The base-url of this service **/
    private String baseurl = null;

    /** A reference to the web-app configuration. **/
    WebConfig config;

    /** One of the files we serve: seq/refSeqs.json **/
    private static final String REF_SEQS = "seq/refSeqs.json";
    /** One of the files we serve: trackList.json **/
    private static final String TRACKS = "trackList.json";

    /** Build a new instance with the injected API
     * @param im InterMine API
     * **/
    public Config(InterMineAPI im) {
        super(im);
    }

    /** Get the prefix used to namespace this service **/
    private String getPropertyPrefix() {
        String modelName = im.getModel().getName();
        String prefix = "org.intermine.webservice.server.jbrowse." + modelName;
        return prefix;
    }

    @Override
    protected void initState() {
        super.initState();

        config = InterMineContext.getWebConfig();
        String prefix = getPropertyPrefix();
        Properties namespaced = new NameSpacedProperties(prefix, webProperties);
        refType        = namespaced.getProperty("referenceClass");
        featType       = namespaced.getProperty("featureClass");
        domainPath     = namespaced.getProperty("domain");
        identPath      = namespaced.getProperty("paths.ident");
        lengthPath     = namespaced.getProperty("paths.length", "length");
        referenceLabel = namespaced.getProperty("reference.label");
        referenceCat   = namespaced.getProperty("reference.category");
        featureCat     = namespaced.getProperty("feature.category");
        referenceKey   = namespaced.getProperty("reference.key");

        String pathInfo = StringUtils.defaultString(request.getPathInfo(), "/")
                                     .trim()
                                     .substring(1);
        String[] parts = pathInfo.split("/", 2);

        if (parts.length != 2) {
            throw new ResourceNotFoundException("NOT FOUND");
        }

        domain = parts[0];
        fileName = parts[1];
        dataset = webProperties.getProperty("project.title") + "-" + domain;

        /*
        // Parse any track configuration found.
        final List<String> userConfiguredFeatures = new ArrayList<String>();
        final String tracksRegex = prefix + ".track\\.([^.]+)\\.feature";
        final Pattern p = Pattern.compile(tracksRegex);

        // FIXME: namespaced is broken for iterating through properties
        for (Map.Entry<Object, Object> entry: webProperties.entrySet()) {
            String key = (String)entry.getKey();
            String value = (String)entry.getValue();
            Matcher matcher = p.matcher(key);

            if (matcher.matches()) {
                userConfiguredFeatures.add(value);
            }
        }
        */

        // Parse any track configuration found.
        final Map<String, Map<String, String>> userConfiguredFeatures =
                new HashMap<String, Map<String, String>>();
        final String tracksRegex = prefix + ".track\\.([^.]+)\\.(.+)";
        final Pattern p = Pattern.compile(tracksRegex);

        // FIXME: namespaced is broken for iterating through properties
        for (Map.Entry<Object, Object> entry : webProperties.entrySet()) {
            final String key = (String) entry.getKey();
            final String value = (String) entry.getValue();

            Matcher matcher = p.matcher(key);

            if (matcher.matches()) {
                final String trackName = matcher.group(1);
                final String propertyName = matcher.group(2);

                if (!userConfiguredFeatures.containsKey(trackName)) {
                    userConfiguredFeatures.put(trackName, new HashMap<String, String>());
                }

                userConfiguredFeatures.get(trackName).put(propertyName, value);
            }
        }

        initTracks(userConfiguredFeatures);
    }

    /**
     * Initialise track information.
     *
     * @param userConfiguredFeatureTracks - If given then only these tracks will be displayed.
     *             If empty then all InterMine classes that are descendants of SequenceFeature
     *             (including SequenceFeature) will have a jbrowse track.
     */
    private void initTracks(Map<String, Map<String, String>> userConfiguredFeatures) {
        tracks = new ArrayList<Map<String, Object>>();
        Model m = im.getModel();

        if (userConfiguredFeatures.size() > 0) {
            for (Entry<String, Map<String, String>> entry : userConfiguredFeatures.entrySet()) {
                final Map<String, String> trackProperties = entry.getValue();

                if (!trackProperties.containsKey("class")) {
                    throw new ResourceNotFoundException("Track named " + entry.getKey()
                            + " has no class property");
                }

                final String className = trackProperties.get("class");
                ClassDescriptor fcd = m.getClassDescriptorByName(className);

                if (fcd == null) {
                    throw new ResourceNotFoundException("No class found for user configured track "
                            + className);
                }

                tracks.add(makeFeatureTrack(fcd, trackProperties));
            }
        } else {
            ClassDescriptor fcd = m.getClassDescriptorByName(featType);
            tracks.add(makeFeatureTrack(fcd, null));

            for (ClassDescriptor cd: m.getAllSubs(fcd)) {
                tracks.add(makeFeatureTrack(cd, null));
            }
        }

        tracks.add(makeReferenceTrack());
    }

    /** Get the URL we can give to others to tell them where our resources are. **/
    private String getBaseUrl() {
        if (baseurl == null) {
            String base = webProperties.getProperty("webapp.baseurl");
            String path = webProperties.getProperty("webapp.path");
            baseurl = base + "/" + path + "/service/jbrowse/";
        }
        return baseurl;
    }

    @Override
    protected Output makeJSONOutput(PrintWriter out, String separator) {
        Formatter f;
        if (REF_SEQS.equals(fileName)) {
            f = new ArrayFormatter(); // This should be an array.
        } else {
            f = new ObjectFormatter(); // This should be an object.
        }
        return new StreamedOutput(out, f, separator);
    }

    @Override
    protected void execute() throws ServiceException {
        if (REF_SEQS.equals(fileName)) {
            serveRefSeqs();
        } else if (TRACKS.equals(fileName)) {
            serveTrackList();
        } else {
            throw new ResourceNotFoundException(fileName);
        }
    }

    /**
     * Tell the requester that:
     * <ul>
     *  <li>We can serve features of all the types in our model that are subtypes of featType</li>
     *  <li>We can serve reference data for the reference track.</li>
     *  <li>We can perform name lookup and autocompletion.</li>
     *  <li>Our name</li>
     * </ul>
     */
    private void serveTrackList() {
        addResultEntry("tracks", tracks, true);
        Map<String, Object> nameConf = new HashMap<String, Object>();
        nameConf.put("url", getBaseUrl() + "names/" + domain);
        nameConf.put("type", "REST");
        addResultEntry("names", nameConf, true);
        addResultEntry("dataset_id", dataset, false);
    }

    /**
     * Make a reference track section that of the form:
     * <pre>
     * {
     *   "type": "SequenceTrack",
     *   "storeClass": "JBrowse/Store/SeqFeature/REST",
     *   "category": "Reference",
     *   "label": "FooMine-1234-Seq",
     *   "key": "Reference Sequence",
     *   "baseUrl": "http://www.foo.com/foomine/service/jbrowse/1234",
     *   "query": { "reference": true }
     * }
     * </pre>
     * @return A description of the reference track.
     */
    private Map<String, Object> makeReferenceTrack() {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("type", "SequenceTrack");
        ret.put("storeClass", "JBrowse/Store/SeqFeature/REST");
        ret.put("category", referenceCat);
        ret.put("key", referenceLabel); // In JBrowse "key" means human-readable.
        ret.put("label", dataset + "-" + referenceKey); // and "label" means machine-readable.
        ret.put("baseUrl", getBaseUrl() + domain);
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("reference", true);
        ret.put("query", query);
        return ret;
    }

    /**
     * Make a feature track of the form (for a feature of the type <code>ClassName</code>):
     * <pre>
     * {
     *   "type": "JBrowse/View/Track/CanvasFeatures",
     *   "storeClass": "JBrowse/Store/SeqFeature/REST",
     *   "category": "Features",
     *   "key": "Class Name",
     *   "label": "ClassName",
     *   "baseUrl": "http://www.foo.com/foomine/service/jbrowse/1234",
     *   "autocomplete": "all",
     *   "region_feature_densities": true,
     *   "query": { "type": "ClassName" }
     * }
     * </pre>
     * @param feature The type of feature to build a track for.
     * @param trackProperties User-configured track properties.  If null then defaults are used.
     * @return A representation of the track configuration.
     */
    private Map<String, Object> makeFeatureTrack(ClassDescriptor feature,
            Map<String, String> trackProperties) {

        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("type", "JBrowse/View/Track/CanvasFeatures");
        ret.put("storeClass", "JBrowse/Store/SeqFeature/REST");
        ret.put("category", featureCat);
        // In JBrowse "key" means human-readable.
        ret.put("key", WebUtil.formatClass(feature, config));
        // and "label" means machine-readable.
        ret.put("label", dataset + "-" + feature.getUnqualifiedName());
        ret.put("baseUrl", getBaseUrl() + domain);
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("type", feature.getUnqualifiedName());
        ret.put("query", query);
        ret.put("autocomplete", "all");
        ret.put("region_feature_densities", true);

        if (trackProperties != null) {
            for (Entry<String, String> entry : trackProperties.entrySet()) {
                String key = entry.getKey();
                Map<String, Object> currentMap = ret;

                // Mask out class property
                // this is just used for retrieving the ClassDescriptor and we don't want it
                // passed to jbrowse
                if ("class".equals(key)) {
                    continue;
                }

                String[] keyComponents = key.split("\\.");

                for (int i = 0; i < keyComponents.length; i++) {
                    String keyComponent = keyComponents[i];

                    if (i == keyComponents.length - 1) {
                        currentMap.put(keyComponent, entry.getValue());
                    } else {
                        if (currentMap.containsKey(keyComponent)) {
                            currentMap = (Map<String, Object>) currentMap.get(keyComponent);
                        } else {
                            HashMap<String, Object> newMap = new HashMap<String, Object>();
                            currentMap.put(keyComponent, newMap);
                            currentMap = newMap;
                        }
                    }
                }
            }

            /*
            Map<String, Object> style = new HashMap<String, Object>();
            style.put("color", "red");
            ret.put("style", style);
            */
        }

        return ret;
    }

    /**
     * Tell the requester about all the reference sequences that have data (are of non-zero
     * length) and are in the correct domain.
     */
    private void serveRefSeqs() {
        Model m = im.getModel();
        ClassDescriptor ref = m.getClassDescriptorByName(refType);
        if (ref == null) {
            throw new ResourceNotFoundException("No class called " + ref);
        }
        PathQuery pq = new PathQuery(m);
        pq.addView(refType + ".id");
        pq.addConstraint(Constraints.greaterThan(refType + "." + lengthPath, "0"));
        pq.addConstraint(Constraints.eq(refType + "." + domainPath, domain));

        Query q = pathQueryToOSQ(pq);
        SingletonResults res = im.getObjectStore().executeSingleton(q);
        if (res.size() == 0) {
            throw new ResourceNotFoundException("No " + refType + "s on " + domain);
        }
        Iterator<Object> it = res.iterator();

        while (it.hasNext()) {
            FastPathObject o = (FastPathObject) it.next();
            Map<String, Object> refseq = makeRefSeq(o);
            addResultItem(refseq, it.hasNext());
        }
    }

    /**
     * Build a representation of the reference sequence appropriate for
     * inclusion in <code>refSeqs.json</code>, eg:
     * <pre>
     * {
     *   "name": "chrFoo",
     *   "start": 0,
     *   "end": 12345
     * }
     * @param referenceSequence The object that represents a reference sequence.
     * @return A representation of the configuration for this reference sequence.
     */
    private Map<String, Object> makeRefSeq(FastPathObject referenceSequence) {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("name", resolveValue(referenceSequence, identPath));
        ret.put("start", 0);
        ret.put("end", resolveValue(referenceSequence, lengthPath));
        return ret;
    }
}
