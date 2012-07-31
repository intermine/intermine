package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
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
 * Form for FindInListAction.
 * @author Xavier Watkins
 */
public class FindInListForm extends ActionForm
{

    private String textToFind;
    private String bagName;

    /**
     * Construct a new Form.
     */
    public FindInListForm() {
        reset();
    }

    /**
     * @return the textToFind
     */
    public String getTextToFind() {
        return textToFind;
    }

    /**
     * @param textToFind the textToFind to set
     */
    public void setTextToFind(String textToFind) {
        this.textToFind = textToFind;
    }

    /**
     * @return the bagName
     */
    public String getBagName() {
        return bagName;
    }

    /**
     * @param bagName the bagName to set
     */
    public void setBagName(String bagName) {
        this.bagName = bagName;
    }

    /**
     * Reset form bean.
     *
     * @param mapping  the action mapping associated with this form bean
     * @param request  the current http servlet request
     */
    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        reset();
    }

    private void reset() {
        textToFind = "";
        bagName = "";
    }
}
