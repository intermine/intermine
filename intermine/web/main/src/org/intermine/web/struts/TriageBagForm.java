package org.intermine.web.struts;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

public class TriageBagForm extends ModifyBagForm
{
    protected String newBagType;

    /**
     * Set the new bag name.
     * @param name the new bag name
     */
    public void setNewBagType(String name) {
        newBagType = name;
    }

    /**
     * Get the new bag name.
     * @return the new bag name
     */
    public String getNewBagType() {
        return newBagType;
    }

    public void initialise() {
        super.initialise();
        newBagType = "";
    }

}
