package org.intermine.web.struts;


import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.ProfileManager;
import org.intermine.web.logic.profile.LoginHandler;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.template.ApiTemplate;
import org.intermine.objectstore.ObjectStoreException;

public class PrecomputeTemplatesAction extends Action
{
    private static final Logger LOG = Logger.getLogger(PrecomputeTemplatesAction.class);

    @Override public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        String msg ="";
        precompute:
        try {
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            ProfileManager pm = im.getProfileManager();
           // Profile profile = pm.getProfile("superuser",pwd,im.getClassKeys());
            Profile profile = pm.getProfile("superuser");
	    if(profile == null){
		LOG.info("Could not create superuse profile");
	  	LOG.info("No templates precomputed");
                msg = "Precompute templates could not create superuse profile";
		break precompute;
	    }
            Map<String, ApiTemplate> templates = profile.getSavedTemplates();
            //TemplateQuery t = templates.get(templateName);
            WebResultsExecutor executor = im.getWebResultsExecutor(profile);
            for(String name : templates.keySet()){
            try {
                LOG.info("precomputing "+name);
                executor.precomputeTemplate(templates.get(name));
            } catch (ObjectStoreException e) {
                LOG.error("Error in precomputing "+name, e);
                msg = msg+("<br> Error in precomputing "+name);
            } finally {
                LOG.info("precomputing" + name + " done");
                msg = msg+("<br> precomputed "+name);
            }
            }
	    LOG.info("Finished pecomputing templates");
            msg = msg+("<br> Finished precomputing templates");
        } catch (RuntimeException e) {
            LOG.error("Error in precompute templates.", e);
            msg = msg +("<br> Error in precompute templates check log.");
        }
        
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(msg);
        response.flushBuffer();


        return null;
    }
}
