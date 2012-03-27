package org.intermine.api.bag;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.util.TypeUtil;
import org.intermine.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Handler for bag-query.xml files.
 *
 * @author Richard Smith
 */
public class BagQueryHandler extends DefaultHandler
{
    private static final Logger LOG = Logger.getLogger(BagQueryHandler.class);
    private List<BagQuery> queryList;
    private List<BagQuery> preDefaultQueryList;
    private Map<String, List<BagQuery>> bagQueries = new HashMap<String, List<BagQuery>>();
    private Map<String, List<BagQuery>> preDefaultBagQueries
        = new HashMap<String, List<BagQuery>>();
    private Map<String, Set<AdditionalConverter>> additionalConverters
        = new HashMap<String, Set<AdditionalConverter>>();
    private String type, message, queryString;
    private Boolean matchesAreIssues;
    private Boolean runBeforeDefault;
    private boolean matchOnFirst = true;
    private Model model;
    private StringBuffer sb;
    private String pkg = null;
    private BagQueryConfig bagQueryConfig = new BagQueryConfig(bagQueries, preDefaultBagQueries,
                                                               additionalConverters);

    /**
     * Create a new BagQueryHandler object.
     *
     * @param model
     *            the Model to use when checking types
     */
    public BagQueryHandler(Model model) {
        super();
        this.model = model;
        this.pkg = model.getPackageName();
    }

    /**
     * Return the bag queries from the XML file.
     *
     * @return a Map from class name to a List of BagQuery objects
     */
    public Map<String, List<BagQuery>> getBagQueries() {
        return bagQueries;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if ("bag-type".equals(qName)) {
            type = attrs.getValue("type");
            if (!model.hasClassDescriptor(pkg + "." + type)) {
                throw new SAXException("Type was not found in model: " + type);
            }
            queryList = new ArrayList<BagQuery>();
            preDefaultQueryList = new ArrayList<BagQuery>();
            if (bagQueries.containsKey(type)) {
                throw new SAXException("Duplicate query lists defined for type: " + type);
            }
            String matchOnFirstStr = attrs.getValue("matchOnFirst");
            if (StringUtils.isNotEmpty(matchOnFirstStr)) {
                matchOnFirst = ("false".equalsIgnoreCase(matchOnFirstStr) ? false : true);
            }
            bagQueryConfig.setMatchOnFirst(matchOnFirst);
        }
        if ("query".equals(qName)) {
            message = attrs.getValue("message");
            matchesAreIssues = Boolean.valueOf(attrs.getValue("matchesAreIssues"));
            runBeforeDefault = Boolean.valueOf(attrs.getValue("runBeforeDefault"));
            sb = new StringBuffer();
        }
        if ("additional-converter".equals(qName)) {
            processAdditionalConverters(attrs);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(char[] ch, int i, int l) {
        int start = i;
        int length = l;
        // DefaultHandler may call this method more than once for a single
        // attribute content -> hold text & create attribute in endElement
        while (length > 0) {
            boolean whitespace = false;
            switch (ch[start]) {
                case ' ':
                case '\r':
                case '\n':
                case '\t':
                    whitespace = true;
                    break;
                default:
                    break;
            }
            if (!whitespace) {
                break;
            }
            ++start;
            --length;
        }

        if (length > 0) {
            sb.append(ch, start, length);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(String uri, String localName, String qName) {
        if ("query".equals(qName)) {
            queryString = sb.toString();
            if (queryString != null && message != null && matchesAreIssues != null) {
                BagQuery bq = new BagQuery(bagQueryConfig, model, queryString, message, pkg,
                                           matchesAreIssues.booleanValue());
                if (runBeforeDefault.booleanValue()) {
                    preDefaultQueryList.add(bq);
                } else {
                    queryList.add(bq);
                }
            }
            queryString = null;
            matchesAreIssues = null;
            message = null;
            runBeforeDefault = null;
        }

        // add bag query to map for specified class and all subclasses
        if ("bag-type".equals(qName)) {
            ClassDescriptor cld = model.getClassDescriptorByName(pkg + "." + type);
            List<ClassDescriptor> clds = new ArrayList<ClassDescriptor>(model.getAllSubs(cld));
            clds.add(cld);
            for (ClassDescriptor nextCld : clds) {
                String clsName = TypeUtil.unqualifiedName(nextCld.getName());
                List<BagQuery> typeQueries = bagQueries.get(clsName);
                if (typeQueries == null) {
                    typeQueries = new ArrayList<BagQuery>();
                    bagQueries.put(clsName, typeQueries);
                }
                typeQueries.addAll(queryList);
                List<BagQuery> preDefaultTypeQueries = preDefaultBagQueries.get(clsName);
                if (preDefaultTypeQueries == null) {
                    preDefaultTypeQueries = new ArrayList<BagQuery>();
                    preDefaultBagQueries.put(clsName, preDefaultTypeQueries);
                }
                preDefaultTypeQueries.addAll(preDefaultQueryList);
            }
        }
    }

    private void processAdditionalConverters(Attributes attrs) {
        String fullyQualifiedName = attrs.getValue("class-name");
        String constraintPath = attrs.getValue("constraint-path");
        String targetType = attrs.getValue("target-type");
        String title = attrs.getValue("title");
        String urlField = attrs.getValue("urlField");

        ClassDescriptor typeCld = model.getClassDescriptorByName(targetType);
        if (typeCld == null) {
            LOG.warn("Invalid target type for additional converter: " + targetType);
            return;
        }

        try {
            new Path(model, constraintPath);
        } catch (PathException e) {
            LOG.warn("Can't add converter to bag-queries.xml, constraint-path '" + constraintPath
                    + "' isn't in model.", e);
            return;
        }

        Set<String> clds = new HashSet<String>();
        clds.add(typeCld.getName());
        for (ClassDescriptor cld : model.getAllSubs(typeCld)) {
            clds.add(cld.getName());
        }

        AdditionalConverter additionalConverter = new AdditionalConverter(constraintPath,
                targetType, fullyQualifiedName, title, urlField);

        for (String nextCld : clds) {
            Util.addToSetMap(additionalConverters, TypeUtil.unqualifiedName(nextCld),
                    additionalConverter);
        }
    }

    /**
     * Return the BagQueryConfig created from the XML.
     *
     * @return the BagQueryConfig
     */
    public BagQueryConfig getBagQueryConfig() {
        return bagQueryConfig;
    }
}
