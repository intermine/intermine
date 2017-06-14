package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;

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
import org.intermine.web.context.MailAction;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Action that processes invitations.
 * @author Alex Kalderimis
 *
 */
public class InvitationAction extends InterMineAction
{

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
            notifyInvitee(pm, invite, p, props);

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

    private void notifyInvitee(
            final ProfileManager pm, final SharingInvite invite,
            final Profile accepter, final Properties props) {
        MailAction action = new MailAction() {
            @Override
            public void act(Emailer emailer) throws Exception {
                emailer.email(
                        pm.getProfileUserName(invite.getBag().getProfileId()),
                        "was-accepted",
                        invite.getCreatedAt(),
                        invite.getInvitee(),
                        invite.getBag().getName(),
                        accepter.getUsername(),
                        props.getProperty("project.title"));
            }
        };
        boolean queued = InterMineContext.queueMessage(action);
        if (!queued) {
            LOG.warn("Mail queue is full - could not send message");
        }
    }

    private ActionForward forwardToBagDetails(ActionMapping mapping,
            SharingInvite invite) {
        ForwardParameters forwardParameters =
                new ForwardParameters(mapping.findForward("bagDetails"));
        forwardParameters.addParameter("name", invite.getBag().getName());
        return forwardParameters.forward();
    }

    private void sendErrorMsg(HttpServletRequest request, Exception e, String key, Object... vals) {
        recordError(new ActionMessage(key, vals), request, e, LOG);
    }

}
