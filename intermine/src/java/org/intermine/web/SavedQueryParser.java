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
import java.util.TreeMap;
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
 * Parse InterMine metadata XML to produce a InterMine Model
 *
 * @author Mark Woodbridge
 */
public class SavedQueryParser
{
    /**
     * Parse saved queries from a Reader
     * @param reader the saved queries
     * @return a Map from query name to QueryInfo for that query
     * @throws Exception if an error occurs in reading or parsing
     */
    public Map process(Reader reader) throws Exception {
        ModelHandler handler = new ModelHandler();
        SAXParser.parse(new InputSource(reader), handler);
        return handler.savedQueries;
    }

    /**
     * Extension of DefaultHandler to handle metadata file
     */
    class ModelHandler extends DefaultHandler
    {
        Map savedQueries = new LinkedHashMap();
        Map qNodes;
        String queryName;
        RightNode qNode;
        Model model;
        List view;

        /**
         * @see DefaultHandler#startElement
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            if (qName.equals("query")) {
                qNodes = new TreeMap();
                queryName = attrs.getValue("name");
                try {
                    model = Model.getInstanceByName(attrs.getValue("model"));
                } catch (Exception e) {
                    throw new SAXException(e);
                }
                view = StringUtil.tokenize(attrs.getValue("view"));
            }
            if (qName.equals("node")) {
                qNode = MainHelper.addNode(qNodes, attrs.getValue("path"), model);
                if (attrs.getValue("type") != null) {
                    qNode.setType(attrs.getValue("type"));
                }
            }
            if (qName.equals("constraint")) {
                int opIndex = toStrings(ConstraintOp.getValues()).indexOf(attrs.getValue("op"));
                ConstraintOp constraintOp = ConstraintOp.getOpForIndex(new Integer(opIndex));
                Object constraintValue = TypeUtil
                    .stringToObject(MainHelper.getClass(qNode.getType()), attrs.getValue("value"));
                qNode.getConstraints().add(new Constraint(constraintOp, constraintValue));
            }
        }

        /**
         * @see DefaultHandler#endElement
         */
        public void endElement(String uri, String localName, String qName) {
            if (qName.equals("query")) {
                savedQueries.put(queryName, new QueryInfo(qNodes, view, null));
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
