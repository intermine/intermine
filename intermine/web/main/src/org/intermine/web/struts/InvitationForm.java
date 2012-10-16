package org.intermine.web.struts;

import org.apache.struts.action.ActionForm;

public class InvitationForm extends ActionForm {

    private static final long serialVersionUID = 8720157226362866628L;

    private String inviteToken;

    public String getInvite() {
        return inviteToken;
    }

    public void setInvite(String invite) {
        this.inviteToken = invite;
    }
}
