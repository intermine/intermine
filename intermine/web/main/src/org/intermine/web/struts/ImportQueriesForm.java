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

import java.io.StringReader;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.validator.ValidatorForm;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Form bean representing query import form.
 *
 * @author  Thomas Riley
 */
public class ImportQueriesForm extends ValidatorForm
{
    private String xml;
    private Map<String, PathQuery> map;
    private String queryBuilder;

    /**
     * Creates a new instance of ImportQueriesForm.
     */
    public ImportQueriesForm() {
        reset();
    }

    /**
     * Return a Map from query name to Query object.
     * @param savedBags map from bag name to bag
     * @return the Map
     * @throws Exception if a problem parsing query XML
     */
    public Map<String, PathQuery> getQueryMap(Map<String, InterMineBag> savedBags)
        throws Exception {
        if (map == null) {
            // multiple queries must be wrapped by <queries> element, add it if not already there
            xml = xml.trim();
            if (!xml.startsWith("<queries>")) {
                xml = "<queries>" + xml + "</queries>";
            }

            // TODO: Assumes we are loading the latest version format.
            map = PathQueryBinding.unmarshalPathQueries(new StringReader(xml),
                    PathQuery.USERPROFILE_VERSION);
        }
        return map;
    }

    /**
     * Get the xml.
     * @return query in xml format
     */
    public String getXml() {
        return xml;
    }

    /**
     * Set the xml.
     * @param xml query in xml format
     */
    public void setXml(String xml) {
        this.xml = xml;
    }

    /**
     * Get the queryBuilder field.  If true and there is only one query submitted, the action will
     * redirect to the query builder rather than the saved query history page.
     * @return queryBuilder the queryBuilder field
     */
    public String getQueryBuilder() {
        return queryBuilder;
    }

    /**
     * Set the queryBuilder field.
     * @param queryBuilder the queryBuilder field
     */
    public void setQueryBuilder(String queryBuilder) {
        this.queryBuilder = queryBuilder;
    }

    /**
     * {@inheritDoc}
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        reset();
    }

    /**
     * Reset the form.
     */
    protected void reset() {
        xml = "";
        queryBuilder = "";
    }

    /**
     * Call inherited method then check whether xml is valid.
     *
     * {@inheritDoc}
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = super.validate(mapping, request);
        if (errors != null && errors.size() > 0) {
            return errors;
        }
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        BagManager bagManager = im.getBagManager();
        Profile profile = SessionMethods.getProfile(session);

        try {
            Map<String, InterMineBag> allBags = bagManager.getBags(profile);
            if (getQueryMap(allBags).size() == 0) {
                if (errors == null) {
                    errors = new ActionErrors();
                }
                errors.add(ActionErrors.GLOBAL_MESSAGE,
                        new ActionMessage("errors.importQuery.noqueries"));
            }
        } catch (Exception err) {
            if (errors == null) {
                errors = new ActionErrors();
            }
            errors.add(ActionErrors.GLOBAL_MESSAGE,
                    new ActionMessage("errors.importFailed", err.getCause().getMessage()));
        }
        return errors;
    }
}
