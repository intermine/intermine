package org.intermine.bio.web;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
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
import org.flymine.model.genomic.Organism;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.util.POSTLink;

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

        ServletContext servletContext = request.getSession().getServletContext();

        InterMineBag bag = (InterMineBag) request.getAttribute("bag");

        InterMineObject imo = null;

        if (bag == null) {
            imo = (InterMineObject) request.getAttribute("object");
        }

        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Model model = os.getModel();

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
                organismReference = (Organism) TypeUtil.getFieldValue(imo, "organism");
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
            + "\\.([^.]+)(\\.list)?\\.(url|text|imageName|usePost)";
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
                Set taxIds = null;

                if (config.containsKey("attributeValue")) {
                    attrValue = config.get("attributeValue");
                } else {
                    try {
                        if (imo != null) {
                            attrValue = TypeUtil.getFieldValue(imo, attrName);
                        } else { //it's a bag!
                            attrValue = getIdList(bag, os, dbName, attrName);
                            if (!taxId.equalsIgnoreCase("*")) {
                                taxIds = getTaxIds (bag, os);

                                //don't display link if
                                // a) not a bioentity (no reference to organism)
                                if (taxIds == null) {
                                    continue;
                                }
                                // b) organism not present
                                Integer taxIdInt = Integer.valueOf(taxId);
                                if (!taxIds.contains(taxIdInt)) {
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

                if (propType.equals("url")) {
                    if (attrValue != null) {
                        String url;
                        if (value.contains(ATTR_MARKER_RE)) {
                            url = value.replaceAll(ATTR_MARKER_RE, String.valueOf(attrValue));
                        } else {
                            url = value + attrValue;
                        }
                        config.put("url", url);
                    }
                }
                else if (propType.equals("imageName")) {
                    config.put("imageName", value);
                }
                else if (propType.equals("usePost")) {
                    config.put("usePost", value);
                }
                else if (propType.equals("text")) {
                    String text;
                    text = value.replaceAll(ATTR_MARKER_RE, String.valueOf(attrValue));
                    config.put("text", text);
                }
            }
        }
        processConfigs(linkConfigs);
        request.setAttribute("attributeLinkConfiguration", linkConfigs);
        request.setAttribute("attributeLinkClassName", className);
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
            if (config.get("usePost") != null
                    && ((String) config.get("usePost")).equalsIgnoreCase("true")) {
                modifyConfigToPost(config);
            }
        }
    }

    private void modifyConfigToPost(ConfigMap config) {
        String urlString = (String) config.get("url");
        POSTLink link;
        try {
            // Verifies, that url is valid
            link = new POSTLink(urlString);
        } catch (MalformedURLException e) {
            LOG.error("Converting url from GET to POST form failed. Url retained in GET form.", e);
            return;
        }
        config.put("url", link.getBaseURL());
        if (link.getParameters().size() > 0) {
            config.put("parameters", link.getParameters());
        }
    }

    /**
     * @see
     * @param bag the bag
     * @param os  the object store
     * @param dbName the database to link to
     * @param attrName the attribute name (identifier, omimId, etc)
     * @return the string of comma separated identifiers
     *    */

    public String getIdList(InterMineBag bag, ObjectStore os, String dbName, String attrName) {
        Results results;

        Query q = new Query();
        QueryClass queryClass;
        try {
            queryClass = new QueryClass(Class.forName(bag.getQualifiedType()));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("no type in the bag??! -> ", e);
        }
        q.addFrom(queryClass);

        QueryField qf = new QueryField(queryClass, attrName);
        q.addToSelect(qf);

        QueryField id = new QueryField(queryClass, "id");

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        //added because sometimes identifier is null, and StringUtil.join complains
        SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.IS_NOT_NULL);

        BagConstraint bagC = new BagConstraint(id, ConstraintOp.IN, bag.getOsb());

        cs.addConstraint(sc);
        cs.addConstraint(bagC);
        q.setConstraint(cs);

        results = os.executeSingleton(q);
        results.setBatchSize(10000);

        if (dbName.equalsIgnoreCase("flybase")) {
            return StringUtil.join(results, "|");
        }
        return StringUtil.join(results, ",");
}

    /**
     * @see
     * @param bag the bag
     * @param os  the object store
     * @return a set of tax ids
     *
     * Note: works with gene and protein QueryClass.
     * TODO merge with similar method in BioUtil
     **/

    public Set<String> getTaxIds(InterMineBag bag, ObjectStore os) {
        Results results;

        Query q = new Query();
        QueryClass queryClass;
        try {
            queryClass = new QueryClass(Class.forName(bag.getQualifiedType()));

            //check if you can query for organism
            final Class<?> qc = Class.forName(bag.getQualifiedType());
            Set<ClassDescriptor> cds = os.getModel().getClassDescriptorsForClass(qc);
            ClassDescriptor cd = cds.iterator().next();

            if (cd.getFieldDescriptorByName("organism") == null) {
                return null;
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("no type in the bag??! -> ", e);
        }

        QueryClass organism = new QueryClass(Organism.class);

        q.addFrom(queryClass);
        q.addFrom(organism);

        QueryField qf = new QueryField(organism, "taxonId");
        q.addToSelect(qf);

        QueryField id = new QueryField(queryClass, "id");

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        //added because sometimes identifier is null
        SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.IS_NOT_NULL);

        BagConstraint bagC = new BagConstraint(id, ConstraintOp.IN, bag.getOsb());

        QueryObjectReference r = new QueryObjectReference(queryClass, "organism");
        ContainsConstraint cc = new ContainsConstraint(r, ConstraintOp.CONTAINS, organism);

        cs.addConstraint(sc);
        cs.addConstraint(bagC);
        cs.addConstraint(cc);

        q.setConstraint(cs);

        results = os.executeSingleton(q);
        results.setBatchSize(10000);

        return new HashSet(results);
}


}
