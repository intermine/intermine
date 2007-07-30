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
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.SavedQuery;

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
     * {@inheritDoc}
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);

        ActionErrors errors = new ActionErrors();
        
        if (request.getParameter("newName") == null && selectedBags.length == 0) {
            errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.modifyBag.none"));
        } else if (request.getParameter("delete") != null) {
            for (int i = 0; i < getSelectedBags().length; i++) {
                Set queries = new HashSet();
                queries.addAll(queriesThatMentionBag(profile.getSavedQueries(),
                        getSelectedBags()[i]));
                queries.addAll(queriesThatMentionBag(profile.getHistory(),
                        getSelectedBags()[i]));
                if (queries.size() > 0) {
                    ActionMessage actionMessage =
                        new ActionMessage("history.baginuse", getSelectedBags()[i], queries);
                    errors.add(ActionMessages.GLOBAL_MESSAGE, actionMessage);
                }
            }
        }
        
        if (request.getParameter("newName") == null
            && (request.getParameter("union") != null
                || request.getParameter("intersect") != null
                || request.getParameter("subtract") != null)) {
            if (StringUtils.isEmpty(getNewBagName())) {
                ActionMessage actionMessage =
                    new ActionMessage("errors.required", "New bag name");
                errors.add(ActionMessages.GLOBAL_MESSAGE, actionMessage);
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
            SavedQuery query = (SavedQuery) savedQueries.get(queryName);
            if (query.getPathQuery().getBagNames().contains(bagName)) {
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
