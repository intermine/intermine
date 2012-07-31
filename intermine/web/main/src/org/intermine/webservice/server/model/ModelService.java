package org.intermine.webservice.server.model;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.util.StringUtil;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.WebServiceRequestParser;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.output.JSONFormatter;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.PlainFormatter;
import org.intermine.webservice.server.output.StreamedOutput;

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

    private static final String FILE_BASE_NAME = "model";
    private Path node = null;

    /**
     * Constructor.
     * @param im The API settings bundle
     */
    public ModelService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected Output makeXMLOutput(PrintWriter out, String separator) {
        ResponseUtil.setXMLHeader(response, FILE_BASE_NAME + ".xml");
        return new StreamedOutput(out, new PlainFormatter(), separator);
    }

    @Override
    protected int getDefaultFormat() {
        return XML_FORMAT;
    }

    @Override
    protected String parseFormatFromPathInfo() {
        String format = super.parseFormatFromPathInfo();
        if (format != null) {
            return format;
        }
        String pathInfo = request.getPathInfo();
        pathInfo = StringUtil.trimSlashes(pathInfo).replace('/', '.');
        if (StringUtils.isBlank(pathInfo)) {
            return null;
        }
        try {
            Map<String, String> subclasses = new HashMap<String, String>();
            for (Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();) {
                String param = e.nextElement();
                subclasses.put(param, request.getParameter(param));
            }
            Path p = new Path(im.getModel(), pathInfo, subclasses);
            node = p;
            String ret = request.getParameter(WebServiceRequestParser.OUTPUT_PARAMETER);
            return StringUtils.isBlank(ret) ? WebServiceRequestParser.FORMAT_PARAMETER_JSON : ret;
        } catch (PathException e) {
            throw new ResourceNotFoundException("Could not find a node with the id: " + pathInfo);
        }
    }

    /**
     * {@inheritDoc}}
     */
    @Override
    protected void execute() {
        final Model model = this.im.getModel();
        final WebConfig config = InterMineContext.getWebConfig();
        if (formatIsJSON()) {
            ResponseUtil.setJSONHeader(response, FILE_BASE_NAME + ".json");
            Map<String, Object> attributes = new HashMap<String, Object>();
            if (formatIsJSONP()) {
                String callback = getCallback();
                if (callback == null || "".equals(callback)) {
                    callback = DEFAULT_CALLBACK;
                }
                attributes.put(JSONFormatter.KEY_CALLBACK, callback);
            }
            if (node == null) {
                attributes.put(JSONFormatter.KEY_INTRO, "\"model\":{");
                attributes.put(JSONFormatter.KEY_OUTRO, "}");
                output.setHeaderAttributes(attributes);
                output.addResultItem(Arrays.asList(model.toJSONString()));
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

    private String getNodeName(Path node) {
        WebConfig webConfig = InterMineContext.getWebConfig();
        if (node.isRootPath()) {
            return WebUtil.formatPath(node, webConfig);
        } else {
            return WebUtil.formatField(node, webConfig);
        }
    }

    private List<String> nodeChildrenToJSON(Path node) {
        List<String> ret = new LinkedList<String>();
        if (!node.endIsAttribute()) {
            ClassDescriptor cd = node.getLastClassDescriptor();
            List<FieldDescriptor> fields = new LinkedList<FieldDescriptor>();
            fields.addAll(cd.getAllAttributeDescriptors());
            fields.addAll(cd.getAllReferenceDescriptors());
            fields.addAll(cd.getAllCollectionDescriptors());
            for (FieldDescriptor fd: fields) {
                try {
                    ret.add(fieldToJSON(node.append(fd.getName())));
                } catch (PathException e) {
                    throw new InternalErrorException("While walking model", e);
                }
            }
        }
        return ret;
    }

    private String fieldToJSON(Path fieldPath) {

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
