package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.SharingInvite;
import org.intermine.api.bag.SharingInvite.NotFoundException;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.webservice.server.core.F;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.core.ListFunctions;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;
import org.intermine.webservice.server.output.JSONFormatter;

/** @author Alex Kalderimis **/
public class ListSharingInvitationDetailsService extends JSONService
{

    /** @param im The InterMine state object **/
    public ListSharingInvitationDetailsService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attrs = super.getHeaderAttributes();
        attrs.put(JSONFormatter.KEY_INTRO, "\"invitation\":");
        return attrs;
    }

    @Override
    protected void execute() throws Exception {
        Profile p = getPermission().getProfile();
        if (!p.isLoggedIn()) {
            throw new ServiceForbiddenException("You must be logged in to use this service");
        }
        String token, pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.matches("/[^/]{20}")) {
            token = pathInfo.substring(1, 21);
        } else {
            token = request.getParameter("uid");
        }

        if (StringUtils.isBlank(token)) {
            sendAllPending();
        } else {
            sendIndividual(token);
        }
    }

    private void sendAllPending() throws SQLException, ObjectStoreException {
        Profile inviter = getPermission().getProfile();
        Collection<SharingInvite> invites = SharingInvite.getInvites(im, inviter);

        addResultItem(ListFunctions.map(invites, new F<SharingInvite, Object>() {
            @Override
            public Object call(SharingInvite a) {
                return inviteToMap(a);
            }
        }), false);
    }

    private void  sendIndividual(String token) throws SQLException, ObjectStoreException {
        SharingInvite invite;

        try {
            invite = SharingInvite.getByToken(im, token);
        } catch (NotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage(), e);
        }

        Map<String, Object> toSerialise = inviteToMap(invite);

        addResultItem(toSerialise, false);

    }

    private Map<String, Object> inviteToMap(SharingInvite invite) {
        InterMineBag bag = invite.getBag();
        Profile inviter = im.getProfileManager().getProfile(bag.getProfileId());

        Map<String, Object> toSerialise = new HashMap<String, Object>();
        toSerialise.put("uid", invite.getToken());
        toSerialise.put("inviter", inviter.getName());
        toSerialise.put("list", bag.getName());
        toSerialise.put("invitee", invite.getInvitee());
        return toSerialise;
    }

}
