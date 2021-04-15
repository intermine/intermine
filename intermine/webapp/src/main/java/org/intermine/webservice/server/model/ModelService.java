package org.intermine.webservice.server.model;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.tag.TagTypes;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.StringUtil;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.output.JSONFormatter;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.PlainFormatter;
import org.intermine.webservice.server.output.StreamedOutput;
import org.json.JSONObject;

/**
 * Web service that returns a serialised representation of the data model. The currently
 * supported formats are JSON and XML.
 *
 * @author Jakub Kulaviak
 * @author Alex Kalderimis
 */
public class ModelService extends WebService
{
    private static final String DEFAULT_CALLBACK = "parseModel";
    private static final Logger LOG = Logger.getLogger(ModelService.class);
    private static final String FILE_BASE_NAME = "model";
    private WebConfig config = null;
    private Path node = null;

    /**
     * Constructor.
     * @param im The API settings bundle
     */
    public ModelService(InterMineAPI im) {
        super(im);
        config = InterMineContext.getWebConfig();
    }

    @Override
    protected Output makeXMLOutput(PrintWriter out, String separator) {
        ResponseUtil.setXMLHeader(response, FILE_BASE_NAME + ".xml");
        return new StreamedOutput(out, new PlainFormatter(), separator);
    }

    @Override
    protected Format getDefaultFormat() {
        return Format.XML;
    }

    private static final String FORMAT_ENDINGS = "^/?(xml|tsv|csv|json|jsonp)$";

    @Override
    protected void initState() {
        super.initState();
        String pathInfo = StringUtils.defaultString(request.getPathInfo(), "");
        if (StringUtils.isBlank(pathInfo)) {
            return;
        }
        if (pathInfo.matches(FORMAT_ENDINGS)) {
            return;
        }
        setFormat(Format.JSON);
        pathInfo = StringUtil.trimSlashes(pathInfo).replace('/', '.');
        try {
            Map<String, String> subclasses = new HashMap<String, String>();
            for (Enumeration<?> e = request.getParameterNames(); e.hasMoreElements();) {
                String param = (String) e.nextElement();
                subclasses.put(param, request.getParameter(param));
            }
            Path p = new Path(im.getModel(), pathInfo, subclasses);
            node = p;
        } catch (PathException e) {
            throw new ResourceNotFoundException("Could not find a node with the id: " + pathInfo);
        }
    }

    @Override
    protected boolean canServe(Format format) {
        return format == Format.XML || format == Format.JSON;
    }

    /**
     * {@inheritDoc}}
     */
    @Override
    protected void execute() {
        final Model model = this.im.getModel();
        if (formatIsJSON()) {
            ResponseUtil.setJSONHeader(response, FILE_BASE_NAME + ".json", formatIsJSONP());
            Map<String, Object> attributes = new HashMap<String, Object>();
            if (formatIsJSONP()) {
                String callback = getCallback();
                if (callback == null || "".equals(callback)) {
                    callback = DEFAULT_CALLBACK;
                }
                attributes.put(JSONFormatter.KEY_CALLBACK, callback);
            }
            if (node == null) {
                attributes.put(JSONFormatter.KEY_INTRO, "\"model\":");
                output.setHeaderAttributes(attributes);
                output.addResultItem(
                        Arrays.asList(new JSONObject(getAnnotatedModel(model)).toString()));
            } else {
                Map<String, String> kvPairs = new HashMap<String, String>();
                kvPairs.put("name", getNodeName(node));
                kvPairs.put("id", node.toStringNoConstraints());
                kvPairs.put("display", WebUtil.formatPath(node, config));
                kvPairs.put("type", "class");
                attributes.put(JSONFormatter.KEY_KV_PAIRS, kvPairs);
                attributes.put(JSONFormatter.KEY_INTRO, "\"fields\":[");
                attributes.put(JSONFormatter.KEY_OUTRO, "]");
                output.setHeaderAttributes(attributes);
                output.addResultItem(nodeChildrenToJSON(node));
            }
        } else {
            output.addResultItem(Arrays.asList(model.toString()));
        }
    }

    private Map<String, Object> getAnnotatedModel(Model model) {
        TagManager tm = im.getTagManager();
        ObjectStoreSummary oss = im.getObjectStoreSummary();
        Model.ModelAST modelData = model.toJsonAST();
        Map<String, Map<String, Object>> classes = modelData.getClasses();
        Profile p = getPermission().getProfile();
        String userName = p.getUsername();
        try {
            Iterator<Map<String, Object>> iterator = classes.values().iterator();
            while (iterator.hasNext()) {
                Map<String, Object> classData = iterator.next();
                String className = (String) classData.get("name");
                // Might be a good idea to add in field names as well,
                // but these have sharper edge cases
                ClassDescriptor cd = model.getClassDescriptorByName(className);
                // Add the display name for this class.
                classData.put("displayName", WebUtil.formatClass(cd, config));
                classData.put("term", cd.getFairTerm());
                String fullyQualifiedClassName = cd.getName();
                try {
                    // Add the count for this class.
                    classData.put("count", oss.getClassCount(fullyQualifiedClassName));
                } catch (RuntimeException e) {
                    LOG.error("No class count", e);
                }
                // Get the tags for this class.
                Set<String> tags = new HashSet<String>();
                if (p.isLoggedIn()) {
                    tags.addAll(tm.getObjectTagNames(fullyQualifiedClassName, TagTypes.CLASS,
                            userName));
                }
                tags.addAll(tm.getPublicTagNames(fullyQualifiedClassName, TagTypes.CLASS));
                classData.put("tags", tags);

                // Get the Attributes for this class.
                Object allAttributes = classData.get("attributes");
                Map<String, Object> attributes = (HashMap<String, Object>) allAttributes;
                Iterator<Entry<String, Object>> attributesIterator
                    = attributes.entrySet().iterator();
                while (attributesIterator.hasNext()) {
                    Map.Entry<String, Object> entry = attributesIterator.next();
                    Map<String, Object> attribute = (HashMap<String, Object>) entry.getValue();
                    String attributeName = entry.getKey();
                    FieldDescriptor fd = cd.getAttributeDescriptorByName(attributeName, true);
                    String displayName = getDisplayName(cd, attributeName, fd);
                    attribute.put("displayName", displayName);
                    attribute.put("term", cd.getFairTerm());
                }

                // Get the refs for this class.
                Object allReferences = classData.get("references");
                Map<String, Object> refs = (HashMap<String, Object>) allReferences;
                Iterator<Entry<String, Object>> refIterator = refs.entrySet().iterator();
                while (refIterator.hasNext()) {
                    Map.Entry<String, Object> entry = refIterator.next();
                    Map<String, Object> ref = (HashMap<String, Object>) entry.getValue();
                    String referenceName = entry.getKey();
                    FieldDescriptor fd = cd.getReferenceDescriptorByName(referenceName, true);
                    String displayName = getDisplayName(cd, referenceName, fd);
                    ref.put("displayName", displayName);
                }

                // Get the collections for this class.
                Object allCollections = classData.get("collections");
                Map<String, Object> collections = (HashMap<String, Object>) allCollections;
                Iterator<Entry<String, Object>> collIterator = collections.entrySet().iterator();
                while (collIterator.hasNext()) {
                    Map.Entry<String, Object> entry = collIterator.next();
                    Map<String, Object> collection = (HashMap<String, Object>) entry.getValue();
                    String collectionName = entry.getKey();
                    FieldDescriptor fd = cd.getCollectionDescriptorByName(collectionName, true);
                    String displayName = getDisplayName(cd, collectionName, fd);
                    collection.put("displayName", displayName);
                }
            }
        } catch (RuntimeException t) {
            LOG.error("Could not annotate model", t);
            throw t;
        }
        return modelData;
    }

    private String getDisplayName(ClassDescriptor cd, String fieldName, FieldDescriptor fd) {
        String displayName = null;
        if (fd != null) {
            final FieldConfig fc = FieldConfigHelper.getFieldConfig(config, cd, fd);
            if (fc != null) {
                displayName = fc.getDisplayName();
            }
        }
        if (displayName == null) {
            displayName = FieldConfig.getFormattedName(fieldName);
        }
        return displayName;
    }

    private static String getNodeName(Path newNode) {
        WebConfig webConfig = InterMineContext.getWebConfig();
        if (newNode.isRootPath()) {
            return WebUtil.formatPath(newNode, webConfig);
        } else {
            return WebUtil.formatField(newNode, webConfig);
        }
    }

    private static List<String> nodeChildrenToJSON(Path newNode) {
        List<String> ret = new LinkedList<String>();
        if (!newNode.endIsAttribute()) {
            ClassDescriptor cd = newNode.getLastClassDescriptor();
            List<FieldDescriptor> fields = new LinkedList<FieldDescriptor>();
            fields.addAll(cd.getAllAttributeDescriptors());
            fields.addAll(cd.getAllReferenceDescriptors());
            fields.addAll(cd.getAllCollectionDescriptors());
            for (FieldDescriptor fd: fields) {
                try {
                    ret.add(fieldToJSON(newNode.append(fd.getName())));
                } catch (PathException e) {
                    throw new ServiceException("While walking model", e);
                }
            }
        }
        return ret;
    }

    private static String fieldToJSON(Path fieldPath) {

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"name\":\"" + getNodeName(fieldPath) + "\"");
        sb.append(",");
        sb.append("\"id\":\"" + fieldPath.toStringNoConstraints() + "\"");
        if (!fieldPath.endIsAttribute()) {
            sb.append(",\"fields\":true");
            if (fieldPath.endIsCollection()) {
                sb.append(",\"type\":\"collection\"");
            } else {
                sb.append(",\"type\":\"reference\"");
            }
            sb.append(",\"references\":\""
                    + fieldPath.getLastClassDescriptor().getUnqualifiedName() + "\"");
        } else {
            String type = ((AttributeDescriptor) fieldPath.getEndFieldDescriptor()).getType();
            type = type.substring(type.lastIndexOf('.') + 1);
            sb.append(",\"type\":\"" + type + "\"");
        }
        sb.append("}");
        return sb.toString();
    }

}
