package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/**
 * Action form that holds query XML.
 * @author Alex Kalderimis
 *
 */
public class QueryForm extends ActionForm
{

    private static final long serialVersionUID = 7673431976068854089L;

    private String xml = null;

    /** @return the query xml **/
    public String getQuery() {
        return xml;
    }

    /** @param xml the query xml **/
    public void setQuery(String xml) {
        this.xml = xml;
    }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        xml = null;
    }
}
