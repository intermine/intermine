package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryResult;
import org.intermine.web.logic.bag.BagQueryRunner;
import org.intermine.web.logic.profile.Profile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrMatcher;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.upload.FormFile;


/**
 * An action that makes a bag from text.
 *
 * @author Kim Rutherford
 */

public class BuildBagAction extends InterMineAction
{

    private static final int READ_AHEAD_CHARS = 10000;

    /**
     * Action for creating a bag of InterMineObjects or Strings from identifiers in text field.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param frm The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm frm,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
    throws Exception {
        HttpSession session = request.getSession();
        BuildBagForm form = (BuildBagForm) frm;
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        String type = form.getType();

        if (StringUtils.isEmpty(type)) {
            recordError(new ActionMessage("bagBuild.noBagPaste"), request);
            return mapping.findForward("bags");
        }

        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
        BagQueryConfig bagQueryConfig =
            (BagQueryConfig) servletContext.getAttribute(Constants.BAG_QUERY_CONFIG);
        BagQueryRunner bagRunner =
            new BagQueryRunner(os, classKeys, bagQueryConfig, servletContext);

        int maxBagSize = WebUtil.getIntSessionProperty(session, "max.bag.size", 100000);
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        if (profile == null || profile.getUsername() == null) {
            int defaultMaxNotLoggedSize = 3;
            maxBagSize = WebUtil.getIntSessionProperty(session, "max.bag.size.notloggedin",
                                                       defaultMaxNotLoggedSize);
        }

        BufferedReader reader = null;

        FormFile formFile = form.getFormFile();
        
        if (isFileUpload(form)) {
            String mimetype = formFile.getContentType();
            if (!mimetype.equals("application/octet-stream") && !mimetype.startsWith("text")) {
                recordError(new ActionMessage("bagBuild.notText",
                                              mimetype), request);
                return mapping.findForward("bags");
            }
            if (form.getFormFile() == null || form.getFormFile().getFileName() == null 
                    || form.getFormFile().getFileName().trim().length() == 0) {
                recordError(new ActionMessage("bagBuild.noBagFile"), request);
                return mapping.findForward("bags");                
            }
            reader = new BufferedReader(new InputStreamReader(formFile.getInputStream()));
        } else { 
            if (form.getText() != null && form.getText().trim().length() != 0) {
                String trimmedText = form.getText().trim();
                reader = new BufferedReader(new StringReader(trimmedText));
            } else {
                recordError(new ActionMessage("bagBuild.noBagPaste"), request);
                return mapping.findForward("bags");                
            }
        } 
        reader.mark(READ_AHEAD_CHARS);

        char buf[] = new char[READ_AHEAD_CHARS];

        int read = reader.read(buf, 0, READ_AHEAD_CHARS);

        for (int i = 0; i < read; i++) {
            if (buf[i] == 0) {
                recordError(new ActionMessage("bagBuild.notText", "binary"), request);
                return mapping.findForward("bags");
            }
        }

        reader.reset();

        String thisLine;
        List<String> list = new ArrayList<String>();
        int elementCount = 0;
        while ((thisLine = reader.readLine()) != null) {
            StrMatcher matcher = StrMatcher.charSetMatcher("\n\t, ");
            StrTokenizer st = new StrTokenizer(thisLine, matcher, StrMatcher.doubleQuoteMatcher());
            while (st.hasNext()) {
                String token = st.nextToken();
                list.add(token);
                elementCount++;
                if (elementCount > maxBagSize) {
                    ActionMessage actionMessage = null;
                    if (profile == null || profile.getUsername() == null) {
                        actionMessage = new ActionMessage("bag.bigNotLoggedIn",
                                                          new Integer(maxBagSize));
                    } else {
                        actionMessage = new ActionMessage("bag.tooBig", new Integer(maxBagSize));
                    }
                    recordError(actionMessage, request);

                    return mapping.findForward("bags");
                }
            }
        }

        BagQueryResult bagQueryResult =
            bagRunner.searchForBag(type, list, form.getExtraFieldValue(), false);
        session.setAttribute("bagQueryResult", bagQueryResult);
        request.setAttribute("bagType", type);

        return mapping.findForward("bagUploadConfirm");
    }
    
    private boolean isFileUpload(BuildBagForm form) {
        if (form.getUploadType() != null) {
            return form.getUploadType().trim().equalsIgnoreCase("file");
        } else {
            FormFile formFile = form.getFormFile();
            return (formFile != null && formFile.getFileName() != null
                    && formFile.getFileName().length() > 0);
        }
    }
}
