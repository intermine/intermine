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

import java.util.LinkedHashMap;
import java.io.Reader;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.util.SAXParser;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.metadata.Model;

/**
 * Parse PathQueries in XML format
 *
 * @author Mark Woodbridge
 */
public class SavedQueryParser
{
    protected Map savedQueries = new LinkedHashMap();

    /**
     * Parse saved queries from a Reader
     * @param reader the saved queries
     * @return a Map from query name to PathQuery
     */
    public Map process(Reader reader) {
        try {
            SAXParser.parse(new InputSource(reader), new QueryHandler());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return savedQueries;
    }

    /**
     * Extension of DefaultHandler to handle metadata file
     */
    class QueryHandler extends DefaultHandler
    {
        String queryName;
        PathQuery query;
        PathNode node;

        /**
         * @see DefaultHandler#startElement
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            if (qName.equals("query")) {
                queryName = attrs.getValue("name");
                Model model;
                try {
                    model = Model.getInstanceByName(attrs.getValue("model"));
                } catch (Exception e) {
                    throw new SAXException(e);
                }
                query = new PathQuery(model);
                query.setView(StringUtil.tokenize(attrs.getValue("view")));
            }
            if (qName.equals("node")) {
                node = query.addNode(attrs.getValue("path"));
                if (attrs.getValue("type") != null) {
                    node.setType(attrs.getValue("type"));
                }
            }
            if (qName.equals("constraint")) {
                int opIndex = toStrings(ConstraintOp.getValues()).indexOf(attrs.getValue("op"));
                ConstraintOp constraintOp = ConstraintOp.getOpForIndex(new Integer(opIndex));
                Object constraintValue = TypeUtil
                    .stringToObject(MainHelper.getClass(node.getType()), attrs.getValue("value"));
                node.getConstraints().add(new Constraint(constraintOp, constraintValue));
            }
        }

        /**
         * @see DefaultHandler#endElement
         */
        public void endElement(String uri, String localName, String qName) {
            if (qName.equals("query")) {
                savedQueries.put(queryName, query);
            }
        }
        
        /**
         * Convert a List of Objects to a List of Strings using toString
         * @param list the Object List
         * @return the String list
         */
        protected List toStrings(List list) {
            List strings = new ArrayList();
            for (Iterator i = list.iterator(); i.hasNext();) {
                strings.add(i.next().toString());
            }
            return strings;
        }
    }
}
