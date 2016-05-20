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

import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.web.context.InterMineContext;

/**
 * This is a generic action to download file from any given directory.
 * Path needs to be setup in controller or jsp.
 *
 * @author Fengyuan Hu
 * @author Alex Kalderimis (specifically the black/white listing)
 */
public class FileDownloadAction extends InterMineAction
{
    private static final Logger LOG = Logger.getLogger(FileDownloadAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response)
        throws Exception {

        try {
            String path = request.getParameter("path");
            String fileName = request.getParameter("fileName");
            String mimeType = request.getParameter("mimeType");
            String mimeExtension = request.getParameter("mimeExtension");

            if (!fileIsPermitted(fileName)) {
                response.sendError(401);
                return null;
            }

            // Read the file into a input stream
            InputStream is = getServlet().getServletContext().getResourceAsStream(path + fileName);

            if (is == null) {
                response.sendError(404);
                return null;
            }

            // MIME type
            if (fileName.endsWith(mimeExtension)) {
                response.setContentType(mimeType);
                response.setHeader("Content-disposition",
                        "attachment; filename=" + fileName);
            }

            ServletOutputStream sos = response.getOutputStream();

            byte[] buff = new byte[2048];
            int bytesRead;

            while (-1 != (bytesRead = is.read(buff, 0, buff.length))) {
                sos.write(buff, 0, bytesRead);
            }

            is.close();
            sos.close();
        } catch (Exception e) {
            e.printStackTrace();
            recordError(new ActionMessage("api.fileDownloadFailed"), request);
            return mapping.findForward("api");
        }

        return null;
    }

    private boolean fileIsPermitted(String fileName) {
        if (fileName == null) {
            return false;
        }
        Properties webProps = InterMineContext.getWebProperties();
        String[] blackList = webProps.getProperty("web.download.blacklist").split(",");
        for (String notAllowed: blackList) {
            if (fileName.contains(notAllowed)) {
                LOG.info("Request denied due to black-list entry: "
                        + fileName + " contains " + notAllowed);
                return false;
            }
        }
        String[] whiteList = webProps.getProperty("web.download.whitelist").split(",");
        if (whiteList.length > 0) {
            for (String mustMatch: whiteList) {
                if (fileName.contains(mustMatch)) {
                    return true;
                }
            }
            LOG.info("Request denied due to white-list: "
                    + fileName + " does not contain any of " + StringUtils.join(whiteList));
            return false;
        } else {
            return true;
        }
    }

}
