package org.flymine.web;

import org.apache.struts.action.ActionForm;

/**
 * Form bean to represent the inputs to a text-based query
 *
 * @author Andrew Varley
 */
public class QueryForm extends ActionForm
{

    protected String querystring;

    /**
     * Set the query string
     *
     * @param querystring the query string
     */
    public void setQuerystring(String querystring) {
        this.querystring = querystring;
    }

    /**
     * Get the query string
     *
     * @return the query string
     */
    public String getQuerystring() {
        return querystring;
    }

}
