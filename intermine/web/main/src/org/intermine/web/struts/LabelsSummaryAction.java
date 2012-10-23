package org.intermine.web.struts;

import java.io.IOException;
import java.io.PrintStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.session.SessionMethods;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

public class LabelsSummaryAction extends InterMineAction
{
    private static final Logger LOG = Logger.getLogger(LabelsSummaryAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
               HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        InterMineAPI api = SessionMethods.getInterMineAPI(request);
        Model model = api.getModel();
        WebConfig config = SessionMethods.getWebConfig(request);
        PrintStream out;
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition",  "inline; filename=labels.csv");
        try {
            out = new PrintStream(response.getOutputStream());
            for (ClassDescriptor cd: model.getClassDescriptors()) {
                out.print(cd.getUnqualifiedName());
                out.print(",");
                out.print(WebUtil.formatPath(cd.getUnqualifiedName(), model, config));
                out.println();
                for (FieldDescriptor fd: cd.getFieldDescriptors()) {
                    String path = cd.getUnqualifiedName() + "." + fd.getName();
                    out.print(path);
                    out.print(",");
                    out.print(WebUtil.formatPath(path, model, config));
                    out.println();
                }
            }
            out.flush();
        } catch (IOException e) {
            LOG.error(e);
            return mapping.findForward("begin");
        }
        return null;
    }
}
