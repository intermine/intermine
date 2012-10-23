package org.intermine.web.struts;

import org.apache.struts.action.ActionForm;

public class QueryForm extends ActionForm {

    private static final long serialVersionUID = 7673431976068854089L;

    private String xml = null;

    public String getQuery() {
        return xml;
    }

    public void setQuery(String xml) {
        this.xml = xml;
    }
}
