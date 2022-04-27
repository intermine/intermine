package org.intermine.bio.webservice;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.TypeUtil;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ServiceException;

import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* bag (and xref) related imports, not used yet
import java.util.LinkedHashMap;
import java.net.MalformedURLException;
import org.intermine.web.util.AttributeLinkURL;
import org.intermine.web.logic.bag.BagHelper;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.util.DynamicUtil;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.util.PathUtil;
import org.intermine.bio.util.BioUtil;
import org.intermine.bio.web.XRef;
*/

import static org.intermine.web.context.InterMineContext.getInterMineAPI;

/**
 * Exports selected web.properties.
 *
 * @author sc
 */
public class ExternalLinksService extends JSONService
{
    protected static final Logger LOG = Logger.getLogger(ExternalLinksService.class);

    /** @param im The InterMine state object. **/
    public ExternalLinksService(InterMineAPI im) {
        super(im);
    }

    static final String ATTR_MARKER_RE = "<<attributeValue>>";
    static final String JSON_KEY = "links";

    @SuppressWarnings("serial")
    private class ConfigMap extends HashMap<String, Object>
    {
        // empty
    }

    @Override
    protected void execute() throws Exception {
//        InterMineBag bag = (InterMineBag) request.getAttribute("bag");
        ReportObject reportObject = null;
        InterMineObject imo = null;
// if (bag == null) {
//     reportObject =
//     (ReportObject) request.getServletContext().getAttribute("reportObject");
//     imo = reportObject.getObject();
// }
        InterMineAPI im = getInterMineAPI();
        ObjectStore os = im.getObjectStore();
        Model model = im.getModel();

        Integer interMineID = Integer.valueOf(request.getParameter("id"));
        imo = im.getObjectStore().getObjectById(interMineID);
        if (imo == null) {
            // returns error (and empty string, could be an empty map if better)
            addResultEntry(JSON_KEY, StringUtils.EMPTY, false);
            throw new ServiceException("no object with ID " + interMineID, 400);
        } else { // remove this clause when dealing also with lists
            Set<ClassDescriptor> classDescriptors;
//        if (imo == null) {
//            classDescriptors = bag.getClassDescriptors();
//        } else {
            classDescriptors = model.getClassDescriptorsForClass(imo.getClass());
//        }
            StringBuffer sb = new StringBuffer();
            for (ClassDescriptor cd : classDescriptors) {
                if (sb.length() <= 0) {
                    sb.append("(");
                } else {
                    sb.append("|");
                }
                sb.append(TypeUtil.unqualifiedName(cd.getName()));
            }
            StringBuffer append = sb.append(")");

            org.intermine.model.bio.Organism organismReference = null;
            String geneOrgKey = sb.toString();

//            if (imo != null) {
            try {
                organismReference = (Organism) imo.getFieldValue("organism");
            } catch (Exception e) {
                // no organism field
            }
            if (organismReference == null || organismReference.getTaxonId() == null) {
                geneOrgKey += "(\\.(\\*))?";
            } else {
                // checking against * as well in case we want it to work for all taxonIds
                geneOrgKey += "(\\.(" + organismReference.getTaxonId() + "|\\*))?";
            }
//            } else { // bag
//                geneOrgKey += "(\\.(\\*|[\\d]+))?";
//            }
            // map from eg. 'Gene.Drosophila.melanogaster' to map from configName (eg. "flybase")
            // to the configuration
            Map<String, ConfigMap> linkConfigs = new HashMap<String, ConfigMap>();
            Properties webProperties =
                    (Properties) request.getServletContext().getAttribute(Constants.WEB_PROPERTIES);

            final String regexp = "attributelink\\.([^.]+)\\." + geneOrgKey
                    + "\\.([^.]+)(\\.list)?\\"
                    + ".(url|text|imageName|usePost|delimiter|enctype|dataset|useCheckbox)";
            Pattern p = Pattern.compile(regexp);
            String className = null;
            String taxId = null;
            for (Map.Entry<Object, Object> entry : webProperties.entrySet()) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                Matcher matcher = p.matcher(key);
                if (matcher.matches()) {

                    String dbName = matcher.group(1);
                    className = matcher.group(2);
                    taxId = matcher.group(4);
                    String attrName = matcher.group(5);
                    String imType = matcher.group(6);
                    String propType = matcher.group(7);

                    // to pick the right type of link (list or object)
                    if (imo != null && imType != null) {
                        continue;
                    }
//                if (bag != null && imType == null) {
//                    continue;
//                }

                    ConfigMap config;
                    if (linkConfigs.containsKey(dbName)) {
                        config = linkConfigs.get(dbName);
                    } else {
                        config = new ConfigMap();
                        config.put("attributeName", attrName);
                        config.put("linkId", dbName);
                        linkConfigs.put(dbName, config);
                    }
                    Object attrValue = null;
                    Collection<String> taxIds = null;
                    if (config.containsKey("attributeValue")) {
                        attrValue = config.get("attributeValue");
                    } else {
                        try {
                            if (imo != null) {
                                attrValue = imo.getFieldValue(attrName);
                            } else { //it's a bag!
//                            attrValue = BagHelper.getAttributesFromBag(bag, os, dbName, attrName);
//                            if (!"*".equalsIgnoreCase(taxId)) {
//                                taxIds = BioUtil.getOrganisms(os, bag.getType(),
//                                        bag.getContentsAsIds(), false, "taxonId");
//                                //don't display link if
//                                // a) not a bioentity (no reference to organism)
//                                if (taxIds == null) {
//                                    continue;
//                                }
//                                // b) organism not present
//                                if (!taxIds.contains(taxId)) {
//                                    continue;
//                                }
//                            }
                            }
                            if (attrValue != null) {
                                config.put("attributeValue", attrValue);
                                config.put("valid", Boolean.TRUE);
                            }
                        } catch (IllegalAccessException e) {
                            config.put("attributeValue", e);
                            config.put("valid", Boolean.FALSE);
                            LOG.error("configuration problem in ExternalLinksService: "
                                    + "couldn't get a value for field " + attrName
                                    + " in class " + className);
                        }
                    }
                    if ("url".equals(propType)) {
                        if (attrValue != null) {
                            String url;
                            if (value.contains(ATTR_MARKER_RE)) {
                                url = value.replaceAll(ATTR_MARKER_RE, String.valueOf(attrValue));
                            } else {
                                url = value + attrValue;
                            }
                            config.put("url", url);
                        }
                    } else if ("imageName".equals(propType)) {
                        config.put("imageName", value);
                    } else if ("usePost".equals(propType)) {
                        config.put("usePost", value);
                    } else if ("delimiter".equals(propType)) {
                        config.put("delimiter", value);
                    } else if ("enctype".equals(propType)) {
                        config.put("enctype", value);
                    } else if ("dataset".equals(propType)) {
                        config.put("dataset", value);
                    } else if ("useCheckbox".equals(propType)) {
                        config.put("useCheckbox", value);
                    } else if ("text".equals(propType)) {
                        config.put("title", value.replaceAll("[^A-Za-z0-9 ]", "")
                                .replaceFirst("attributeValue", ""));
                        String text = value.replaceAll(ATTR_MARKER_RE, String.valueOf(attrValue));
                        config.put("text", text);
                    }
                }
            }
            // this to change into post if required
            //linkConfigs = processConfigs(im, linkConfigs, reportObject);

            // entry for the JSON response
            addResultEntry(JSON_KEY, linkConfigs, false);

        /*
        // TODO HACKed for Xref
        // Logic:
        // 1.parse xref.properties
        // 2.create a data structure to store the info
        Map<String, XRef> xrefMap = new LinkedHashMap<String, XRef>();
        final String xrefRegExp = "xreflink\\.([^.]+)\\.(url|imageName)";
        Pattern xrefpat = Pattern.compile(xrefRegExp);

        for (Map.Entry<Object, Object> entry: webProperties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();

            Matcher matcher = xrefpat.matcher(key);
            if (matcher.matches()) {

                String sourceName = matcher.group(1);
                String propType = matcher.group(2);

                XRef xref = new XRef();
                xref.setSourceName(sourceName);
                if ("url".equals(propType)) {
                    xref.setUrl(value);
                } else if ("imageName".equals(propType)) {
                    xref.setSourceName(value);
                }

                xrefMap.put(sourceName, xref);
            }
        }
        request.setAttribute("xrefMap", xrefMap);
        */
        }
    }

    /* =========================================================
    // All the methods below should be useful for the bag links
    // ========================================================*/

    /**
     * Process configs. Configs that have specified that POST method
     * should be used when request is submitted to third party site are modified with this method.
     * GET form of url is modified to POST form.
     * @param linkConfigs
     */
/*
    private Map<String, ConfigMap> processConfigs(InterMineAPI im,
                                                  Map<String, ConfigMap> linkConfigs,
                                                  ReportObject reportObject) {
        Map<String, ConfigMap> newMap = new HashMap<String, ConfigMap>(linkConfigs);
        for (Map.Entry<String, ConfigMap> entry : newMap.entrySet()) {
            ConfigMap config = entry.getValue();
            if (config.get("delimiter") != null) {
                modifyIdString(config);
            }
            if (config.get("usePost") != null
                    && ((String) config.get("usePost")).equalsIgnoreCase("true")) {
                modifyConfigToPost(config);
            }
            if (config.get("dataset") != null) {
                String datasetToMatch = (String) config.get("dataset");
                boolean hasValidDataset = false;
                try {
                    hasValidDataset = hasDataset(im, reportObject, datasetToMatch);
                } catch (Exception e) {
                    // no dataset
                }
                if (!hasValidDataset) {
                    linkConfigs.remove(entry.getKey());
                }
            }
        }
        return linkConfigs;
    }

    private static boolean hasDataset(InterMineAPI im, ReportObject reportObject,
                                      String datasetToMatch) throws PathException {
        LOG.info("in hasDataset");
        boolean isValidDataset = false;
        InterMineObject imo = reportObject.getObject();
        Path path = new Path(im.getModel(), DynamicUtil.getSimpleClass(
                imo.getClass()).getSimpleName()
                + ".dataSets");
        Set<Object> listOfListObjects = PathUtil.resolveCollectionPath(path, imo);
        for (Object listObject : listOfListObjects) {
            InterMineObject interMineListObject = (InterMineObject) listObject;
            Object value = null;
            try {
                // get field values from the object
                value = interMineListObject.getFieldValue("name");
            } catch (IllegalAccessException e) {
                // no dataset
                continue;
            }
            if (value != null && value.toString().equals(datasetToMatch)) {
                isValidDataset = true;
            }
        }
        return isValidDataset;
    }

    private static void modifyIdString(ConfigMap config) {
        LOG.info("in modifyIdStrin");

        String delim = (String) config.get("delimiter");
        String urlString = (String) config.get("url");

        String idString = (String) config.get("attributeValue");

        if ("NEWLINE".equals(delim)) {
            urlString = urlString.replace(",", System.getProperty("line.separator"));
            idString = idString.replace(",", System.getProperty("line.separator"));
        } else {
            urlString = urlString.replace(",", delim);
            idString = idString.replace(",", delim);
        }
        config.put("url", urlString);
        config.put("attributeValue", idString);
    }

    private static void modifyConfigToPost(ConfigMap config) {
        LOG.info("in modifyConfigPost");

        String urlString = (String) config.get("url");
        AttributeLinkURL link;
        try {
            // Verifies, that url is valid
            link = new AttributeLinkURL(urlString);
        } catch (MalformedURLException e) {
            LOG.error("Converting url from GET to POST form failed. Url retained in GET form.", e);
            return;
        }
        config.put("url", link.getBaseURL());
        if (link.getParameters().size() > 0) {
            config.put("parameters", link.getParameters());
        }
    }
*/
}
