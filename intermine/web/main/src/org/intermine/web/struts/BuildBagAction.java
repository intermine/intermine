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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.profile.Profile;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.session.SessionMethods;


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
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ServletContext servletContext = request.getSession().getServletContext();
        Properties webProperties
            = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        BuildBagForm buildBagForm = (BuildBagForm) form;

        String type = buildBagForm.getType();

        if (StringUtils.isEmpty(type)) {
            recordError(new ActionMessage("bagBuild.typeNotSet"), request);
            return mapping.findForward("bags");
        }

        BagQueryRunner bagRunner = im.getBagQueryRunner();

        int maxBagSize = WebUtil.getIntSessionProperty(session, "max.bag.size", 100000);
        Profile profile = SessionMethods.getProfile(session);
        if (profile == null || profile.getUsername() == null) {
            int defaultMaxNotLoggedSize = 3;
            maxBagSize = WebUtil.getIntSessionProperty(session, "max.bag.size.notloggedin",
                    defaultMaxNotLoggedSize);
        }
        BufferedReader reader = null;
        FormFile formFile = buildBagForm.getFormFile();

        /*
         * FormFile used from Struts works a bit strangely.
         * 1. Although the file does't exist formFile.getInputStream() doesn't
         * throw FileNotFoundException.
         * 2. When user specified empty file path or very invalid file path,
         * like file path not starting at '/' then formFile.getFileName() returns empty string.
         */
        if (formFile != null && formFile.getFileName() != null
                && formFile.getFileName().length() > 0) {
            // attach file name as the name of the bag
            String fileName = formFile.getFileName();
            // strip suffix
            Integer lastPos = new Integer(fileName.lastIndexOf('.'));
            if (lastPos.intValue() > 0) {
                fileName = fileName.substring(0, lastPos.intValue());
            }
            // replace underscores
            fileName = fileName.replaceAll("_", " ");
            // attach
            request.setAttribute("bagName", fileName);

            String mimetype = formFile.getContentType();
            if (!"application/octet-stream".equals(mimetype) && !mimetype.startsWith("text")) {
                recordError(new ActionMessage("bagBuild.notText", mimetype), request);
                return mapping.findForward("bags");
            }
            if (formFile.getFileSize() == 0) {
                recordError(new ActionMessage("bagBuild.noBagFileOrEmpty"), request);
                return mapping.findForward("bags");
            }
            reader = new BufferedReader(new InputStreamReader(formFile.getInputStream()));
        } else if (buildBagForm.getText() != null && buildBagForm.getText().length() != 0) {
            String trimmedText = buildBagForm.getText().trim();
            if (trimmedText.length() == 0) {
                recordError(new ActionMessage("bagBuild.noBagPaste"), request);
                return mapping.findForward("bags");
            }
            reader = new BufferedReader(new StringReader(trimmedText));
        } else {
            recordError(new ActionMessage("bagBuild.noBagFile"), request);
            return mapping.findForward("bags");
        }

        reader.mark(READ_AHEAD_CHARS);

        char[] buf = new char[READ_AHEAD_CHARS];

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
            // append whitespace to valid delimiters
            String bagUploadDelims = (String) webProperties.get("list.upload.delimiters") + " ";
            StrMatcher matcher = StrMatcher.charSetMatcher(bagUploadDelims);
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
        BagQueryResult bagQueryResult = bagRunner.search(type, list,
                buildBagForm.getExtraFieldValue(), false, buildBagForm.getCaseSensitive());
        session.setAttribute("bagQueryResult", bagQueryResult);
        request.setAttribute("bagType", type);
        request.setAttribute("bagExtraFilter", buildBagForm.getExtraFieldValue());
        //buildNewBag used by jsp to set editable the bag name field
        request.setAttribute("buildNewBag", "true");
        return mapping.findForward("bagUploadConfirm");
    }
}
