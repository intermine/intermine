package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.io.Reader;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.intermine.util.SAXParser;

/**
 * Convert PathQueries to and from XML
 *
 * @author Mark Woodbridge
 */
public class TemplateQueryBinding
{
    /**
     * Convert a TemplateQuery to XML
     *
     * @param template the PathQuery
     * @return the corresponding XML String
     */
    public String marshal(TemplateQuery template) {
        StringBuffer sb = new StringBuffer();
        sb.append("<template name='" + template.getName()
                + "' description='" + template.getDescription()
                + "' category='" + template.getCategory()
                + "'>\n");
        sb.append(new PathQueryBinding().marshal(template.getQuery(),
                                                 template.getName(),
                                                 template.getQuery().getModel().getName()));
        sb.append("</template>\n");
        System.out.println(sb.toString());
        return sb.toString();
    }

    /**
     * Parse TemplateQuerys from XML
     * @param reader the saved templates
     * @return a Map from template name to TemplateQuery
     */
    public Map unmarshal(Reader reader) {
        Map templates = new LinkedHashMap();
        try {
            SAXParser.parse(new InputSource(reader), new TemplateQueryHandler(templates));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return templates;
    }

    /**
     * Extension of DefaultHandler to handle parsing TemplateQueries
     */
    class TemplateQueryHandler extends PathQueryBinding.QueryHandler
    {
        Map templates;
        String templateName;
        String templateDesc;
        String templateCat;

        /**
         * Constructor
         * @param templates Map from template name to TemplateQuery
         */
        public TemplateQueryHandler(Map templates) {
            super(new HashMap());
            this.templates = templates;
        }

        /**
         * @see DefaultHandler#startElement
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            if (qName.equals("template")) {
                templateName = attrs.getValue("name");
                templateDesc = attrs.getValue("description");
                templateCat = attrs.getValue("category");
            }
            super.startElement(uri, localName, qName, attrs);
        }
        
        /**
         * @see DefaultHandler#endElement
         */
        public void endElement(String uri, String localName, String qName) {
            super.endElement(uri, localName, qName);
            if (qName.equals("template")) {
                templates.put(templateName, new TemplateQuery(templateName,
                                                              templateDesc,
                                                              templateCat,
                                                              query));
            }
        }
    }
}
