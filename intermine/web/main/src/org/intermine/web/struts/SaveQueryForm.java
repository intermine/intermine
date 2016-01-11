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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.util.NameUtil;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Form bean to represent the inputs to the query saving action
 *
 * @author Andrew Varley
 * @author Matthew Wakeling
 */
public class SaveQueryForm extends ActionForm
{
    protected String queryName = "";

    /**
     * Set the query name
     *
     * @param queryName the query name
     */
    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }

    /**
     * Get the query name
     *
     * @return the query name
     */
    public String getQueryName() {
        return queryName.trim();
    }

    /**
     * {@inheritDoc}
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Profile profile = SessionMethods.getProfile(session);
        Map<String, SavedQuery> savedQueries = profile.getSavedQueries();

        ActionErrors errors = null;
        if ("".equals(queryName)) {
            errors = new ActionErrors();
            errors.add(ActionMessages.GLOBAL_MESSAGE,
                       new ActionMessage("errors.savequery.blank", queryName));

        } else if (!NameUtil.isValidName(queryName)) {

            errors = new ActionErrors();
            errors.add(ActionMessages.GLOBAL_MESSAGE,
                       new ActionMessage("errors.badChars"));

        } else if (savedQueries != null && savedQueries.containsKey(queryName)) {
            errors = new ActionErrors();
            errors.add(ActionMessages.GLOBAL_MESSAGE,
                       new ActionMessage("errors.savequery.existing", queryName));
        }
        return errors;
    }

    /**
     * {@inheritDoc}
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        queryName = "";
    }
}
