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

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * A template query, which consists of a PathQuery and its (templated) description string
 * @author Mark Woodbridge
 */
public class TemplateQuery
{
    protected static final String PATTERN = "\\[(.*?)\\]";

    protected String identifier, description, indexedDescription, category;
    protected PathQuery query;
    protected List nodes = new ArrayList();

    /**
     * Constructor
     * @param identifier unique name for query
     * @param category name of category that this query falls under
     * @param description the description, containing references to paths in the query
     * @param query the query itself
     */
    public TemplateQuery(String identifier, String description, String category, PathQuery query) {
        this.description = description;
        this.query = query;
        this.category = category;
        this.identifier = identifier;
        
        if (description != null) {
            int i = 1;
            StringBuffer sb = new StringBuffer();
            Matcher m = Pattern.compile(PATTERN).matcher(description);
            while (m.find()) {
                nodes.add(query.getNodes().get(m.group(1)));
                m.appendReplacement(sb, "[" + (i++) + "]");
            }
            m.appendTail(sb);
            indexedDescription = sb.toString();
        }
    }

    /**
     * Get the description (eg. "For a given company [Company.name]")
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the query (eg. select c from Company as c where c.name = 'CompanyA')
     * @return the query
     */
    public PathQuery getQuery() {
        return query;
    }

    /**
     * Get the nodes from the description, in order (eg. {Company.name})
     * @return the nodes
     */
    public List getNodes() {
        return nodes;
    }
    
    /**
     * Get the query identifier/short name.
     * @return the query identifier string
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Get the indexed description (eg. "For a given company [1]")
     * @return the indexed description
     */
    public String getIndexedDescription() {
        return indexedDescription;
    }

    /**
     * Get the "clean" description (eg. "For a given company")
     * @return the clean description
     */
    public String getCleanDescription() {
        return description.replaceAll(" " + PATTERN, "");
    }
    
    /**
     * Get the category that this template belongs to.
     * @return category for template
     */
    public String getCategory() {
        return category;
    }
}