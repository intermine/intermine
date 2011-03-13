package org.intermine.bio.web.displayer;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.web.displayer.CustomDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.DisplayObject;

public class GeneStructureDisplayer extends CustomDisplayer {

    public GeneStructureDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, DisplayObject displayObject) {
        request.setAttribute("message", "Monkeys rule!");
    }

//    @Override
//    public String getJspPage() {
//        return "model/geneStructureDisplayer.jsp";
//    }

//    @Override
//    public Set<String> getReplacedFieldExprs() {
//        Set<String> replacedFieldExprs = new HashSet<String>();
//        replacedFieldExprs.add("transcripts");
//        return replacedFieldExprs;
//    }

}
