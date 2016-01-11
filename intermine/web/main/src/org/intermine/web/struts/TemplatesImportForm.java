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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.template.TemplateHelper;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;
import org.intermine.template.xml.TemplateQueryBinding;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Form bean representing template import form.
 *
 * @author Thomas Riley
 * @author Daniela Butano
 */
public class TemplatesImportForm extends ImportXMLForm
{
    private boolean overwriting = false;
    private boolean deleteTracks = false;
    private Map<String, TemplateQuery> map;

    /**
     * Get the overwrite flag.
     * @return  true to overwrite existing template, false to add
     */
    public boolean isOverwriting() {
        return overwriting;
    }

    /**
     * Set the overwriting flag.
     * @param overwriting true to overwrite existing templates, false to add
     */
    public void setOverwriting(boolean overwriting) {
        this.overwriting = overwriting;
    }

    /**
     * Get the deleteTracks flag.
     * @return  true true to delete tracks associated to the template, false to keep
     */
    public boolean isDeleteTracks() {
        return deleteTracks;
    }

    /**
     * Set the deleteTracks flag.
     * @param deleteTracks true to delete tracks associated to the template, false to keep
     */
    public void setDeleteTracks(boolean deleteTracks) {
        this.deleteTracks = deleteTracks;
    }

    /**
     * Reset the form.
     */
    protected void reset() {
        super.reset();
        overwriting = false;
        deleteTracks = false;
    }

    /**
     * Call inherited method then check whether xml is valid.
     *
     * {@inheritDoc}
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        Profile profile = SessionMethods.getProfile(session);

        BagManager bagManager = im.getBagManager();
        ActionErrors errors = super.validate(mapping, request);
        if (errors != null && errors.size() > 0) {
            return errors;
        }
        if (formFile != null && formFile.getFileName() != null
                && formFile.getFileName().length() > 0) {
            String mimetype = formFile.getContentType();
            if (!"application/octet-stream".equals(mimetype) && !mimetype.startsWith("text")) {
                errors.add(ActionErrors.GLOBAL_MESSAGE,
                    new ActionMessage("importTemplates.notText", mimetype));
                return errors;
            }
            if (formFile.getFileSize() == 0) {
                errors.add(ActionErrors.GLOBAL_MESSAGE,
                    new ActionMessage("importTemplates.noTemplateFileOrEmpty"));
                return errors;
            }
        }
        try {
            xml = xml.trim();
            if (!xml.isEmpty()) {
                Map<String, InterMineBag> allBags = bagManager.getBags(profile);
                TemplateHelper.xmlToTemplateMap(getXml(), allBags, PathQuery.USERPROFILE_VERSION);
            } else if (formFile != null) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(formFile.getInputStream()));
                map = TemplateQueryBinding.unmarshalTemplates(reader,
                    PathQuery.USERPROFILE_VERSION);
            }
        } catch (Exception err) {
            if (errors == null) {
                errors = new ActionErrors();
            }
            errors.add(ActionErrors.GLOBAL_MESSAGE,
                        new ActionMessage("errors.badtemplatexml", err.getMessage()));
        }
        return errors;
    }

    /**
     * Return a Map from template name to Template object.
     * @param bagManager An object capable of getting bags for a profile.
     * @param profile The current user's profile.
     * @return a Map from template name to Template object.
     * @throws Exception if a problem parsing query XML
     */
    public Map<String, TemplateQuery> getQueryMap(BagManager bagManager, Profile profile)
        throws Exception {
        Map<String, InterMineBag> allBags = bagManager.getBags(profile);
        if (map == null) {
            // multiple templates must be wrapped by <templates> element,
            // add it if not already there
            xml = xml.trim();
            if (!xml.isEmpty()) {
                if (!xml.startsWith("<templates>")) {
                    xml = "<templates>" + xml + "</templates>";
                }
                map = TemplateHelper.xmlToTemplateMap(getXml(), allBags,
                    PathQuery.USERPROFILE_VERSION);
            } else if (formFile != null) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(formFile.getInputStream()));
                map = TemplateQueryBinding.unmarshalTemplates(reader,
                    PathQuery.USERPROFILE_VERSION);
            }
        }
        return map;
    }
}
