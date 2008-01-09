package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.SavedQuery;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

/**
 * Form bean to used in combining bags
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class ModifyBagForm extends ActionForm
{
    protected String[] selectedBags;
    protected String newBagName;
    protected String pageName;
    protected String listsButton;

    /**
     * Constructor
     */
    public ModifyBagForm() {
        initialise();
    }

    /**
     * Initialiser
     */
   public void initialise() {
        selectedBags = new String[0];
        newBagName = "";
    }

    /**
     * Sets the selected bags
     *
     * @param selectedBags the selected bags
     */
    public void setSelectedBags(String[] selectedBags) {
        this.selectedBags = selectedBags;
    }

    /**
     * Gets the selected bags
     *
     * @return the selected bags
     */
    public String[] getSelectedBags() {
        return selectedBags;
    }

    /**
     * Set the new bag name.
     * @param name the new bag name
     */
    public void setNewBagName(String name) {
        newBagName = name;
    }

    /**
     * Get the new bag name.
     * @return the new bag name
     */
    public String getNewBagName() {
        return newBagName;
    }

    /**
     * @return the pageName
     */
    public String getPageName() {
        return pageName;
    }

    /**
     * @param pageName the pageName to set
     */
    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    /**
     *
     * @return listsButton
     */
    public String getListsButton() {
        return listsButton;
    }

    /**
     *
     * @param listsButton lists button
     */
    public void setListsButton(String listsButton) {
        this.listsButton = listsButton;
    }

    /**
     * Reset the form to the initial state
     *
     * @param mapping the mapping
     * @param request the request
     */
    public void reset(@SuppressWarnings("unused") ActionMapping mapping,
                      @SuppressWarnings("unused") HttpServletRequest request) {
        initialise();
    }
}
