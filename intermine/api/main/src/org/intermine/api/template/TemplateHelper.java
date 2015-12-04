package org.intermine.api.template;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.pathquery.OrderElement;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.template.TemplateQuery;
import org.intermine.template.xml.TemplateQueryBinding;

/**
 * Static helper routines related to templates.
 *
 * @author Thomas Riley
 * @author Richard Smith
 */
public final class TemplateHelper
{


    /**
     * A logger.
     */
    private static final Logger LOG = Logger.getLogger(TemplateHelper.class);

    private TemplateHelper() {
        // don't
    }

    /**
     * Given a Map of TemplateQueries (mapping from template name to TemplateQuery)
     * return a string containing each template seriaised as XML. The root element
     * will be a <code>template-queries</code> element.
     *
     * @param templates  map from template name to TemplateQuery
     * @param version the version number of the XML format
     * @return  all template queries serialised as XML
     * @see  TemplateQuery
     */
    public static String templateMapToXml(Map<String, TemplateQuery> templates, int version) {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            writer.writeStartElement("template-queries");
            for (TemplateQuery template : templates.values()) {
                TemplateQueryBinding.marshal(template, writer, version);
            }
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        return sw.toString();
    }

    /**
     * Removed from the view all the direct attributes that aren't editable constraints
     * @param tq The template to remove attributes from.
     * @return altered template query
     */
    public static TemplateQuery removeDirectAttributesFromView(TemplateQuery tq) {
        TemplateQuery templateQuery = tq.clone();
        List<String> viewPaths = templateQuery.getView();
        String rootClass = null;
        try {
            rootClass = templateQuery.getRootClass();
            for (String viewPath : viewPaths) {
                Path path = templateQuery.makePath(viewPath);
                if (path.getElementClassDescriptors().size() == 1
                    && path.getLastClassDescriptor().getUnqualifiedName().equals(rootClass)) {
                    if (templateQuery.getEditableConstraints(viewPath).isEmpty()) {
                        templateQuery.removeView(viewPath);
                        for (OrderElement oe : templateQuery.getOrderBy()) {
                            if (oe.getOrderPath().equals(viewPath)) {
                                templateQuery.removeOrderBy(viewPath);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (PathException pe) {
            LOG.error("Error updating the template's view", pe);
        }
        return templateQuery;
    }

    /**
     * Serialse a map of templates to XML.
     * @param templates The templates to serialise.
     * @param version The UserProfile version to use.
     * @return An XML serialise.
     */
    public static String apiTemplateMapToXml(Map<String, ApiTemplate> templates, int version) {
        return templateMapToXml(downCast(templates), version);
    }

    private static Map<String, TemplateQuery> downCast(Map<String, ApiTemplate> templates) {
        Map<String, TemplateQuery> ret = new HashMap<String, TemplateQuery>();
        for (Entry<String, ApiTemplate> pair: templates.entrySet()) {
            ret.put(pair.getKey(), pair.getValue());
        }
        return ret;
    }

    /**
     * Transform a map of templates into a map of API templates.
     * @param templates The original, non-api templates.
     * @return templates brought into the realm of the API.
     */
    public static Map<String, ApiTemplate> upcast(Map<String, TemplateQuery> templates) {
        Map<String, ApiTemplate> ret = new HashMap<String, ApiTemplate>();
        for (Entry<String, TemplateQuery> pair: templates.entrySet()) {
            ret.put(pair.getKey(), new ApiTemplate(pair.getValue()));
        }
        return ret;
    }

    /**
     * Routine for serialising map of templates to JSON.
     * @param templates The templates to serialise.
     * @param im intermine API
     * @return A JSON string.
     */
    private static String templateMapToJson(InterMineAPI im, Map<String, ApiTemplate> templates) {
        StringBuilder sb = new StringBuilder("{");
        Iterator<String> keys = templates.keySet().iterator();
        while (keys.hasNext()) {
            String name = keys.next();
            ApiTemplate template = templates.get(name);
            template.setAPI(im);
            sb.append("\"" + name + "\":" + template.toJson());
            if (keys.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("}");
        String result = sb.toString();
        return result;
    }

    /**
     * Helper routine for serialising a map of templates to JSON.
     * @param templates The map of templates to serialise.
     * @param im intermine API
     * @return A JSON string.
     */
    public static String apiTemplateMapToJson(InterMineAPI im,
            Map<String, ApiTemplate> templates) {
        return templateMapToJson(im, templates);
    }

    /**
     * Parse templates in XML format and return a map from template name to
     * TemplateQuery.
     *
     * @param xml         the template queries in xml format
     * @param savedBags   Map from bag name to bag
     * @param version the version of the xml format, an attribute on ProfileManager
     * @return            Map from template name to TemplateQuery
     * @throws Exception  when a parse exception occurs (wrapped in a RuntimeException)
     */
    public static Map<String, TemplateQuery> xmlToTemplateMap(String xml,
            Map<String, InterMineBag> savedBags, int version) throws Exception {
        Reader templateQueriesReader = new StringReader(xml);
        return TemplateQueryBinding.unmarshalTemplates(templateQueriesReader, version);
    }

}
