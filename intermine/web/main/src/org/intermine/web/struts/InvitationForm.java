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

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/**
 * A form that contains the information for requests that invite users to share resources.
 * @author Alex Kalderimis
 *
 */
public class InvitationForm extends ActionForm
{

    private static final long serialVersionUID = 8720157226362866628L;

    private String inviteToken;

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        inviteToken = null;
    }

    /** @return the invitation token **/
    public String getInvite() {
        return inviteToken;
    }

    /** @param invite the invitation token **/
    public void setInvite(String invite) {
        this.inviteToken = invite;
    }
}
