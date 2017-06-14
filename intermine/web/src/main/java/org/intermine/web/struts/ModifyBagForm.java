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
    protected String listLeft;
    protected String listRight;

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
        listLeft = "";
        listRight = "";
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

    /** @return the list on the left **/
    public String getListLeft() {
        return listLeft;
    }

    /** @param listLeft The list on the left. **/
    public void setListLeft(String listLeft) {
        this.listLeft = listLeft;
    }

    /** @return the list on the right. **/
    public String getListRight() {
        return listRight;
    }

    /** @param listRight the list on the right. **/
    public void setListRight(String listRight) {
        this.listRight = listRight;
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
    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        initialise();
    }
}
