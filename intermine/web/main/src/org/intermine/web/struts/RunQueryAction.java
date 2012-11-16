package org.intermine.web.struts;

import java.io.StringReader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.web.logic.session.SessionMethods;

public class RunQueryAction extends InterMineAction {

    private static final Logger LOG = Logger.getLogger(RunQueryAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        QueryForm qf = (QueryForm) form;
        try {
            PathQuery pq = PathQueryBinding.unmarshalPathQuery(new StringReader(qf.getQuery()), PathQuery.USERPROFILE_VERSION);
            HttpSession session = request.getSession();
            SessionMethods.setQuery(session, pq);
        } catch (Exception e) {
            recordError(new ActionMessage("struts.runquery.failed", e.getMessage()), request, e, LOG);
            return mapping.findForward("failure");
        }
        return mapping.findForward("success");
    }
}
