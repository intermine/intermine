package org.intermine.web.displayer;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;

public abstract class CustomDisplayer {

    protected ReportDisplayerConfig config;
    protected InterMineAPI im;

    public CustomDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        this.config = config;
        this.im = im;
    }

    public void execute(HttpServletRequest request, ReportObject reportObject) {
        request.setAttribute("reportObject", reportObject);
        request.setAttribute("jspPage", getJspPage());
        display(request, reportObject);
    }

    public abstract void display(HttpServletRequest request, ReportObject reportObject);

    public String getJspPage() {
        return config.getJspName();
    }

    public Set<String> getReplacedFieldExprs() {
        return config.getReplacedFieldNames();
    }

}
