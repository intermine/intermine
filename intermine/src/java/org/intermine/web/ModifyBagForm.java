package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;

/**
 * Form bean to used in combining bags
 * @author Mark Woodbridge
 */
public class ModifyBagForm extends ActionForm
{
    protected String[] selectedBags;

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
     * @see ActionForm#validate
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);

        ActionErrors errors = new ActionErrors();

        if (selectedBags.length == 0) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.modifyBag.none"));
        } else if (request.getParameter("delete") != null) {
            for (int i = 0; i < getSelectedBags().length; i++) {
                List queries = queriesThatMentionBag(profile.getSavedQueries(),
                                                     getSelectedBags()[i]);
                if (queries.size() > 0) {
                    ActionMessage actionMessage =
                        new ActionMessage("history.baginuse", getSelectedBags()[i], queries);
                    errors.add(ActionMessages.GLOBAL_MESSAGE, actionMessage);
                }
            }
        }

        return errors;
    }

    /**
     * Provide a list of queries that mention a named bag
     * @param savedQueries a saved queries map (name -> query)
     * @param bagName the name of a bag
     * @return the list of queries
     */
    public List queriesThatMentionBag(Map savedQueries, String bagName) {
        List queries = new ArrayList();
        for (Iterator i = savedQueries.keySet().iterator(); i.hasNext();) {
            String queryName = (String) i.next();
            PathQuery query = (PathQuery) savedQueries.get(queryName);
            if (query.getBagNames().contains(bagName)) {
                queries.add(queryName);
            }
        }
        return queries;
    }

    /**
     * Reset the form to the initial state
     *
     * @param mapping the mapping
     * @param request the request
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        initialise();
    }
}
