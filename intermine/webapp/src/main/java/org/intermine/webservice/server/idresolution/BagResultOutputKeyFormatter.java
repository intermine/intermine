package org.intermine.webservice.server.idresolution;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.ConvertedObjectPair;
import org.intermine.api.idresolution.Job;
import org.intermine.api.util.PathUtil;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.util.DynamicUtil;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;

/**
 * Format a bag result grouping by matched object.
 * @author Alex Kalderimis
 */
public class BagResultOutputKeyFormatter implements BagResultFormatter
{

    private static final Logger LOG = Logger.getLogger(BagResultOutputKeyFormatter.class);

    private final InterMineAPI im;

    /** @param api The InterMine state object **/
    public BagResultOutputKeyFormatter(InterMineAPI api) {
        this.im = api;
    }

    @Override
    public Map<String, Object> format(Job job) {
        final BagQueryResult bqr = job.getResult();
        final Map<String, Object> ret = new HashMap<String, Object>();

        doMatches(ret, bqr);
        doDuplicates(ret, bqr, BagQueryResult.DUPLICATE);
        doDuplicates(ret, bqr, BagQueryResult.WILDCARD);
        doDuplicates(ret, bqr, BagQueryResult.OTHER);
        doDuplicates(ret, bqr, BagQueryResult.TYPE_CONVERTED);

        return ret;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void doDuplicates(final Map<String, Object> ret, BagQueryResult bqr, String key) {
        Map<String, Map<String, List>> issues = bqr.getIssues().get(key);
        if (issues == null) {
            return;
        }
        for (Map<String, List> issueSet: issues.values()) {
            for (Entry<String, List> identToObjects: issueSet.entrySet()) {
                String ident = identToObjects.getKey();
                for (Object o: identToObjects.getValue()) {
                    InterMineObject imo;
                    Map<String, Object> resultItem;
                    if (o instanceof Integer) {
                        try {
                            imo = im.getObjectStore().getObjectById((Integer) o);
                        } catch (ObjectStoreException e) {
                            throw new IllegalStateException(
                                    "Could not retrieve object reported as match", e);
                        }
                    } else if (o instanceof ConvertedObjectPair) {
                        imo = ((ConvertedObjectPair) o).getNewObject();
                    } else {
                        imo = (InterMineObject) o;
                    }
                    String idKey = String.valueOf(imo.getId());
                    if (ret.containsKey(idKey)) {
                        resultItem = (Map<String, Object>) ret.get(idKey);
                    } else {
                        resultItem = new HashMap<String, Object>();
                        resultItem.put("identifiers", new HashMap<String, Object>());
                    }
                    if (!resultItem.containsKey("summary")) {
                        resultItem.put("summary", getObjectDetails(imo));
                    }
                    Map<String, Object> identifiers =
                            (Map<String, Object>) resultItem.get("identifiers");

                    if (!identifiers.containsKey(ident)) {
                        identifiers.put(ident, new HashSet<String>());
                    }
                    Set<String> categories = (Set<String>) identifiers.get(ident);
                    categories.add(key);
                    String className = DynamicUtil.getSimpleClassName(imo.getClass());
                    resultItem.put("type", className.replaceAll("^.*\\.", ""));
                    ret.put(idKey, resultItem);
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void doMatches(Map<String, Object> ret, BagQueryResult bqr) {
        for (Entry<Integer, List> pair: bqr.getMatches().entrySet()) {
            Map<String, Object> resultItem;
            InterMineObject imo;
            try {
                imo = im.getObjectStore().getObjectById(pair.getKey());
            } catch (ObjectStoreException e) {
                throw new IllegalStateException("Could not retrieve object reported as match", e);
            }
            String idKey = String.valueOf(imo.getId());
            if (ret.containsKey(idKey)) {
                resultItem = (Map<String, Object>) ret.get(idKey);
            } else {
                resultItem = new HashMap<String, Object>();
                resultItem.put("identifiers", new HashMap<String, Object>());
            }
            if (!resultItem.containsKey("summary")) {
                resultItem.put("summary", getObjectDetails(imo));
            }
            Map<String, Object> identifiers = (Map<String, Object>) resultItem.get("identifiers");
            for (Object o: pair.getValue()) {
                String ident = (String) o;
                if (!identifiers.containsKey(ident)) {
                    identifiers.put(ident, new HashSet<String>());
                }
                Set<String> categories = (Set<String>) identifiers.get(ident);
                categories.add("MATCH");
            }
            String className = DynamicUtil.getSimpleClassName(imo.getClass());
            resultItem.put("type", className.replaceAll("^.*\\.", ""));
            ret.put(idKey, resultItem);
        }
    }

    private Map<String, Object> getObjectDetails(InterMineObject imo) {
        WebConfig webConfig = InterMineContext.getWebConfig();
        Model m = im.getModel();
        Map<String, Object> objectDetails = new HashMap<String, Object>();
        String className = DynamicUtil.getSimpleClassName(imo.getClass());
        ClassDescriptor cd = m.getClassDescriptorByName(className);
        for (FieldConfig fc : FieldConfigHelper.getClassFieldConfigs(webConfig, cd)) {
            try {
                Path p = new Path(m, cd.getUnqualifiedName() + "." + fc.getFieldExpr());
                if (p.endIsAttribute() && fc.getShowInSummary()) {
                    objectDetails.put(
                            p.getNoConstraintsString().replaceAll("^[^.]*\\.", ""),
                            PathUtil.resolvePath(p, imo));
                }
            } catch (PathException e) {
                LOG.error(e);
            }
        }
        return objectDetails;
    }


}
