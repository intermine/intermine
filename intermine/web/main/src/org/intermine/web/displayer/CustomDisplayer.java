package org.intermine.web.displayer;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.DisplayObject;

public abstract class CustomDisplayer {

    private ReportDisplayerConfig config;

    public CustomDisplayer(ReportDisplayerConfig config) {
        this.config = config;
    }
    
    public void execute(HttpServletRequest request, DisplayObject displayObject) {
        request.setAttribute("displayObject", displayObject);
        request.setAttribute("jspPage", getJspPage());
        display(request, displayObject);
    }

    public abstract void display(HttpServletRequest request, DisplayObject displayObject);

    public String getJspPage() {
        return config.getJspName();
    }

    public Set<String> getReplacedFieldExprs() {
        return config.getReplacedFieldNames();
    }
}
