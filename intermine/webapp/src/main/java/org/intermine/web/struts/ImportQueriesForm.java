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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;

/**
 * Form bean representing query import form.
 *
 * @author Thomas Riley
 * @author Daniela Butano
 */
public class ImportQueriesForm extends ImportXMLForm
{
    private Map<String, PathQuery> map;
    private String queryBuilder;

    /**
     * Return a Map from query name to Query object.
     * @return the Map
     * @throws Exception if a problem parsing query XML
     */
    public Map<String, PathQuery> getQueryMap()
        throws Exception {
        if (map == null) {
            // multiple queries must be wrapped by <queries> element, add it if not already there
            xml = xml.trim();
            if (!xml.isEmpty()) {
                if (!xml.startsWith("<queries>")) {
                    xml = "<queries>" + xml + "</queries>";
                }

                // TODO: Assumes we are loading the latest version format.
                map = PathQueryBinding.unmarshalPathQueries(new StringReader(xml),
                    PathQuery.USERPROFILE_VERSION);
            } else if (formFile != null) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(formFile.getInputStream()));
                map = PathQueryBinding.unmarshalPathQueries(reader, PathQuery.USERPROFILE_VERSION);
            }
        }
        return map;
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
     * Reset the form.
     */
    protected void reset() {
        super.reset();
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
        if (formFile != null && formFile.getFileName() != null
                && formFile.getFileName().length() > 0) {
            String mimetype = formFile.getContentType();
            if (!"application/octet-stream".equals(mimetype) && !mimetype.startsWith("text")) {
                errors.add(ActionErrors.GLOBAL_MESSAGE,
                    new ActionMessage("errors.importQuery.notText", mimetype));
                return errors;
            }
            if (formFile.getFileSize() == 0) {
                errors.add(ActionErrors.GLOBAL_MESSAGE,
                    new ActionMessage("errors.importQuery.noQueryFileOrEmpty"));
                return errors;
            }
        }
        try {
            if (getQueryMap().size() == 0) {
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
