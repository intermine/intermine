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


import org.intermine.web.logic.bag.InterMineBag;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/**
 *
 * @author Julie Sullivan
 */
public class WidgetForm extends ActionForm
{

    private String link;
    private String bagName;
    private String[] selected;
    
    
    /**
     * @return the link
     */
    public String getLink() {
        return link;
    }

    /**
     * @param link the link to set
     */
    public void setLink(String link) {
        this.link = link;
    }

    /**
     * Constructor
     */
    public WidgetForm() {
        initialise();
    }

    /**
     * Initialiser
     */
    public void initialise() {

        link = "";
        bagName = "";    
        selected = new String[0];
    }


    /**
     * @return the bagName
     */
    public String getBagName() {
        return bagName;
    }


    /**
     * name of bag that this widget is using.  we need both the bag and the bagname because 
     * sometimes we don't have the bag object.
     * @param bagName the bagName to set
     */
    public void setBagName(String bagName) {
        this.bagName = bagName;
    }


    /**
     * 
     * @return the selected go terms
     */
    public String[] getSelected() {
        return selected;
    }

    /**
     * @param selected the selected go terms
     */
    public void setSelected(String[] selected) {
        this.selected = selected;
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void reset(@SuppressWarnings("unused") ActionMapping mapping, 
                      @SuppressWarnings("unused") HttpServletRequest request) {
        initialise();
    }
}
