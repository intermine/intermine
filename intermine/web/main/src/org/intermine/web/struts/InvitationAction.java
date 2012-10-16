package org.intermine.web.struts;

import java.util.Properties;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.SharedBagManager;
import org.intermine.api.bag.SharingInvite;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.util.Emailer;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.session.SessionMethods;

public class InvitationAction extends InterMineAction {

    private static final Logger LOG = Logger.getLogger(InvitationAction.class);

    @SuppressWarnings("deprecation")
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        InvitationForm inviteForm = (InvitationForm) form;
        InterMineAPI api = SessionMethods.getInterMineAPI(request);
        SharedBagManager sbm = SharedBagManager.getInstance(api.getProfileManager());
        ProfileManager pm = api.getProfileManager();
        Profile p = SessionMethods.getProfile(request.getSession());
        Properties props = SessionMethods.getWebProperties(request);
        if (!p.isLoggedIn()) {
            ForwardParameters fp = new ForwardParameters(mapping.findForward("login"));
            fp.addParameter("returnto", "/accept.do?invite=" + inviteForm.getInvite());
            return fp.forward();
        }
        SharingInvite invite = null;

        try {
            invite = SharingInvite.getByToken(api, inviteForm.getInvite());
        } catch (Exception e) {
            recordError(new ActionMessage("Could not retrieve invitation", false), request, e, LOG);
            return mapping.findForward("mymine");
        }
        try {
            sbm.acceptInvitation(invite, p);
            Emailer emailer = InterMineContext.getEmailer();
            emailer.email(pm.getProfileUserName(invite.getBag().getProfileId()), "was-accepted",
                invite.getCreatedAt(), invite.getInvitee(), invite.getBag(), p.getUsername(), props.getProperty("project.title"));
        } catch (MessagingException e) {
            LOG.warn("Could not send the owner an email - are they an OpenID user?", e);
        } catch (IllegalStateException e) {
            recordError(new ActionMessage("Could not accept invitation: " + e.getMessage(), false), request, e, LOG);
            return mapping.findForward("mymine");
        } catch (Exception e) {
            recordError(new ActionMessage("Could not accept invitation", false), request, e, LOG);
            return mapping.findForward("mymine");
        }

        ForwardParameters forwardParameters =
                new ForwardParameters(mapping.findForward("bagDetails"));
        forwardParameters.addParameter("name", invite.getBag().getName());

        return forwardParameters.forward();
    }

}
