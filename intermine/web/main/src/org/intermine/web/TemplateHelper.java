package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.intermine.objectstore.query.ConstraintOp;

import org.apache.commons.lang.StringUtils;

/**
 * Static helper routines related to templates.
 *
 * @author  Thomas Riley
 */
public class TemplateHelper
{
    /** Type parameter indicating globally shared template. */
    public static final String GLOBAL_TEMPLATE = "global";
    /** Type parameter indicating group shared template. */
    public static final String SHARED_TEMPLATE = "shared";
    /** Type parameter indicating private user template. */
    public static final String USER_TEMPLATE = "user";
    
    /**
     * Locate TemplateQuery by identifier. The type parameter
     *
     * @param session     the http session
     * @param identifier  template query identifier/name
     * @param type        type of tempate, either GLOBAL_TEMPLATE, SHARED_TEMPLATE or USER_TEMPLATE
     * @return            the located template query with matching identifier
     */
    public static TemplateQuery findTemplate(HttpSession session,
                                             String identifier,
                                             String type) {

        ServletContext servletContext = session.getServletContext();
        Map templates = null;
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        
        if (USER_TEMPLATE.equals(type)) {
            templates = profile.getSavedTemplates();
        } else if (SHARED_TEMPLATE.equals(type)) {
            // TODO implement shared templates
        } else if (GLOBAL_TEMPLATE.equals(type)) {
            templates = (Map) SessionMethods.getSuperUserProfile(servletContext)
                .getSavedTemplates();
        } else {
            throw new IllegalArgumentException("type: " + type);
        }
        
        return (TemplateQuery) templates.get(identifier);
    }
    
    /**
     * Create a new PathQuery with input submitted by user contained within
     * a TemplateForm bean.
     *
     * @param tf        the template form bean
     * @param template  the template query involved
     * @return          a new PathQuery matching template with user supplied constraints
     */
    public static PathQuery templateFormToQuery(TemplateForm tf, TemplateQuery template) {
        PathQuery queryCopy = (PathQuery) template.getQuery().clone();
        
        // Step over nodes and their constraints in order, ammending our
        // PathQuery copy as we go
        int j = 0;
        for (Iterator i = template.getNodes().iterator(); i.hasNext();) {
            PathNode node = (PathNode) i.next();
            for (Iterator ci = template.getConstraints(node).iterator(); ci.hasNext();) {
                Constraint c = (Constraint) ci.next();
                String key = "" + (j + 1);
                PathNode nodeCopy = (PathNode) queryCopy.getNodes().get(node.getPath());
                
                if (tf.getUseBagConstraint(key)) {
                    // Replace constraint with bag constraint
                    ConstraintOp constraintOp = ConstraintOp.
                    getOpForIndex(Integer.valueOf(tf.getBagOp(key)));
                    Object constraintValue = tf.getBag(key);
                    nodeCopy.getConstraints().set(node.getConstraints().indexOf(c),
                            new Constraint(constraintOp, constraintValue, false,
                                    c.getDescription(), c.getCode(), c.getIdentifier()));
                } else {
                    // Parse user input
                    String op = (String) tf.getAttributeOps(key);
                    ConstraintOp constraintOp = ConstraintOp.getOpForIndex(Integer.valueOf(op));
                    Object constraintValue = tf.getParsedAttributeValues(key);
                    
                    // In query copy, replace old constraint with new one
                    nodeCopy.getConstraints().set(node.getConstraints().indexOf(c),
                            new Constraint(constraintOp, constraintValue, false,
                                    c.getDescription(), c.getCode(), c.getIdentifier()));
                }
                j++;
            }
        }
        
        // Set the desired view list
        if (!StringUtils.isEmpty(tf.getView())) {
            queryCopy.setView(template.getQuery().getAlternativeView(tf.getView()));
        }
        
        return queryCopy;
    }
    
    /**
     * Given a Map of TemplateQuerys (mapping from template name to TemplateQuery)
     * return a string containing each template seriaised as XML. The root element
     * will be a <code>template-list</code> element.
     *
     * @param templates  map from template name to TemplateQuery
     * @return  all template queries serialised as XML
     * @see  TemplateQuery
     */
    public static String templateMapToXml(Map templates) {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        TemplateQueryBinding binding = new TemplateQueryBinding();
        Iterator iter = templates.values().iterator();
        
        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            writer.writeStartElement("template-list");
            while (iter.hasNext()) {
                TemplateQueryBinding.marshal((TemplateQuery) iter.next(), writer);
            }
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        
        return sw.toString();
    }
    
    /**
     * Parse templates in XML format and return a map from template name to
     * TemplateQuery.
     *
     * @param xml  the template queries in xml format
     * @return     Map from template name to TemplateQuery
     * @throws Exception  when a parse exception occurs (wrapped in a RuntimeException)
     */
    public static Map xmlToTemplateMap(String xml) throws Exception {
        Reader templateQueriesReader = new StringReader(xml);
        return new TemplateQueryBinding().unmarshal(templateQueriesReader);
    }

    /**
     * Build a template query given a TemplateBuildState and a PathQuery
     * 
     * @param tbs the template build state
     * @param query the path query
     * @return a template query
     */
    public static TemplateQuery buildTemplateQuery(TemplateBuildState tbs, PathQuery query) {
        TemplateQuery template = new TemplateQuery(tbs.getName(),
                                                   tbs.getDescription(),
                                                   (PathQuery) query.clone(), tbs.isImportant(),
                                                   tbs.getKeywords());
        return template;
    }
}
