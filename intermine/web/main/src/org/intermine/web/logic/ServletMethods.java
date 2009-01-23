package org.intermine.web.logic;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.io.StringReader;
import java.util.Map;

import javax.servlet.ServletContext;

import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.MainHelper;

/**
 * Helper methods for ServletContext.
 * @author Kim Rutherford
 */
public class ServletMethods
{
    /**
     * Rematerialise single query from XML.
     * @param xml PathQuery XML
     * @return a PathQuery object
     * @param savedBags Map from bag name to bag
     * @param servletContext global ServletContext object
     */
    public static PathQuery fromXml(String xml, Map<String, InterMineBag> savedBags, 
            ServletContext servletContext) {
        Map<String, PathQuery> queries = PathQueryBinding.unmarshal(new StringReader(xml));
        MainHelper.checkPathQueries(queries, savedBags);
        return (PathQuery) queries.values().iterator().next();
    }
}
