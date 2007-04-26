package org.intermine.web.logic.template;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.web.logic.query.PathQueryHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Extension of PathQueryHandler to handle parsing TemplateQueries
 * @author Xavier Watkins
 */
public class TemplateQueryHandler extends PathQueryHandler
{
    Map templates;
    String templateName;
    String templateDesc;
    String templateCat;
    String keywords;
    String templateTitle;
    String templateComment;
    boolean important;

    /**
     * Constructor
     * @param templates Map from template name to TemplateQuery
     * @param savedBags Map from bag name to bag
     * @param classKeys class key fields for the model
     */
    public TemplateQueryHandler(Map templates, Map savedBags, Map classKeys) {
        super(new HashMap(), savedBags, classKeys);
        this.templates = templates;
        reset();
    }

    /**
     * {@inheritDoc}
     */
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if (qName.equals("template")) {
            templateName = attrs.getValue("name");
            templateTitle = attrs.getValue("title");
            templateDesc = attrs.getValue("longDescription");
            if (attrs.getValue("description") != null && templateTitle == null) {
                // support old serialisation format: description -> title
                templateTitle = attrs.getValue("description");
            }
            templateComment = attrs.getValue("comment");
            templateCat = attrs.getValue("category");
            keywords = attrs.getValue("keywords");
            if (keywords == null) {
                keywords = "";
            }
            important = Boolean.valueOf(attrs.getValue("important")).booleanValue();
        }
        super.startElement(uri, localName, qName, attrs);
    }
    
    /**
     * {@inheritDoc}
     */
    public void endElement(String uri, String localName, String qName) {
        super.endElement(uri, localName, qName);
        if (qName.equals("template")) {
            if (StringUtils.isNotEmpty(templateCat)) {
                if (keywords == null) {
                    keywords = "";
                }
                if (StringUtils.isNotEmpty(keywords)) {
                    keywords += ", " + templateCat;
                } else {
                    keywords = templateCat;
                }
            }
            templates.put(templateName, new TemplateQuery(templateName,
                                                          templateTitle,
                                                          templateDesc,
                                                          templateComment,
                                                          query,
                                                          keywords));
            reset();
        }
    }
    
    private void reset() {
        keywords = "";
        templateCat = "";
        templateName = "";
        templateDesc = "";
    }
}
