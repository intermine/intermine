package org.intermine.bio.web;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.bio.web.logic.BioUtil;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.BagHelper;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.util.AttributeLinkURL;

/**
 * Set up maps for the attributeLinkDisplayer.jsp
 * @author Kim Rutherford
 */
public class AttributeLinkDisplayerController extends TilesAction
{

    protected static final Logger LOG = Logger.getLogger(AttributeLinkDisplayerController.class);

    static final String ATTR_MARKER_RE = "<<attributeValue>>";

    private class ConfigMap extends HashMap<String, Object>
    {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response) {

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());

        ServletContext servletContext = request.getSession().getServletContext();

        InterMineBag bag = (InterMineBag) request.getAttribute("bag");

        InterMineObject imo = null;

        if (bag == null) {
            imo = (InterMineObject) request.getAttribute("object");
        }

        ObjectStore os = im.getObjectStore();
        Model model = im.getModel();

        Set<ClassDescriptor> classDescriptors;

        if (imo == null) {
            classDescriptors = bag.getClassDescriptors();
        } else {
            classDescriptors = model.getClassDescriptorsForClass(imo.getClass());
        }

        StringBuffer sb = new StringBuffer();
        for (ClassDescriptor cd : classDescriptors) {
            if (sb.length() <= 0) {
                sb.append("(");
            } else {
                sb.append("|");
            }
            sb.append(TypeUtil.unqualifiedName(cd.getName()));
        }
        sb.append(")");
        Organism organismReference = null;
        String geneOrgKey = sb.toString();

        if (imo != null) {
            try {
                organismReference = (Organism) imo.getFieldValue("organism");
            } catch (IllegalAccessException e) {
                // no organism field
            }

            if (organismReference == null || organismReference.getTaxonId() == null) {
                geneOrgKey += "(\\.(\\*))?";
            } else {
                // we need to check against * as well in case we want it to work for all taxonIds
                geneOrgKey += "(\\.(" + organismReference.getTaxonId() + "|\\*))?";
            }
        } else { // bag
            geneOrgKey += "(\\.(\\*|[\\d]+))?";
        }

        // map from eg. 'Gene.Drosophila.melanogaster' to map from configName (eg. "flybase")
        // to the configuration
        Map<String, ConfigMap> linkConfigs = new HashMap<String, ConfigMap>();
        Properties webProperties =
            (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        final String regexp = "attributelink\\.([^.]+)\\." + geneOrgKey
            + "\\.([^.]+)(\\.list)?\\.(url|text|imageName|usePost|delimiter|enctype)";
        Pattern p = Pattern.compile(regexp);
        String className = null;
        String taxId = null;
        for (Map.Entry<Object, Object> entry: webProperties.entrySet()) {
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
                if (bag != null && imType == null) {
                    continue;
                }

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
                            attrValue = BagHelper.getIdList(bag, os, dbName, attrName);
                            if (!taxId.equalsIgnoreCase("*")) {
                                taxIds = BioUtil.getOrganisms(os, bag, false, "taxonId");

                                //don't display link if
                                // a) not a bioentity (no reference to organism)
                                if (taxIds == null) {
                                    continue;
                                }
                                // b) organism not present
                                if (!taxIds.contains(taxId)) {
                                    continue;
                                }
                            }
                        }
                        if (attrValue != null) {
                            config.put("attributeValue", attrValue);
                            config.put("valid", Boolean.TRUE);
                        }
                    } catch (IllegalAccessException e) {
                        config.put("attributeValue", e);
                        config.put("valid", Boolean.FALSE);
                        LOG.error("configuration problem in AttributeLinkDisplayerController: "
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
                } else if ("text".equals(propType)) {
                    // parse out the title of the external link, e.g. "blabla: <>" => "blabla"
                    config.put("title", value.replaceAll("[^A-Za-z0-9 ]", "")
                            .replaceFirst("attributeValue", ""));

                    String text;
                    text = value.replaceAll(ATTR_MARKER_RE, String.valueOf(attrValue));
                    config.put("text", text);
                }
            }
        }
        processConfigs(linkConfigs);
        request.setAttribute("attributeLinkConfiguration", linkConfigs);
        request.setAttribute("attributeLinkClassName", className);

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

        return null;
    }

    /**
     * Process configs. Configs that have specified that POST method
     * should be used when request is submitted to third party site are modified with this method.
     * GET form of url is modified to POST form.
     * @param linkConfigs
     */
    private void processConfigs(Map<String, ConfigMap> linkConfigs) {
        for (ConfigMap config : linkConfigs.values()) {
            if (config.get("delimiter") != null) {
                modifyIdString(config);
            }
            if (config.get("usePost") != null
                    && ((String) config.get("usePost")).equalsIgnoreCase("true")) {
                modifyConfigToPost(config);
            }
        }
    }

    private void modifyIdString(ConfigMap config) {

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

    private void modifyConfigToPost(ConfigMap config) {
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
}
