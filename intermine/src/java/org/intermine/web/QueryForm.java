package org.flymine.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;

public class QueryForm extends ActionForm {

    protected String querystring;

    public void setQuerystring(String querystring) {
        this.querystring = querystring;
    }

    public String getQuerystring() {
        return querystring;
    }

}
