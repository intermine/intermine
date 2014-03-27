package org.intermine.webservice.server.jbrowse;

import static org.intermine.webservice.server.jbrowse.Queries.pathQueryToOSQ;
import static org.intermine.webservice.server.jbrowse.Queries.resolveValue;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.intermine.webservice.server.core.Pair;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.jbrowse.util.ArrayFormatter;
import org.intermine.webservice.server.jbrowse.util.ObjectFormatter;
import org.intermine.webservice.server.output.Formatter;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;

public class Config extends JSONService {

    private String domain = "", fileName = "";

    private String refType;

    private String domainPath;

    private String identPath;

    private String lengthPath;

    private String featType;

    private String referenceLabel;

    private String referenceCat;

    private String referenceKey;

    private String dataset;

    private String featureCat;
    WebConfig config; 

    private static final String REF_SEQS = "seq/refSeqs.json";
    private static final String TRACKS = "trackList.json";

    public Config(InterMineAPI im) {
        super(im);
    }

    private String getPropertyPrefix() {
        String modelName = im.getModel().getName();
        String prefix = "org.intermine.webservice.server.jbrowse." + modelName + ".";
        return prefix;
    }
    
    @Override
    protected void initState() {
        super.initState();
        config = InterMineContext.getWebConfig();
        String prefix = getPropertyPrefix();
        refType = webProperties.getProperty(prefix + "referenceClass");
        featType = webProperties.getProperty(prefix + "featureClass");
        domainPath = webProperties.getProperty(prefix + "domain");
        identPath = webProperties.getProperty(prefix + "paths.ident");
        lengthPath = webProperties.getProperty(prefix + "paths.length", "length");
        referenceLabel = webProperties.getProperty(prefix + "reference.label");
        referenceCat = webProperties.getProperty(prefix + "reference.category");
        featureCat = webProperties.getProperty(prefix + "feature.category");
        referenceKey = webProperties.getProperty(prefix + "reference.key");

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
    }

    private String baseurl = null;

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
            f = new ArrayFormatter();
        } else {
            f = new ObjectFormatter();
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

    private void serveTrackList() {
        Model m = im.getModel();
        List<Map<String, Object>> tracks = new ArrayList<Map<String, Object>>();
        ClassDescriptor fcd = m.getClassDescriptorByName(featType);
        tracks.add(featureTrack(fcd));
        for (ClassDescriptor cd: m.getAllSubs(fcd)) {
            tracks.add(featureTrack(cd));
        }
        tracks.add(referenceTrack());
        addResultEntry("tracks", tracks, true);
        Map<String, Object> nameConf = new HashMap<String, Object>();
        nameConf.put("url", getBaseUrl() + "names/" + domain);
        nameConf.put("type", "REST");
        addResultEntry("names", nameConf, true);
        addResultEntry("dataset_id", dataset, false);
    }

    private Map<String, Object> referenceTrack() {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("type", "SequenceTrack");
        ret.put("storeClass", "JBrowse/Store/SeqFeature/REST");
        ret.put("category", referenceCat);
        ret.put("key", referenceLabel); // In JBrowse "key" means human-readable.
        ret.put("label", dataset + referenceKey); // and "label" means machine-readable.
        ret.put("baseUrl", getBaseUrl() + domain);
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("reference", true);
        ret.put("query", query);
        return ret;
    }

    private Map<String, Object> featureTrack(ClassDescriptor fcd) {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("type", "JBrowse/View/Track/CanvasFeatures");
        ret.put("storeClass", "JBrowse/Store/SeqFeature/REST");
        ret.put("category", featureCat);
        ret.put("key", WebUtil.formatClass(fcd, config)); // In JBrowse "key" means human-readable.
        ret.put("label", dataset + "-" + fcd.getUnqualifiedName()); // and "label" means machine-readable.
        ret.put("baseUrl", getBaseUrl() + domain);
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("type", fcd.getUnqualifiedName());
        ret.put("query", query);
        ret.put("autocomplete", "all");
        ret.put("region_feature_densities", true);
        return ret;
    }

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

    private Map<String, Object> makeRefSeq(FastPathObject o) {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("name", resolveValue(o, identPath));
        ret.put("start", 0);
        ret.put("end", resolveValue(o, lengthPath));
        return ret;
    }

    

}
