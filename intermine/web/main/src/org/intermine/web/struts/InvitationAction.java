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
import org.intermine.api.bag.SharingInvite.NotFoundException;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.UserAlreadyShareBagException;
import org.intermine.util.Emailer;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.session.SessionMethods;

public class InvitationAction extends InterMineAction {

    private static final Logger LOG = Logger.getLogger(InvitationAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        InvitationForm inviteForm = (InvitationForm) form;
        InterMineAPI api = SessionMethods.getInterMineAPI(request);
        SharedBagManager sbm = SharedBagManager.getInstance(api.getProfileManager());
        ProfileManager pm = api.getProfileManager();
        Profile p = SessionMethods.getProfile(request.getSession());
        Properties props = SessionMethods.getWebProperties(request);
        Emailer emailer = InterMineContext.getEmailer();

        if (!p.isLoggedIn()) {
            ActionMessage message = new ActionMessage("invitation-errors.mustlogin");
            recordMessage(message, request);
            ForwardParameters fp = new ForwardParameters(mapping.findForward("login"));
            fp.addParameter("returnto", "/accept.do?invite=" + inviteForm.getInvite());
            return fp.forward();
        }

        SharingInvite invite = null;

        try {
            invite = SharingInvite.getByToken(api, inviteForm.getInvite());
            sbm.acceptInvitation(invite, p);
            emailer.email(
                pm.getProfileUserName(invite.getBag().getProfileId()), "was-accepted",
                invite.getCreatedAt(), invite.getInvitee(), invite.getBag(), p.getUsername(),
                props.getProperty("project.title"));
            return forwardToBagDetails(mapping, invite);
        } catch (MessagingException e) {
            LOG.warn("Failed to email the owner of the list", e); // The logged in user doesn't need to know.
            return forwardToBagDetails(mapping, invite);
        } catch (UserAlreadyShareBagException e) {
            sendErrorMsg(request, e, "invitation-errors.alreadyshared", new Object[]{});
            return forwardToBagDetails(mapping, invite);
        } catch (IllegalStateException e) {
            sendErrorMsg(request, e, "invitation-errors.couldntaccept", e.getMessage());
        } catch (NotFoundException e) {
            sendErrorMsg(request, e, "invitation-errors.couldntretrieve", new Object[]{});
        }
        return mapping.findForward("mymine");
    }

    private ActionForward forwardToBagDetails(ActionMapping mapping,
            SharingInvite invite) {
        ForwardParameters forwardParameters = new ForwardParameters(mapping.findForward("bagDetails"));
        forwardParameters.addParameter("name", invite.getBag().getName());
        return forwardParameters.forward();
    }

    private void sendErrorMsg(HttpServletRequest request, Exception e, String key, Object... vals) {
        recordError(new ActionMessage(key, vals), request, e, LOG);
    }

}
