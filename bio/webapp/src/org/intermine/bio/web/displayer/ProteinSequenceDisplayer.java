package org.intermine.bio.web.displayer;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.model.InterMineObject;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;

/**
 * Displayer for protein sequence
 * @author Fengyuan Hu
 */
public class ProteinSequenceDisplayer extends ReportDisplayer
{
    /**
     * Construct with config and the InterMineAPI.
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public ProteinSequenceDisplayer(ReportDisplayerConfig config,
            InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        InterMineObject imObj = reportObject.getObject();

        String imoClassName = imObj.getClass().getSimpleName();
        if (imoClassName.endsWith("Shadow")) {
            imoClassName = imoClassName.substring(0, imoClassName.indexOf("Shadow"));
        }
        request.setAttribute("objectClass", imoClassName);
    }
}
